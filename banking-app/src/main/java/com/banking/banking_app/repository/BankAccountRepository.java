package com.banking.banking_app.repository;

import com.banking.banking_app.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    // Get all accounts belonging to a user
    List<BankAccount> findByUserId(Long userId);

    // Find by account number (used during transfers)
    Optional<BankAccount> findByAccountNumber(String accountNumber);

    // Check if account belongs to a specific user (security check)
    boolean existsByIdAndUserId(Long id, Long userId);
}