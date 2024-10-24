package book;


import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.*;

public class LambdaCompilerHelper {

    public static ByteBuffer compileAndZip(String className, String sourceCode) throws IOException {
        // 創建內存中的源文件
        JavaFileObject sourceFile = new JavaSourceFromString(className, sourceCode);

        // 獲取編譯器
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("No Java compiler available. Make sure you're using a JDK, not a JRE.");
        }

        // 設置編譯選項
        List<String> options = new ArrayList<>(Arrays.asList("-d", System.getProperty("java.io.tmpdir")));
        
        // 添加 AWS Lambda 運行時庫到類路徑
        String awsLambdaRuntimePath = getAWSLambdaRuntimePath();
        if (awsLambdaRuntimePath != null) {
            options.addAll(Arrays.asList("-classpath", awsLambdaRuntimePath));
        }

        // 編譯
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, Collections.singletonList(sourceFile));
            boolean success = task.call();

            if (!success) {
                StringBuilder sb = new StringBuilder();
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    sb.append(diagnostic.toString()).append("\n");
                }
                throw new RuntimeException("Compilation failed: " + sb.toString());
            }
        }

        // Create a zip file
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String classFilePath = System.getProperty("java.io.tmpdir") + className.replace(".", File.separator)+ ".class";
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
        	
            File classFile = new File(classFilePath);
            ZipEntry ze = new ZipEntry(className.replace(".", File.separator)+ ".class");
            zos.putNextEntry(ze);
            byte[] classData = java.nio.file.Files.readAllBytes(classFile.toPath());
            zos.write(classData, 0, classData.length);
            zos.closeEntry();
        }

        // 清理臨時文件
        new File(classFilePath).delete();

        return ByteBuffer.wrap(baos.toByteArray());
    }

    private static String getAWSLambdaRuntimePath() {
        try {
            // 使用系統屬性或環境變量來獲取 AWS Lambda 運行時庫的路徑
            String lambdaRuntimePath = System.getProperty("aws.lambda.runtime.path");
            if (lambdaRuntimePath == null) {
                lambdaRuntimePath = System.getenv("AWS_LAMBDA_RUNTIME_PATH");
            }
            if (lambdaRuntimePath != null) {
                return lambdaRuntimePath;
            }
            // 如果無法找到路徑，返回 null
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class JavaSourceFromString extends SimpleJavaFileObject {
        final String code;

        JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
    public static byte[] getTestJarBytes() {
        try {
            // 獲取當前項目的target目錄
            String projectDir = System.getProperty("user.dir");
            Path jarPath = findJarFile(Paths.get(projectDir, "target"));
            
            if (jarPath == null) {
                // 如果找不到jar檔案，嘗試即時編譯
                compileProject();
                jarPath = findJarFile(Paths.get(projectDir, "target"));
                
                if (jarPath == null) {
                    throw new RuntimeException("無法找到或創建Lambda函數的jar檔案");
                }
            }
            
            return Files.readAllBytes(jarPath);
        } catch (Exception e) {
            throw new RuntimeException("讀取jar檔案時發生錯誤", e);
        }
    }
    
    private static Path findJarFile(Path targetDir) throws IOException {
        // 使用Files.walk來搜尋jar檔案
        try (Stream<Path> walk = Files.walk(targetDir)) {
            return walk
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".jar"))
                .filter(p -> !p.toString().contains("original"))  // 排除original-開頭的jar
                .filter(p -> !p.toString().contains("-test"))     // 排除測試jar
                .findFirst()
                .orElse(null);
        }
    }
    
    private static void compileProject() {
        try {
            // 創建Maven打包命令
            ProcessBuilder processBuilder = new ProcessBuilder(
                isWindows() ? "mvn.cmd" : "mvn",
                "package",
                "-DskipTests"  // 跳過測試以避免循環依賴
            );
            
            // 設置工作目錄為當前項目根目錄
            processBuilder.directory(new File(System.getProperty("user.dir")));
            
            // 重定向錯誤流到標準輸出
            processBuilder.redirectErrorStream(true);
            
            // 啟動進程
            Process process = processBuilder.start();
            
            // 讀取並輸出編譯日誌
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            
            // 等待編譯完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Maven編譯失敗，退出碼: " + exitCode);
            }
            
        } catch (Exception e) {
            throw new RuntimeException("項目編譯失敗", e);
        }
    }
    
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
}