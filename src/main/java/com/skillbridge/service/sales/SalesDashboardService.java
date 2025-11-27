package com.skillbridge.service.sales;

import com.skillbridge.dto.sales.response.*;
import com.skillbridge.entity.auth.User;
import com.skillbridge.entity.contact.Contact;
import com.skillbridge.entity.contract.ChangeRequest;
import com.skillbridge.entity.contract.Contract;
import com.skillbridge.entity.contract.SOWContract;
import com.skillbridge.entity.opportunity.Opportunity;
import com.skillbridge.entity.proposal.Proposal;
import com.skillbridge.repository.contact.ContactRepository;
import com.skillbridge.repository.contract.ChangeRequestRepository;
import com.skillbridge.repository.contract.ContractRepository;
import com.skillbridge.repository.contract.SOWContractRepository;
import com.skillbridge.repository.opportunity.OpportunityRepository;
import com.skillbridge.repository.proposal.ProposalRepository;
import com.skillbridge.repository.auth.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sales Dashboard Service
 * Handles business logic for Sales Dashboard data aggregation with role-based filtering
 */
@Service
@Transactional
public class SalesDashboardService {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private SOWContractRepository sowContractRepository;

    @Autowired
    private ChangeRequestRepository changeRequestRepository;

    @Autowired
    private UserRepository userRepository;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Check if user is Sales Manager
     */
    private boolean isSalesManager(User user) {
        return user != null && "SALES_MANAGER".equals(user.getRole());
    }

    /**
     * Get dashboard summary statistics
     * For Sales Manager: Returns all data
     * For Sales Rep: Returns only assigned data
     */
    public SalesDashboardSummaryDTO getSummary(User currentUser) {
        SalesDashboardSummaryDTO summary = new SalesDashboardSummaryDTO();
        boolean isManager = isSalesManager(currentUser);
        Integer assigneeUserId = isManager ? null : currentUser.getId();

        // Get contacts summary
        List<Contact> contacts = getContacts(assigneeUserId);
        summary.getContacts().setAll(contacts.size());
        long newCount = contacts.stream()
            .filter(c -> "New".equalsIgnoreCase(c.getStatus()))
            .count();
        summary.getContacts().setNewCount((int) newCount);

        // Get opportunities summary
        List<Opportunity> opportunities = getOpportunities(assigneeUserId);
        summary.getOpportunities().setAll(opportunities.size());
        long underReviewOppCount = opportunities.stream()
            .filter(o -> {
                String status = o.getStatus();
                return status != null && (
                    status.equalsIgnoreCase("PROPOSAL_DRAFTING") ||
                    status.equalsIgnoreCase("PROPOSAL_SENT") ||
                    status.equalsIgnoreCase("REVISION")
                );
            })
            .count();
        summary.getOpportunities().setUnderReview((int) underReviewOppCount);

        // Get proposals summary
        List<Proposal> proposals = getProposals(assigneeUserId);
        summary.getProposals().setAll(proposals.size());
        long underReviewPropCount = proposals.stream()
            .filter(p -> {
                String status = p.getStatus();
                return status != null && (
                    status.equalsIgnoreCase("under review") ||
                    status.equalsIgnoreCase("Under Review") ||
                    status.equalsIgnoreCase("revision_requested")
                );
            })
            .count();
        summary.getProposals().setUnderReview((int) underReviewPropCount);

        // Get contracts summary
        List<Contract> msaContracts = getMSAContracts(assigneeUserId);
        List<SOWContract> sowContracts = getSOWContracts(assigneeUserId);
        int totalContracts = msaContracts.size() + sowContracts.size();
        summary.getContracts().setAll(totalContracts);
        
        long underReviewContractCount = msaContracts.stream()
            .filter(c -> {
                Contract.ContractStatus status = c.getStatus();
                return status != null && (
                    status == Contract.ContractStatus.Under_Review ||
                    status == Contract.ContractStatus.Request_for_Change
                );
            })
            .count();
        underReviewContractCount += sowContracts.stream()
            .filter(c -> {
                SOWContract.SOWContractStatus status = c.getStatus();
                return status != null && (
                    status == SOWContract.SOWContractStatus.Under_Review ||
                    status == SOWContract.SOWContractStatus.Request_for_Change
                );
            })
            .count();
        summary.getContracts().setUnderReview((int) underReviewContractCount);

        // Get change requests summary
        List<ChangeRequest> changeRequests = getChangeRequests(assigneeUserId);
        summary.getChangeRequests().setAll(changeRequests.size());
        long underReviewCRCount = changeRequests.stream()
            .filter(cr -> {
                String status = cr.getStatus();
                return status != null && (
                    status.equalsIgnoreCase("Under Review") ||
                    status.equalsIgnoreCase("Client Under Review") ||
                    status.equalsIgnoreCase("Pending")
                );
            })
            .count();
        summary.getChangeRequests().setUnderReview((int) underReviewCRCount);

        // Get revenue (Sales Manager only)
        if (isManager) {
            List<SalesDashboardSummaryDTO.RevenueItem> revenue = calculateRevenue();
            summary.setRevenue(revenue);
        }

        return summary;
    }

