package com.banking.banking_app.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "current_accounts")
@DiscriminatorValue("CURRENT")
public class CurrentAccount extends BankAccount {

    @Column(name = "overdraft_limit", precision = 19, scale = 4, nullable = false)
    private BigDecimal overdraftLimit;

    public CurrentAccount() { super(); }

    public BigDecimal getOverdraftLimit() { return overdraftLimit; }
    public void setOverdraftLimit(BigDecimal overdraftLimit) { this.overdraftLimit = overdraftLimit; }
}