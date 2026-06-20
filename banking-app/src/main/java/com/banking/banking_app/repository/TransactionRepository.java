package com.banking.banking_app.repository;

import com.banking.banking_app.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // All transactions for an account, newest first
    List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}