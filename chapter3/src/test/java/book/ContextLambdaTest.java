package book;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.ArraySizeComparator;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream; 
import software.amazon.awssdk.core.SdkBytes;  
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*; 

class ContextLambdaTest extends AbstractLambdaTest{
	@Test
	public void testHandler() throws Exception {
		String functionName = "ContextLambda";
		String className = "book.ContextLambda";
		String handler = "book.ContextLambda::handler";
		
		// Lambda 函數代碼
		String functionCode = functionCode("src/main/java/book/ContextLambda.java");

		// 編譯並打包 Lambda 函數
		ByteBuffer zippedCode = LambdaCompilerHelper.compileAndZip(className, functionCode);
		ByteBufferBackedInputStream is = new ByteBufferBackedInputStream(zippedCode);
		SdkBytes fileToUpload = SdkBytes.fromInputStream(is);

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
		SDKv2LambdaUtils.waitForFunctionActive(lambdaClient, functionName);

		// Need a SdkBytes instance for the payload.

		// SdkBytes payload = pseudoPayload();
//		SdkBytes payload = SdkBytes.fromString("\"World\"", StandardCharsets.UTF_8);

		String result = SDKv2LambdaUtils.invokeFunction(lambdaClient, functionName, null);
		System.out.println(DASHES);

		assertNotNull( result); 
		JSONAssert.assertEquals(
				  "{\"getFunctionName\":\"ContextLambda\",getMemoryLimitInMB:\"128\",\"getLogGroupName\":\"/aws/lambda/ContextLambda\"}", 
				  result, 
				  JSONCompareMode.LENIENT);
	}

}
