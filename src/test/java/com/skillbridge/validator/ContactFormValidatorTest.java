package com.skillbridge.validator;

import com.skillbridge.dto.contact.request.ContactFormData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ContactFormValidator
 * Tests validation logic for contact form fields
 */
class ContactFormValidatorTest {

    private ContactFormValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ContactFormValidator();
    }

    @Test
    @DisplayName("Should validate contact form with valid data")
    void testValidContactForm() {
        // Arrange
        ContactFormData form = new ContactFormData();
        form.setName("John Doe");
        form.setCompanyName("ABC Company");
        form.setPhone("070-3345-3223");
        form.setEmail("john@example.com");
        form.setTitle("Project Consulting");
        form.setMessage("I am interested in your services. Please contact me.");

        // Act
        ValidationResult result = validator.validate(form);

        // Assert
        assertTrue(result.isValid());
        assertEquals("Validation passed", result.getMessage());
    }

    @Test
    @DisplayName("Should reject contact form with invalid phone format")
    void testInvalidPhoneFormat() {
        // Arrange
        ContactFormData form = new ContactFormData();
        form.setName("John Doe");
        form.setCompanyName("ABC Company");
        form.setPhone("abc123"); // ❌ Invalid: contains letters
        form.setEmail("john@example.com");
        form.setTitle("Project Consulting");
        form.setMessage("I am interested in your services.");

        // Act
        ValidationResult result = validator.validate(form);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Phone number format is invalid", result.getMessage());
    }

    @Test
    @DisplayName("Should reject contact form with phone containing invalid special characters")
    void testPhoneWithInvalidSpecialChars() {
        // Arrange
        ContactFormData form = new ContactFormData();
        form.setName("John Doe");
        form.setCompanyName("ABC Company");
        form.setPhone("070@3345#3223"); // ❌ Invalid: contains @ and #
        form.setEmail("john@example.com");
        form.setTitle("Project Consulting");
        form.setMessage("I am interested in your services.");

        // Act
        ValidationResult result = validator.validate(form);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Phone number format is invalid", result.getMessage());
    }

    @Test
    @DisplayName("Should accept phone number with valid formats")
    void testValidPhoneFormats() {
        // Test various valid phone formats
        String[] validPhones = {
                "07033453223",           // Digits only
                "070-3345-3223",         // With hyphens
                "+84 70 3345 3223",      // With plus and spaces
                "(070) 3345-3223",       // With parentheses and hyphen
                "+84-70-3345-3223"       // With plus and hyphens
        };

        for (String phone : validPhones) {
            ContactFormData form = new ContactFormData();
            form.setName("John Doe");
            form.setCompanyName("ABC Company");
            form.setPhone(phone);
            form.setEmail("john@example.com");
            form.setTitle("Project Consulting");
            form.setMessage("I am interested in your services.");

            ValidationResult result = validator.validate(form);

            assertTrue(result.isValid(), "Phone format should be valid: " + phone);
        }
    }

    @Test
    @DisplayName("Should reject contact form with blank phone number")
    void testBlankPhone() {
        // Arrange
        ContactFormData form = new ContactFormData();
        form.setName("John Doe");
        form.setCompanyName("ABC Company");
        form.setPhone(""); // ❌ Blank phone
        form.setEmail("john@example.com");
        form.setTitle("Project Consulting");
        form.setMessage("I am interested in your services.");

        // Act
        ValidationResult result = validator.validate(form);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Phone is required", result.getMessage());
    }

    @Test
    @DisplayName("Should reject contact form with blank name")
    void testBlankName() {
        // Arrange
        ContactFormData form = new ContactFormData();
        form.setName(""); // ❌ Blank name
        form.setCompanyName("ABC Company");
        form.setPhone("070-3345-3223");
        form.setEmail("john@example.com");
        form.setTitle("Project Consulting");
        form.setMessage("I am interested in your services.");

        // Act
        ValidationResult result = validator.validate(form);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Name is required", result.getMessage());
    }

    @Test
    @DisplayName("Should reject contact form with name too short")
    void testNameTooShort() {
        // Arrange
        ContactFormData form = new ContactFormData();
        form.setName("A"); // ❌ Too short (1 character, minimum is 2)
        form.setCompanyName("ABC Company");
        form.setPhone("070-3345-3223");
        form.setEmail("john@example.com");
        form.setTitle("Project Consulting");
        form.setMessage("I am interested in your services.");

        // Act
        ValidationResult result = validator.validate(form);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Name must be at least 2 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should reject contact form with name too long")
    void testNameTooLong() {
        // Arrange
        ContactFormData form = new ContactFormData();
        form.setName("A".repeat(101)); // ❌ Too long (101 characters, maximum is 100)
        form.setCompanyName("ABC Company");
        form.setPhone("070-3345-3223");
        form.setEmail("john@example.com");
        form.setTitle("Project Consulting");
        form.setMessage("I am interested in your services.");

        // Act
        ValidationResult result = validator.validate(form);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Name must be less than 100 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should reject contact form with blank company name")
    void testBlankCompanyName() {
        // Arrange
        ContactFormData form = new ContactFormData();
        form.setName("John Doe");
        form.setCompanyName(""); // ❌ Blank company name
        form.setPhone("070-3345-3223");
        form.setEmail("john@example.com");
        form.setTitle("Project Consulting");
        form.setMessage("I am interested in your services.");

        // Act
        ValidationResult result = validator.validate(form);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Company name is required", result.getMessage());
    }

    @Test
    @DisplayName("Should reject contact form with invalid email format")
    void testInvalidEmail() {
        // Arrange
        ContactFormData form = new ContactFormData();
        form.setName("John Doe");
        form.setCompanyName("ABC Company");
        form.setPhone("070-3345-3223");
        form.setEmail("invalid-email"); // ❌ Invalid email format
        form.setTitle("Project Consulting");
        form.setMessage("I am interested in your services.");

        // Act
        ValidationResult result = validator.validate(form);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Email must be valid", result.getMessage());
    }

    @Test
    @DisplayName("Should reject contact form with blank email")
    void testBlankEmail() {
        // Arrange
        ContactFormData form = new ContactFormData();
        form.setName("John Doe");
        form.setCompanyName("ABC Company");
        form.setPhone("070-3345-3223");
        form.setEmail(""); // ❌ Blank email
        form.setTitle("Project Consulting");
        form.setMessage("I am interested in your services.");

        // Act
        ValidationResult result = validator.validate(form);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Email is required", result.getMessage());
    }

    @Test
    @DisplayName("Should reject contact form with blank title")
    void testBlankTitle() {
        // Arrange
        ContactFormData form = new ContactFormData();
        form.setName("John Doe");
        form.setCompanyName("ABC Company");
        form.setPhone("070-3345-3223");
        form.setEmail("john@example.com");
        form.setTitle(""); // ❌ Blank title
        form.setMessage("I am interested in your services.");

        // Act
        ValidationResult result = validator.validate(form);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Title is required", result.getMessage());
    }

    @Test
    @DisplayName("Should reject contact form with title too short")
    void testTitleTooShort() {
        // Arrange
        ContactFormData form = new ContactFormData();
        form.setName("John Doe");
        form.setCompanyName("ABC Company");
        form.setPhone("070-3345-3223");
        form.setEmail("john@example.com");
        form.setTitle("A"); // ❌ Too short (1 character, minimum is 2)
        form.setMessage("I am interested in your services.");

        // Act
        ValidationResult result = validator.validate(form);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Title must be at least 2 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should reject contact form with message too short")
    void testMessageTooShort() {
        // Arrange
        ContactFormData form = new ContactFormData();
        form.setName("John Doe");
        form.setCompanyName("ABC Company");
        form.setPhone("070-3345-3223");
        form.setEmail("john@example.com");
        form.setTitle("Project Consulting");
        form.setMessage("Short"); // ❌ Too short (5 characters, minimum is 10)

        // Act
        ValidationResult result = validator.validate(form);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Message must be at least 10 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should reject contact form with blank message")
    void testBlankMessage() {
        // Arrange
        ContactFormData form = new ContactFormData();
        form.setName("John Doe");
        form.setCompanyName("ABC Company");
        form.setPhone("070-3345-3223");
        form.setEmail("john@example.com");
        form.setTitle("Project Consulting");
        form.setMessage(""); // ❌ Blank message

        // Act
        ValidationResult result = validator.validate(form);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Message is required", result.getMessage());
    }

    @Test
    @DisplayName("Should reject contact form with message too long")
    void testMessageTooLong() {
        // Arrange
        ContactFormData form = new ContactFormData();
        form.setName("John Doe");
        form.setCompanyName("ABC Company");
        form.setPhone("070-3345-3223");
        form.setEmail("john@example.com");
        form.setTitle("Project Consulting");
        form.setMessage("A".repeat(1001)); // ❌ Too long (1001 characters, maximum is 1000)

        // Act
        ValidationResult result = validator.validate(form);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Message must be less than 1000 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should validate name with leading/trailing spaces")
    void testNameWithSpaces() {
        // Arrange
        ContactFormData form = new ContactFormData();
        form.setName("  John Doe  "); // With spaces
        form.setCompanyName("ABC Company");
        form.setPhone("070-3345-3223");
        form.setEmail("john@example.com");
        form.setTitle("Project Consulting");
        form.setMessage("I am interested in your services.");

        // Act
        ValidationResult result = validator.validate(form);

        // Assert
        assertTrue(result.isValid(), "Name with spaces should be trimmed and valid");
    }
}

