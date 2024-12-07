package book.pipeline ;


import book.pipeline.bulk.BulkEventsLambda;
import book.pipeline.common.WeatherEvent;
import book.pipeline.single.SingleEventLambda; 
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider; 
import software.amazon.awssdk.core.SdkBytes; 
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;

import java.io.*; 
import java.util.ArrayList;
import java.util.List; 

import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*;

@Testcontainers
public class PipelineIntegrationTestV2 {

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:4")
            ).withServices(S3, SNS, LAMBDA, IAM ,CLOUDWATCH  )
    		 .withEnv("DEBUG", "1")
    		 .withEnv("LAMBDA_EXECUTOR", "local")
    		 ;

    private static S3Client s3Client;
    private static SnsClient snsClient;
    private static LambdaClient lambdaClient;
    private static IamClient iamClient;
    
    private static String bucketName;
    private static String topicArn;
    private static String bulkLambdaArn;
    private static String singleLambdaArn;
    private static String roleArn;
    
    private static ObjectMapper objectMapper;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    
    
    @BeforeAll
    static void setupClients() {
        // Configure AWS clients
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
        );
        Region region = Region.of(localstack.getRegion());

        // Configure S3 client with LocalStack endpoint
        s3Client = S3Client.builder()
                .endpointOverride(localstack.getEndpointOverride(S3))
                .credentialsProvider(credentialsProvider)
                .region(region)
                .forcePathStyle(true)  // Important for LocalStack
                .build();
        
        
        // Configure SNS client with LocalStack endpoint
        snsClient = SnsClient.builder()
                .endpointOverride(localstack.getEndpointOverride(SNS))
                .credentialsProvider(credentialsProvider)
                .region(region)
                .build();

        lambdaClient = LambdaClient.builder()
                .endpointOverride(localstack.getEndpointOverride(LAMBDA))
                .credentialsProvider(credentialsProvider)
                .region(region)
                .build();

        iamClient = IamClient.builder()
                .endpointOverride(localstack.getEndpointOverride(IAM))
                .credentialsProvider(credentialsProvider)
                .region(region)
                .build();

        objectMapper = new ObjectMapper();
        bucketName = "test-pipeline-bucket";
        
        // Create test bucket
        s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(bucketName)
                .build());

     // Setup test resources
        setupIamRole();
        setupS3Bucket();
        setupSnsTopic();
        deployLambdaFunctions();
        configureS3Trigger();
        configureSnsSubscription();
    }
    private static void setupIamRole() {
        // Create IAM role
        String assumeRolePolicy = """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Principal": {
                                "Service": "lambda.amazonaws.com"
                            },
                            "Action": "sts:AssumeRole"
                        }
                    ]
                }
                """;

        CreateRoleRequest createRoleRequest = CreateRoleRequest.builder()
                .roleName("lambda-role")
                .assumeRolePolicyDocument(assumeRolePolicy)
                .build();

        CreateRoleResponse createRoleResponse = iamClient.createRole(createRoleRequest);
        roleArn = createRoleResponse.role().arn();

        // Attach policies
        String policyDocument = """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Action": [
                                "s3:GetObject",
                                "sns:Publish",
                                "logs:CreateLogGroup",
                                "logs:CreateLogStream",
                                "logs:PutLogEvents"
                            ],
                            "Resource": "*"
                        }
                    ]
                }
                """;

        PutRolePolicyRequest putRolePolicyRequest = PutRolePolicyRequest.builder()
                .roleName("lambda-role")
                .policyName("lambda-policy")
                .policyDocument(policyDocument)
                .build();

        iamClient.putRolePolicy(putRolePolicyRequest);
    }

    private static void setupS3Bucket() {
        s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(bucketName)
                .build());
    }

    private static void setupSnsTopic() {
        CreateTopicResponse createTopicResponse = snsClient.createTopic(
                CreateTopicRequest.builder()
                        .name("test-topic")
                        .build()
        );
        topicArn = createTopicResponse.topicArn();
        System.setProperty("FAN_OUT_TOPIC", topicArn);
    }

    private static void deployLambdaFunctions()  {
        try {
			// Deploy BulkEventsLambda
        	byte[] bulkLambdaZip = createLambdaZip(BulkEventsLambda.class);
            CreateFunctionRequest bulkFunctionRequest = CreateFunctionRequest.builder()
                    .functionName("bulk-events-lambda")
                    .runtime("java17")
                    .role(roleArn)
                    .handler("book.pipeline.bulk.BulkEventsLambda::handler")
                    .code(FunctionCode.builder().zipFile(SdkBytes.fromByteArray(bulkLambdaZip)).build())
                    .timeout(30)
                    .memorySize(512)
                    .environment(Environment.builder()
                            .variables(java.util.Map.of("FAN_OUT_TOPIC", topicArn))
                            .build())
                    .build();

            CreateFunctionResponse bulkFunctionResponse = lambdaClient.createFunction(bulkFunctionRequest);
            bulkLambdaArn = bulkFunctionResponse.functionArn();
            System.out.println("-------------------------------------------");
            System.out.println(localstack.getLogs());
            // 等待 Lambda 函數創建完成
            Thread.sleep(1000);
            System.out.println(localstack.getLogs());

            // 確保函數已經準備就緒
            GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder()
                    .functionName("bulk-events-lambda")
                    .build();
            lambdaClient.getFunction(getFunctionRequest);

			// Deploy SingleEventLambda
			byte[] singleLambdaZip = createLambdaZip(SingleEventLambda.class);
			CreateFunctionRequest singleFunctionRequest = CreateFunctionRequest.builder()
			        .functionName("single-event-lambda")
			        .runtime("java17")
			        .role(roleArn)
			        .handler("book.pipeline.single.SingleEventLambda::handler")
			        .code(FunctionCode.builder().zipFile(SdkBytes.fromByteArray(singleLambdaZip)).build())
			        .timeout(30)
			        .memorySize(512)
			        .build();

			CreateFunctionResponse singleFunctionResponse = lambdaClient.createFunction(singleFunctionRequest);
			singleLambdaArn = singleFunctionResponse.functionArn();
			System.out.println(localstack.getLogs());
			
			 // 等待 Lambda 函數創建完成
            Thread.sleep(1000);
            System.out.println(localstack.getLogs());
            // 確保函數已經準備就緒
            GetFunctionRequest getFunctionRequest2 = GetFunctionRequest.builder()
                    .functionName("single-event-lambda")
                    .build();
            lambdaClient.getFunction(getFunctionRequest2);
            System.out.println(localstack.getLogs());
		} catch ( Exception e) { 
			System.err.println("Error deploying bulk events lambda: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
		}  
    }
 // 添加測試輔助方法
    private static void waitForLambdaReady(String functionName) {
        int maxRetries = 10;
        int retryCount = 0;
        while (retryCount < maxRetries) {
            try {
                GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder()
                        .functionName(functionName)
                        .build();
                lambdaClient.getFunction(getFunctionRequest);
                return; // 如果成功就返回
            } catch (Exception e) {
                System.out.println("Waiting for Lambda function to be ready...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                retryCount++;
            }
        }
        throw new RuntimeException("Lambda function not ready after maximum retries");
    }

    private static void configureS3Trigger() {
    	// 首先確保 Lambda ARN 使用正確的格式
        String localstackLambdaArn = String.format("arn:aws:lambda:%s:000000000000:function:%s",
                localstack.getRegion(), "bulk-events-lambda");

        // 添加 Lambda 調用權限
        try {
            AddPermissionRequest addPermissionRequest = AddPermissionRequest.builder()
                    .functionName("bulk-events-lambda")
                    .statementId("s3-permission")
                    .action("lambda:InvokeFunction")
                    .principal("s3.amazonaws.com")
                    .sourceArn(String.format("arn:aws:s3:::%s", bucketName))
                    .build();

            lambdaClient.addPermission(addPermissionRequest);
        } catch (Exception e) {
            System.out.println("Permission already exists or error adding permission: " + e.getMessage());
        }

        // 設置 S3 事件通知
        try {
            LambdaFunctionConfiguration lambdaConfig = LambdaFunctionConfiguration.builder()
                    .id("S3ToLambdaConfig")
                    .lambdaFunctionArn(localstackLambdaArn)
                    .events(Event.S3_OBJECT_CREATED_PUT, Event.S3_OBJECT_CREATED_POST)
                    .build();

            NotificationConfiguration notificationConfig = NotificationConfiguration.builder()
                    .lambdaFunctionConfigurations(lambdaConfig)
                    .build();

            PutBucketNotificationConfigurationRequest notificationRequest = 
                PutBucketNotificationConfigurationRequest.builder()
                    .bucket(bucketName)
                    .notificationConfiguration(notificationConfig)
                    .build();

            // 在設置通知之前等待一下，確保 Lambda 權限已經生效
            Thread.sleep(1000);
            
            s3Client.putBucketNotificationConfiguration(notificationRequest);
        } catch (Exception e) {
            System.err.println("Error configuring S3 trigger: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 新增一個輔助方法來驗證通知配置
    private static void verifyBucketNotificationConfiguration() {
    	GetBucketNotificationConfigurationRequest getBucketNotificationConfigurationRequest = 
            GetBucketNotificationConfigurationRequest.builder()
                .bucket(bucketName)
                .build();

    	 
    	 GetBucketNotificationConfigurationResponse config = 
            s3Client.getBucketNotificationConfiguration(getBucketNotificationConfigurationRequest);
        
        System.out.println("Current bucket notification configuration:");
        if (config.lambdaFunctionConfigurations() != null) {
            config.lambdaFunctionConfigurations().forEach(lambda -> 
                System.out.println("Lambda ARN: " + lambda.lambdaFunctionArn()));
        }
    }

    private static void configureSnsSubscription() {
        AddPermissionRequest addPermissionRequest = AddPermissionRequest.builder()
                .functionName("single-event-lambda")
                .statementId("sns-permission")
                .action("lambda:InvokeFunction")
                .principal("sns.amazonaws.com")
                .sourceArn(topicArn)
                .build();

        lambdaClient.addPermission(addPermissionRequest);

        // Subscribe Lambda to SNS topic
        software.amazon.awssdk.services.sns.model.SubscribeRequest subscribeRequest = 
                software.amazon.awssdk.services.sns.model.SubscribeRequest.builder()
                        .topicArn(topicArn)
                        .protocol("lambda")
                        .endpoint(singleLambdaArn)
                        .build();

        snsClient.subscribe(subscribeRequest);
    }

	private static byte[] createLambdaZip(Class<?> lambdaClass) throws IOException {
		// bulk-events-stage/target/lambda.zip
		// single-event-stage/target/lambda.zip
		String simpleName = lambdaClass.getSimpleName();
		String lambdaName = switch (simpleName) {
		case "BulkEventsLambda" -> "bulk-events-stage";
		case "SingleEventLambda" -> "single-event-stage";
		default -> null;
		};
		return LambdaCompilerHelper.getTestJarBytes(lambdaName);
	}
    @BeforeEach
    void setup() {
        System.setOut(new PrintStream(outputStreamCaptor));
    }
    private static final String BULK_EVENTS_FUNCTION = "bulk-events-lambda" ;
    private static final String SINGLE_EVENT_FUNCTION = "single-event-lambda" ;
    
    //testBulkEeventsLambda
//    @Test
//    void testBulkEeventsLambda() {
//    	
//    }
    @Test
    void testLambdaIfReady() throws IOException, InterruptedException {
    	// 確保 Lambda 函數已經準備就緒
        waitForLambdaReady(BULK_EVENTS_FUNCTION);
        waitForLambdaReady(SINGLE_EVENT_FUNCTION);
        
        // 測試 WeatherEventLambda
        GetFunctionResponse eventLambdaResponse = lambdaClient.getFunction(
            GetFunctionRequest.builder()
                .functionName(BULK_EVENTS_FUNCTION)
                .build()
        );
        assertEquals(BULK_EVENTS_FUNCTION, eventLambdaResponse.configuration().functionName());
        
     // 測試 WeatherQueryLambda
        GetFunctionResponse queryLambdaResponse = lambdaClient.getFunction(
            GetFunctionRequest.builder()
                .functionName(SINGLE_EVENT_FUNCTION)
                .build()
        );
        assertEquals(SINGLE_EVENT_FUNCTION, queryLambdaResponse.configuration().functionName());
    }
    @Test
    void testWeatherEventPipelineV2() throws IOException, InterruptedException {
    	// 確保 Lambda 函數已經準備就緒
        waitForLambdaReady(BULK_EVENTS_FUNCTION);
        waitForLambdaReady("single-event-lambda");
    	
    	// 驗證通知配置
        verifyBucketNotificationConfiguration();
    	
        // Create test weather events
        List<WeatherEvent> testEvents = new ArrayList<>();
        testEvents.add(new WeatherEvent("Tokyo", 25.5, 1635739200000L, 139.6917, 35.6895));
        testEvents.add(new WeatherEvent("London", 15.0, 1635739200000L, -0.1276, 51.5074));

        // Upload test data to S3
        String key = "test-events.json";
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.putObject(putObjectRequest, 
                RequestBody.fromBytes(objectMapper.writeValueAsBytes(testEvents)));

        // Wait for processing
        Thread.sleep(5000);

        // Verify Lambda execution by checking CloudWatch logs or function metrics
        ListFunctionEventInvokeConfigsRequest listInvokesRequest = 
                ListFunctionEventInvokeConfigsRequest.builder()
                        .functionName("bulk-events-lambda")
                        .build();
        
        // Verify the functions were invoked
        assertTrue(lambdaClient.listFunctionEventInvokeConfigs(listInvokesRequest)
                .functionEventInvokeConfigs().size() > 0);
        
     // 驗證 Lambda 執行
        try {
            GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder()
                    .functionName("bulk-events-lambda")
                    .build();
            GetFunctionResponse functionResponse = lambdaClient.getFunction(getFunctionRequest);
            assertNotNull(functionResponse);
            assertEquals("Active", functionResponse.configuration().state());
        } catch (Exception e) {
            fail("Failed to verify Lambda function: " + e.getMessage());
        }
    }

}
