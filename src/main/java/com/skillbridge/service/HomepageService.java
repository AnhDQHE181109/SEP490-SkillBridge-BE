package com.skillbridge.service;

import com.skillbridge.dto.HomepageStatisticsDTO;
import org.springframework.stereotype.Service;

@Service
public class HomepageService {

    public HomepageStatisticsDTO getHomepageStatistics() {
        // Temporary placeholder values. Replace with real counts wired to repositories when available.
        return new HomepageStatisticsDTO(0L, 0L);
    }

}
