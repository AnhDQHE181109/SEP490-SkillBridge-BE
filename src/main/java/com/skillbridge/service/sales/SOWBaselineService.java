package com.skillbridge.service.sales;

import com.skillbridge.entity.contract.RetainerBillingBase;
import com.skillbridge.entity.contract.SOWEngagedEngineerBase;
import com.skillbridge.repository.contract.RetainerBillingBaseRepository;
import com.skillbridge.repository.contract.SOWEngagedEngineerBaseRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class SOWBaselineService {

    @Autowired
    private RetainerBillingBaseRepository retainerBillingBaseRepository;

    @Autowired
    private SOWEngagedEngineerBaseRepository sowEngagedEngineerBaseRepository;

    /**
     * Get baseline resources (engineers) for a SOW contract
     * @param sowContractId SOW contract ID
     * @return List of baseline engineers
     */
    public List<SOWEngagedEngineerBase> getBaselineResources(Integer sowContractId) {
        return sowEngagedEngineerBaseRepository.findBySowContractIdOrderByStartDateAsc(sowContractId);
    }

    /**
     * Get baseline billing for a SOW contract
     * @param sowContractId SOW contract ID
     * @return List of baseline billing
     */
    public List<RetainerBillingBase> getBaselineBilling(Integer sowContractId) {
        return retainerBillingBaseRepository.findBySowContractIdOrderByBillingMonthDesc(sowContractId);
    }

}
