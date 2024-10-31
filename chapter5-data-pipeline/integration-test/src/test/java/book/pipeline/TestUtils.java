package book.pipeline;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;

import java.util.Arrays;
import java.util.Collections;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;

public class TestUtils {

    public static S3Event createS3Event(String bucketName, String key) {
        S3EventNotification.UserIdentityEntity userIdentity = 
            new S3EventNotification.UserIdentityEntity("EXAMPLE");
        
        S3EventNotification.S3BucketEntity bucket = 
            new S3EventNotification.S3BucketEntity(bucketName, userIdentity, "arn:aws:s3:::" + bucketName);
        
        S3EventNotification.S3ObjectEntity object = 
            new S3EventNotification.S3ObjectEntity(
                key,                           // key
                1024L,                         // size
                "d41d8cd98f00b204e9800998ecf8427e", // eTag
                "0A1B2C3D4E5F678901",         // versionId
                "1.0"                          // sequencer
            );
        
        S3EventNotification.S3Entity s3Entity = 
            new S3EventNotification.S3Entity(
                "1.0",                         // configurationId
                bucket,
                object,
                "arn:aws:s3:::" + bucketName + "/" + key // s3SchemaVersion
            );
        
        S3EventNotification.RequestParametersEntity requestParameters = 
            new S3EventNotification.RequestParametersEntity("127.0.0.1");
        
        S3EventNotification.ResponseElementsEntity responseElements = 
            new S3EventNotification.ResponseElementsEntity(
                "EXAMPLE123/5678abcdefghijklambdaisawesome/EXAMPLE",
                "EXAMPLE123/5678abcdefghijklambdaisawesome/EXAMPLE"
            );
        // 產生 ISO-8601 格式的時間戳
        String eventTime = Instant.now().toString();
        S3EventNotification.S3EventNotificationRecord record = 
            new S3EventNotification.S3EventNotificationRecord(
                "us-east-1",                   // awsRegion
                "ObjectCreated:Put",           // eventName
                "aws:s3",                      // eventSource
                eventTime , // eventTime
                "2.0",                         // eventVersion
                requestParameters,
                responseElements,
                s3Entity,
                userIdentity
            );

        return new S3Event(Collections.singletonList(record));
    }

    public static SNSEvent createSNSEvent(String topicArn, String message) {
        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setTopicArn(topicArn);
        sns.setMessage(message);
        sns.setType("Notification");
        sns.setSubject("Test Subject");
        sns.setTimestamp(org.joda.time.DateTime.now() );
        sns.setSignatureVersion("1");
        sns.setSignature("EXAMPLE");
        sns.setMessageId("95df01b4-ee98-5cb9-9903-4c221d41eb5e");

        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        record.setSns(sns);
        record.setEventVersion("1.0");
        record.setEventSource("aws:sns");
        record.setEventSubscriptionArn(topicArn);

        SNSEvent event = new SNSEvent();
        event.setRecords(Collections.singletonList(record));

        return event;
    }
}