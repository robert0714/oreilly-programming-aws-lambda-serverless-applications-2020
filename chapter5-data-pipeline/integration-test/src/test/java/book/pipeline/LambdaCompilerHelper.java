package book.pipeline;

 
import java.io.*; 
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths; 
import java.util.stream.Stream; 

public class LambdaCompilerHelper {
    //bulk-events-stage/target/lambda.zip
	//single-event-stage/target/lambda.zip
    public static byte[] getTestJarBytes(String otherDir) {
        try {
            // 獲取當前項目的target目錄
            String projectDir = System.getProperty("user.dir");
            String homePomPath = Paths.get(projectDir).getParent().toFile().getAbsolutePath();
            Path calculatedPath;
			if (otherDir != null) {
				calculatedPath = Paths.get(homePomPath ,otherDir );
			}else {
				calculatedPath = Paths.get( projectDir );
			}
            
            Path directoryPath = Paths.get(calculatedPath.toFile().getAbsolutePath(), "target"); 
            
           
            Path lambdaZipPath = findLambdaZipFile(directoryPath);  
            if(lambdaZipPath!=null) {
            	return Files.readAllBytes(lambdaZipPath);
            }
            
            Path jarPath = findJarFile(directoryPath);            
            if (jarPath == null) {
                // 如果找不到jar檔案，嘗試即時編譯
                compileProject();
                
                lambdaZipPath =  findLambdaZipFile(directoryPath);  
                if(lambdaZipPath!=null) {
                	return Files.readAllBytes(lambdaZipPath);
                }
                
                
                jarPath = findJarFile(directoryPath);                
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
        try (Stream<Path> walk = Files.walk(targetDir , 1 )) {
            return walk
                .filter(Files::isRegularFile) 
                .filter(p -> p.toString().endsWith(".jar"))
                .filter(p -> !p.toString().contains("original"))  // 排除original-開頭的jar
                .filter(p -> !p.toString().contains("-test"))     // 排除測試jar
                .findFirst()
                .orElse(null);
        }
    } 
    private static Path findLambdaZipFile(Path targetDir) throws IOException { 
        // 使用Files.walk來搜尋lambda.zip檔案
        try (Stream<Path> walk = Files.walk(targetDir , 1)) {
            return walk
                .filter(Files::isRegularFile) 
                .filter(p -> p.toString().endsWith(".zip")) 
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