package book.pipeline.single;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class SingleEventLambdaFunctionalTest {
	
	private final ObjectMapper objectMapper = JsonMapper.builder()
			                                      .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES).
			                                     build().
			                                   registerModule(new JodaModule());

    private ByteArrayOutputStream outputStreamCaptor;
    private PrintStream standardOut;

    @BeforeEach
    void setUp() {    	 
        standardOut = System.out;
        outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @Test
    public void testHandler() throws IOException {

        // Fixture SNS event
        SNSEvent snsEvent = objectMapper.readValue(getClass().getResourceAsStream("/sns_event.json"), SNSEvent.class);

        // Construct Lambda function class, and invoke handler
        SingleEventLambda lambda = new SingleEventLambda();
        lambda.handler(snsEvent);

        String captor = outputStreamCaptor.toString();
        
        assertTrue(captor.contains(
        		"Received weather event:"
            ));
        assertTrue(captor.contains(
        		"WeatherEvent{locationName='Brooklyn, NY', temperature=91.0, timestamp=1564428897, longitude=-73.99, latitude=40.7}"
            )); 
    }

    @Test
    void testHandlerNoJackson() {
        // Fixture SNS content, record, and event
        SNSEvent.SNS snsContent = new SNSEvent.SNS()
                .withMessage("{\"locationName\":\"Brooklyn, NY\",\"temperature\":91.0,\"timestamp\":1564428897,\"longitude\":-73.99,\"latitude\":40.7}");
        SNSEvent.SNSRecord snsRecord = new SNSEvent.SNSRecord().withSns(snsContent);
        SNSEvent snsEvent = new SNSEvent().withRecords(Collections.singletonList(snsRecord));

        // Construct Lambda function class, and invoke handler
        SingleEventLambda lambda = new SingleEventLambda();
        lambda.handler(snsEvent);

        String output = outputStreamCaptor.toString() ;
        
        assertTrue(output.contains(
        		"Received weather event:"
            ));
        assertTrue(output.contains(
        		"WeatherEvent{locationName='Brooklyn, NY', temperature=91.0, timestamp=1564428897, longitude=-73.99, latitude=40.7}"
            ));
    }

    @Test
    public void testBadData() throws IOException {

        // Fixture SNS event
        SNSEvent snsEvent = objectMapper.readValue(getClass().getResourceAsStream("/sns_event_bad_data.json"), SNSEvent.class);

        // Construct Lambda function class, and verify exception
        SingleEventLambda lambda = new SingleEventLambda();
        
        Exception exception = assertThrows(RuntimeException.class, () -> lambda.handler(snsEvent));
        
        Throwable cause = exception.getCause();
        assertTrue(cause instanceof InvalidFormatException);
        
        assertTrue(exception.getMessage().contains(
        		"Cannot deserialize value of type `java.lang.Long` from String \"Wrong data type\": not a valid `java.lang.Long` value"
        ));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        System.setOut(standardOut);
    }
}