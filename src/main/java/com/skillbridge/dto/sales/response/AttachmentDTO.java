package com.skillbridge.dto.sales.response;

public class AttachmentDTO {

    private String s3Key;
    private String fileName;
    private Long fileSize;

    public AttachmentDTO() {
    }

    public AttachmentDTO(String s3Key, String fileName, Long fileSize) {
        this.s3Key = s3Key;
        this.fileName = fileName;
        this.fileSize = fileSize;
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

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

}