    /**
     * Get approvals waiting from clients
     */
    public SalesDashboardApprovalsDTO getApprovalsWaiting(User currentUser) {
        SalesDashboardApprovalsDTO response = new SalesDashboardApprovalsDTO();
        boolean isManager = isSalesManager(currentUser);
        Integer assigneeUserId = isManager ? null : currentUser.getId();

        List<SalesDashboardApprovalsDTO.ApprovalItem> approvals = new ArrayList<>();

        // Get proposals waiting for client approval
        List<Proposal> proposals = getProposals(assigneeUserId);
        for (Proposal proposal : proposals) {
            String status = proposal.getStatus();
            if (status != null && (
                status.equalsIgnoreCase("sent_to_client") ||
                status.equalsIgnoreCase("revision_requested")
            )) {
                SalesDashboardApprovalsDTO.ApprovalItem item = new SalesDashboardApprovalsDTO.ApprovalItem();
                item.setId(proposal.getId());
                item.setEntityType("PROPOSAL");
                // Proposal doesn't have proposalId field, use ID or title
                item.setEntityNumber("P-" + proposal.getId());
                item.setEntityId(proposal.getId());
                
                // Get client name from contact or opportunity
                String clientName = "Unknown";
                if (proposal.getContact() != null && proposal.getContact().getClientUser() != null) {
                    clientName = proposal.getContact().getClientUser().getFullName() != null 
                        ? proposal.getContact().getClientUser().getFullName() 
                        : (proposal.getContact().getClientUser().getEmail() != null 
                            ? proposal.getContact().getClientUser().getEmail() 
                            : "Unknown");
                } else if (proposal.getOpportunity() != null) {
                    clientName = proposal.getOpportunity().getClientName() != null 
                        ? proposal.getOpportunity().getClientName() 
                        : "Unknown";
                }
                item.setClientName(clientName);
                
                item.setStatus("Sent");
                if (proposal.getCreatedAt() != null) {
                    item.setSentDate(proposal.getCreatedAt().format(DATE_FORMATTER));
                }
                
                String description = String.format("Proposal #%s - %s (Sent %s)",
                    item.getEntityNumber(),
                    item.getClientName(),
                    proposal.getCreatedAt() != null 
                        ? proposal.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM"))
                        : "");
                item.setDescription(description);
                
                approvals.add(item);
            }
        }

        // Get contracts waiting for client review
        List<Contract> msaContracts = getMSAContracts(assigneeUserId);
        for (Contract contract : msaContracts) {
            Contract.ContractStatus status = contract.getStatus();
            if (status == Contract.ContractStatus.Under_Review) {
                SalesDashboardApprovalsDTO.ApprovalItem item = new SalesDashboardApprovalsDTO.ApprovalItem();
                item.setId(contract.getId());
                item.setEntityType("MSA");
                // Generate contract ID: MSA-YYYY-NN
                String contractId = generateMSAContractId(contract.getId(), contract.getCreatedAt());
                item.setEntityNumber(contractId);
                item.setEntityId(contract.getId());
                
                // Get client name from clientId
                String clientName = "Unknown";
                if (contract.getClientId() != null) {
                    userRepository.findById(contract.getClientId())
                        .ifPresent(client -> {
                            if (client.getFullName() != null) {
                                item.setClientName(client.getFullName());
                            } else if (client.getEmail() != null) {
                                item.setClientName(client.getEmail());
                            } else {
                                item.setClientName("Unknown");
                            }
                        });
                } else {
                    item.setClientName("Unknown");
                }
                
                item.setStatus("Client Review");
                item.setDescription(String.format("MSA #%s - %s (Client Review)",
                    contractId,
                    item.getClientName()));
                approvals.add(item);
            }
        }

        List<SOWContract> sowContracts = getSOWContracts(assigneeUserId);
        for (SOWContract contract : sowContracts) {
            SOWContract.SOWContractStatus status = contract.getStatus();
            if (status == SOWContract.SOWContractStatus.Under_Review) {
                SalesDashboardApprovalsDTO.ApprovalItem item = new SalesDashboardApprovalsDTO.ApprovalItem();
                item.setId(contract.getId());
                item.setEntityType("SOW");
                // Generate contract ID: SOW-YYYY-MM-DD-NN
                String contractId = generateSOWContractId(contract.getId(), contract.getCreatedAt());
                item.setEntityNumber(contractId);
                item.setEntityId(contract.getId());
                
                // Get client name from clientId
                String clientName = "Unknown";
                if (contract.getClientId() != null) {
                    userRepository.findById(contract.getClientId())
                        .ifPresent(client -> {
                            if (client.getFullName() != null) {
                                item.setClientName(client.getFullName());
                            } else if (client.getEmail() != null) {
                                item.setClientName(client.getEmail());
                            } else {
                                item.setClientName("Unknown");
                            }
                        });
                } else {
                    item.setClientName("Unknown");
                }
                
                item.setStatus("Client Review");
                item.setDescription(String.format("SOW #%s - %s (Client Review)",
                    contractId,
                    item.getClientName()));
                approvals.add(item);
            }
        }

        // Sort by date (newest first)
        approvals.sort(Comparator.comparing((SalesDashboardApprovalsDTO.ApprovalItem a) -> {
            if (a.getSentDate() != null) {
                try {
                    return LocalDate.parse(a.getSentDate(), DATE_FORMATTER);
                } catch (Exception e) {
                    return LocalDate.MIN;
                }
            }
            return LocalDate.MIN;
        }).reversed());

        // Limit to 10 most recent
        if (approvals.size() > 10) {
            approvals = approvals.subList(0, 10);
        }

        response.setApprovals(approvals);
        return response;
    }

