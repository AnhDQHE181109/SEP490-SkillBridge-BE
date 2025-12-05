package com.skillbridge.controller.api.engineer;

import com.skillbridge.dto.engineer.request.SearchCriteria;
import com.skillbridge.dto.engineer.response.EngineerSearchResponse;
import com.skillbridge.service.engineer.EngineerSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EngineerSearchControllerTest {

    @Mock
    private EngineerSearchService engineerSearchService;

    @InjectMocks
    private EngineerSearchController controller;

    @Test
    @DisplayName("searchEngineers - builds SearchCriteria correctly and delegates to service")
    void testSearchEngineers_BuildsCriteria() {
        EngineerSearchResponse responseBody = new EngineerSearchResponse();
        when(engineerSearchService.searchEngineers(org.mockito.ArgumentMatchers.any(SearchCriteria.class)))
                .thenReturn(responseBody);

        ResponseEntity<EngineerSearchResponse> response = controller.searchEngineers(
                "q",
                List.of("Java", "React"),
                List.of("EN"),
                3,
                10,
                List.of("Senior"),
                List.of("Tokyo"),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(5000),
                true,
                1,
                50,
                "experience"
        );

        assertSame(responseBody, response.getBody());

        ArgumentCaptor<SearchCriteria> captor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(engineerSearchService).searchEngineers(captor.capture());
        SearchCriteria criteria = captor.getValue();
        assertEquals("q", criteria.getQuery());
        assertEquals(List.of("Java", "React"), criteria.getSkills());
        assertEquals(List.of("EN"), criteria.getLanguages());
        assertEquals(3, criteria.getExperienceMin());
        assertEquals(10, criteria.getExperienceMax());
        assertEquals(List.of("Senior"), criteria.getSeniority());
        assertEquals(List.of("Tokyo"), criteria.getLocation());
        assertEquals(BigDecimal.valueOf(1000), criteria.getSalaryMin());
        assertEquals(BigDecimal.valueOf(5000), criteria.getSalaryMax());
        assertEquals(true, criteria.getAvailability());
        assertEquals(1, criteria.getPage());
        assertEquals(50, criteria.getSize());
        assertEquals("experience", criteria.getSortBy());
    }

    @Test
    @DisplayName("getAvailableSkills - delegates to service")
    void testGetAvailableSkills() {
        List<String> skills = List.of("Java", "React");
        when(engineerSearchService.getAvailableSkills()).thenReturn(skills);

        ResponseEntity<List<String>> response = controller.getAvailableSkills();

        assertSame(skills, response.getBody());
    }

    @Test
    @DisplayName("getAvailableLocations - delegates to service")
    void testGetAvailableLocations() {
        List<String> locations = List.of("Tokyo", "Hanoi");
        when(engineerSearchService.getAvailableLocations()).thenReturn(locations);

        ResponseEntity<List<String>> response = controller.getAvailableLocations();

        assertSame(locations, response.getBody());
    }

    @Test
    @DisplayName("getAvailableSeniorities - delegates to service")
    void testGetAvailableSeniorities() {
        List<String> seniorities = List.of("Junior", "Senior");
        when(engineerSearchService.getAvailableSeniorities()).thenReturn(seniorities);

        ResponseEntity<List<String>> response = controller.getAvailableSeniorities();

        assertSame(seniorities, response.getBody());
    }
}


