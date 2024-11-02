package book.pipeline.bulk;

import book.pipeline.common.WeatherEvent;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Assertions;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
 
import java.io.InputStream;
import java.util.List;

@ExtendWith(SystemStubsExtension.class)
public class BulkEventsLambdaUnitTest {
    
    @SystemStub
    private EnvironmentVariables environmentVariables;

    @BeforeEach 
    public void before() {
        environmentVariables.set(BulkEventsLambda.FAN_OUT_TOPIC_ENV, "test-topic");
    }

    @Test
    public void testReadWeatherEvents() {
        // Fixture data
        InputStream inputStream = getClass().getResourceAsStream("/bulk_data.json");

        // Construct Lambda function class, and invoke
        BulkEventsLambda lambda = new BulkEventsLambda(null, null);
        List<WeatherEvent> weatherEvents = lambda.readWeatherEvents(inputStream);

        // Assert
        Assertions.assertEquals(3, weatherEvents.size());

        Assertions.assertEquals("Brooklyn, NY", weatherEvents.get(0).locationName);
        Assertions.assertEquals(91.0, weatherEvents.get(0).temperature, 0.0);
        Assertions.assertEquals(1564428897L, weatherEvents.get(0).timestamp);
        Assertions.assertEquals(40.7, weatherEvents.get(0).latitude, 0.0);
        Assertions.assertEquals(-73.99, weatherEvents.get(0).longitude, 0.0);

        Assertions.assertEquals("Oxford, UK", weatherEvents.get(1).locationName);
        Assertions.assertEquals(64.0, weatherEvents.get(1).temperature, 0.0);
        Assertions.assertEquals(1564428898L, weatherEvents.get(1).timestamp);
        Assertions.assertEquals(51.75, weatherEvents.get(1).latitude, 0.0);
        Assertions.assertEquals(-1.25, weatherEvents.get(1).longitude, 0.0);

        Assertions.assertEquals("Charlottesville, VA", weatherEvents.get(2).locationName);
        Assertions.assertEquals(87.0, weatherEvents.get(2).temperature, 0.0);
        Assertions.assertEquals(1564428899L, weatherEvents.get(2).timestamp);
        Assertions.assertEquals(38.02, weatherEvents.get(2).latitude, 0.0);
        Assertions.assertEquals(-78.47, weatherEvents.get(2).longitude, 0.0);
    }

    @Test
    public void testReadWeatherEventsBadData() {
        // Fixture data
        InputStream inputStream = getClass().getResourceAsStream("/bad_data.json");

        // Expect exception
        BulkEventsLambda lambda = new BulkEventsLambda(null, null);

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            lambda.readWeatherEvents(inputStream);
        });

        Assertions.assertNotNull(thrown.getCause());
        Assertions.assertTrue(thrown.getCause() instanceof InvalidFormatException);
        String msg = thrown.getMessage();
        Assertions.assertTrue(msg.contains("Cannot deserialize value of type `java.lang.Long` from String \"Wrong data type\": not a valid `java.lang.Long` value"));
    }

    @Test
    public void testWeatherEventToSnsMessage() {
        WeatherEvent weatherEvent = new WeatherEvent();
        weatherEvent.locationName = "Foo, Bar";
        weatherEvent.latitude = 100.0;
        weatherEvent.longitude = -100.0;
        weatherEvent.temperature = 32.0;
        weatherEvent.timestamp = 0L;

        BulkEventsLambda lambda = new BulkEventsLambda(null, null);
        String message = lambda.weatherEventToSnsMessage(weatherEvent);

        Assertions.assertEquals(
                "{\"locationName\":\"Foo, Bar\",\"temperature\":32.0,\"timestamp\":0,\"longitude\":-100.0,\"latitude\":100.0}",
                message);
    }
}