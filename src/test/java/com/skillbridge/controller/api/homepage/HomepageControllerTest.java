package com.skillbridge.controller.api.homepage;

import com.skillbridge.dto.common.HomepageStatistics;
import com.skillbridge.dto.engineer.response.EngineerProfile;
import com.skillbridge.service.common.HomepageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomepageControllerTest {

    @Mock
    private HomepageService homepageService;

    @InjectMocks
    private HomepageController controller;

    @Test
    @DisplayName("getHomepageStatistics - delegates to service and returns stats")
    void testGetHomepageStatistics() {
        HomepageStatistics stats = new HomepageStatistics(10L, 5L);
        when(homepageService.getHomepageStatistics()).thenReturn(stats);

        ResponseEntity<HomepageStatistics> response = controller.getHomepageStatistics();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(stats, response.getBody());
    }

    @Test
    @DisplayName("getHomepageEngineers - returns all category engineers")
    void testGetHomepageEngineers() {
        List<EngineerProfile> profiles = List.of(new EngineerProfile(), new EngineerProfile());
        when(homepageService.getAllCategoryEngineers()).thenReturn(profiles);

        ResponseEntity<List<EngineerProfile>> response = controller.getHomepageEngineers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(profiles, response.getBody());
    }

    @Test
    @DisplayName("getEngineersByCategory - returns engineers for given category")
    void testGetEngineersByCategory() {
        List<EngineerProfile> profiles = List.of(new EngineerProfile());
        when(homepageService.getEngineersByCategory("web")).thenReturn(profiles);

        ResponseEntity<List<EngineerProfile>> response = controller.getEngineersByCategory("web");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(profiles, response.getBody());
    }
}


