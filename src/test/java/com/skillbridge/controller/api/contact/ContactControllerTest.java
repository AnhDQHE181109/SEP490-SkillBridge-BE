package com.skillbridge.controller.api.contact;

import com.skillbridge.dto.contact.request.ContactFormData;
import com.skillbridge.dto.contact.response.ContactSubmissionResponse;
import com.skillbridge.service.contact.ContactService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactControllerTest {

    @Mock
    private ContactService contactService;

    @InjectMocks
    private ContactController contactController;

    @Test
    @DisplayName("submitContact - success returns 200 with response from service")
    void testSubmitContact_Success() {
        ContactFormData formData = new ContactFormData();
        ContactSubmissionResponse serviceResponse =
                new ContactSubmissionResponse(true, "OK", 1);

        when(contactService.processContactSubmission(formData)).thenReturn(serviceResponse);

        ResponseEntity<ContactSubmissionResponse> response =
                contactController.submitContact(formData);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ContactSubmissionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(true, body.isSuccess());
        assertEquals("OK", body.getMessage());
        verify(contactService).processContactSubmission(formData);
    }

    @Test
    @DisplayName("submitContact - IllegalArgumentException returns 400 with message")
    void testSubmitContact_IllegalArgumentException() {
        ContactFormData formData = new ContactFormData();

        when(contactService.processContactSubmission(formData))
                .thenThrow(new IllegalArgumentException("Invalid data"));

        ResponseEntity<ContactSubmissionResponse> response =
                contactController.submitContact(formData);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ContactSubmissionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.isSuccess());
        assertEquals("Invalid data", body.getMessage());
    }

    @Test
    @DisplayName("submitContact - generic exception returns 500 with generic message")
    void testSubmitContact_GenericException() {
        ContactFormData formData = new ContactFormData();

        when(contactService.processContactSubmission(formData))
                .thenThrow(new RuntimeException("DB down"));

        ResponseEntity<ContactSubmissionResponse> response =
                contactController.submitContact(formData);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ContactSubmissionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.isSuccess());
        assertEquals("Failed to process contact submission. Please try again later.",
                body.getMessage());
    }

    @Test
    @DisplayName("handleValidationErrors - builds message from field errors")
    void testHandleValidationErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error1 = new FieldError("contactFormData", "email", "Email invalid");
        FieldError error2 = new FieldError("contactFormData", "phone", "Phone invalid");

        when(bindingResult.getAllErrors()).thenReturn(List.of(error1, error2));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ContactSubmissionResponse> response =
                contactController.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ContactSubmissionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.isSuccess());
        // Order of errors in list is not guaranteed, just assert content
        String message = body.getMessage();
        assertEquals(true, message.startsWith("Validation failed:"));
        org.junit.jupiter.api.Assertions.assertTrue(
                message.contains("Email invalid") && message.contains("Phone invalid"));
    }
}