    /**
     * Get recent client activities
     */
    public SalesDashboardActivitiesDTO getRecentActivities(User currentUser) {
        SalesDashboardActivitiesDTO response = new SalesDashboardActivitiesDTO();
        boolean isManager = isSalesManager(currentUser);
        Integer assigneeUserId = isManager ? null : currentUser.getId();

        List<SalesDashboardActivitiesDTO.ActivityItem> activities = new ArrayList<>();

        // Get recent change requests submitted by clients
        List<ChangeRequest> changeRequests = getChangeRequests(assigneeUserId);
        for (ChangeRequest cr : changeRequests) {
            if (cr.getCreatedAt() != null) {
                SalesDashboardActivitiesDTO.ActivityItem item = new SalesDashboardActivitiesDTO.ActivityItem();
                item.setId(cr.getId());
                item.setEntityType("CHANGE_REQUEST");
                item.setEntityId(cr.getId());
                
                // Get client name from contract
                String clientName = "Unknown";
                if (cr.getSowContractId() != null) {
                    sowContractRepository.findById(cr.getSowContractId())
                        .ifPresent(contract -> {
                            if (contract.getClientId() != null) {
                                userRepository.findById(contract.getClientId())
                                    .ifPresent(client -> {
                                        if (client.getFullName() != null) {
                                            item.setClientName(client.getFullName());
                                        } else if (client.getEmail() != null) {
                                            item.setClientName(client.getEmail());
                                        } else {
                                            item.setClientName("Unknown");
                                        }
                                    });
                            } else {
                                item.setClientName("Unknown");
                            }
                        });
                } else if (cr.getContractId() != null) {
                    contractRepository.findById(cr.getContractId())
                        .ifPresent(contract -> {
                            if (contract.getClientId() != null) {
                                userRepository.findById(contract.getClientId())
                                    .ifPresent(client -> {
                                        if (client.getFullName() != null) {
                                            item.setClientName(client.getFullName());
                                        } else if (client.getEmail() != null) {
                                            item.setClientName(client.getEmail());
                                        } else {
                                            item.setClientName("Unknown");
                                        }
                                    });
                            } else {
                                item.setClientName("Unknown");
                            }
                        });
                }
                if (item.getClientName() == null || item.getClientName().isEmpty()) {
                    item.setClientName("Unknown");
                }
                
                item.setDescription(String.format("CR-%s submitted by %s",
                    cr.getChangeRequestId() != null ? cr.getChangeRequestId() : cr.getId(),
                    item.getClientName()));
                
                item.setTimestamp(cr.getCreatedAt().toString());
                item.setTimeAgo(calculateTimeAgo(cr.getCreatedAt()));
                
                activities.add(item);
            }
        }

        // Sort by timestamp (newest first)
        activities.sort(Comparator.comparing((SalesDashboardActivitiesDTO.ActivityItem a) -> {
            if (a.getTimestamp() != null) {
                try {
                    return LocalDateTime.parse(a.getTimestamp().replace("Z", ""));
                } catch (Exception e) {
                    return LocalDateTime.MIN;
                }
            }
            return LocalDateTime.MIN;
        }).reversed());

        // Limit to 10 most recent
        if (activities.size() > 10) {
            activities = activities.subList(0, 10);
        }

        response.setActivities(activities);
        return response;
    }

