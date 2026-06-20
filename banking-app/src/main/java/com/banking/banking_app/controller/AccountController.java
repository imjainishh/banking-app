package com.banking.banking_app.controller;

import com.banking.banking_app.dto.*;
import com.banking.banking_app.model.User;
import com.banking.banking_app.repository.UserRepository;
import com.banking.banking_app.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final UserRepository userRepository;

    public AccountController(AccountService accountService,
                             UserRepository userRepository) {
        this.accountService = accountService;
        this.userRepository = userRepository;
    }

    // POST /api/accounts — open a new account
    @PostMapping
    public ResponseEntity<AccountResponse> openAccount(
            @RequestBody OpenAccountRequest request,
            Authentication auth) {

        Long userId = getLoggedInUserId(auth);
        AccountResponse response = accountService.openAccount(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/accounts — list all my accounts
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Authentication auth) {
        Long userId = getLoggedInUserId(auth);
        return ResponseEntity.ok(accountService.getUserAccounts(userId));
    }

    // GET /api/accounts/{id}/balance — get balance for one account
    @GetMapping("/{id}/balance")
    public ResponseEntity<?> getBalance(@PathVariable Long id, Authentication auth) {
        Long userId = getLoggedInUserId(auth);
        return ResponseEntity.ok(accountService.getBalance(id, userId));
    }

    // GET /api/accounts/{id}/transactions — transaction history
    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @PathVariable Long id,
            Authentication auth) {
        Long userId = getLoggedInUserId(auth);
        return ResponseEntity.ok(accountService.getTransactionHistory(id, userId));
    }

    // Helper — gets the logged-in user's DB id from the JWT
    private Long getLoggedInUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}