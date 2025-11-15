package com.skillbridge.service.dashboard;

import com.skillbridge.dto.dashboard.response.*;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.entity.contract.ChangeRequest;
import com.skillbridge.entity.contract.Contract;
import com.skillbridge.entity.contract.SOWContract;
import com.skillbridge.entity.proposal.Proposal;
import com.skillbridge.repository.contact.ContactRepository;
import com.skillbridge.repository.contract.ChangeRequestRepository;
import com.skillbridge.repository.contract.ContractHistoryRepository;
import com.skillbridge.repository.contract.ContractRepository;
import com.skillbridge.repository.contract.SOWContractRepository;
import com.skillbridge.repository.proposal.ProposalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dashboard Service
 * Handles business logic for dashboard data aggregation
 */
@Service
@Transactional
public class DashboardService {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private SOWContractRepository sowContractRepository;

    @Autowired
    private ChangeRequestRepository changeRequestRepository;

    @Autowired
    private ContractHistoryRepository contractHistoryRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * Get dashboard summary statistics
     */
    public DashboardSummaryDTO getSummary(Integer clientUserId) {
        DashboardSummaryDTO summary = new DashboardSummaryDTO();

        // Get contacts summary
        List<Contact> contacts = contactRepository.findByClientUserId(clientUserId);
        long inprogressCount = contacts.stream()
            .filter(c -> "InProgress".equals(c.getStatus()))
            .count();
        long newCount = contacts.stream()
            .filter(c -> "New".equals(c.getStatus()))
            .count();
        summary.getContacts().setInprogress((int) inprogressCount);
        summary.getContacts().setNewCount((int) newCount);

        // Get proposals summary
        // Proposals are linked to contacts, so we need to get proposals through contacts
        List<Proposal> allProposals = new ArrayList<>();
        for (Contact contact : contacts) {
            List<Proposal> proposals = proposalRepository.findByContactId(contact.getId());
            allProposals.addAll(proposals);
        }
        long underReviewCount = allProposals.stream()
            .filter(p -> "under review".equalsIgnoreCase(p.getStatus()) || 
                        "Under Review".equalsIgnoreCase(p.getStatus()))
            .count();
        long reviewedCount = allProposals.stream()
            .filter(p -> "Reviewed".equalsIgnoreCase(p.getStatus()) || 
                        "reviewed".equalsIgnoreCase(p.getStatus()) ||
                        "Sent to client".equalsIgnoreCase(p.getStatus()))
            .count();
        summary.getProposals().setUnderReview((int) underReviewCount);
        summary.getProposals().setReviewed((int) reviewedCount);

        // Get contracts summary
        // Note: clientUserId is used as clientId for contracts
        Integer clientId = clientUserId;
        // Use repository queries instead of findAll() for better performance
        List<Contract> msaContracts = contractRepository.findAll().stream()
            .filter(c -> c.getClientId() != null && c.getClientId().equals(clientId))
            .collect(Collectors.toList());
        List<SOWContract> sowContracts = sowContractRepository.findAll().stream()
            .filter(c -> c.getClientId() != null && c.getClientId().equals(clientId))
            .collect(Collectors.toList());

        long activeCount = msaContracts.stream()
            .filter(c -> Contract.ContractStatus.Active.equals(c.getStatus()))
            .count();
        activeCount += sowContracts.stream()
            .filter(c -> SOWContract.SOWContractStatus.Active.equals(c.getStatus()))
            .count();

        long draftCount = msaContracts.stream()
            .filter(c -> Contract.ContractStatus.Draft.equals(c.getStatus()))
            .count();
        draftCount += sowContracts.stream()
            .filter(c -> SOWContract.SOWContractStatus.Draft.equals(c.getStatus()))
            .count();

        summary.getContracts().setActive((int) activeCount);
        summary.getContracts().setDraft((int) draftCount);

        // Get change requests summary
        List<ChangeRequest> allChangeRequests = new ArrayList<>();
        for (Contract contract : msaContracts) {
            List<ChangeRequest> crs = changeRequestRepository.findByContractIdOrderByCreatedAtDesc(contract.getId());
            allChangeRequests.addAll(crs);
        }
        for (SOWContract contract : sowContracts) {
            List<ChangeRequest> crs = changeRequestRepository.findBySowContractIdOrderByCreatedAtDesc(contract.getId());
            allChangeRequests.addAll(crs);
        }

        long underReviewCRCount = allChangeRequests.stream()
            .filter(cr -> "Under Review".equalsIgnoreCase(cr.getStatus()) ||
                         "under review".equalsIgnoreCase(cr.getStatus()))
            .count();
        long approvedCRCount = allChangeRequests.stream()
            .filter(cr -> "Approved".equalsIgnoreCase(cr.getStatus()) ||
                         "approved".equalsIgnoreCase(cr.getStatus()))
            .count();

        summary.getChangeRequests().setUnderReview((int) underReviewCRCount);
        summary.getChangeRequests().setApproved((int) approvedCRCount);

        return summary;
    }