    // Helper methods for data retrieval with role-based filtering

    private List<Contact> getContacts(Integer assigneeUserId) {
        if (assigneeUserId == null) {
            return contactRepository.findAll();
        }
        return contactRepository.findAll().stream()
            .filter(c -> assigneeUserId.equals(c.getAssigneeUserId()))
            .collect(Collectors.toList());
    }

    private List<Opportunity> getOpportunities(Integer assigneeUserId) {
        if (assigneeUserId == null) {
            return opportunityRepository.findAll();
        }
        return opportunityRepository.findAll().stream()
            .filter(o -> assigneeUserId.equals(o.getAssigneeUserId()))
            .collect(Collectors.toList());
    }

    private List<Proposal> getProposals(Integer assigneeUserId) {
        List<Proposal> allProposals = proposalRepository.findAll();
        if (assigneeUserId == null) {
            return allProposals;
        }
        // Proposals are linked to opportunities, so filter through opportunities
        return allProposals.stream()
            .filter(p -> {
                if (p.getOpportunityId() != null) {
                    return opportunityRepository.findById(p.getOpportunityId())
                        .map(o -> assigneeUserId.equals(o.getAssigneeUserId()))
                        .orElse(false);
                }
                return false;
            })
            .collect(Collectors.toList());
    }

    private List<Contract> getMSAContracts(Integer assigneeUserId) {
        if (assigneeUserId == null) {
            return contractRepository.findAll();
        }
        return contractRepository.findAll().stream()
            .filter(c -> assigneeUserId.equals(c.getAssigneeUserId()))
            .collect(Collectors.toList());
    }

    private List<SOWContract> getSOWContracts(Integer assigneeUserId) {
        if (assigneeUserId == null) {
            return sowContractRepository.findAll();
        }
        return sowContractRepository.findAll().stream()
            .filter(c -> assigneeUserId.equals(c.getAssigneeUserId()))
            .collect(Collectors.toList());
    }

    private List<ChangeRequest> getChangeRequests(Integer assigneeUserId) {
        List<ChangeRequest> allCRs = changeRequestRepository.findAll();
        if (assigneeUserId == null) {
            return allCRs;
        }
        // Change requests are linked to contracts, so filter through contracts
        return allCRs.stream()
            .filter(cr -> {
                if (cr.getSowContractId() != null) {
                    return sowContractRepository.findById(cr.getSowContractId())
                        .map(c -> assigneeUserId.equals(c.getAssigneeUserId()))
                        .orElse(false);
                } else if (cr.getContractId() != null) {
                    return contractRepository.findById(cr.getContractId())
                        .map(c -> assigneeUserId.equals(c.getAssigneeUserId()))
                        .orElse(false);
                }
                return false;
            })
            .collect(Collectors.toList());
    }

