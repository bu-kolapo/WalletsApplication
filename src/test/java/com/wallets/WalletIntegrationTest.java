package com.wallets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallets.Auth.AuthResponse;
import com.wallets.Auth.LoginRequest;
import com.wallets.Auth.RegisterRequest;
import com.wallets.Auth.UserRepository;
import com.wallets.dto.request.CreateWalletRequest;
import com.wallets.model.Wallet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.math.BigDecimal;
import com.wallets.dto.request.WalletRequest;
import com.wallets.repository.TransactionRepository;
import com.wallets.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")                  // 👈 uses application-test.properties
@Transactional                           // 👈 rolls back after each test
class WalletIntegrationTest {

    // ❌ Remove these completely
    // @Container
    // static PostgreSQLContainer<?> postgres = ...
    // @DynamicPropertySource
    // static void configureProperties(...)

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private String authToken;
    private String testUserId;

    @BeforeEach
    void setUp() throws Exception {
        // Clean DB before each test
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        // Register and get token
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);

        authToken = authResponse.getToken();
        testUserId = authResponse.getUserId();
    }

    // --- Auth Integration Tests ---
    @Test
    void register_Integration_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.message").value("Registration successful"));
    }

    @Test
    void register_Integration_DuplicateUsername() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_Integration_Success() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void login_Integration_WrongPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // --- Wallet Integration Tests ---
    @Test
    void createWallet_Integration_Success() throws Exception {
        CreateWalletRequest request = CreateWalletRequest.builder()
                .userId(testUserId)
                .build();

        mockMvc.perform(post("/api/wallet/create")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.responseCode").value(201));
    }

    @Test
    void createWallet_Integration_NoToken() throws Exception {
        CreateWalletRequest request = CreateWalletRequest.builder()
                .userId(testUserId)
                .build();

        mockMvc.perform(post("/api/wallet/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void fundWallet_Integration_Success() throws Exception {
        walletRepository.save(Wallet.builder()
                .userId(testUserId)
                .balance(BigDecimal.ZERO)
                .build());

        WalletRequest request = WalletRequest.builder()
                .userId(testUserId)
                .amount(new BigDecimal("5000.00"))
                .build();

        mockMvc.perform(post("/api/wallet/fund")
                        .header("Authorization", "Bearer " + authToken)
                        .header("Idempotency-Key", "TXN-FUND-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("5000")));
    }

    @Test
    void fundWallet_Integration_Idempotency() throws Exception {
        walletRepository.save(Wallet.builder()
                .userId(testUserId)
                .balance(new BigDecimal("5000.00"))
                .build());

        WalletRequest request = WalletRequest.builder()
                .userId(testUserId)
                .amount(new BigDecimal("1000.00"))
                .build();

        // First request
        mockMvc.perform(post("/api/wallet/fund")
                        .header("Authorization", "Bearer " + authToken)
                        .header("Idempotency-Key", "TXN-FUND-IDEM-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("6000")));

        // Same request again
        mockMvc.perform(post("/api/wallet/fund")
                        .header("Authorization", "Bearer " + authToken)
                        .header("Idempotency-Key", "TXN-FUND-IDEM-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction already processed"));
    }

    @Test
    void debitWallet_Integration_InsufficientFunds() throws Exception {
        walletRepository.save(Wallet.builder()
                .userId(testUserId)
                .balance(new BigDecimal("100.00"))
                .build());

        WalletRequest request = WalletRequest.builder()
                .userId(testUserId)
                .amount(new BigDecimal("9000.00"))
                .build();

        mockMvc.perform(post("/api/wallet/debit")
                        .header("Authorization", "Bearer " + authToken)
                        .header("Idempotency-Key", "TXN-DEBIT-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Insufficient funds")));
    }

    @Test
    void getWalletDetails_Integration_Success() throws Exception {
        walletRepository.save(Wallet.builder()
                .userId(testUserId)
                .balance(new BigDecimal("5000.00"))
                .build());

        mockMvc.perform(get("/api/wallet/details/" + testUserId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("5000")));
    }

    @Test
    void getTransactionHistory_Integration_Success() throws Exception {
        walletRepository.save(Wallet.builder()
                .userId(testUserId)
                .balance(new BigDecimal("5000.00"))
                .build());

        WalletRequest request = WalletRequest.builder()
                .userId(testUserId)
                .amount(new BigDecimal("1000.00"))
                .build();

        mockMvc.perform(post("/api/wallet/fund")
                        .header("Authorization", "Bearer " + authToken)
                        .header("Idempotency-Key", "TXN-HISTORY-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/wallet/transactions/" + testUserId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].type").value("CREDIT"))
                .andExpect(jsonPath("$[0].amount").value(1000.00));
    }
}