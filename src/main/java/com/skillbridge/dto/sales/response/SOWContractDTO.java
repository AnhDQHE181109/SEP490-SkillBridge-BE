package com.skillbridge.dto.sales.response;

/**
 * Response DTO for SOW contract
 */
public class SOWContractDTO {
    private Integer id;
    private String contractId; // Format: SOW-YYYY-MM-DD-NN
    private String contractName;
    private String status;

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

