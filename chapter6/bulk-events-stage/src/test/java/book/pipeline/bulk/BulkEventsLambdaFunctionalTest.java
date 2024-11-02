package book.pipeline.bulk;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.lambda.runtime.serialization.PojoSerializer;
import com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
 
@ExtendWith(SystemStubsExtension.class)
public class BulkEventsLambdaFunctionalTest {

    private final ObjectMapper objectMapper =
    		 new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new JodaModule());
    
    private final PojoSerializer<S3Event> s3EventSerializer =
            LambdaEventSerializers.serializerFor(S3Event.class, ClassLoader.getSystemClassLoader());  
    
    @SystemStub
	private EnvironmentVariables environmentVariables;

     
    @Test 
    public void testHandler() throws IOException {
        // Set up mock AWS SDK v2 clients
        SnsClient mockSNS = Mockito.mock(SnsClient.class);
        S3Client mockS3 = Mockito.mock(S3Client.class);
        
        // Fixture S3 event
        //sam local generate-event s3 put
        //S3Event s3Event = objectMapper.readValue(getClass().getResourceAsStream("/s3_event.json"), S3Event.class);
        S3Event s3Event = s3EventSerializer.fromJson(getClass().getResourceAsStream("/s3_event.json"));
        
        String bucket = s3Event.getRecords().get(0).getS3().getBucket().getName();
        String key = s3Event.getRecords().get(0).getS3().getObject().getKey();

        // Fixture S3 return value
        InputStream contentStream = getClass().getResourceAsStream(String.format("/%s", key));
        ResponseInputStream<GetObjectResponse> responseInputStream = 
            new ResponseInputStream<>(GetObjectResponse.builder().build(), contentStream);
            
        Mockito.when(mockS3.getObject(Mockito.any(GetObjectRequest.class)))
            .thenReturn(responseInputStream);

        // Fixture environment
        String topic = "test-topic";
        environmentVariables.set(BulkEventsLambda.FAN_OUT_TOPIC_ENV, topic);

        // Mock SNS publish response
        Mockito.when(mockSNS.publish(Mockito.any(PublishRequest.class)))
            .thenReturn(PublishResponse.builder().messageId("test-message-id").build());

        // Construct Lambda function class, and invoke handler
        BulkEventsLambda lambda = new BulkEventsLambda(mockSNS, mockS3);
        lambda.handler(s3Event);

        // Capture outbound SNS messages
        ArgumentCaptor<PublishRequest> publishRequestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
        Mockito.verify(mockSNS, Mockito.times(3)).publish(publishRequestCaptor.capture());

        // Extract topics and messages from captured requests
        var capturedRequests = publishRequestCaptor.getAllValues();
        String[] capturedTopics = capturedRequests.stream()
            .map(PublishRequest::topicArn)
            .toArray(String[]::new);
        String[] capturedMessages = capturedRequests.stream()
            .map(PublishRequest::message)
            .toArray(String[]::new);

        // Assert
        Assertions.assertArrayEquals(
            new String[]{"test-topic", "test-topic", "test-topic"}, 
            capturedTopics
        );
        Assertions.assertArrayEquals(
            new String[]{
                "{\"locationName\":\"Brooklyn, NY\",\"temperature\":91.0,\"timestamp\":1564428897,\"longitude\":-73.99,\"latitude\":40.7}",
                "{\"locationName\":\"Oxford, UK\",\"temperature\":64.0,\"timestamp\":1564428898,\"longitude\":-1.25,\"latitude\":51.75}",
                "{\"locationName\":\"Charlottesville, VA\",\"temperature\":87.0,\"timestamp\":1564428899,\"longitude\":-78.47,\"latitude\":38.02}"
            },
            capturedMessages
        );
    }

    @Test 
    public void testBadData() throws IOException {
        // Set up mock AWS SDK v2 clients
        SnsClient mockSNS = Mockito.mock(SnsClient.class);
        S3Client mockS3 = Mockito.mock(S3Client.class);

        // Fixture S3 event
        // S3Event s3Event = objectMapper.readValue(getClass().getResourceAsStream("/s3_event_bad_data.json"), S3Event.class);
        S3Event s3Event = s3EventSerializer.fromJson(getClass().getResourceAsStream("/s3_event_bad_data.json"));
        String bucket = s3Event.getRecords().get(0).getS3().getBucket().getName();
        String key = s3Event.getRecords().get(0).getS3().getObject().getKey();

        // Fixture S3 return value
        InputStream contentStream = getClass().getResourceAsStream(String.format("/%s", key));
        ResponseInputStream<GetObjectResponse> responseInputStream = 
            new ResponseInputStream<>(GetObjectResponse.builder().build(), contentStream);
            
        Mockito.when(mockS3.getObject(Mockito.any(GetObjectRequest.class)))
            .thenReturn(responseInputStream);

        // Fixture environment
        String topic = "test-topic";
        environmentVariables.set(BulkEventsLambda.FAN_OUT_TOPIC_ENV, topic);

        // Construct Lambda function class, and invoke handler with expected exception
        BulkEventsLambda lambda = new BulkEventsLambda(mockSNS, mockS3);

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            lambda.handler(s3Event);
        });

        Assertions.assertNotNull(thrown.getCause());
        Assertions.assertTrue(thrown.getCause() instanceof InvalidFormatException);
        String msg = thrown.getMessage();
        Assertions.assertTrue(msg.contains("Cannot deserialize value of type `java.lang.Long` from String \"Wrong data type\": not a valid `java.lang.Long` value"));
//        Assertions.assertTrue(msg.contains("Cannot deserialize value of type java.lang.Long from String \"Wrong data type\": not a valid java.lang.Long value"));
    }

    @Test
    public void testBadEnvironment() throws IOException {
        // Set up mock AWS SDK v2 clients
        SnsClient mockSNS = Mockito.mock(SnsClient.class);
        S3Client mockS3 = Mockito.mock(S3Client.class);

        // Fixture S3 event
        S3Event s3Event = objectMapper.readValue(getClass().getResourceAsStream("/s3_event.json"), S3Event.class);

        // Construct Lambda function class, and invoke handler with expected exception
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            BulkEventsLambda lambda = new BulkEventsLambda(mockSNS, mockS3);        
            lambda.handler(s3Event);
        });

        String msg = thrown.getMessage();
        Assertions.assertTrue(msg.contains("FAN_OUT_TOPIC must be set"));
    }
}