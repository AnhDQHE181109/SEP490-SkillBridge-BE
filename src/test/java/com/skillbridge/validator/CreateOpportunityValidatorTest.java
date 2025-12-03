package com.skillbridge.validator;

import com.skillbridge.dto.sales.request.CreateOpportunityRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CreateOpportunityValidator
 * Tests validation logic for create opportunity request fields
 */
class CreateOpportunityValidatorTest {

    private CreateOpportunityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CreateOpportunityValidator();
    }

    @Test
    @DisplayName("Should validate create opportunity request with valid data")
    void testValidCreateOpportunityRequest() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientCompany("ABC Company");
        request.setClientEmail("john@example.com");
        request.setAssigneeUserId(1);
        request.setProbability(75);
        request.setEstValue(new BigDecimal("1000000.00"));
        request.setCurrency("JPY");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
        assertEquals("Validation passed", result.getMessage());
    }

    @Test
    @DisplayName("Should validate create opportunity request with minimal valid data")
    void testValidMinimalData() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("john@example.com");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
        assertEquals("Validation passed", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with blank client name")
    void testBlankClientName() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName(""); // ❌ Blank client name
        request.setClientEmail("john@example.com");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Client name is required", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with null client name")
    void testNullClientName() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName(null); // ❌ Null client name
        request.setClientEmail("john@example.com");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Client name is required", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with client name too short")
    void testClientNameTooShort() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("A"); // ❌ Too short (1 character, minimum is 2)
        request.setClientEmail("john@example.com");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Client name must be at least 2 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with client name too long")
    void testClientNameTooLong() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("A".repeat(256)); // ❌ Too long (256 characters, maximum is 255)
        request.setClientEmail("john@example.com");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Client name must be less than 255 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should accept valid client company")
    void testValidClientCompany() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientCompany("ABC Company");
        request.setClientEmail("john@example.com");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should accept null client company")
    void testNullClientCompany() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientCompany(null); // ✅ Optional field
        request.setClientEmail("john@example.com");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should accept empty client company")
    void testEmptyClientCompany() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientCompany(""); // ✅ Optional field
        request.setClientEmail("john@example.com");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should reject request with client company too short")
    void testClientCompanyTooShort() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientCompany("A"); // ❌ Too short (1 character, minimum is 2)
        request.setClientEmail("john@example.com");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Client company must be at least 2 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with client company too long")
    void testClientCompanyTooLong() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientCompany("A".repeat(256)); // ❌ Too long (256 characters, maximum is 255)
        request.setClientEmail("john@example.com");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Client company must be less than 255 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with blank client email")
    void testBlankClientEmail() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail(""); // ❌ Blank email

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Client email is required", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with null client email")
    void testNullClientEmail() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail(null); // ❌ Null email

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Client email is required", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with invalid email format")
    void testInvalidEmailFormat() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("invalid-email"); // ❌ Invalid email format

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Client email must be valid", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with email missing @ symbol")
    void testEmailMissingAtSymbol() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("john.example.com"); // ❌ Missing @ symbol

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Client email must be valid", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with email missing domain")
    void testEmailMissingDomain() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("john@"); // ❌ Missing domain

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Client email must be valid", result.getMessage());
    }

    @Test
    @DisplayName("Should accept valid email formats")
    void testValidEmailFormats() {
        // Test various valid email formats
        String[] validEmails = {
                "john@example.com",
                "john.doe@example.com",
                "john+tag@example.co.jp",
                "user_name@example-domain.com"
        };

        for (String email : validEmails) {
            CreateOpportunityRequest request = new CreateOpportunityRequest();
            request.setClientName("John Doe");
            request.setClientEmail(email);

            ValidationResult result = validator.validate(request);

            assertTrue(result.isValid(), "Email format should be valid: " + email);
        }
    }

    @Test
    @DisplayName("Should reject request with client email too long")
    void testClientEmailTooLong() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("a".repeat(250) + "@example.com"); // ❌ Too long (> 255 characters)

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Client email must be less than 255 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should accept valid probability values")
    void testValidProbabilityValues() {
        // Test valid probability values (0-100)
        Integer[] validProbabilities = {0, 1, 50, 99, 100};

        for (Integer probability : validProbabilities) {
            CreateOpportunityRequest request = new CreateOpportunityRequest();
            request.setClientName("John Doe");
            request.setClientEmail("john@example.com");
            request.setProbability(probability);

            ValidationResult result = validator.validate(request);

            assertTrue(result.isValid(), "Probability should be valid: " + probability);
        }
    }

    @Test
    @DisplayName("Should accept null probability")
    void testNullProbability() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("john@example.com");
        request.setProbability(null); // ✅ Optional field

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should reject request with probability less than 0")
    void testProbabilityLessThanZero() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("john@example.com");
        request.setProbability(-1); // ❌ Less than 0

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Probability must be between 0 and 100", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with probability greater than 100")
    void testProbabilityGreaterThan100() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("john@example.com");
        request.setProbability(101); // ❌ Greater than 100

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Probability must be between 0 and 100", result.getMessage());
    }

    @Test
    @DisplayName("Should accept valid estimated value")
    void testValidEstimatedValue() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("john@example.com");
        request.setEstValue(new BigDecimal("1000000.50"));

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should accept zero estimated value")
    void testZeroEstimatedValue() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("john@example.com");
        request.setEstValue(BigDecimal.ZERO);

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should accept null estimated value")
    void testNullEstimatedValue() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("john@example.com");
        request.setEstValue(null); // ✅ Optional field

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should reject request with negative estimated value")
    void testNegativeEstimatedValue() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("john@example.com");
        request.setEstValue(new BigDecimal("-1000.00")); // ❌ Negative value

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Estimated value must be greater than or equal to 0", result.getMessage());
    }

    @Test
    @DisplayName("Should accept valid currency codes")
    void testValidCurrencyCodes() {
        // Test valid currency codes (ISO 4217 format)
        String[] validCurrencies = {"JPY", "USD", "EUR", "GBP", "VND"};

        for (String currency : validCurrencies) {
            CreateOpportunityRequest request = new CreateOpportunityRequest();
            request.setClientName("John Doe");
            request.setClientEmail("john@example.com");
            request.setCurrency(currency);

            ValidationResult result = validator.validate(request);

            assertTrue(result.isValid(), "Currency should be valid: " + currency);
        }
    }

    @Test
    @DisplayName("Should accept null currency")
    void testNullCurrency() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("john@example.com");
        request.setCurrency(null); // ✅ Optional field

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should accept empty currency")
    void testEmptyCurrency() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("john@example.com");
        request.setCurrency(""); // ✅ Optional field

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should reject request with invalid currency format")
    void testInvalidCurrencyFormat() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("john@example.com");
        request.setCurrency("jpy"); // ❌ Lowercase, should be uppercase

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Currency code must be 3 uppercase letters (e.g., JPY, USD, EUR)", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with currency code too short")
    void testCurrencyCodeTooShort() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("john@example.com");
        request.setCurrency("JP"); // ❌ Too short (2 characters, should be 3)

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Currency code must be 3 uppercase letters (e.g., JPY, USD, EUR)", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with currency code too long")
    void testCurrencyCodeTooLong() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("john@example.com");
        request.setCurrency("JPYY"); // ❌ Too long (4 characters, should be 3)

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Currency code must be 3 uppercase letters (e.g., JPY, USD, EUR)", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with currency code containing numbers")
    void testCurrencyCodeWithNumbers() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("john@example.com");
        request.setCurrency("JP1"); // ❌ Contains number

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Currency code must be 3 uppercase letters (e.g., JPY, USD, EUR)", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with currency code exceeding maximum length")
    void testCurrencyCodeExceedingMaxLength() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("john@example.com");
        request.setCurrency("A".repeat(11)); // ❌ Too long (11 characters, maximum is 10)

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Currency code must be less than 10 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should validate client name with leading/trailing spaces")
    void testClientNameWithSpaces() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("  John Doe  "); // With spaces
        request.setClientEmail("john@example.com");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid(), "Client name with spaces should be trimmed and valid");
    }

    @Test
    @DisplayName("Should validate client email with leading/trailing spaces")
    void testClientEmailWithSpaces() {
        // Arrange
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setClientName("John Doe");
        request.setClientEmail("  john@example.com  "); // With spaces

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid(), "Client email with spaces should be trimmed and valid");
    }
}

