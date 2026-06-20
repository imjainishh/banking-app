package com.banking.banking_app.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "savings_accounts")
@DiscriminatorValue("SAVINGS")
public class SavingsAccount extends BankAccount {

    @Column(name = "interest_rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal interestRate;

    public SavingsAccount() { super(); }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
}