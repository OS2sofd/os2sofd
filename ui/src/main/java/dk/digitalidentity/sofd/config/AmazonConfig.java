package dk.digitalidentity.sofd.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Configuration
public class AmazonConfig {

	@Autowired
	private SofdConfiguration sofdConfiguration;
	
	@Bean
	public AmazonS3 s3() {
		String region = sofdConfiguration.getS3().getRegion();
		String accessKey = sofdConfiguration.getS3().getAWSAccessKeyId();
		String secretKey = sofdConfiguration.getS3().getAWSSecretAccessKey();

		AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
		return AmazonS3ClientBuilder
				.standard()
				.withRegion(region)
				.withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
				.build();
	}
}