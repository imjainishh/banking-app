package com.banking.banking_app.dto;

public class OpenAccountRequest {

    private String accountType; // "SAVINGS" or "CURRENT"

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
}