    /**
     * Get recent activities
     */
    public ActivitiesResponseDTO getRecentActivities(Integer clientUserId, int limit) {
        List<ActivityDTO> activities = new ArrayList<>();

        // Get contact activities
        List<Contact> contacts = contactRepository.findByClientUserIdOrderByCreatedAtDesc(clientUserId);
        for (Contact contact : contacts) {
            if (contact.getCreatedAt() != null) {
                ActivityDTO activity = new ActivityDTO(
                    contact.getId(),
                    contact.getCreatedAt().format(DATE_FORMATTER),
                    String.format("Contact %s created", contact.getTitle() != null ? contact.getTitle() : "request"),
                    "CONTACT",
                    contact.getId(),
                    "contact"
                );
                activities.add(activity);
            }
        }

        // Get proposal activities
        for (Contact contact : contacts) {
            List<Proposal> proposals = proposalRepository.findByContactId(contact.getId());
            for (Proposal proposal : proposals) {
                if (proposal.getCreatedAt() != null) {
                    String description = String.format("Proposal %s sent for %s.",
                        proposal.getTitle() != null ? proposal.getTitle() : "v" + proposal.getId(),
                        contact.getTitle() != null ? contact.getTitle() : "project");
                    ActivityDTO activity = new ActivityDTO(
                        proposal.getId(),
                        proposal.getCreatedAt().format(DATE_FORMATTER),
                        description,
                        "PROPOSAL",
                        proposal.getId(),
                        "proposal"
                    );
                    activities.add(activity);
                }
            }
        }

        // Get contract activities
        Integer clientId = clientUserId;
        List<Contract> msaContracts = contractRepository.findAll().stream()
            .filter(c -> c.getClientId() != null && c.getClientId().equals(clientId))
            .collect(Collectors.toList());
        List<SOWContract> sowContracts = sowContractRepository.findAll().stream()
            .filter(c -> c.getClientId() != null && c.getClientId().equals(clientId))
            .collect(Collectors.toList());

        for (Contract contract : msaContracts) {
            if (contract.getCreatedAt() != null) {
                String description = String.format("Contract %s signed by client.",
                    contract.getContractName() != null ? contract.getContractName() : "MSA-" + contract.getId());
                ActivityDTO activity = new ActivityDTO(
                    contract.getId(),
                    contract.getCreatedAt().format(DATE_FORMATTER),
                    description,
                    "CONTRACT",
                    contract.getId(),
                    "contract"
                );
                activities.add(activity);
            }
        }

        for (SOWContract contract : sowContracts) {
            if (contract.getCreatedAt() != null) {
                String description = String.format("Contract %s signed by client.",
                    contract.getContractName() != null ? contract.getContractName() : "SOW-" + contract.getId());
                ActivityDTO activity = new ActivityDTO(
                    contract.getId(),
                    contract.getCreatedAt().format(DATE_FORMATTER),
                    description,
                    "CONTRACT",
                    contract.getId(),
                    "contract"
                );
                activities.add(activity);
            }
        }

        // Get change request activities
        List<ChangeRequest> allChangeRequests = new ArrayList<>();
        for (Contract contract : msaContracts) {
            List<ChangeRequest> crs = changeRequestRepository.findByContractIdOrderByCreatedAtDesc(contract.getId());
            allChangeRequests.addAll(crs);
        }
        for (SOWContract contract : sowContracts) {
            List<ChangeRequest> crs = changeRequestRepository.findBySowContractIdOrderByCreatedAtDesc(contract.getId());
            allChangeRequests.addAll(crs);
        }

        for (ChangeRequest cr : allChangeRequests) {
            if (cr.getCreatedAt() != null) {
                String statusText = cr.getStatus() != null ? " - " + cr.getStatus() : "";
                String description = String.format("CR-%s created (%s)%s.",
                    cr.getChangeRequestId() != null ? cr.getChangeRequestId() : String.valueOf(cr.getId()),
                    cr.getTitle() != null ? cr.getTitle() : "Change Request",
                    statusText);
                ActivityDTO activity = new ActivityDTO(
                    cr.getId(),
                    cr.getCreatedAt().format(DATE_FORMATTER),
                    description,
                    "CHANGE_REQUEST",
                    cr.getId(),
                    "changeRequest"
                );
                activities.add(activity);
            }
        }

        // Sort by date descending and limit
        activities.sort(Comparator.comparing(ActivityDTO::getDate).reversed());
        List<ActivityDTO> limitedActivities = activities.stream()
            .limit(limit)
            .collect(Collectors.toList());

        return new ActivitiesResponseDTO(limitedActivities, limitedActivities.size());
    }

