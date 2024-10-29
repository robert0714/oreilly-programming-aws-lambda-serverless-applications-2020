package book;

import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.apigateway.model.ApiGatewayException;
import software.amazon.awssdk.services.apigateway.model.CreateDeploymentRequest;
import software.amazon.awssdk.services.apigateway.model.CreateDeploymentResponse;
import software.amazon.awssdk.services.apigateway.model.CreateRestApiRequest;
import software.amazon.awssdk.services.apigateway.model.CreateRestApiResponse;

public class SDKv2ApiGatewayUtils {
	
   // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_api-gateway_code_examples.html
	 public static String createNewDeployment(ApiGatewayClient apiGateway, String restApiId, String stageName) {

	        try {
	            CreateDeploymentRequest request = CreateDeploymentRequest.builder()
	                    .restApiId(restApiId)
	                    .description("Created using the AWS API Gateway Java API")
	                    .stageName(stageName)
	                    .build();

	            CreateDeploymentResponse response = apiGateway.createDeployment(request);
	            System.out.println("The id of the deployment is " + response.id());
	            return response.id();

	        } catch (ApiGatewayException e) {
	            System.err.println(e.awsErrorDetails().errorMessage());
	            System.exit(1);
	        }
	        return "";
	 }
	 public static String createAPI(ApiGatewayClient apiGateway, String restApiId, String restApiName) {

	        try {
	            CreateRestApiRequest request = CreateRestApiRequest.builder()
	                    .cloneFrom(restApiId)
	                    .description("Created using the Gateway Java API")
	                    .name(restApiName)
	                    .build();

	            CreateRestApiResponse response = apiGateway.createRestApi(request);
	            System.out.println("The id of the new api is " + response.id());
	            return response.id();

	        } catch (ApiGatewayException e) {
	            System.err.println(e.awsErrorDetails().errorMessage());
	            System.exit(1);
	        }
	        return "";
	    }
}
