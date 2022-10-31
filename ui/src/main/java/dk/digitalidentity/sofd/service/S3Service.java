package dk.digitalidentity.sofd.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;

@Service
public class S3Service {
	
	@Autowired
	private AmazonS3 amazonS3;

	public void upload(String bucket, String key, byte[] file) {
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(file.length);
		PutObjectRequest request = new PutObjectRequest(bucket, key, new ByteArrayInputStream(file), metadata);

		try {
			amazonS3.putObject(request);
		}
		catch (AmazonServiceException e) {
			throw new IllegalStateException("Failed to upload the file", e);
		}
	}

	public byte[] download(String bucket, String key) {

		try {
			S3Object object = amazonS3.getObject(bucket, key);
			S3ObjectInputStream objectContent = object.getObjectContent();
			return IOUtils.toByteArray(objectContent);
		}
		catch (AmazonServiceException | IOException e) {
			throw new IllegalStateException("Failed to download the file", e);
		}
	}

	public List<String> list(String bucket, String folder) {
		ListObjectsV2Request request = new ListObjectsV2Request().withBucketName(bucket).withPrefix(folder);
		ListObjectsV2Result result = amazonS3.listObjectsV2(request);
		return result.getObjectSummaries().stream().map(obj -> obj.getKey().replace(folder + "/", "")).collect(Collectors.toList());
	}
}
