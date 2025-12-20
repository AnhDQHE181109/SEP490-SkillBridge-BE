package com.skillbridge.service.common;

import com.skillbridge.entity.common.EmailTemplate;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.entity.auth.User;
import com.skillbridge.dto.contact.request.ContactFormData;
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
     * Send welcome email to newly created user
     * @param user User entity
     * @param plainPassword Plain text initial password
     */
    public void sendWelcomeEmail(User user, String plainPassword) {
        try {
            String userName = user.getFullName() != null ? user.getFullName() : "User";
            String roleDisplay = "SALES_MANAGER".equals(user.getRole()) ? "Sale Manager" : 
                               "SALES_REP".equals(user.getRole()) ? "Sale Rep" : user.getRole();
            String loginUrl = baseUrl + "/admin/login";

            // Build email subject
            String subject = "Welcome to SkillBridge - Your Account Has Been Created";

            // Build email body
            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append("Dear ").append(userName).append(",\n\n");
            bodyBuilder.append("Welcome to SkillBridge! Your account has been created successfully.\n\n");
            
            // Account information
            bodyBuilder.append("Account Details:\n");
            bodyBuilder.append("- Name: ").append(userName).append("\n");
            bodyBuilder.append("- Role: ").append(roleDisplay).append("\n");
            bodyBuilder.append("- Email: ").append(user.getEmail()).append("\n\n");

            // Login credentials
            bodyBuilder.append("Login Credentials:\n");
            bodyBuilder.append("Login URL: ").append(loginUrl).append("\n");
            bodyBuilder.append("Email: ").append(user.getEmail()).append("\n");
            bodyBuilder.append("Initial Password: ").append(plainPassword).append("\n\n");

            // Security reminder
            bodyBuilder.append("IMPORTANT SECURITY REMINDER:\n");
            bodyBuilder.append("For security reasons, please change your password after your first login.\n\n");

            // Support information
            bodyBuilder.append("If you have any questions or need assistance, please contact our support team.\n\n");
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
                    System.out.println("Welcome email sent successfully to: " + user.getEmail());
                    System.out.println("From: " + fromEmailAddress);
                } catch (Exception e) {
                    // Log detailed error information
                    System.err.println("=== ERROR: Failed to send welcome email ===");
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
                    // This ensures user creation is not blocked by email sending failure
                }
            } else {
                System.out.println("JavaMailSender is not configured. Email will not be sent.");
            }

            // Log email content (for development/testing)
            System.out.println("=== Welcome Email ===");
            System.out.println("To: " + user.getEmail());
            System.out.println("Subject: " + subject);
            System.out.println("Body:\n" + body);
            System.out.println("\n=== NEW USER ACCOUNT CREDENTIALS ===");
            System.out.println("Email: " + user.getEmail());
            System.out.println("Password: " + plainPassword);
            System.out.println("Login URL: " + loginUrl);
            System.out.println("===================================");
            System.out.println("================================================");

        } catch (Exception e) {
            // Log error but don't fail the user creation
            System.err.println("Failed to prepare welcome email: " + e.getMessage());
            e.printStackTrace();
        }
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

    /**
     * Send new contact notification email to Sales team
     * @param contactData Contact form data
     * @param contactId Contact ID
     */
    public void sendNewContactNotificationEmail(ContactFormData contactData, Integer contactId) {
        try {
            String salesEmail = "khangnhhe160625@fpt.edu.vn";
            String clientName = contactData.getName() != null ? contactData.getName() : "Guest";
            String companyName = contactData.getCompanyName() != null ? contactData.getCompanyName() : "N/A";
            String clientEmail = contactData.getEmail() != null ? contactData.getEmail() : "N/A";
            String phone = contactData.getPhone() != null ? contactData.getPhone() : "N/A";
            String title = contactData.getTitle() != null ? contactData.getTitle() : "Contact Request";
            String message = contactData.getMessage() != null ? contactData.getMessage() : "No message provided";

            // Build email subject
            String subject = "New Contact Request - " + clientName + " (" + companyName + ")";

            // Build email body
            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append("Dear Sales Team,\n\n");
            bodyBuilder.append("A new contact request has been submitted through the Contact Us form.\n\n");
            
            // Contact information
            bodyBuilder.append("Contact Details:\n");
            bodyBuilder.append("- Name: ").append(clientName).append("\n");
            bodyBuilder.append("- Company: ").append(companyName).append("\n");
            bodyBuilder.append("- Email: ").append(clientEmail).append("\n");
            bodyBuilder.append("- Phone: ").append(phone).append("\n");
            bodyBuilder.append("- Title: ").append(title).append("\n");
            bodyBuilder.append("- Message: ").append(message).append("\n");
            bodyBuilder.append("- Contact ID: ").append(contactId).append("\n\n");

            bodyBuilder.append("Please review this contact request and follow up with the client as soon as possible.\n\n");
            bodyBuilder.append("Best regards,\n");
            bodyBuilder.append(smtpFromName).append("\n");
            bodyBuilder.append("SkillBridge System");

            String body = bodyBuilder.toString();

            // Send email if JavaMailSender is configured
            if (javaMailSender != null) {
                try {
                    SimpleMailMessage messageObj = new SimpleMailMessage();
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
                    messageObj.setFrom(fromEmailAddress);
                    messageObj.setTo(salesEmail);
                    messageObj.setSubject(subject);
                    messageObj.setText(body);
                    javaMailSender.send(messageObj);
                    System.out.println("New contact notification email sent successfully to Sales: " + salesEmail);
                    System.out.println("From: " + fromEmailAddress);
                } catch (Exception e) {
                    // Log detailed error information
                    System.err.println("=== ERROR: Failed to send new contact notification email ===");
                    System.err.println("To: " + salesEmail);
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
            System.out.println("=== New Contact Notification Email ===");
            System.out.println("To: " + salesEmail);
            System.out.println("Subject: " + subject);
            System.out.println("Body:\n" + body);
            System.out.println("================================================");

        } catch (Exception e) {
            // Log error but don't fail the contact submission
            System.err.println("Failed to prepare new contact notification email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send new proposal notification email to Client
     * @param clientUser Client user entity
     * @param proposalTitle Proposal title
     * @param proposalLink Proposal link (S3 key or URL)
     * @param opportunityTitle Opportunity title (optional)
     */
    public void sendNewProposalNotificationEmail(User clientUser, String proposalTitle, String proposalLink, String opportunityTitle) {
        try {
            String clientName = clientUser.getFullName() != null ? clientUser.getFullName() : "Client";
            String clientEmail = clientUser.getEmail() != null ? clientUser.getEmail() : null;
            
            if (clientEmail == null || clientEmail.isEmpty()) {
                System.err.println("Client email is missing. Cannot send proposal notification email.");
                return;
            }

            // Build email subject
            String subject = "New Proposal Available - " + proposalTitle;

            // Build email body
            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append("Dear ").append(clientName).append(",\n\n");
            bodyBuilder.append("We are pleased to inform you that a new proposal has been approved and is now available for your review.\n\n");
            
            // Proposal information
            bodyBuilder.append("Proposal Details:\n");
            bodyBuilder.append("- Title: ").append(proposalTitle).append("\n");
            if (opportunityTitle != null && !opportunityTitle.isEmpty()) {
                bodyBuilder.append("- Opportunity: ").append(opportunityTitle).append("\n");
            }
            bodyBuilder.append("\n");

            // Login information
            String loginUrl = baseUrl + "/client/login";
            bodyBuilder.append("Please log in to your account to review the proposal:\n");
            bodyBuilder.append(loginUrl).append("\n\n");

            bodyBuilder.append("You can access your proposals and provide feedback through your client portal.\n\n");
            bodyBuilder.append("If you have any questions, please feel free to contact us.\n\n");
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
                    message.setTo(clientEmail);
                    message.setSubject(subject);
                    message.setText(body);
                    javaMailSender.send(message);
                    System.out.println("New proposal notification email sent successfully to Client: " + clientEmail);
                    System.out.println("From: " + fromEmailAddress);
                } catch (Exception e) {
                    // Log detailed error information
                    System.err.println("=== ERROR: Failed to send new proposal notification email ===");
                    System.err.println("To: " + clientEmail);
                    System.err.println("From: " + smtpFromEmail);
                    System.err.println("Error: " + e.getClass().getSimpleName());
                    System.err.println("Message: " + e.getMessage());
                    if (e.getCause() != null) {
                        System.err.println("Cause: " + e.getCause().getMessage());
                    }
                    e.printStackTrace();
                    System.err.println("================================================");
                    // Don't throw exception - just log and continue
                    // This ensures proposal approval is not blocked by email sending failure
                }
            } else {
                System.out.println("JavaMailSender is not configured. Email will not be sent.");
            }

            // Log email content (for development/testing)
            System.out.println("=== New Proposal Notification Email ===");
            System.out.println("To: " + clientEmail);
            System.out.println("Subject: " + subject);
            System.out.println("Body:\n" + body);
            System.out.println("================================================");

        } catch (Exception e) {
            // Log error but don't fail the proposal approval
            System.err.println("Failed to prepare new proposal notification email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send contract pending approval notification email to Client
     * @param clientUser Client user entity
     * @param contractType Contract type ("MSA" or "SOW")
     * @param contractName Contract name
     * @param contractId Contract ID (formatted, e.g., "MSA-2025-01" or "SOW-2025-01")
     */
    public void sendContractPendingApprovalNotificationEmail(User clientUser, String contractType, String contractName, String contractId) {
        try {
            String clientName = clientUser.getFullName() != null ? clientUser.getFullName() : "Client";
            String clientEmail = clientUser.getEmail() != null ? clientUser.getEmail() : null;
            
            if (clientEmail == null || clientEmail.isEmpty()) {
                System.err.println("Client email is missing. Cannot send contract pending approval notification email.");
                return;
            }

            // Build email subject
            String contractTypeDisplay = "MSA".equalsIgnoreCase(contractType) ? "MSA" : "SOW";
            String subject = "Contract Pending Your Approval - " + contractName + " (" + contractId + ")";

            // Build email body
            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append("Dear ").append(clientName).append(",\n\n");
            bodyBuilder.append("We are pleased to inform you that a new ").append(contractTypeDisplay).append(" contract has been approved by our Sales Manager and is now pending your review and approval.\n\n");
            
            // Contract information
            bodyBuilder.append("Contract Details:\n");
            bodyBuilder.append("- Type: ").append(contractTypeDisplay).append("\n");
            bodyBuilder.append("- Name: ").append(contractName != null ? contractName : "Contract").append("\n");
            bodyBuilder.append("- Contract ID: ").append(contractId != null ? contractId : "N/A").append("\n");
            bodyBuilder.append("\n");

            // Login information
            String loginUrl = baseUrl + "/client/login";
            bodyBuilder.append("Please log in to your account to review and approve the contract:\n");
            bodyBuilder.append(loginUrl).append("\n\n");

            bodyBuilder.append("You can access your contracts and provide approval through your client portal.\n\n");
            bodyBuilder.append("If you have any questions or need clarification, please feel free to contact us.\n\n");
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
                    message.setTo(clientEmail);
                    message.setSubject(subject);
                    message.setText(body);
                    javaMailSender.send(message);
                    System.out.println("Contract pending approval notification email sent successfully to Client: " + clientEmail);
                    System.out.println("From: " + fromEmailAddress);
                } catch (Exception e) {
                    // Log detailed error information
                    System.err.println("=== ERROR: Failed to send contract pending approval notification email ===");
                    System.err.println("To: " + clientEmail);
                    System.err.println("From: " + smtpFromEmail);
                    System.err.println("Error: " + e.getClass().getSimpleName());
                    System.err.println("Message: " + e.getMessage());
                    if (e.getCause() != null) {
                        System.err.println("Cause: " + e.getCause().getMessage());
                    }
                    e.printStackTrace();
                    System.err.println("================================================");
                    // Don't throw exception - just log and continue
                    // This ensures contract approval is not blocked by email sending failure
                }
            } else {
                System.out.println("JavaMailSender is not configured. Email will not be sent.");
            }

            // Log email content (for development/testing)
            System.out.println("=== Contract Pending Approval Notification Email ===");
            System.out.println("To: " + clientEmail);
            System.out.println("Subject: " + subject);
            System.out.println("Body:\n" + body);
            System.out.println("================================================");

        } catch (Exception e) {
            // Log error but don't fail the contract approval
            System.err.println("Failed to prepare contract pending approval notification email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send password reset email to user
     * @param user User entity
     * @param resetLink Password reset link with token
     * @param expirationMinutes Token expiration time in minutes
     */
    public void sendPasswordResetEmail(User user, String resetLink, int expirationMinutes) {
        try {
            String userName = user.getFullName() != null ? user.getFullName() : "User";

            // Try to get email template
            String subject = "Password Reset Request";
            String body = "Hello " + userName + ",\n\n" +
                    "You requested a password reset. Please click the link below to reset your password:\n\n" +
                    resetLink + "\n\n" +
                    "This link will expire in " + expirationMinutes + " minutes.\n\n" +
                    "If you did not request this, please ignore this email.\n\n" +
                    "Best regards,\nSkillBridge Team";

            if (emailTemplateRepository != null) {
                try {
                    EmailTemplate template = emailTemplateRepository
                            .findByTemplateName("password_reset")
                            .orElse(null);
                    
                    if (template != null) {
                        subject = template.getSubject();
                        body = template.getBody()
                                .replace("{name}", userName)
                                .replace("{reset_link}", resetLink)
                                .replace("{expiration_minutes}", String.valueOf(expirationMinutes));
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load email template, using default: " + e.getMessage());
                }
            }

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
                    System.out.println("Password reset email sent successfully to: " + user.getEmail());
                    System.out.println("From: " + fromEmailAddress);
                } catch (Exception e) {
                    // Log detailed error information
                    System.err.println("=== ERROR: Failed to send password reset email ===");
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
                    // This ensures password reset request is not blocked by email sending failure
                }
            } else {
                System.out.println("JavaMailSender is not configured. Email will not be sent.");
            }

            // Log email content (for development/testing)
            System.out.println("=== Password Reset Email ===");
            System.out.println("To: " + user.getEmail());
            System.out.println("Subject: " + subject);
            System.out.println("Body:\n" + body);
            System.out.println("Reset Link: " + resetLink);
            System.out.println("================================================");

        } catch (Exception e) {
            // Log error but don't fail the password reset request
            System.err.println("Failed to prepare password reset email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

