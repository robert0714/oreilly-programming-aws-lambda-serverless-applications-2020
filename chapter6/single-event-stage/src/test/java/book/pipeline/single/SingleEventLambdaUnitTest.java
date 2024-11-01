package book.pipeline.single;

import book.pipeline.common.WeatherEvent;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.Test; 

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
 

public class SingleEventLambdaUnitTest {
    @Test
    public void testReadWeatherEvent() {
        String message = "{\"locationName\":\"Brooklyn, NY\",\"temperature\":91.0,\"timestamp\":1564428897,\"longitude\":-73.99,\"latitude\":40.7}";

        SingleEventLambda lambda = new SingleEventLambda();
        WeatherEvent weatherEvent = lambda.readWeatherEvent(message);
        
        assertEquals("Brooklyn, NY", weatherEvent.locationName);
        assertEquals(91.0, weatherEvent.temperature, 0.0);
        assertEquals(1564428897L, weatherEvent.timestamp);
        assertEquals(40.7, weatherEvent.latitude, 0.0);
        assertEquals(-73.99, weatherEvent.longitude, 0.0);
    }

    @Test
    public void testReadWeatherEventBadData() {
        String message = "{\"locationName\":\"Brooklyn, NY\",\"temperature\":91.0,\"timestamp\":\"Wrong data type\",\"longitude\":-73.99,\"latitude\":40.7}";
        
        SingleEventLambda lambda = new SingleEventLambda();        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            lambda.readWeatherEvent(message);
        });
        
        assertTrue(exception.getCause() instanceof InvalidFormatException);
        String msg = exception.getMessage() ;
        Assertions.assertTrue(msg.contains("Cannot deserialize value of type `java.lang.Long` from String \"Wrong data type\": not a valid `java.lang.Long` value"));
    } 

    @Test
    public void testLogWeatherEvent() {
        WeatherEvent weatherEvent = new WeatherEvent();
        weatherEvent.locationName = "Foo, Bar";
        weatherEvent.latitude = 100.0;
        weatherEvent.longitude = -100.0;
        weatherEvent.temperature = 32.0;
        weatherEvent.timestamp = 0L;
        
        SingleEventLambda lambda = new SingleEventLambda();
        
        // Capture System.out
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(outContent));
        
        lambda.logWeatherEvent(weatherEvent);
        
        String console =  outContent.toString() ;
        Assertions.assertTrue(console.contains("Received weather event:"));        
        Assertions.assertTrue(console.contains("WeatherEvent{locationName='Foo, Bar', temperature=32.0, timestamp=0, longitude=-100.0, latitude=100.0}"));
         
        
        // Reset System.out
        System.setOut(System.out);
    }
}