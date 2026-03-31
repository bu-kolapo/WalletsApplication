package com.wallets.controller;

import com.wallets.dto.request.CreateWalletRequest;
import com.wallets.dto.request.WalletRequest;
import com.wallets.dto.response.WalletResponse;
import com.wallets.model.Transaction;
import com.wallets.service.WalletService;
import com.wallets.utils.AppUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@Validated
@Tag(name = "Wallet", description = "Wallet management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/create")
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest walletRequest) {
        WalletResponse response = walletService.createWallet(walletRequest);
        return ResponseEntity.status(response.getResponseCode()).body(response);
    }

    @PostMapping("/fund")
    public ResponseEntity<WalletResponse> fundWallet(
            @Valid @RequestBody WalletRequest walletRequest,
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey) {

        WalletResponse response = walletService.fundWallet(walletRequest, idempotencyKey);
        return ResponseEntity.status(response.getResponseCode()).body(response);
    }

    @PostMapping("/debit")
    public ResponseEntity<WalletResponse> debitWallet(
            @Valid @RequestBody WalletRequest walletRequest,
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey) {

        WalletResponse response = walletService.debitWallet(walletRequest, idempotencyKey);
        return ResponseEntity.status(response.getResponseCode()).body(response);
    }

    @GetMapping("/details/{userId}")
    public ResponseEntity<WalletResponse> getWalletDetails(@PathVariable String userId) {
        WalletRequest walletRequest = WalletRequest.builder().userId(userId).build();
        WalletResponse response = walletService.getWalletDetails(userId);
        return ResponseEntity.status(response.getResponseCode()).body(response);
    }

    @GetMapping("/transactions/{userId}")
    public ResponseEntity<?> getTransactionHistory(@PathVariable String userId) {
        List<Transaction> transactions = walletService.getTransactionHistory(userId);
        if (transactions.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(AppUtils.walletResponse(404, false, "No transactions found for userId: " + userId));
        }
        return ResponseEntity.status(200).body(transactions);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllWallets() {
        return ResponseEntity.ok(walletService.getAllWallets());
    }
}