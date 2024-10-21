package book;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.LAMBDA;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

@Testcontainers
public class AbstractLambdaTest {
	public static final String DASHES = new String(new char[80]).replace("\0", "-");

	@Container
	protected static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
	           .withServices(LAMBDA);

	protected static LambdaClient  lambdaClient;

	   @BeforeAll
	public static void setup() {   	  
	       lambdaClient = LambdaClient.builder()
	       		 .endpointOverride(localStack.getEndpoint())
	       		 .credentialsProvider(
	       		        StaticCredentialsProvider.create(
	       		            AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())
	       		        )
	       		    )
	       		 .region(Region.of(localStack.getRegion())) 
	             .build();
	   }
	   
		protected String functionCode(String path) {
			File file = new File(path);
			String content = null;
			try (FileInputStream fis = new FileInputStream(file);) {
				content = new String(fis.readAllBytes(), StandardCharsets.UTF_8);

			} catch (IOException e) {
				e.printStackTrace();
			}

			return content;
		}
}
