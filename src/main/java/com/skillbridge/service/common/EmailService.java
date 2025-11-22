package com.skillbridge.service.common;

import com.skillbridge.entity.common.EmailTemplate;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.common.EmailTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;

/**
 * Email Service
 * Handles email sending using AWS SES
 */
@Service
public class EmailService {

    @Autowired(required = false)
    private AmazonSimpleEmailService amazonSES;

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    @Autowired(required = false)
    private EmailTemplateRepository emailTemplateRepository;

    @Value("${aws.ses.enabled:false}")
    private boolean sesEnabled;

    @Value("${aws.ses.from-email:noreply@skillbridge.com}")
    private String fromEmail;

    @Value("${email.from:support_skillbridge.inisoft.vn}")
    private String smtpFromEmail;

    @Value("${email.from-name:SkillBridge Support}")
    private String smtpFromName;

    @Value("${aws.ses.from-name:SkillBridge Team}")
    private String fromName;

    @Value("${aws.ses.configuration-set:default}")
    private String configurationSet;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    /**
     * Send confirmation email to user
     * @param user User who submitted the contact form
     * @param contact Contact record
     * @param plainPassword Plain text password (null for existing users)
     */
    public void sendConfirmationEmail(User user, Contact contact, String plainPassword) {
        try {
            String clientName = user.getFullName() != null ? user.getFullName() : "Client";
            String companyName = user.getCompanyName() != null ? user.getCompanyName() : "";
            String contactTitle = contact.getTitle() != null ? contact.getTitle() : "";
            String loginUrl = baseUrl + "/client/login";

            // Build email subject
            String subject = "Thank you for your contact request - SkillBridge";

            // Build email body
            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append("Dear ").append(clientName).append(",\n\n");
            bodyBuilder.append("Thank you for contacting SkillBridge. We have received your contact request and appreciate your interest in our services.\n\n");

            // Contact information
            bodyBuilder.append("Contact Details:\n");
            bodyBuilder.append("- Title: ").append(contactTitle).append("\n");
            if (!companyName.isEmpty()) {
                bodyBuilder.append("- Company: ").append(companyName).append("\n");
            }
            bodyBuilder.append("- Email: ").append(user.getEmail()).append("\n\n");

            // Account information for new users
            if (plainPassword != null) {
                bodyBuilder.append("Your account has been created successfully. Please use the following credentials to log in:\n\n");
                bodyBuilder.append("Login URL: ").append(loginUrl).append("\n");
                bodyBuilder.append("Email: ").append(user.getEmail()).append("\n");
                bodyBuilder.append("Initial Password: ").append(plainPassword).append("\n\n");
                bodyBuilder.append("For security reasons, please change your password after your first login.\n\n");
            } else {
                bodyBuilder.append("You can log in to your account using the following link:\n");
                bodyBuilder.append(loginUrl).append("\n\n");
            }

            bodyBuilder.append("Our team will review your request and get back to you as soon as possible.\n\n");
            bodyBuilder.append("Best regards,\n");
            bodyBuilder.append(smtpFromName).append("\n");
            bodyBuilder.append("SkillBridge Team");

            String body = bodyBuilder.toString();

            // Send email if JavaMailSender is configured
            if (javaMailSender != null) {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    // Validate and format from email properly
                    String fromEmailAddress = smtpFromEmail;
                    if (fromEmailAddress == null || fromEmailAddress.trim().isEmpty()) {
                        fromEmailAddress = "noreply@skillbridge.com";
                    } else if (!fromEmailAddress.contains("@")) {
                        // If from email doesn't have @, use a default format
                        fromEmailAddress = fromEmailAddress + "@skillbridge.com";
                    }
                    // Validate email format
                    if (!fromEmailAddress.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                        System.err.println("Invalid from email format: " + fromEmailAddress);
                        fromEmailAddress = "noreply@skillbridge.com";
                    }
                    message.setFrom(fromEmailAddress);
                    message.setTo(user.getEmail());
                    message.setSubject(subject);
                    message.setText(body);
                    javaMailSender.send(message);
                    System.out.println("Confirmation email sent successfully to: " + user.getEmail());
                    System.out.println("From: " + fromEmailAddress);
                } catch (Exception e) {
                    // Log detailed error information
                    System.err.println("=== ERROR: Failed to send confirmation email ===");
                    System.err.println("To: " + user.getEmail());
                    System.err.println("From: " + smtpFromEmail);
                    System.err.println("Error: " + e.getClass().getSimpleName());
                    System.err.println("Message: " + e.getMessage());
                    if (e.getCause() != null) {
                        System.err.println("Cause: " + e.getCause().getMessage());
                    }
                    e.printStackTrace();
                    System.err.println("================================================");
                    // Don't throw exception - just log and continue
                    // This ensures contact creation is not blocked by email sending failure
                }
            } else {
                System.out.println("JavaMailSender is not configured. Email will not be sent.");
            }

            // Log email content (for development/testing)
            System.out.println("=== Contact Confirmation Email ===");
            System.out.println("To: " + user.getEmail());
            System.out.println("Subject: " + subject);
            System.out.println("Body:\n" + body);
            if (plainPassword != null) {
                System.out.println("\n=== NEW USER ACCOUNT CREDENTIALS ===");
                System.out.println("Email: " + user.getEmail());
                System.out.println("Password: " + plainPassword);
                System.out.println("Login URL: " + loginUrl);
                System.out.println("===================================");
            }
            System.out.println("================================================");

        } catch (Exception e) {
            // Log error but don't fail the contact submission
            System.err.println("Failed to prepare confirmation email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Getters for use in NotificationService
    public boolean isSesEnabled() {
        return sesEnabled;
    }

    public AmazonSimpleEmailService getAmazonSES() {
        return amazonSES;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public EmailTemplateRepository getEmailTemplateRepository() {
        return emailTemplateRepository;
    }

    // Default constructor
    public EmailService() {
        // Default constructor for Spring
    }

    /**
     * Send meeting invitation email to client
     * @param clientEmail Client email address
     * @param clientName Client name
     * @param meetingLink Online meeting link (e.g., zoom.us/123124541141)
     * @param meetingDateTime Meeting date and time (format: YYYY/MM/DD HH:MM)
     */
    public void sendMeetingInvitation(String clientEmail, String clientName, String meetingLink, String meetingDateTime) {
        if (javaMailSender == null) {
            // Log email content if mail sender is not configured
            System.out.println("=== Meeting Invitation Email (Mail Sender not configured) ===");
            System.out.println("To: " + clientEmail);
            System.out.println("Subject: Meeting Invitation - SkillBridge");
            System.out.println("Body:");
            System.out.println("Dear " + (clientName != null ? clientName : "Client") + ",");
            System.out.println("");
            System.out.println("We would like to invite you to an online meeting.");
            System.out.println("");
            System.out.println("Meeting Link: " + meetingLink);
            System.out.println("Date & Time: " + meetingDateTime);
            System.out.println("");
            System.out.println("Best regards,");
            System.out.println("SkillBridge Support Team");
            System.out.println("================================================================");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(smtpFromEmail);
            message.setTo(clientEmail);
            message.setSubject("Meeting Invitation - SkillBridge");

            String body = "Dear " + (clientName != null ? clientName : "Client") + ",\n\n";
            body += "We would like to invite you to an online meeting.\n\n";
            body += "Meeting Link: " + meetingLink + "\n";
            body += "Date & Time: " + meetingDateTime + "\n\n";
            body += "Best regards,\n";
            body += smtpFromName;

            message.setText(body);
            javaMailSender.send(message);

            System.out.println("Meeting invitation email sent successfully to: " + clientEmail);
        } catch (Exception e) {
            System.err.println("Failed to send meeting invitation email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send meeting invitation email", e);
        }
    }
}

