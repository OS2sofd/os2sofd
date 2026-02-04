package dk.digitalidentity.sofd.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AmazonConfig {

    @Autowired
    private SofdConfiguration sofdConfiguration;

    @Bean
    public S3Client s3Client() {
        String region = sofdConfiguration.getS3().getRegion();
        String accessKey = sofdConfiguration.getS3().getAWSAccessKeyId();
        String secretKey = sofdConfiguration.getS3().getAWSSecretAccessKey();

        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
    }
}
