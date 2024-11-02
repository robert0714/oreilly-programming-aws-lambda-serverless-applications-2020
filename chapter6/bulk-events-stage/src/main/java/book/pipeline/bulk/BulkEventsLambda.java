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
import java.util.stream.Collectors;

public class BulkEventsLambda {
    static String FAN_OUT_TOPIC_ENV = "FAN_OUT_TOPIC";
    private final ObjectMapper objectMapper =
            new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final SnsClient snsClient;
    private final S3Client s3Client;
    private final String snsTopic;

    public BulkEventsLambda() {
        this( SnsClient.builder().build(), S3Client.builder().build());
    }

    public BulkEventsLambda(SnsClient sns, S3Client s3) {
        this.snsClient = sns;
        this.s3Client = s3;
        this.snsTopic = System.getenv(FAN_OUT_TOPIC_ENV);
        if (this.snsTopic == null) {
            throw new RuntimeException(String.format("%s must be set", FAN_OUT_TOPIC_ENV));
        }
    }

    public void handler(S3Event event) {
    	// Read and deserialize WeatherEvent objects from S3
    	List<WeatherEvent> events = event.getRecords().stream()
                .map(this::getObjectFromS3)
                .map(this::readWeatherEvents)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    	
    	// Serialize and publish WeatherEvent messages to SNS
        events.stream()
                .map(this::weatherEventToSnsMessage)
                .forEach(message -> publishToSns(snsTopic, message));
    	
    	System.out.println("Published " + events.size() + " weather events to SNS");
    } 
     
    private InputStream getObjectFromS3(S3EventNotification.S3EventNotificationRecord record) {
        String bucket = record.getS3().getBucket().getName();
        String key = record.getS3().getObject().getKey();
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return s3Client.getObject(getObjectRequest) ;
    }

    List<WeatherEvent> readWeatherEvents(InputStream inputStream) {
        try (InputStream is = inputStream) {
            return Arrays.asList(objectMapper.readValue(is, WeatherEvent[].class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    String weatherEventToSnsMessage(WeatherEvent weatherEvent) {
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
