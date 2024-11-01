package book.pipeline.bulk;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sns.AmazonSNS;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Assertions; 
import org.mockito.ArgumentCaptor; 
import org.mockito.Mockito;

import java.io.IOException;
 
@ExtendWith(SystemStubsExtension.class)
public class BulkEventsLambdaFunctionalTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @SystemStub
	private EnvironmentVariables environmentVariables;

     
    @Test 
    public void testHandler() throws IOException {

        // Set up mock AWS SDK clients
        AmazonSNS mockSNS = Mockito.mock(AmazonSNS.class);
        AmazonS3 mockS3 = Mockito.mock(AmazonS3.class);

        // Fixture S3 event
        S3Event s3Event = objectMapper.readValue(getClass().getResourceAsStream("/s3_event.json"), S3Event.class);
        String bucket = s3Event.getRecords().get(0).getS3().getBucket().getName();
        String key = s3Event.getRecords().get(0).getS3().getObject().getKey();

        // Fixture S3 return value
        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(getClass().getResourceAsStream(String.format("/%s", key)));
        Mockito.when(mockS3.getObject(bucket, key)).thenReturn(s3Object);

        // Fixture environment
        String topic = "test-topic";
        environmentVariables.set(BulkEventsLambda.FAN_OUT_TOPIC_ENV, topic);

        // Construct Lambda function class, and invoke handler
        BulkEventsLambda lambda = new BulkEventsLambda(mockSNS, mockS3);
        lambda.handler(s3Event);

        // Capture outbound SNS messages
        ArgumentCaptor<String> topics = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mockSNS, Mockito.times(3)).publish(topics.capture(), messages.capture());

        // Assert
        Assertions.assertArrayEquals(new String[]{"test-topic", "test-topic", "test-topic"}, topics.getAllValues().toArray());
        Assertions.assertArrayEquals(new String[]{
                "{\"locationName\":\"Brooklyn, NY\",\"temperature\":91.0,\"timestamp\":1564428897,\"longitude\":-73.99,\"latitude\":40.7}",
                "{\"locationName\":\"Oxford, UK\",\"temperature\":64.0,\"timestamp\":1564428898,\"longitude\":-1.25,\"latitude\":51.75}",
                "{\"locationName\":\"Charlottesville, VA\",\"temperature\":87.0,\"timestamp\":1564428899,\"longitude\":-78.47,\"latitude\":38.02}"
        }, messages.getAllValues().toArray());
    }

    @Test 
    public void testBadData() throws IOException {

        // Set up mock AWS SDK clients
        AmazonSNS mockSNS = Mockito.mock(AmazonSNS.class);
        AmazonS3 mockS3 = Mockito.mock(AmazonS3.class);

        // Fixture S3 event
        S3Event s3Event = objectMapper.readValue(getClass().getResourceAsStream("/s3_event_bad_data.json"), S3Event.class);
        String bucket = s3Event.getRecords().get(0).getS3().getBucket().getName();
        String key = s3Event.getRecords().get(0).getS3().getObject().getKey();

        // Fixture S3 return value
        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(getClass().getResourceAsStream(String.format("/%s", key)));
        Mockito.when(mockS3.getObject(bucket, key)).thenReturn(s3Object);

        // Construct Lambda function class, and invoke handler with expected exception

        // Fixture environment
        String topic = "test-topic";
        environmentVariables.set(BulkEventsLambda.FAN_OUT_TOPIC_ENV, topic);
        BulkEventsLambda lambda = new BulkEventsLambda(mockSNS, mockS3);

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            lambda.handler(s3Event);
        });

        Assertions.assertNotNull(thrown.getCause());
        Assertions.assertTrue(thrown.getCause() instanceof InvalidFormatException);
        String msg = thrown.getMessage() ;
        Assertions.assertTrue(msg.contains("Cannot deserialize value of type `java.lang.Long` from String \"Wrong data type\": not a valid `java.lang.Long` value"));
    }

    @Test
    public void testBadEnvironment() throws IOException { 

        // Set up mock AWS SDK clients
        AmazonSNS mockSNS = Mockito.mock(AmazonSNS.class);
        AmazonS3 mockS3 = Mockito.mock(AmazonS3.class);

        // Fixture S3 event
        S3Event s3Event = objectMapper.readValue(getClass().getResourceAsStream("/s3_event.json"), S3Event.class);

        // Construct Lambda function class, and invoke handler with expected exception
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
        	
            BulkEventsLambda lambda = new BulkEventsLambda(mockSNS, mockS3);        
            lambda.handler(s3Event);
        });

        String msg = thrown.getMessage() ;
        Assertions.assertTrue(msg.contains("FAN_OUT_TOPIC must be set"));
    }
}