    /**
     * Calculate revenue by month (Sales Manager only)
     */
    private List<SalesDashboardSummaryDTO.RevenueItem> calculateRevenue() {
        List<SalesDashboardSummaryDTO.RevenueItem> revenue = new ArrayList<>();
        
        // Get all active contracts
        List<Contract> msaContracts = contractRepository.findAll().stream()
            .filter(c -> c.getStatus() == Contract.ContractStatus.Active)
            .collect(Collectors.toList());
        
        List<SOWContract> sowContracts = sowContractRepository.findAll().stream()
            .filter(c -> c.getStatus() == SOWContract.SOWContractStatus.Active)
            .collect(Collectors.toList());

        // Calculate revenue for current month and next month
        LocalDate now = LocalDate.now();
        LocalDate currentMonth = now.withDayOfMonth(1);
        LocalDate nextMonth = currentMonth.plusMonths(1);

        // Current month revenue
        BigDecimal currentMonthRevenue = BigDecimal.ZERO;
        for (Contract contract : msaContracts) {
            if (contract.getValue() != null && contract.getPeriodStart() != null && contract.getPeriodEnd() != null) {
                if (!contract.getPeriodStart().isAfter(currentMonth.plusMonths(1).minusDays(1)) &&
                    !contract.getPeriodEnd().isBefore(currentMonth)) {
                    // Contract is active in current month
                    currentMonthRevenue = currentMonthRevenue.add(contract.getValue());
                }
            }
        }
        for (SOWContract contract : sowContracts) {
            if (contract.getValue() != null && contract.getPeriodStart() != null && contract.getPeriodEnd() != null) {
                if (!contract.getPeriodStart().isAfter(currentMonth.plusMonths(1).minusDays(1)) &&
                    !contract.getPeriodEnd().isBefore(currentMonth)) {
                    // Contract is active in current month
                    currentMonthRevenue = currentMonthRevenue.add(contract.getValue());
                }
            }
        }

        revenue.add(new SalesDashboardSummaryDTO.RevenueItem(
            currentMonth.format(MONTH_FORMATTER),
            currentMonthRevenue.longValue()
        ));

        // Next month revenue
        BigDecimal nextMonthRevenue = BigDecimal.ZERO;
        for (Contract contract : msaContracts) {
            if (contract.getValue() != null && contract.getPeriodStart() != null && contract.getPeriodEnd() != null) {
                if (!contract.getPeriodStart().isAfter(nextMonth.plusMonths(1).minusDays(1)) &&
                    !contract.getPeriodEnd().isBefore(nextMonth)) {
                    nextMonthRevenue = nextMonthRevenue.add(contract.getValue());
                }
            }
        }
        for (SOWContract contract : sowContracts) {
            if (contract.getValue() != null && contract.getPeriodStart() != null && contract.getPeriodEnd() != null) {
                if (!contract.getPeriodStart().isAfter(nextMonth.plusMonths(1).minusDays(1)) &&
                    !contract.getPeriodEnd().isBefore(nextMonth)) {
                    nextMonthRevenue = nextMonthRevenue.add(contract.getValue());
                }
            }
        }

        revenue.add(new SalesDashboardSummaryDTO.RevenueItem(
            nextMonth.format(MONTH_FORMATTER),
            nextMonthRevenue.longValue()
        ));

        return revenue;
    }

    /**
     * Calculate time ago string (e.g., "2 min ago", "1 hour ago")
     */
    private String calculateTimeAgo(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(timestamp, now);
        
        if (minutes < 1) {
            return "just now";
        } else if (minutes < 60) {
            return minutes + " min ago";
        } else {
            long hours = ChronoUnit.HOURS.between(timestamp, now);
            if (hours < 24) {
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else {
                long days = ChronoUnit.DAYS.between(timestamp, now);
                return days + " day" + (days > 1 ? "s" : "") + " ago";
            }
        }
    }

    /**
     * Generate MSA contract ID in format: MSA-YYYY-NN
     */
    private String generateMSAContractId(Integer id, LocalDateTime createdAt) {
        int year = createdAt != null ? createdAt.getYear() : 2025;
        int sequenceNumber = id % 100;
        return String.format("MSA-%d-%02d", year, sequenceNumber);
    }

    /**
     * Generate SOW contract ID in format: SOW-YYYY-MM-DD-NN
     */
    private String generateSOWContractId(Integer id, LocalDateTime createdAt) {
        int year = createdAt != null ? createdAt.getYear() : 2025;
        int month = createdAt != null ? createdAt.getMonthValue() : 1;
        int day = createdAt != null ? createdAt.getDayOfMonth() : 1;
        int sequenceNumber = id % 100;
        return String.format("SOW-%d-%02d-%02d-%02d", year, month, day, sequenceNumber);
    }
}