    /**
     * Get alerts/notifications
     */
    public AlertsResponseDTO getAlerts(Integer clientUserId, int limit) {
        List<AlertDTO> alerts = new ArrayList<>();

        // Get change requests that need client decision
        Integer clientId = clientUserId;
        List<Contract> msaContracts = contractRepository.findAll().stream()
            .filter(c -> c.getClientId() != null && c.getClientId().equals(clientId))
            .collect(Collectors.toList());
        List<SOWContract> sowContracts = sowContractRepository.findAll().stream()
            .filter(c -> c.getClientId() != null && c.getClientId().equals(clientId))
            .collect(Collectors.toList());

        List<ChangeRequest> allChangeRequests = new ArrayList<>();
        for (Contract contract : msaContracts) {
            List<ChangeRequest> crs = changeRequestRepository.findByContractIdOrderByCreatedAtDesc(contract.getId());
            allChangeRequests.addAll(crs);
        }
        for (SOWContract contract : sowContracts) {
            List<ChangeRequest> crs = changeRequestRepository.findBySowContractIdOrderByCreatedAtDesc(contract.getId());
            allChangeRequests.addAll(crs);
        }

        for (ChangeRequest cr : allChangeRequests) {
            if ("Under Review".equalsIgnoreCase(cr.getStatus()) || 
                "under review".equalsIgnoreCase(cr.getStatus())) {
                String message = String.format("CR-%s needs client decision.",
                    cr.getChangeRequestId() != null ? cr.getChangeRequestId() : String.valueOf(cr.getId()));
                AlertDTO alert = new AlertDTO(
                    cr.getId(),
                    message,
                    "HIGH",
                    "CHANGE_REQUEST_DECISION",
                    cr.getId(),
                    "changeRequest"
                );
                alerts.add(alert);
            }
        }

        // Limit alerts
        List<AlertDTO> limitedAlerts = alerts.stream()
            .limit(limit)
            .collect(Collectors.toList());

        return new AlertsResponseDTO(limitedAlerts, limitedAlerts.size());
    }
}

