package book.api;

 
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

//import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient; 
 

import java.io.IOException; 

//https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_dynamodb_code_examples.html
//https://github.com/aws/aws-sdk-java-v2/tree/master/services-custom/dynamodb-enhanced
public class WeatherEventLambda {
	
    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  
    private DynamoDbEnhancedClient enhancedClient= getDynamoDbEnhancedClient() ;
    
    private final String tableName = System.getenv("LOCATIONS_TABLE");

    public ApiGatewayResponse handler(ApiGatewayRequest request) throws IOException {
    	
        final WeatherEvent weatherEvent = objectMapper.readValue(request.body, WeatherEvent.class);         
        
        DynamoDbTable<WeatherEvent> mappedTable = enhancedClient
        	      .table(tableName, TableSchema.fromBean(WeatherEvent.class));
        
        mappedTable.putItem(weatherEvent);
        
        return new ApiGatewayResponse(200, weatherEvent.locationName);
    }

	public static DynamoDbEnhancedClient getDynamoDbEnhancedClient() {
//		DynamoDbEnhancedClient enhancedClient= DynamoDbEnhancedClient.builder().dynamoDbClient(getDynamoDbClient()).build(); 
// 		return enhancedClient;
		return DynamoDbEnhancedClient .create(); 
	}
//    public static DynamoDbClient getDynamoDbClient() {
//        DynamoDbClient ddb = DynamoDbClient.builder()
//          .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
//          .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
//          .overrideConfiguration(ClientOverrideConfiguration.builder() 
//            .build())
//          .build();
//        return ddb;
//    }
}