package com.skillbridge.controller.client.proposal;

import com.skillbridge.dto.proposal.response.ProposalListResponse;
import com.skillbridge.service.proposal.ProposalListService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("/client")
@CrossOrigin(origins = "*")
public class ClientProposalController {

    private static final Logger log = LogManager.getLogger(ClientProposalController.class);
    @Autowired
    private ProposalListService proposalListService;

    @GetMapping("/proposals")
    public ResponseEntity<ProposalListResponse> getProposals(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestHeader(value = "X-User-Id", required = false) Integer userID
    ) {
        try {
            //Placeholder user ID - TODO: Implement fetching user ID from JWT token
            if (userID == null) {
                userID = 1;
            }

            ProposalListResponse proposalsListResponse = proposalListService.getProposalsList(
                    userID,
                    search,
                    status,
                    page,
                    size
            );

            return new ResponseEntity<>(proposalsListResponse, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());

            //Returning empty response to prevent frontend crash
            ProposalListResponse proposalsListResponse = new ProposalListResponse();
            proposalsListResponse.setProposalsList(new ArrayList<>());
            proposalsListResponse.setTotalItems((long) 0);
            proposalsListResponse.setCurrentPage(0);
            proposalsListResponse.setTotalPages(0);

            return new ResponseEntity<>(proposalsListResponse, HttpStatus.OK);
        }

    }

}
