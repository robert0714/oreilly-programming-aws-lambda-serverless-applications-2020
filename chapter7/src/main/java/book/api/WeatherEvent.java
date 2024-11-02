package book.api;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey; 

@DynamoDbBean
public class WeatherEvent implements Serializable {
	
	private static final long serialVersionUID = 1L;
	  
	
	@JsonProperty("locationName")
    private String locationName;
	
	@JsonProperty("temperature")
    private Double temperature;
	
	@JsonProperty("timestamp")
    private Long timestamp;
	
	@JsonProperty("longitude")
    private Double longitude;
	
	@JsonProperty("latitude")
    private Double latitude;

    public WeatherEvent() {
    }

    public WeatherEvent(String locationName, Double temperature, Long timestamp, Double longitude, Double latitude) {
        this.locationName = locationName;
        this.temperature = temperature;
        this.timestamp = timestamp;
        this.longitude = longitude;
        this.latitude = latitude;
    }
	
    @DynamoDbPartitionKey
	public String getLocationName() {
		return locationName;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
    
}
