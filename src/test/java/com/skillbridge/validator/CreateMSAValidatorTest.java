package com.skillbridge.validator;

import com.skillbridge.dto.sales.request.CreateMSARequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CreateMSAValidator
 * Tests validation logic for create MSA request fields
 */
class CreateMSAValidatorTest {

    private CreateMSAValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CreateMSAValidator();
    }

    @Test
    @DisplayName("Should validate create MSA request with valid data")
    void testValidCreateMSARequest() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setCurrency("JPY");
        request.setPaymentTerms("Net 30");
        request.setInvoicingCycle("Monthly");
        request.setBillingDay("1st of month");
        request.setTaxWithholding("10%");
        request.setIpOwnership("Client");
        request.setGoverningLaw("Japanese Law");
        request.setNote("Additional notes");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
        assertEquals("Validation passed", result.getMessage());
    }

    @Test
    @DisplayName("Should validate create MSA request with minimal valid data")
    void testValidMinimalData() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
        assertEquals("Validation passed", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with null client ID")
    void testNullClientId() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(null); // ❌ Null client ID
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Client ID is required", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with null assignee user ID")
    void testNullAssigneeUserId() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(null); // ❌ Null assignee user ID
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Assignee user ID is required", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with null client contact ID")
    void testNullClientContactId() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(null); // ❌ Null client contact ID
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Client contact ID is required", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with null landbridge contact ID")
    void testNullLandbridgeContactId() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(null); // ❌ Null landbridge contact ID
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("LandBridge contact ID is required", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with null effective start date")
    void testNullEffectiveStart() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart(null); // ❌ Null effective start date
        request.setEffectiveEnd("2025-12-31");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Effective start date is required", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with blank effective start date")
    void testBlankEffectiveStart() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart(""); // ❌ Blank effective start date
        request.setEffectiveEnd("2025-12-31");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Effective start date is required", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with null effective end date")
    void testNullEffectiveEnd() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd(null); // ❌ Null effective end date

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Effective end date is required", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with blank effective end date")
    void testBlankEffectiveEnd() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd(""); // ❌ Blank effective end date

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Effective end date is required", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with invalid effective start date format")
    void testInvalidEffectiveStartFormat() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("01-01-2025"); // ❌ Wrong format (DD-MM-YYYY)
        request.setEffectiveEnd("2025-12-31");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Effective start date must be in format YYYY-MM-DD", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with invalid effective end date format")
    void testInvalidEffectiveEndFormat() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("31-12-2025"); // ❌ Wrong format (DD-MM-YYYY)

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Effective end date must be in format YYYY-MM-DD", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with invalid date string")
    void testInvalidDateString() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-13-01"); // ❌ Invalid month (13)
        request.setEffectiveEnd("2025-12-31");

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Effective start date must be in format YYYY-MM-DD", result.getMessage());
    }

    @Test
    @DisplayName("Should accept valid date formats")
    void testValidDateFormats() {
        // Test various valid date formats
        String[] validStartDates = {
                "2025-01-01",
                "2025-12-31",
                "2024-02-29" // Leap year (2024 is a leap year)
        };

        String[] validEndDates = {
                "2025-12-31",
                "2026-01-01",
                "2024-12-31"
        };

        for (int i = 0; i < validStartDates.length; i++) {
            CreateMSARequest request = new CreateMSARequest();
            request.setClientId(1);
            request.setAssigneeUserId(2);
            request.setClientContactId(3);
            request.setLandbridgeContactId(4);
            request.setEffectiveStart(validStartDates[i]);
            request.setEffectiveEnd(validEndDates[i]);

            ValidationResult result = validator.validate(request);

            assertTrue(result.isValid(), "Date format should be valid: " + validStartDates[i] + " to " + validEndDates[i]);
        }
    }

    @Test
    @DisplayName("Should reject request when end date is before start date")
    void testEndDateBeforeStartDate() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-12-31");
        request.setEffectiveEnd("2025-01-01"); // ❌ End date before start date

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Effective end date must be after effective start date", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request when end date equals start date")
    void testEndDateEqualsStartDate() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-01-01"); // ❌ End date equals start date

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Effective end date must be after effective start date", result.getMessage());
    }

    @Test
    @DisplayName("Should accept valid currency codes")
    void testValidCurrencyCodes() {
        // Test valid currency codes (ISO 4217 format)
        String[] validCurrencies = {"JPY", "USD", "EUR", "GBP", "VND"};

        for (String currency : validCurrencies) {
            CreateMSARequest request = new CreateMSARequest();
            request.setClientId(1);
            request.setAssigneeUserId(2);
            request.setClientContactId(3);
            request.setLandbridgeContactId(4);
            request.setEffectiveStart("2025-01-01");
            request.setEffectiveEnd("2025-12-31");
            request.setCurrency(currency);

            ValidationResult result = validator.validate(request);

            assertTrue(result.isValid(), "Currency should be valid: " + currency);
        }
    }

    @Test
    @DisplayName("Should accept null currency")
    void testNullCurrency() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
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
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setCurrency(""); // ✅ Optional field

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should reject request with currency code exceeding maximum length")
    void testCurrencyCodeExceedingMaxLength() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setCurrency("A".repeat(17)); // ❌ Too long (17 characters, maximum is 16)

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Currency code must be less than 16 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with invalid currency format when exactly 3 characters")
    void testInvalidCurrencyFormat() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setCurrency("jpy"); // ❌ Lowercase, should be uppercase

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Currency code must be 3 uppercase letters (e.g., JPY, USD, EUR)", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with payment terms too long")
    void testPaymentTermsTooLong() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setPaymentTerms("A".repeat(129)); // ❌ Too long (129 characters, maximum is 128)

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Payment terms must be less than 128 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should accept valid payment terms")
    void testValidPaymentTerms() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setPaymentTerms("Net 30"); // ✅ Valid

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should accept null payment terms")
    void testNullPaymentTerms() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setPaymentTerms(null); // ✅ Optional field

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should reject request with invoicing cycle too long")
    void testInvoicingCycleTooLong() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setInvoicingCycle("A".repeat(65)); // ❌ Too long (65 characters, maximum is 64)

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Invoicing cycle must be less than 64 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should accept valid invoicing cycle")
    void testValidInvoicingCycle() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setInvoicingCycle("Monthly"); // ✅ Valid

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should reject request with billing day too long")
    void testBillingDayTooLong() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setBillingDay("A".repeat(65)); // ❌ Too long (65 characters, maximum is 64)

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Billing day must be less than 64 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with tax withholding too long")
    void testTaxWithholdingTooLong() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setTaxWithholding("A".repeat(17)); // ❌ Too long (17 characters, maximum is 16)

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Tax withholding must be less than 16 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with IP ownership too long")
    void testIpOwnershipTooLong() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setIpOwnership("A".repeat(129)); // ❌ Too long (129 characters, maximum is 128)

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("IP ownership must be less than 128 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with governing law too long")
    void testGoverningLawTooLong() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setGoverningLaw("A".repeat(65)); // ❌ Too long (65 characters, maximum is 64)

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Governing law must be less than 64 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should reject request with note too long")
    void testNoteTooLong() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setNote("A".repeat(1001)); // ❌ Too long (1001 characters, maximum is 1000)

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Note must be less than 1000 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should accept valid note")
    void testValidNote() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setNote("This is a valid note with important information."); // ✅ Valid

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should reject request with review notes too long")
    void testReviewNotesTooLong() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setReviewNotes("A".repeat(1001)); // ❌ Too long (1001 characters, maximum is 1000)

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Review notes must be less than 1000 characters", result.getMessage());
    }

    @Test
    @DisplayName("Should accept valid review notes")
    void testValidReviewNotes() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("2025-01-01");
        request.setEffectiveEnd("2025-12-31");
        request.setReviewNotes("Approved with minor comments."); // ✅ Valid

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should validate dates with leading/trailing spaces")
    void testDatesWithSpaces() {
        // Arrange
        CreateMSARequest request = new CreateMSARequest();
        request.setClientId(1);
        request.setAssigneeUserId(2);
        request.setClientContactId(3);
        request.setLandbridgeContactId(4);
        request.setEffectiveStart("  2025-01-01  "); // With spaces
        request.setEffectiveEnd("  2025-12-31  "); // With spaces

        // Act
        ValidationResult result = validator.validate(request);

        // Assert
        assertTrue(result.isValid(), "Dates with spaces should be trimmed and valid");
    }
}

