package com.skillbridge.service.proposal;

import com.skillbridge.dto.proposal.response.ProposalListItemDTO;
import com.skillbridge.dto.proposal.response.ProposalListResponse;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.entity.proposal.Proposal;
import com.skillbridge.repository.proposal.ProposalRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class ProposalsListService {

    private static final Logger log = LogManager.getLogger(ProposalsListService.class);
    @Autowired
    private ProposalRepository proposalRepository;

    public ProposalListResponse getProposalsList(
            Integer clientUserID,
            String search,
            String status,
            int page,
            int size
    ) {
        //Normalizing the input search string
        String searchQuery = null;
        if (search != null && !search.trim().isEmpty()) {
            searchQuery = search;
        }

        //Mapping frontend's status filter to those in backend
        String statusFilter = null;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("All")) {
            switch(status) {
                case "Under review":
                    statusFilter = "under review";
                    break;

                case "Request for change":
                    statusFilter = "Reject";
                    break;

                case "Approved":
                    statusFilter = "Approved";
                    break;

                default:
                    statusFilter = status.trim();
                    break;
            }
        }

        try {
            //Creating a pageable with sorting criterium of descending by "Created at" date
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            //Executing the search query
            Page<Proposal> proposalsPage = proposalRepository.findProposalsForClient(
                    clientUserID,
                    searchQuery,
                    statusFilter,
                    pageable
            );

            //Debugging proposals page object
            log.info("proposalsPage: {}", proposalsPage.getContent().size());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new ProposalListResponse();
    }

    /**
     * This method takes in a Proposal object and returns a Proposal ID with the format of P-YYYY-NN
     * YYYY : Year (4 digits)
     * NN : Sequential number within that year (2 digits, zero-padded)
     * @param proposal The input proposal object fetched from DB
     * @return String The proposal ID as a string of format P-YYYY-NN
     */
    private String generateProposalID(Proposal proposal) {
        if (proposal.getCreatedAt() == null || proposal.getID() == null) {
            //Fallback to a simpler format if lacking data
            if (proposal.getID() != null) {
                return "P-0000-" + String.format("%02d", proposal.getID());
            }

            return "P-0000-00";
        }

        //Sufficient data fetched, begin calculating
        int year = proposal.getCreatedAt().getYear();

        //Count proposals in the same year, with the ID less than or equal to the current ID
        long countInYear = proposalRepository.findAll().stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().getYear() == year)
                .filter(p -> p.getID() != null && p.getID() <= proposal.getID())
                .count();

        //Format: P-YYYY-NN
        return String.format("P-%d-%02d", year, countInYear);
    }

    /**
     * This method generates a contact ID based on the given Contact object
     * with a format of CT-YYYY-MM-DD-{clientUserID}-{contactID}
     * @param contact The input Contact object
     * @return String The contact ID displayed on the list with the format
     */
    private String generateContactID(Contact contact) {
        //If either of the created date, client user id & contact ID is null, fallback to the simpler format
        if (contact.getCreatedAt() == null || contact.getClientUserId() == null || contact.getId() == null) {
            if (contact.getId() != null) {
                return "CT-0000-00-00-0-" + contact.getId();
            }

            return "CT-0000-00-00-0-0";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dateString =  contact.getCreatedAt().format(formatter);

        return String.format("CT-%s-%d-%d", dateString, contact.getClientUserId(),  contact.getId());
    }

}
