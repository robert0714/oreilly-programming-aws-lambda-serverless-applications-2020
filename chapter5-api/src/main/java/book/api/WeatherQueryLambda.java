package book.api;
 
 
import software.amazon.awssdk.services.dynamodb.DynamoDbClient; 
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
 

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

//https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_dynamodb_code_examples.html
public class WeatherQueryLambda {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static DynamoDbClient dynamoDbClient= DynamoDbClient.builder().build(); 
    private final String tableName = System.getenv("LOCATIONS_TABLE");

    private static final String DEFAULT_LIMIT = "50";

    public ApiGatewayResponse handler(ApiGatewayRequest request) throws IOException {
        final String limitParam = request.queryStringParameters == null
                ? DEFAULT_LIMIT
                : request.queryStringParameters.getOrDefault("limit", DEFAULT_LIMIT);
        final int limit = Integer.parseInt(limitParam);

        final ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tableName)
                .limit(limit) 
                .build();
        final ScanResponse  scanResult = dynamoDbClient.scan(scanRequest);

        final List<WeatherEvent> events = scanResult.items().stream()
                .map(item -> new WeatherEvent(
                        item.get("locationName").s(),
                        Double.parseDouble(item.get("temperature").n()),
                        Long.parseLong(item.get("timestamp").n()),
                        Double.parseDouble(item.get("longitude").n()),
                        Double.parseDouble(item.get("latitude").n())
                ))
                .collect(Collectors.toList());

        final String json = objectMapper.writeValueAsString(events);

        return new ApiGatewayResponse(200, json);
    }
}
