package book.api;

 
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.io.IOException; 

//https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_dynamodb_code_examples.html
//https://github.com/aws/aws-sdk-java-v2/tree/master/services-custom/dynamodb-enhanced
public class WeatherEventLambda {
	
    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
   
    private final String tableName = System.getenv("LOCATIONS_TABLE");
    
    private DDBUtils ddbUtils;

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


    public ApiGatewayResponse handler(ApiGatewayRequest request) throws IOException {
    	
        final WeatherEvent weatherEvent = this.objectMapper.readValue(request.body, WeatherEvent.class); 
        
        String locationName =  this.ddbUtils.persistWeatherEvent(weatherEvent ,tableName);
        
        return new ApiGatewayResponse(200, locationName);
    } 
}