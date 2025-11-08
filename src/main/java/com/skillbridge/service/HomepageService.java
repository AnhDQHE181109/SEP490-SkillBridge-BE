package com.skillbridge.service;

import com.skillbridge.dto.HomepageStatisticsDTO;
import com.skillbridge.repository.EngineersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HomepageService {

    @Autowired
    private EngineersRepository engineersRepository;

    public HomepageStatisticsDTO getHomepageStatistics() {
        Long totalEngineers = engineersRepository.countEngineersByStatus("AVAILABLE");

        return new HomepageStatisticsDTO(totalEngineers, 0L);
    }

}
