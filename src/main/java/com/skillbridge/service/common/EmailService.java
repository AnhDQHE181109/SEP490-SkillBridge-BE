package com.skillbridge.service.common;

import com.skillbridge.entity.contact.Contact;
import com.skillbridge.entity.common.EmailTemplate;
import com.skillbridge.entity.auth.User;
import com.skillbridge.repository.common.EmailTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private EmailTemplateRepository emailTemplateRepository;

    @Value("${aws.ses.enabled:false}")
    private boolean sesEnabled;

    @Value("${aws.ses.from-email:noreply@skillbridge.com}")
    private String fromEmail;

    @Value("${aws.ses.from-name:SkillBridge Team}")
    private String fromName;

    @Value("${aws.ses.configuration-set:default}")
    private String configurationSet;

    /**
     * Send confirmation email to user
     * @param user User who submitted the contact form
     * @param contact Contact record
     * @param plainPassword Plain text password (null for existing users)
     */
    public void sendConfirmationEmail(User user, Contact contact, String plainPassword) {
        try {
            EmailTemplate template = emailTemplateRepository.findByTemplateName("contact_confirmation")
                .orElseThrow(() -> new RuntimeException("Email template not found"));

            String subject = template.getSubject();
            String body = template.getBody()
                .replace("{name}", user.getFullName() != null ? user.getFullName() : "")
                .replace("{company_name}", user.getCompanyName() != null ? user.getCompanyName() : "")
                .replace("{title}", contact.getTitle() != null ? contact.getTitle() : "");

            // Add password information for new users
            if (plainPassword != null) {
                body += "\n\nYour account has been created with the following credentials:\n";
                body += "Email: " + user.getEmail() + "\n";
                body += "Password: " + plainPassword + "\n";
                body += "Please change your password after your first login for security.";
            }

            // TODO: Uncomment when SES is configured
            // Send email via AWS SES
            /*
            if (sesEnabled && amazonSES != null) {
                SendEmailRequest request = new SendEmailRequest()
                    .withDestination(new Destination().withToAddresses(user.getEmail()))
                    .withMessage(new Message()
                        .withBody(new Body()
                            .withHtml(new Content()
                                .withCharset("UTF-8")
                                .withData(body)))
                        .withSubject(new Content()
                            .withCharset("UTF-8")
                            .withData(subject)))
                    .withSource(fromEmail)
                    .withConfigurationSetName(configurationSet);

                amazonSES.sendEmail(request);
                System.out.println("Confirmation email sent via SES to: " + user.getEmail());
            } else {
                System.out.println("SES is not enabled or configured. Email content prepared but not sent:");
                System.out.println("To: " + user.getEmail());
                System.out.println("Subject: " + subject);
                System.out.println("Body: " + body);
            }
            */

            // Log email content (for development/testing)
            System.out.println("=== Email Content Prepared (SES not enabled) ===");
            System.out.println("To: " + user.getEmail());
            System.out.println("Subject: " + subject);
            System.out.println("Body: " + body);
            if (plainPassword != null) {
                System.out.println("=== NEW USER ACCOUNT CREDENTIALS ===");
                System.out.println("Email: " + user.getEmail());
                System.out.println("Password: " + plainPassword);
                System.out.println("===================================");
            }
            System.out.println("================================================");

        } catch (Exception e) {
            // Log error but don't fail the contact submission
            System.err.println("Failed to prepare confirmation email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Build email content
     * @param subject Email subject
     * @param body Email body
     * @param user User
     * @param contact Contact
     * @return Email content
     */
    private String buildEmailContent(String subject, String body, User user, Contact contact) {
        // This method can be used to build HTML email templates
        return body;
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
}

