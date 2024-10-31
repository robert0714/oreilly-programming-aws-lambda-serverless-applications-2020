package book.pipeline;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.testcontainers.shaded.com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.CreateFunctionResponse;
import software.amazon.awssdk.services.lambda.model.FunctionCode;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest;
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;
import software.amazon.awssdk.services.lambda.model.ListFunctionsResponse;
import software.amazon.awssdk.services.lambda.model.ResourceNotFoundException;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.services.lambda.waiters.LambdaWaiter;

public class SDKv2LambdaUtils {
	
   //  https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_lambda_code_examples.html	
   public static String createLambdaFunction(LambdaClient awsLambda ,
		                                     String functionName , 
		                                     String handler ,
		                                     SdkBytes fileToUpload  ) {
       try {
           LambdaWaiter waiter = awsLambda.waiter(); 
            
          
           FunctionCode code = FunctionCode.builder()
                   .zipFile(fileToUpload)
                   .build();

           CreateFunctionRequest functionRequest =  CreateFunctionRequest.builder()
                   .functionName(functionName) 
                   .runtime(Runtime.JAVA17)
                   .role("arn:aws:iam::000000000000:role/lambda-role")
                   .handler(handler)
                   .code(code)
                   .build();

           // Create a Lambda function using a waiter
           CreateFunctionResponse functionResponse = awsLambda.createFunction(functionRequest);
           GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder()
                   .functionName(functionName)
                   .build();
           WaiterResponse<GetFunctionResponse> waiterResponse = waiter.waitUntilFunctionExists(getFunctionRequest);
           waiterResponse.matched().response().ifPresent(System.out::println);
           return functionResponse.functionArn();

       } catch (LambdaException e   ) {
           System.err.println(e.getMessage());
           System.exit(1);
       }
       return "";
   }
   public static void getFunction(LambdaClient awsLambda, String functionName) {
       try {
           GetFunctionRequest functionRequest = GetFunctionRequest.builder()
                   .functionName(functionName)
                   .build();

           GetFunctionResponse response = awsLambda.getFunction(functionRequest);
           System.out.println("The runtime of this Lambda function is " + response.configuration().runtime());

       } catch (LambdaException e) {
           System.err.println(e.getMessage());
           System.exit(1);
       }
   }

   public static void listFunctions(LambdaClient awsLambda) {
       try {
           ListFunctionsResponse functionResult = awsLambda.listFunctions();
           List<FunctionConfiguration> list = functionResult.functions();
           for (FunctionConfiguration config : list) {
               System.out.println("The function name is " + config.functionName());
           }

       } catch (LambdaException e) {
           System.err.println(e.getMessage());
           System.exit(1);
       }
   }
   public static String invokeFunction(LambdaClient awsLambda, String functionName ,SdkBytes payload) {
       InvokeResponse res;
       
       try {         
           InvokeRequest request = InvokeRequest.builder()
                   .functionName(functionName)
                   .payload(payload)
                   .build();

           res = awsLambda.invoke(request);
           String value = res.payload().asUtf8String();
           System.out.println(value);
           return value ;
       } catch (LambdaException e) {
           System.err.println(e.getMessage());
           System.exit(1);
       }
       return null;
   }
   
   public static void waitForFunctionActive(LambdaClient awsLambda,String functionName) throws TimeoutException, InterruptedException {
	 long startTime = System.currentTimeMillis();
	 long timeout = Duration.ofMinutes(2).toMillis();  // 設置2分鐘超時

	 while (System.currentTimeMillis() - startTime < timeout) {
	     try {
	         GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder().functionName(functionName).build();
	         GetFunctionResponse getFunctionResult = awsLambda.getFunction(getFunctionRequest);
	        
	         String stateAsString =   getFunctionResult.configuration().stateAsString() ; 
	         if ("Active".equals(stateAsString)) {
	             return;  // 函數已經處於活動狀態
	         }
	     } catch (ResourceNotFoundException e) {
	         // 函數尚未創建，繼續等待
	     }
	    
	     Thread.sleep(5000);  // 等待5秒後再次檢查
	 }
	
	 throw new TimeoutException("Function did not become active within the timeout period");
	}
   public static void waitForFunctionActiveV2(LambdaClient awsLambda,String functionName) throws TimeoutException, InterruptedException {
		 long startTime = System.currentTimeMillis();
		 long timeout = Duration.ofMinutes(2).toMillis();  // 設置2分鐘超時

		 while (System.currentTimeMillis() - startTime < timeout) {
		     try {
		         GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder().functionName(functionName).build();
		         GetFunctionResponse getFunctionResult = awsLambda.getFunction(getFunctionRequest);
		        
		         String stateAsString =   getFunctionResult.configuration().stateAsString() ; 
		         if ("Active".equals(stateAsString)) {
		             return;  // 函數已經處於活動狀態
		         }
		     } catch (ResourceNotFoundException e) {
		         // 函數尚未創建，繼續等待
		     }
		    
		     Thread.sleep(5000);  // 等待5秒後再次檢查
		 }
		
		 throw new TimeoutException("Function did not become active within the timeout period");
		}
}
