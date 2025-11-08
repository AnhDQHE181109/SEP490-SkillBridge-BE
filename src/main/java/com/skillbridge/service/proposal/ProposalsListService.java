package com.skillbridge.service.proposal;

import com.skillbridge.repository.proposal.ProposalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProposalsListService {

    @Autowired
    private ProposalRepository proposalRepository;



}
