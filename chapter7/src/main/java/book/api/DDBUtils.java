package book.api;
 
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
 
/***
 * https://github.com/aws-samples/serverless-test-samples/blob/main/java-test-samples/apigw-lambda-ddb/TicketsFunction/src/main/java/com/example/utils/DDBUtils.java
 * ***/
public class DDBUtils {

  private DynamoDbEnhancedClient enhancedClient;

  public DDBUtils(DynamoDbEnhancedClient enhancedClient) {
    if (enhancedClient == null) {
      DynamoDbClient ddb = getDynamoDbClient();
      this.enhancedClient = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(ddb)
        .build();
    } else {
      this.enhancedClient = enhancedClient;
    }

  }

  public static DynamoDbClient getDynamoDbClient() {
    DynamoDbClient ddb = DynamoDbClient.builder()
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
      .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
      .overrideConfiguration(ClientOverrideConfiguration.builder() 
        .build())
      .build();
    return ddb;
  } 


  public String persistWeatherEvent(final WeatherEvent weatherEvent ,final  String tableName) {

    DynamoDbTable<WeatherEvent> mappedTable = enhancedClient
      .table(tableName, TableSchema.fromBean(WeatherEvent.class)); 

    mappedTable.putItem(weatherEvent);

    return weatherEvent.getLocationName();
  }
}