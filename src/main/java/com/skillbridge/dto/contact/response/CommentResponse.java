package com.skillbridge.dto.contact.response;

/**
 * Comment Response DTO
 * Response DTO for adding a proposal comment
 */
public class CommentResponse {
    private boolean success;
    private String message;
    private Integer commentId;

    public CommentResponse() {
    }

    public CommentResponse(boolean success, String message, Integer commentId) {
        this.success = success;
        this.message = message;
        this.commentId = commentId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCommentId() {
        return commentId;
    }

    public void setCommentId(Integer commentId) {
        this.commentId = commentId;
    }
}

