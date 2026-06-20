package com.banking.banking_app.service;

import com.banking.banking_app.dto.OpenAccountRequest;
import com.banking.banking_app.dto.TransactionRequest;
import com.banking.banking_app.exception.InsufficientFundsException;
import com.banking.banking_app.exception.ResourceNotFoundException;
import com.banking.banking_app.exception.UnauthorizedAccessException;
import com.banking.banking_app.model.*;
import com.banking.banking_app.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AccountService.
 *
 * Key concepts tested here:
 *   - @ExtendWith(MockitoExtension)  → tells JUnit to use Mockito
 *   - @Mock                          → fake repository, no real DB
 *   - @InjectMocks                   → creates AccountService with mocks injected
 *   - verify()                       → confirms a method was called
 *   - assertThrows()                 → confirms an exception was thrown
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private BankAccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    // ── Test data ─────────────────────────────────────────────────────
    private User testUser;
    private SavingsAccount testAccount;

    @BeforeEach
    void setUp() {
        // Runs before every test — fresh data each time
        testUser = new User("Alice Smith", "alice@bank.com", "hashedPassword");
        testUser.setId(1L);

        testAccount = new SavingsAccount();
        testAccount.setId(1L);
        testAccount.setAccountNumber("ACC-TEST01");
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setStatus(AccountStatus.ACTIVE);
        testAccount.setUser(testUser);
        testAccount.setInterestRate(new BigDecimal("0.035"));
    }

    // ══ openAccount tests ════════════════════════════════════════════

    @Test
    @DisplayName("Should open a savings account successfully")
    void openAccount_savings_success() {
        // ARRANGE — set up what the mocks return
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        OpenAccountRequest request = new OpenAccountRequest();
        request.setAccountType("SAVINGS");

        // ACT — call the method
        var response = accountService.openAccount(1L, request);

        // ASSERT — check the result
        assertNotNull(response);
        assertEquals("SAVINGS", response.getAccountType());
        assertEquals(BigDecimal.ZERO, response.getBalance());

        // Verify save() was called exactly once
        verify(accountRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found")
    void openAccount_userNotFound_throwsException() {
        // ARRANGE — user doesn't exist
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        OpenAccountRequest request = new OpenAccountRequest();
        request.setAccountType("SAVINGS");

        // ACT + ASSERT — confirm the right exception is thrown
        assertThrows(ResourceNotFoundException.class, () ->
                accountService.openAccount(99L, request)
        );
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid account type")
    void openAccount_invalidType_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        OpenAccountRequest request = new OpenAccountRequest();
        request.setAccountType("INVALID");

        assertThrows(IllegalArgumentException.class, () ->
                accountService.openAccount(1L, request)
        );
    }

    // ══ deposit tests ════════════════════════════════════════════════

    @Test
    @DisplayName("Should deposit money and increase balance")
    void deposit_success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any())).thenReturn(testAccount);
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TransactionRequest request = new TransactionRequest();
        request.setAccountId(1L);
        request.setAmount(new BigDecimal("500.00"));
        request.setDescription("Test deposit");

        var response = accountService.deposit(1L, request);

        assertNotNull(response);
        // Balance should now be 1000 + 500 = 1500
        assertEquals(new BigDecimal("1500.00"), response.getBalanceAfter());
        assertEquals("DEPOSIT", response.getType());

        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for zero deposit")
    void deposit_zeroAmount_throwsException() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        TransactionRequest request = new TransactionRequest();
        request.setAccountId(1L);
        request.setAmount(BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () ->
                accountService.deposit(1L, request)
        );
    }

    @Test
    @DisplayName("Should throw UnauthorizedAccessException if account belongs to different user")
    void deposit_wrongUser_throwsException() {
        // Account belongs to user 1, but user 2 is trying to deposit
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        TransactionRequest request = new TransactionRequest();
        request.setAccountId(1L);
        request.setAmount(new BigDecimal("100.00"));

        assertThrows(UnauthorizedAccessException.class, () ->
                accountService.deposit(2L, request) // ← different userId
        );
    }

    // ══ withdraw tests ═══════════════════════════════════════════════

    @Test
    @DisplayName("Should withdraw money and decrease balance")
    void withdraw_success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any())).thenReturn(testAccount);
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TransactionRequest request = new TransactionRequest();
        request.setAccountId(1L);
        request.setAmount(new BigDecimal("200.00"));
        request.setDescription("ATM withdrawal");

        var response = accountService.withdraw(1L, request);

        // Balance should be 1000 - 200 = 800
        assertEquals(new BigDecimal("800.00"), response.getBalanceAfter());
        assertEquals("WITHDRAWAL", response.getType());
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException when balance too low")
    void withdraw_insufficientFunds_throwsException() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        TransactionRequest request = new TransactionRequest();
        request.setAccountId(1L);
        // Try to withdraw more than the 1000.00 balance
        request.setAmount(new BigDecimal("9999.00"));

        InsufficientFundsException ex = assertThrows(
                InsufficientFundsException.class, () ->
                        accountService.withdraw(1L, request)
        );

        // Also check the error message contains useful info
        assertTrue(ex.getMessage().contains("Insufficient funds"));
    }

    @Test
    @DisplayName("Current account should allow withdrawal up to overdraft limit")
    void withdraw_currentAccount_allowsOverdraft() {
        CurrentAccount currentAccount = new CurrentAccount();
        currentAccount.setId(2L);
        currentAccount.setBalance(new BigDecimal("100.00"));
        currentAccount.setStatus(AccountStatus.ACTIVE);
        currentAccount.setUser(testUser);
        currentAccount.setOverdraftLimit(new BigDecimal("500.00"));

        when(accountRepository.findById(2L)).thenReturn(Optional.of(currentAccount));
        when(accountRepository.save(any())).thenReturn(currentAccount);
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TransactionRequest request = new TransactionRequest();
        request.setAccountId(2L);
        // Withdraw 400 from a 100 balance — allowed because overdraft is 500
        request.setAmount(new BigDecimal("400.00"));

        var response = accountService.withdraw(1L, request);

        // Balance should be 100 - 400 = -300
        assertEquals(new BigDecimal("-300.00"), response.getBalanceAfter());
    }

    // ══ transfer tests ═══════════════════════════════════════════════

    @Test
    @DisplayName("Should transfer money between two accounts atomically")
    void transfer_success() {
        SavingsAccount targetAccount = new SavingsAccount();
        targetAccount.setId(2L);
        targetAccount.setAccountNumber("ACC-TARGET1");
        targetAccount.setBalance(new BigDecimal("200.00"));
        targetAccount.setStatus(AccountStatus.ACTIVE);
        targetAccount.setUser(testUser);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.findByAccountNumber("ACC-TARGET1"))
                .thenReturn(Optional.of(targetAccount));
        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TransactionRequest request = new TransactionRequest();
        request.setAccountId(1L);
        request.setAmount(new BigDecimal("300.00"));
        request.setTargetAccountNumber("ACC-TARGET1");

        var response = accountService.transfer(1L, request);

        // Source: 1000 - 300 = 700
        assertEquals(new BigDecimal("700.00"), response.getBalanceAfter());
        assertEquals("TRANSFER_OUT", response.getType());

        // Verify both accounts were saved
        verify(accountRepository, times(2)).save(any());
        // Verify two transaction records were created (TRANSFER_OUT + TRANSFER_IN)
        verify(transactionRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException if transfer amount exceeds balance")
    void transfer_insufficientFunds_throwsException() {
        SavingsAccount targetAccount = new SavingsAccount();
        targetAccount.setId(2L);
        targetAccount.setAccountNumber("ACC-TARGET1");
        targetAccount.setBalance(BigDecimal.ZERO);
        targetAccount.setStatus(AccountStatus.ACTIVE);
        targetAccount.setUser(testUser);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.findByAccountNumber("ACC-TARGET1"))
                .thenReturn(Optional.of(targetAccount));

        TransactionRequest request = new TransactionRequest();
        request.setAccountId(1L);
        request.setAmount(new BigDecimal("9999.00")); // more than balance
        request.setTargetAccountNumber("ACC-TARGET1");

        assertThrows(InsufficientFundsException.class, () ->
                accountService.transfer(1L, request)
        );

        // Most important: verify NO money was saved — full rollback
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when transferring to same account")
    void transfer_sameAccount_throwsException() {
        // Target account number is same as source
        testAccount.setAccountNumber("ACC-TEST01");

        SavingsAccount sameAccount = new SavingsAccount();
        sameAccount.setId(1L); // same id as source
        sameAccount.setAccountNumber("ACC-TEST01");
        sameAccount.setUser(testUser);
        sameAccount.setBalance(new BigDecimal("1000.00"));
        sameAccount.setStatus(AccountStatus.ACTIVE);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.findByAccountNumber("ACC-TEST01"))
                .thenReturn(Optional.of(sameAccount));

        TransactionRequest request = new TransactionRequest();
        request.setAccountId(1L);
        request.setAmount(new BigDecimal("100.00"));
        request.setTargetAccountNumber("ACC-TEST01");

        assertThrows(IllegalArgumentException.class, () ->
                accountService.transfer(1L, request)
        );
    }
}