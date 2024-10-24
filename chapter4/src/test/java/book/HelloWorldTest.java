package book;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers; 
import org.testcontainers.utility.DockerImageName; 

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes; 
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient; 
 
 
import java.nio.charset.StandardCharsets; 

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.LAMBDA;

@Testcontainers
public class HelloWorldTest {

	   public static final String DASHES = new String(new char[80]).replace("\0", "-");

	   @Container
	   public static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
	           .withServices(LAMBDA);

	   private static LambdaClient  lambdaClient;

	   @BeforeAll
	   public static void setup() {   	  
	       lambdaClient = LambdaClient.builder()
	       		 .endpointOverride(localStack.getEndpoint())
	       		 .credentialsProvider(
	       		        StaticCredentialsProvider.create(
	       		            AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())
	       		        )
	       		    )
	       		 .region(Region.of(localStack.getRegion())) 
	             .build();
	   }
 

	   @Test
	   public void testHandler() throws Exception {
		   String functionName = "HelloWorldJava"; 
		   String handler = "book.HelloWorld::handler"; 
		   
	       // 編譯並打包 Lambda 函數 
           SdkBytes fileToUpload = SdkBytes.fromByteArray(LambdaCompilerHelper.getTestJarBytes());
	       
	       // 驗證結果
			String funArn = SDKv2LambdaUtils.createLambdaFunction(lambdaClient, functionName, handler, fileToUpload);
	       System.out.println("The AWS Lambda ARN is " + funArn);
	       System.out.println(DASHES);
	       
	       System.out.println(DASHES);
	       System.out.println("2. Get the " + functionName + " AWS Lambda function.");
	       SDKv2LambdaUtils.getFunction(lambdaClient, functionName);
	       System.out.println(DASHES);

	       System.out.println(DASHES);
	       System.out.println("3. List all AWS Lambda functions.");
	       SDKv2LambdaUtils.listFunctions(lambdaClient);
	       System.out.println(DASHES);

	       System.out.println(DASHES);
	       System.out.println("4. Invoke the Lambda function.");
	       System.out.println("*** Sleep for 1 min to get Lambda function ready.");       
	       SDKv2LambdaUtils. waitForFunctionActive(lambdaClient , functionName);       
	       
	    // Need a SdkBytes instance for the payload. 
		   SdkBytes payload = SdkBytes.fromString("\"World\"", StandardCharsets.UTF_8);
	       
	       String  result = SDKv2LambdaUtils.invokeFunction(lambdaClient, functionName ,payload);
	       System.out.println(DASHES);
	       
	       assertEquals("\"Hello, World\"", result);
	   } 
    
 } 
