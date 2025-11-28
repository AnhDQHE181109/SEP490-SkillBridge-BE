package com.skillbridge.service.sales;

import com.skillbridge.entity.contract.*;
import com.skillbridge.repository.contract.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Transactional
public class CREventService {

    private static final Logger logger = LoggerFactory.getLogger(CREventService.class);

    @Autowired
    private CRResourceEventRepository crResourceEventRepository;

    /**
     * Create resource event from Change Request
     * @param cr Change Request
     * @param action Action type (ADD, REMOVE, MODIFY)
     * @param engineerId Engineer ID (for MODIFY/REMOVE)
     * @param role Role
     * @param level Level
     * @param ratingOld Old rating (for MODIFY)
     * @param ratingNew New rating
     * @param unitRateOld Old unit rate (for MODIFY)
     * @param unitRateNew New unit rate
     * @param startDateOld Old start date (for MODIFY)
     * @param startDateNew New start date
     * @param endDateOld Old end date (for MODIFY)
     * @param endDateNew New end date
     * @param effectiveStart Effective start date
     * @return Created resource event
     */
    public CRResourceEvent createResourceEvent(
            ChangeRequest cr,
            CRResourceEvent.ResourceAction action,
            Integer engineerId,
            String role,
            String level,
            BigDecimal ratingOld,
            BigDecimal ratingNew,
            BigDecimal unitRateOld,
            BigDecimal unitRateNew,
            LocalDate startDateOld,
            LocalDate startDateNew,
            LocalDate endDateOld,
            LocalDate endDateNew,
            LocalDate effectiveStart) {

        CRResourceEvent event = new CRResourceEvent();
        event.setChangeRequestId(cr.getId());
        event.setAction(action);
        event.setEngineerId(engineerId);
        event.setRole(role);
        event.setLevel(level);
        event.setRatingOld(ratingOld);
        event.setRatingNew(ratingNew);
        event.setUnitRateOld(unitRateOld);
        event.setUnitRateNew(unitRateNew);
        event.setStartDateOld(startDateOld);
        event.setStartDateNew(startDateNew);
        event.setEndDateOld(endDateOld);
        event.setEndDateNew(endDateNew);
        event.setEffectiveStart(effectiveStart);

        return crResourceEventRepository.save(event);
    }

}
