package book.api;

import org.junit.jupiter.api.AfterAll; 
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers; 
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import book.LambdaCompilerHelper;
import book.SDKv2ApiGatewayUtils;
import book.SDKv2LambdaUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;  
import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.apigateway.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest; 
import software.amazon.awssdk.services.lambda.model.*;
import software.amazon.awssdk.services.lambda.waiters.LambdaWaiter;
import software.amazon.awssdk.utils.IoUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.LAMBDA;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.API_GATEWAY;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;  
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WeatherEventLambdaTest {
	public static final String DASHES = new String(new char[80]).replace("\0", "-");

	   @Container
	   public static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
			   .withServices(LAMBDA, 
	                         API_GATEWAY, 
	                         DYNAMODB);

	   private static LambdaClient lambdaClient;
	    private static ApiGatewayClient apiGatewayClient;
	    private static DynamoDbClient dynamoDbClient;
	    private static SdkHttpClient  httpClient;
	    
	    private static final ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	     
	    private static final String TABLE_NAME = "LocationsTable";
	    private static final String WEATHER_EVENT_FUNCTION = "WeatherEventLambda";
	    private static final String WEATHER_QUERY_FUNCTION = "WeatherQueryLambda";
	    
	    private static String apiId;
	    private static String apiEndpoint;
	   

	   @AfterAll
	   public  static void tearDown() throws  Exception {
	    	cleanupTestData(); 
	   }
	   @BeforeAll
	   public static void setup() {   
		   StaticCredentialsProvider scp = credentials();
		   createApiGatewayClient(scp);
		   createLambdaClient(scp);
		   createDynamoDbClient(scp);
	        httpClient = UrlConnectionHttpClient.builder().build();
//	        httpClient =  ApacheHttpClient.builder().build();
	        setupInfrastructure();
	   }
	   
	   protected static StaticCredentialsProvider credentials() {
		  StaticCredentialsProvider scp = StaticCredentialsProvider.create(
  		            AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())
  		        );
		  return scp; 
	   }
	   static void createLambdaClient(StaticCredentialsProvider credentials){
			// 初始化Lambda Client
		   lambdaClient = LambdaClient.builder()
				     .endpointOverride(localStack.getEndpointOverride(Service.LAMBDA))
		       		 .credentialsProvider(credentials)
		       		 .region(Region.of(localStack.getRegion())) 
		             .build();
		   }
	   static void createApiGatewayClient(StaticCredentialsProvider credentials){
		// 初始化API Gateway客戶端
	        apiGatewayClient = ApiGatewayClient.builder()
	        		.endpointOverride(localStack.getEndpointOverride(Service.API_GATEWAY))
	                .credentialsProvider(credentials)
	                .region(Region.of(localStack.getRegion()))
	                .build();
	   }
	   static void createDynamoDbClient(StaticCredentialsProvider credentials){
			// 初始化DynamoDb Client
		   dynamoDbClient = DynamoDbClient.builder()
	                .endpointOverride(localStack.getEndpointOverride(Service.DYNAMODB))
	                .credentialsProvider(credentials)
	                .region(Region.of(localStack.getRegion()))
	                .build();
	  }
	   private static void setupInfrastructure()   {
	        // 1. 創建DynamoDB表
	        createDynamoDbTable();
	        
	        // 2. 創建Lambda函數
	        byte[] zipBytes = LambdaCompilerHelper.getTestJarBytes();
	        createLambdaFunction(WEATHER_EVENT_FUNCTION, zipBytes);
	        createLambdaFunction(WEATHER_QUERY_FUNCTION, zipBytes);
	        
	         
	        
	        // 3. 創建和配置API Gateway
	        setupApiGateway();
	    }
	   private static String createLambdaFunction(String functionName, byte[] zipBytes) {
	       try {
	    	   LambdaWaiter waiter = lambdaClient.waiter();
	    	   
	           CreateFunctionRequest createFunctionRequest = CreateFunctionRequest.builder()
	                .functionName(functionName)
	                .runtime("java17")
	                .role("arn:aws:iam::000000000000:role/lambda-role")
	                .handler("book.api." + functionName + "::handler")
	                .code(FunctionCode.builder()
	                        .zipFile(SdkBytes.fromByteArray(zipBytes))
	                        .build())
	                .environment(Environment.builder()
	                        .variables(Map.of("LOCATIONS_TABLE", TABLE_NAME))
	                        .build())
	                .memorySize(512)
	                .timeout(25)
	                .build();

	          // Create a Lambda function using a waiter
	           CreateFunctionResponse functionResponse = lambdaClient.createFunction(createFunctionRequest);
	           GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder()
	                   .functionName(functionName)
	                   .build(); 
	           
	           WaiterResponse<GetFunctionResponse> waiterResponse = waiter.waitUntilFunctionExists(getFunctionRequest);
	           waiterResponse.matched().response().ifPresent(System.out::println);
//	         SDKv2LambdaUtils. waitForFunctionActive(lambdaClient , functionName);
	           
	           return  functionResponse.functionArn();
	    	 }catch (Exception e) {
	             throw new RuntimeException("創建 Lambda 函數失敗", e);
	         } 
	    }
	   private static void createDynamoDbTable() {
	        CreateTableRequest createTableRequest = CreateTableRequest.builder()
	                .tableName(TABLE_NAME)
	                .keySchema(KeySchemaElement.builder()
	                        .attributeName("locationName")
	                        .keyType(KeyType.HASH)
	                        .build())
	                .attributeDefinitions(AttributeDefinition.builder()
	                        .attributeName("locationName")
	                        .attributeType(ScalarAttributeType.S)
	                        .build())
	                .billingMode(BillingMode.PAY_PER_REQUEST)
	                .build();

	        dynamoDbClient.createTable(createTableRequest);
	    }
	   private static void setupApiGateway() {
	        // 創建API
	        CreateRestApiRequest createApiRequest = CreateRestApiRequest.builder()
	                .name("WeatherAPI")
	                .description("Weather API")
	                .build();

	        CreateRestApiResponse createApiResponse = apiGatewayClient.createRestApi(createApiRequest);
	        String apiId = createApiResponse.id();

	        // 獲取根資源ID
	        String rootResourceId = apiGatewayClient.getResources(GetResourcesRequest.builder()
	                .restApiId(apiId)
	                .build())
	                .items()
	                .get(0)
	                .id();

	        // 創建/events資源和方法
	        setupApiResource(apiId, rootResourceId, "events", WEATHER_EVENT_FUNCTION, "POST");
	        
	        // 創建/locations資源和方法
	        setupApiResource(apiId, rootResourceId, "locations", WEATHER_QUERY_FUNCTION, "GET");

	        // 部署API
	        CreateDeploymentRequest deployRequest = CreateDeploymentRequest.builder()
	                .restApiId(apiId)
	                .stageName("prod")
	                .build();

	        CreateDeploymentResponse createDeploymentResponse=  apiGatewayClient.createDeployment(deployRequest);
	        
	        Map<String, Map<String, MethodSnapshot>> map = createDeploymentResponse.apiSummary();
	        
	       // Set API endpoint
	        apiEndpoint = String.format("http://%s:%d/restapis/%s/local/_user_request_",
	                localStack.getHost(),
	                localStack.getMappedPort(4566),
	                apiId);
	    }

		private static void setupApiResource(String apiId, String parentId, String pathPart, String lambdaFunction,
				String httpMethod) {
            // 創建資源
			CreateResourceRequest createResourceRequest = CreateResourceRequest.builder().restApiId(apiId)
					.parentId(parentId).pathPart(pathPart).build();

			CreateResourceResponse createResourceResponse = apiGatewayClient.createResource(createResourceRequest);
			String resourceId = createResourceResponse.id();

            // 創建方法
			PutMethodRequest putMethodRequest = PutMethodRequest.builder().restApiId(apiId).resourceId(resourceId)
					.httpMethod(httpMethod).authorizationType("NONE").build();

			apiGatewayClient.putMethod(putMethodRequest);

             // 設置Lambda集成
			String functionArn = String.format("arn:aws:lambda:%s:000000000000:function:%s", localStack.getRegion(),
					lambdaFunction);

			PutIntegrationRequest putIntegrationRequest = PutIntegrationRequest.builder().restApiId(apiId)
					.resourceId(resourceId).httpMethod(httpMethod).type(IntegrationType.AWS_PROXY)
					.integrationHttpMethod("POST")
					.uri(String.format("arn:aws:apigateway:%s:lambda:path/2015-03-31/functions/%s/invocations",
							localStack.getRegion(), functionArn))
					.build();

			apiGatewayClient.putIntegration(putIntegrationRequest);
		}
	   
	   private static void cleanupTestData() {
	        try {
	            // 清理 DynamoDB 表中的測試數據
	            ScanRequest scanRequest = ScanRequest.builder()
	                    .tableName(TABLE_NAME)
	                    .build();
	            
	            dynamoDbClient.scan(scanRequest)
	                    .items()
	                    .forEach(item -> {
	                        DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
	                                .tableName(TABLE_NAME)
	                                .key(Map.of("locationName", item.get("locationName")))
	                                .build();
	                        dynamoDbClient.deleteItem(deleteRequest);
	                    });
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	   @Test
	    void testWeatherApi() {
	        // 測試插入位置數據
	        PutItemRequest putItemRequest = PutItemRequest.builder()
	                .tableName(TABLE_NAME)
	                .item(Map.of(
		                    "locationName", AttributeValue.builder().s("Oxford, UK").build(),
		                    "temperature", AttributeValue.builder().n("64").build(),
		                    "timestamp", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build(),
		                    "latitude", AttributeValue.builder().n("51.75").build(),
		                    "longitude", AttributeValue.builder().n("-1.25").build()
	                ))
	                .build();

	        dynamoDbClient.putItem(putItemRequest);

	        // 測試Weather Event Lambda
	        InvokeRequest eventInvokeRequest = InvokeRequest.builder()
	                .functionName(WEATHER_EVENT_FUNCTION)
	                .payload(SdkBytes.fromUtf8String(
	           "{ \"body\":\"{\\\"locationName\\\":\\\"Brooklyn, NY\\\" , \\\"temperature\\\":91 , \\\"timestamp\\\":1564428897 , \\\"latitude\\\": 40.70, \\\"longitude\\\": -73.99 }\"  }"
	                		
	                		))
	                .build();

	        InvokeResponse eventResponse = lambdaClient.invoke(eventInvokeRequest);
	        assertEquals(200, eventResponse.statusCode());	        
	        String result1 = new String(eventResponse.payload().asByteArray(), StandardCharsets.UTF_8);
	        System.out.println(result1);
	        assertEquals("{\"statusCode\":200,\"body\":\"Brooklyn, NY\"}", result1);
	        
	        // 測試Weather Query Lambda
	        InvokeRequest queryInvokeRequest = InvokeRequest.builder()
	                .functionName(WEATHER_QUERY_FUNCTION)
	                .payload(SdkBytes.fromUtf8String("{}"))
	                .build();

	        InvokeResponse queryResponse = lambdaClient.invoke(queryInvokeRequest);
	        assertEquals(200, queryResponse.statusCode());
	        String result2 = new String(queryResponse.payload().asByteArray(), StandardCharsets.UTF_8);
	         
	        System.out.println(result2);
	        assertTrue( result2.contains("\\\"statusCode\\\":200") || result2.contains("Brooklyn, NY"),result2);
	    } 
	   @Test
	    @Order(1)
	    void testDynamoDBTableCreation() {
	        DescribeTableResponse response = dynamoDbClient.describeTable(
	            DescribeTableRequest.builder()
	                .tableName(TABLE_NAME)
	                .build()
	        );
	        
	        assertEquals(TABLE_NAME, response.table().tableName());
	        assertEquals("locationName", response.table().keySchema().get(0).attributeName());
	    }

	    @Test
	    @Order(2)
	    void testLambdaFunctionsCreation() {
	        // 測試 WeatherEventLambda
	        GetFunctionResponse eventLambdaResponse = lambdaClient.getFunction(
	            GetFunctionRequest.builder()
	                .functionName(WEATHER_EVENT_FUNCTION)
	                .build()
	        );
	        assertEquals(WEATHER_EVENT_FUNCTION, eventLambdaResponse.configuration().functionName());
	        
	        // 測試 WeatherQueryLambda
	        GetFunctionResponse queryLambdaResponse = lambdaClient.getFunction(
	            GetFunctionRequest.builder()
	                .functionName(WEATHER_QUERY_FUNCTION)
	                .build()
	        );
	        assertEquals(WEATHER_QUERY_FUNCTION, queryLambdaResponse.configuration().functionName());
	    }

	    @Test
	    @Order(3)
	    void testWeatherEventEndToEnd() throws Exception {
	        // 準備測試數據
	        Map<String, Object> weatherEvent = new HashMap<>();
	        weatherEvent.put("locationName", "Tokyo");
	        weatherEvent.put("temperature", 25.5);
	        weatherEvent.put("timestamp", System.currentTimeMillis());
	        
	        // 通過 API Gateway 發送請求
	        String response = makeHttpRequest("/events", "POST", weatherEvent);
	        assertNotNull(response);
	        
	        // 驗證數據已存入 DynamoDB
	        GetItemResponse getItemResponse = dynamoDbClient.getItem(
	            GetItemRequest.builder()
	                .tableName(TABLE_NAME)
	                .key(Map.of("locationName", AttributeValue.builder().s("Tokyo").build()))
	                .build()
	        );
	        
	        Map<String, AttributeValue> item = getItemResponse.item();
	        assertNotNull(item);        
	        assertEquals("Tokyo", item.get("locationName").s());
	    }

	    @Test
	    @Order(4)
	    void testWeatherQueryEndToEnd() throws Exception {
	        // 先插入一些測試數據
	        PutItemRequest putItemRequest = PutItemRequest.builder()
	                .tableName(TABLE_NAME)
	                .item(Map.of(
	                    "locationName", AttributeValue.builder().s("Oxford, UK").build(),
	                    "temperature", AttributeValue.builder().n("64").build(),
	                    "timestamp", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build(),
	                    "latitude", AttributeValue.builder().n("51.75").build(),
	                    "longitude", AttributeValue.builder().n("-1.25").build()
	                    
	                ))
	                .build();
	        PutItemResponse putItemRresponse =   dynamoDbClient.putItem(putItemRequest);
	        System.out.println(TABLE_NAME + " was successfully updated. The request id is "
	                + putItemRresponse.responseMetadata().requestId());
	        
	        
	        // 通過 API Gateway 查詢數據
	        String response = makeHttpRequest("/locations", "GET", null);
	        assertNotNull(response);
	        System.out.println(response);
	        
	        // 驗證返回的數據
//	        if(response.contains("[")) {
	        	List<?> locations = objectMapper.readValue(response, List.class);
		        assertFalse(locations.isEmpty());
//	        }  
	    }
	    
	    @Test
	    @Order(5)
	    void testErrorHandling() throws Exception {
	        // 測試無效的請求
	        Map<String, Object> invalidEvent = new HashMap<>();
	        invalidEvent.put("invalidField", "invalidValue");
	        
	        String response = makeHttpRequest("/events", "POST", invalidEvent);
	        assertTrue(response.contains("error") || response.contains("Error"));
	    }

	    private static String makeHttpRequest(String path, String method, Object body) throws IOException    {
	    	
	    	SdkHttpFullRequest.Builder requestBuilder = SdkHttpFullRequest.builder()
	                .uri(URI.create(apiEndpoint.toString() + path))
	                .method(SdkHttpMethod.fromValue(method));
	                
	        if (body != null) {
	            requestBuilder.putHeader("Content-Type", "application/json"); 
	            
	            final byte[] bytes = objectMapper.writeValueAsBytes(body) ; 
	            
	            requestBuilder.contentStreamProvider(() -> 
	                new ByteArrayInputStream(bytes));
	        } 
	        SdkHttpRequest request = requestBuilder.build() ;
	        
	        // Create executable request
	        HttpExecuteRequest executeRequest = HttpExecuteRequest.builder()
	                .request(request)
	                .contentStreamProvider(requestBuilder.contentStreamProvider() )
	                .build();
	         
	        // Call request 
	        HttpExecuteResponse executeResponse = httpClient.prepareRequest(executeRequest).call();

	        boolean ok = executeResponse.httpResponse().statusCode() == 200;
	        if(ok) {
	            String response = IoUtils.toUtf8String(executeResponse.responseBody().orElse(AbortableInputStream.createEmpty()));
	            return response ;
	        }else {
	            System.out.println("------------------------");
	           	System.out.println("statusCode: "+ executeResponse.httpResponse().statusCode());
	            System.out.println("statusText: "+ executeResponse.httpResponse().statusText());
	            String response = new String(executeResponse.responseBody().get().readAllBytes());
	            System.out.println("response content: "+response);
	            return response ;
	        } 
	    }
 
}
