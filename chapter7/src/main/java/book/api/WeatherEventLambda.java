package book.api;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.io.IOException; 

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent; 

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import java.io.IOException;
import java.util.HashMap;
//import java.io.PrintWriter;
//import java.io.StringWriter;



//https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_dynamodb_code_examples.html
//https://github.com/aws/aws-sdk-java-v2/tree/master/services-custom/dynamodb-enhanced
public class WeatherEventLambda {

    private static Logger logger = LogManager.getLogger();

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private DDBUtils ddbUtils;
    private final String tableName = System.getenv("LOCATIONS_TABLE");

    public WeatherEventLambda(DDBUtils ddbUtils) {
        if (ddbUtils == null) {
          DynamoDbClient ddb = DDBUtils.getDynamoDbClient();
          DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(ddb)
            .build();
          this.ddbUtils = new DDBUtils(enhancedClient);
        } else {
          this.ddbUtils = ddbUtils;
        }
      }

      public WeatherEventLambda() {
        this(null);
      }
    
    
    public APIGatewayProxyResponseEvent handler(APIGatewayProxyRequestEvent request, Context context) throws IOException {

//        StringWriter stringWriter = new StringWriter();
//        Exception e = new Exception("Test exception");
//        e.printStackTrace(new PrintWriter(stringWriter));
//
//        System.err.println(String.format("System.err: %s", stringWriter.toString()));
//        context.getLogger().log(String.format("LambdaLogger: %s", stringWriter.toString()));
//
//        logger.info("Log4J logger");
//        logger.error("Log4J logger", e);

        final WeatherEvent weatherEvent = objectMapper.readValue(request.getBody(), WeatherEvent.class);

        String locationName =  this.ddbUtils.persistWeatherEvent(weatherEvent ,tableName);

        HashMap<Object, Object> message = new HashMap<>();
        message.put("action", "record");
        message.put("locationName", weatherEvent.getLocationName());
        message.put("temperature", weatherEvent.getTemperature());
        message.put("timestamp", weatherEvent.getTimestamp());

        logger.info(new ObjectMessage(message));

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(locationName);
    }
}