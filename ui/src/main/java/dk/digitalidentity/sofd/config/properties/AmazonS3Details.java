package dk.digitalidentity.sofd.config.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AmazonS3Details {
	private String region = "eu-west-1";
	private String AWSAccessKeyId = "";
	private String AWSSecretAccessKey = "";
	private String BucketName = "";
	private String HistoricalReportsPath = "sofdreports";
}
