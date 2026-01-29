package dk.digitalidentity.sofd.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
public class S3Service {

    @Autowired
    private S3Client s3Client;

    public void upload(String bucket, String key, byte[] file) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(file));
        }
        catch (S3Exception ex) {
            throw new IllegalStateException("Failed to upload the file", ex);
        }
    }

    public byte[] download(String bucket, String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request)) {
            return response.readAllBytes();
        }
        catch (S3Exception | IOException ex) {
            throw new IllegalStateException("Failed to download the file", ex);
        }
    }

    public List<String> list(String bucket, String folder) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(folder)
                .build();

        ListObjectsV2Response result = s3Client.listObjectsV2(request);

        return result.contents().stream()
                .map(S3Object::key)
                .map(k -> k.replace(folder + "/", ""))
                .collect(Collectors.toList());
    }
}