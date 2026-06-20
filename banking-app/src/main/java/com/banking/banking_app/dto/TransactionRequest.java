package com.banking.banking_app.dto;

import java.math.BigDecimal;

public class TransactionRequest {

    private Long accountId;
    private BigDecimal amount;
    private String description;

    // For transfers only
    private String targetAccountNumber;

    public Long getAccountId() { return accountId; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public String getTargetAccountNumber() { return targetAccountNumber; }

    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDescription(String description) { this.description = description; }
    public void setTargetAccountNumber(String targetAccountNumber) {
        this.targetAccountNumber = targetAccountNumber;
    }
}