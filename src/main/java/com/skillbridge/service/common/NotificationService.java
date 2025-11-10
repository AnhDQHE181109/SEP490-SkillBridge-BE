package com.skillbridge.service.common;

import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.repository.auth.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Notification Service
 * Handles notifications to sales managers
 */
@Service
public class NotificationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Notify sales manager about new contact
     * @param contact Contact record
     */
    public void notifySalesManager(Contact contact) {
        try {
            // Find sales manager
            List<User> salesManagers = userRepository.findByRole("SALES_MANAGER");
            if (salesManagers.isEmpty()) {
                // Fallback to admin users
                salesManagers = userRepository.findByRole("ADMIN");
            }

            if (!salesManagers.isEmpty()) {
                User salesManager = salesManagers.get(0);
                sendSalesNotification(salesManager, contact);
            } else {
                System.out.println("No sales manager or admin found to notify");
            }
        } catch (Exception e) {
            // Log error but don't fail the contact submission
            System.err.println("Failed to notify sales manager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send sales notification
     * @param salesManager Sales manager user
     * @param contact Contact record
     */
    private void sendSalesNotification(User salesManager, Contact contact) {
        // Log notification content (for development/testing)
        System.out.println("=== Sales Manager Notification Prepared (SES not enabled) ===");
        System.out.println("Sales Manager: " + salesManager.getEmail());
        System.out.println("Contact ID: " + contact.getId());
        if (contact.getClientUser() != null) {
            System.out.println("Client: " + contact.getClientUser().getFullName());
            System.out.println("Company: " + contact.getClientUser().getCompanyName());
        }
        System.out.println("==========================================================");
    }
}

