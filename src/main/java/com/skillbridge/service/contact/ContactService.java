package com.skillbridge.service.contact;

import com.skillbridge.dto.contact.request.ContactFormData;
import com.skillbridge.dto.contact.response.ContactSubmissionResponse;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.entity.contact.ContactStatusHistory;
import com.skillbridge.repository.auth.UserRepository;
import com.skillbridge.repository.contact.ContactRepository;
import com.skillbridge.repository.contact.ContactStatusHistoryRepository;
import com.skillbridge.service.auth.PasswordService;
import com.skillbridge.service.common.EmailService;
import com.skillbridge.service.common.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Contact Service
 * Handles business logic for contact form submissions
 */
@Service
@Transactional
public class ContactService {

    private static final Logger logger = LoggerFactory.getLogger(ContactService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ContactStatusHistoryRepository contactStatusHistoryRepository;

    @Autowired
    private PasswordService passwordService;

    /**
     * Process contact form submission
     * @param contactData Contact form data
     * @return ContactSubmissionResponse
     */
    public ContactSubmissionResponse processContactSubmission(ContactFormData contactData) {
        logger.info("Processing contact form submission for email: {}", contactData.getEmail());
        
        // Create or update user (with password generation for new users)
        UserPasswordResult userResult = createOrUpdateUser(contactData);

        // Create contact record
        Contact contact = createContactRecord(contactData, userResult.getUser());

        // Send confirmation email (commented out - will log instead)
        // Include password in email for new users
        emailService.sendConfirmationEmail(userResult.getUser(), contact, userResult.getPlainPassword());

        // Send notification email to Sales team
        emailService.sendNewContactNotificationEmail(contactData, contact.getId());

        // Notify sales manager (commented out - will log instead)
        notificationService.notifySalesManager(contact);

        // Log status change
        logStatusChange(contact, "Guest", "New");

        logger.info("Contact submission completed successfully. Contact ID: {}", contact.getId());
        return new ContactSubmissionResponse(true, "Contact submitted successfully", contact.getId());
    }

    /**
     * Create or update user from contact form data
     * Generate password for new users only
     * @param contactData Contact form data
     * @return UserPasswordResult containing User entity and plain password (null for existing users)
     */
    private UserPasswordResult createOrUpdateUser(ContactFormData contactData) {
        logger.debug("Checking if user exists with email: {}", contactData.getEmail());
        Optional<User> existingUser = userRepository.findByEmail(contactData.getEmail());

        if (existingUser.isPresent()) {
            // Update existing user (don't change password)
            logger.info("User already exists with email: {}. Updating user info", contactData.getEmail());
            User user = existingUser.get();
            user.setFullName(contactData.getName());
            user.setCompanyName(contactData.getCompanyName());
            user.setPhone(contactData.getPhone());
            user.setRole("CLIENT");
            user.setActive(true);
            userRepository.save(user);
            logger.info("Existing user updated successfully. User ID: {}", user.getId());
            return new UserPasswordResult(user, null);
        } else {
            // Create new user with auto-generated password
            logger.info("Creating new user account for email: {}", contactData.getEmail());
            User newUser = new User();
            newUser.setEmail(contactData.getEmail());
            newUser.setFullName(contactData.getName());
            newUser.setCompanyName(contactData.getCompanyName());
            newUser.setPhone(contactData.getPhone());
            newUser.setRole("CLIENT");
            newUser.setActive(true);

            // Generate random password
            String plainPassword = passwordService.generateRandomPassword();
            String hashedPassword = passwordService.hashPassword(plainPassword);
            newUser.setPassword(hashedPassword);
            
            // Store first password (plain text) in database for reference
            newUser.setFirstPassword(plainPassword);

            userRepository.save(newUser);
            logger.info("New user saved to database. User ID: {}, First password stored in database", newUser.getId());
            
            return new UserPasswordResult(newUser, plainPassword);
        }
    }

    /**
     * Inner class to return both User and plain password
     */
    private static class UserPasswordResult {
        private final User user;
        private final String plainPassword;

        public UserPasswordResult(User user, String plainPassword) {
            this.user = user;
            this.plainPassword = plainPassword;
        }

        public User getUser() {
            return user;
        }

        public String getPlainPassword() {
            return plainPassword;
        }
    }

    /**
     * Create contact record from form data
     * @param contactData Contact form data
     * @param user User entity
     * @return Contact entity
     */
    private Contact createContactRecord(ContactFormData contactData, User user) {
        Contact contact = new Contact();
        contact.setClientUserId(user.getId());
        contact.setTitle(contactData.getTitle() != null ? contactData.getTitle() : "Contact Request from " + contactData.getName());
        contact.setDescription(contactData.getMessage());
        contact.setStatus("New");
        contact.setRequestType("General Inquiry");
        contact.setPriority("Medium");
        contact.setCommunicationProgress("AutoReply");
        contact.setCreatedBy(user.getId());

        return contactRepository.save(contact);
    }

    /**
     * Log status change in history
     * @param contact Contact entity
     * @param fromStatus Previous status
     * @param toStatus New status
     */
    private void logStatusChange(Contact contact, String fromStatus, String toStatus) {
        ContactStatusHistory history = new ContactStatusHistory();
        history.setContactId(contact.getId());
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedBy(contact.getCreatedBy());
        contactStatusHistoryRepository.save(history);
    }
}

