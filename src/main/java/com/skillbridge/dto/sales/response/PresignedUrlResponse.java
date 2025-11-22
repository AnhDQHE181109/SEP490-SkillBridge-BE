package com.skillbridge.dto.sales.response;

/**
 * Presigned URL Response DTO
 */
public class PresignedUrlResponse {
    private String presignedUrl;
    private String s3Key;
    private int expirationMinutes;

    public PresignedUrlResponse() {
    }

    public PresignedUrlResponse(String presignedUrl, String s3Key, int expirationMinutes) {
        this.presignedUrl = presignedUrl;
        this.s3Key = s3Key;
        this.expirationMinutes = expirationMinutes;
    }

    public String getPresignedUrl() {
        return presignedUrl;
    }

    public void setPresignedUrl(String presignedUrl) {
        this.presignedUrl = presignedUrl;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public int getExpirationMinutes() {
        return expirationMinutes;
    }

    public void setExpirationMinutes(int expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }
}

