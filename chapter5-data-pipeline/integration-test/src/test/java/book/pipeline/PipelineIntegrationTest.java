package book.pipeline ;
import book.pipeline.bulk.BulkEventsLambda;
import book.pipeline.common.WeatherEvent;
import book.pipeline.single.SingleEventLambda;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;

@Testcontainers
public class PipelineIntegrationTest {

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.8")
            ).withServices(S3, SNS);

    private static S3Client s3Client;
    private static SnsClient snsClient;
    private static String bucketName;
    private static String topicArn;
    private static ObjectMapper objectMapper;
    private static BulkEventsLambda bulkEventsLambda;
    private static SingleEventLambda singleEventLambda;
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

        objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        bucketName = "test-pipeline-bucket";
        
        // Create test bucket
        s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(bucketName)
                .build());

        // Create SNS topic
        CreateTopicResponse createTopicResponse = snsClient.createTopic(
                CreateTopicRequest.builder()
                        .name("test-topic")
                        .build()
        );
        topicArn = createTopicResponse.topicArn();

        // Set environment variable for lambda
        System.setProperty("FAN_OUT_TOPIC", topicArn);
    }

    @BeforeEach
    void setup() {
        // Redirect System.out to capture lambda output
        System.setOut(new PrintStream(outputStreamCaptor));
        
        // Create Lambda instances with LocalStack clients
        bulkEventsLambda = new BulkEventsLambda(objectMapper, snsClient, s3Client, topicArn);
        singleEventLambda = new SingleEventLambda();
    }
    @Test
    void testWeatherEventPipeline() throws IOException {
        // Create test weather events
        List<WeatherEvent> testEvents = new ArrayList<>();
        testEvents.add(new WeatherEvent("Tokyo", 25.5, 1635739200000L, 139.6917, 35.6895));
        testEvents.add(new WeatherEvent("London", 15.0, 1635739200000L, -0.1276, 51.5074));

        // Upload test data to S3
        String key = "test-events.json";
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build(),
                RequestBody.fromBytes(objectMapper.writeValueAsBytes(testEvents))
        );

        // Create S3Event
        S3Event s3Event = TestUtils.createS3Event(bucketName, key);
        
        // Process bulk events
        bulkEventsLambda.handler(s3Event);

        // Create SNS event from the published message
        SNSEvent snsEvent = TestUtils.createSNSEvent(topicArn, objectMapper.writeValueAsString(testEvents.get(0)));
        
        // Process single event
        singleEventLambda.handler(snsEvent);

        // Verify output contains weather event information
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Published 2 weather events to SNS"));
        assertTrue(output.contains("Tokyo"));
        assertTrue(output.contains("25.5"));
    }

}
