package com.banking.banking_app.service;

import com.banking.banking_app.dto.*;
import com.banking.banking_app.exception.*;
import com.banking.banking_app.model.*;
import com.banking.banking_app.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final BankAccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public AccountService(BankAccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    // ── Open a new account ────────────────────────────────────────────
    @Transactional
    public AccountResponse openAccount(Long userId, OpenAccountRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BankAccount account;
        String type = request.getAccountType().toUpperCase();

        if (type.equals("SAVINGS")) {
            SavingsAccount savings = new SavingsAccount();
            savings.setInterestRate(new BigDecimal("0.035")); // 3.5% default
            account = savings;
        } else if (type.equals("CURRENT")) {
            CurrentAccount current = new CurrentAccount();
            current.setOverdraftLimit(new BigDecimal("500.00")); // £500 default
            account = current;
        } else {
            throw new IllegalArgumentException(
                    "Invalid account type. Use 'SAVINGS' or 'CURRENT'");
        }

        // Set shared fields
        account.setAccountNumber(generateAccountNumber());
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setUser(user);

        BankAccount saved = accountRepository.save(account);
        return toAccountResponse(saved, type);
    }

    // ── Get all accounts for a user ───────────────────────────────────
    @Transactional(readOnly = true)
    public List<AccountResponse> getUserAccounts(Long userId) {
        return accountRepository.findByUserId(userId)
                .stream()
                .map(acc -> toAccountResponse(acc, acc.getClass().getSimpleName()
                        .replace("Account", "").toUpperCase()))
                .collect(Collectors.toList());
    }

    // ── Get balance ───────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long accountId, Long userId) {
        BankAccount account = getAccountAndVerifyOwner(accountId, userId);
        return account.getBalance();
    }

    // ── Deposit ───────────────────────────────────────────────────────
    @Transactional
    public TransactionResponse deposit(Long userId, TransactionRequest request) {

        BankAccount account = getAccountAndVerifyOwner(request.getAccountId(), userId);

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }

        // Add money
        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        // Record the transaction
        Transaction tx = recordTransaction(
                account, TransactionType.DEPOSIT,
                request.getAmount(), newBalance,
                request.getDescription());

        return toTransactionResponse(tx);
    }

    // ── Withdrawal ────────────────────────────────────────────────────
    @Transactional
    public TransactionResponse withdraw(Long userId, TransactionRequest request) {

        BankAccount account = getAccountAndVerifyOwner(request.getAccountId(), userId);

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
        }

        // Check sufficient funds (current accounts can go to -overdraftLimit)
        BigDecimal minimumAllowed = BigDecimal.ZERO;
        if (account instanceof CurrentAccount current) {
            minimumAllowed = current.getOverdraftLimit().negate();
        }

        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
        if (newBalance.compareTo(minimumAllowed) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Available balance: " + account.getBalance());
        }

        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction tx = recordTransaction(
                account, TransactionType.WITHDRAWAL,
                request.getAmount(), newBalance,
                request.getDescription());

        return toTransactionResponse(tx);
    }

    // ── Transfer ──────────────────────────────────────────────────────
    @Transactional
    public TransactionResponse transfer(Long userId, TransactionRequest request) {

        // Source account — must belong to logged-in user
        BankAccount source = getAccountAndVerifyOwner(request.getAccountId(), userId);

        // Target account — find by account number
        BankAccount target = accountRepository
                .findByAccountNumber(request.getTargetAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Target account not found: " + request.getTargetAccountNumber()));

        if (source.getId().equals(target.getId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        // Check source has enough funds
        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Available balance: " + source.getBalance());
        }

        // Debit source
        BigDecimal sourceNewBalance = source.getBalance().subtract(request.getAmount());
        source.setBalance(sourceNewBalance);
        accountRepository.save(source);

        // Credit target — both happen in the same @Transactional block
        // If anything fails, BOTH changes are rolled back automatically
        BigDecimal targetNewBalance = target.getBalance().add(request.getAmount());
        target.setBalance(targetNewBalance);
        accountRepository.save(target);

        // Record on source account as TRANSFER_OUT
        Transaction tx = recordTransaction(
                source, TransactionType.TRANSFER_OUT,
                request.getAmount(), sourceNewBalance,
                "Transfer to " + target.getAccountNumber());

        // Record on target account as TRANSFER_IN
        recordTransaction(
                target, TransactionType.TRANSFER_IN,
                request.getAmount(), targetNewBalance,
                "Transfer from " + source.getAccountNumber());

        return toTransactionResponse(tx);
    }

    // ── Transaction history ───────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(Long accountId, Long userId) {
        getAccountAndVerifyOwner(accountId, userId); // security check
        return transactionRepository
                .findByAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────────

    // Loads account and throws if it doesn't belong to the logged-in user
    private BankAccount getAccountAndVerifyOwner(Long accountId, Long userId) {
        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with id: " + accountId));

        if (!account.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException(
                    "You do not have access to account: " + accountId);
        }
        return account;
    }

    // Saves a Transaction row — called after every money movement
    private Transaction recordTransaction(BankAccount account,
                                          TransactionType type,
                                          BigDecimal amount,
                                          BigDecimal balanceAfter,
                                          String description) {
        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(balanceAfter);
        tx.setDescription(description);
        return transactionRepository.save(tx);
    }

    // Generates a unique account number like ACC-A1B2C3D4
    private String generateAccountNumber() {
        return "ACC-" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase();
    }

    // Entity → DTO
    private AccountResponse toAccountResponse(BankAccount acc, String type) {
        return new AccountResponse(
                acc.getId(),
                acc.getAccountNumber(),
                type,
                acc.getBalance(),
                acc.getStatus().name(),
                acc.getCreatedAt()
        );
    }

    private TransactionResponse toTransactionResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getType().name(),
                tx.getAmount(),
                tx.getBalanceAfter(),
                tx.getDescription(),
                tx.getCreatedAt()
        );
    }
}