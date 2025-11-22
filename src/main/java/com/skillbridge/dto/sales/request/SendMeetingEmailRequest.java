package com.skillbridge.dto.sales.request;

/**
 * Send Meeting Email Request DTO
 */
public class SendMeetingEmailRequest {
    private String onlineMtgLink;
    private String onlineMtgDateTime; // Format: YYYY/MM/DD HH:MM or YYYY-MM-DDTHH:mm

    public SendMeetingEmailRequest() {
    }

    public String getOnlineMtgLink() {
        return onlineMtgLink;
    }

    public void setOnlineMtgLink(String onlineMtgLink) {
        this.onlineMtgLink = onlineMtgLink;
    }

    public String getOnlineMtgDateTime() {
        return onlineMtgDateTime;
    }

    public void setOnlineMtgDateTime(String onlineMtgDateTime) {
        this.onlineMtgDateTime = onlineMtgDateTime;
    }
}

