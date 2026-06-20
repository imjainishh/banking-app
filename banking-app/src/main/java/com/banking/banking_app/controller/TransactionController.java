package com.banking.banking_app.controller;

import com.banking.banking_app.dto.*;
import com.banking.banking_app.repository.UserRepository;
import com.banking.banking_app.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final AccountService accountService;
    private final UserRepository userRepository;

    public TransactionController(AccountService accountService,
                                 UserRepository userRepository) {
        this.accountService = accountService;
        this.userRepository = userRepository;
    }

    // POST /api/transactions/deposit
    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @RequestBody TransactionRequest request,
            Authentication auth) {
        Long userId = getLoggedInUserId(auth);
        return ResponseEntity.ok(accountService.deposit(userId, request));
    }

    // POST /api/transactions/withdraw
    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @RequestBody TransactionRequest request,
            Authentication auth) {
        Long userId = getLoggedInUserId(auth);
        return ResponseEntity.ok(accountService.withdraw(userId, request));
    }

    // POST /api/transactions/transfer
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @RequestBody TransactionRequest request,
            Authentication auth) {
        Long userId = getLoggedInUserId(auth);
        return ResponseEntity.ok(accountService.transfer(userId, request));
    }

    private Long getLoggedInUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}