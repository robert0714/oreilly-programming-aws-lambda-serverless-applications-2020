package book.pipeline.bulk;

import book.pipeline.common.WeatherEvent;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper; 
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class BulkEventsLambda {
	private final ObjectMapper objectMapper;
    private final SnsClient snsClient;
    private final S3Client s3Client;
    private final String snsTopic;

    // Default constructor for AWS Lambda
    public BulkEventsLambda() {
        this(
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false),
            SnsClient.builder().build(),
            S3Client.builder().build(),
            System.getenv("FAN_OUT_TOPIC")
        );
    }

    // Constructor for testing
    public BulkEventsLambda(ObjectMapper objectMapper, SnsClient snsClient, S3Client s3Client, String snsTopic) {
        this.objectMapper = objectMapper;
        this.snsClient = snsClient;
        this.s3Client = s3Client;
        this.snsTopic = snsTopic;
    }

    public void handler(S3Event event) {
        event.getRecords().forEach(this::processS3EventRecord);
    }

    private void processS3EventRecord(S3EventNotification.S3EventNotificationRecord record) {
        final List<WeatherEvent> weatherEvents = readWeatherEventsFromS3(
                record.getS3().getBucket().getName(),
                record.getS3().getObject().getKey());

        weatherEvents.stream()
                .map(this::weatherEventToSnsMessage)
                .forEach(message -> publishToSns(snsTopic, message));

        System.out.println("Published " + weatherEvents.size() + " weather events to SNS");
    }

    private List<WeatherEvent> readWeatherEventsFromS3(String bucket, String key) {
    	GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        try (InputStream s3InputStream = s3Client.getObject(getObjectRequest)) {
            WeatherEvent[] weatherEvents = objectMapper.readValue(s3InputStream, WeatherEvent[].class);
            return Arrays.asList(weatherEvents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String weatherEventToSnsMessage(WeatherEvent weatherEvent) {
        try {
            return objectMapper.writeValueAsString(weatherEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void publishToSns(String topicArn, String message) {
        PublishRequest publishRequest = PublishRequest.builder()
                .topicArn(topicArn)
                .message(message)
                .build();

        snsClient.publish(publishRequest);
    }
}
