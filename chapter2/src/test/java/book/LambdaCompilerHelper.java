package book;


import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.*;
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
}