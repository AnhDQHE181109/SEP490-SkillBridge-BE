package com.skillbridge.service.common;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

/**
 * S3 Service
 * Handles file uploads to AWS S3
 */
@Service
public class S3Service {

    @Autowired(required = false)
    private AmazonS3 amazonS3;

    @Value("${aws.s3.bucket-name:skillbridge-proposals}")
    private String bucketName;

    @Value("${aws.s3.enabled:false}")
    private boolean s3Enabled;

    /**
     * Upload file to S3
     * @param file MultipartFile to upload
     * @param folder Folder path in S3 (e.g., "proposals", "contracts")
     * @return S3 key of uploaded file (not URL, use getPresignedUrl to get download URL)
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        if (!s3Enabled || amazonS3 == null) {
            throw new RuntimeException("S3 is not configured or enabled");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        String s3Key = folder + "/" + uniqueFilename;

        // Upload to S3 (private, no public access)
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName,
                    s3Key,
                    inputStream,
                    metadata
            );

            amazonS3.putObject(putObjectRequest);

            // Return S3 key (not URL)
            return s3Key;
        }
    }

    /**
     * Generate presigned URL for downloading file from S3
     * @param s3Key S3 key of the file
     * @param expirationMinutes Expiration time in minutes (default 10)
     * @return Presigned URL
     */
    public String getPresignedUrl(String s3Key, int expirationMinutes) {
        if (!s3Enabled || amazonS3 == null) {
            throw new RuntimeException("S3 is not configured or enabled");
        }

        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000L * 60 * expirationMinutes; // Add expiration minutes
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, s3Key)
                .withMethod(HttpMethod.GET)
                .withExpiration(expiration);

        URL url = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }

    /**
     * Generate presigned URL with default 10 minutes expiration
     */
    public String getPresignedUrl(String s3Key) {
        return getPresignedUrl(s3Key, 10);
    }

    /**
     * Delete file from S3
     * @param s3Key S3 key of the file to delete
     */
    public void deleteFile(String s3Key) {
        if (!s3Enabled || amazonS3 == null) {
            return;
        }

        try {
            amazonS3.deleteObject(bucketName, s3Key);
        } catch (Exception e) {
            // Log error but don't throw
            System.err.println("Failed to delete file from S3: " + s3Key);
            e.printStackTrace();
        }
    }

    /**
     * Delete file from S3 by URL (for backward compatibility)
     * @param s3Url S3 URL of the file to delete
     */
    public void deleteFileByUrl(String s3Url) {
        if (!s3Enabled || amazonS3 == null) {
            return;
        }

        try {
            // Extract key from URL
            String key = s3Url.substring(s3Url.indexOf(bucketName) + bucketName.length() + 1);
            amazonS3.deleteObject(bucketName, key);
        } catch (Exception e) {
            // Log error but don't throw
            System.err.println("Failed to delete file from S3: " + s3Url);
            e.printStackTrace();
        }
    }
}

