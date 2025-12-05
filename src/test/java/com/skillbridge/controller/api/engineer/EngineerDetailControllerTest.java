package com.skillbridge.controller.api.engineer;

import com.skillbridge.dto.engineer.response.EngineerDetailDTO;
import com.skillbridge.service.engineer.EngineerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EngineerDetailControllerTest {

    @Mock
    private EngineerService engineerService;

    @InjectMocks
    private EngineerDetailController controller;

    @Test
    @DisplayName("getEngineerById - returns 200 when engineer exists")
    void testGetEngineerById_Found() {
        EngineerDetailDTO dto = new EngineerDetailDTO();
        when(engineerService.getEngineerDetailById(1)).thenReturn(dto);

        ResponseEntity<EngineerDetailDTO> response = controller.getEngineerById(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    @DisplayName("getEngineerById - returns 404 when engineer is null")
    void testGetEngineerById_NotFound() {
        when(engineerService.getEngineerDetailById(1)).thenReturn(null);

        ResponseEntity<EngineerDetailDTO> response = controller.getEngineerById(1);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}


