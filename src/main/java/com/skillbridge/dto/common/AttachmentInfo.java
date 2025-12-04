package com.skillbridge.dto.common;

/**
 * Attachment Info DTO
 * Stores S3 key and original file name for attachments
 */
public class AttachmentInfo {
    private String s3Key;
    private String fileName;

    public AttachmentInfo() {
    }

    public AttachmentInfo(String s3Key, String fileName) {
        this.s3Key = s3Key;
        this.fileName = fileName;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
