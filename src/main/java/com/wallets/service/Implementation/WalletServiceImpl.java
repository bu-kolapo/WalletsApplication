package com.wallets.service.Implementation;

import com.wallets.dto.WalletRequest;
import com.wallets.dto.WalletResponse;
import com.wallets.exception.WalletNotFoundException;
import com.wallets.model.Transaction;
import com.wallets.model.Wallet;
import com.wallets.repository.TransactionRepository;
import com.wallets.repository.WalletRepository;
import com.wallets.service.WalletService;
import com.wallets.utils.AppUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletServiceImpl(WalletRepository walletRepository,
                             TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public WalletResponse createWallet(WalletRequest walletRequest) {
        try {
            if (walletRepository.existsByUserId(walletRequest.getUserId())) {
                return AppUtils.walletResponse(400, false, "Wallet already exists for userId: " + walletRequest.getUserId());
            }

            Wallet wallet = Wallet.builder()
                    .userId(walletRequest.getUserId())
                    .balance(BigDecimal.ZERO)
                    .build();

            walletRepository.save(wallet);
            log.info("Wallet created for userId: {}", walletRequest.getUserId());

            return AppUtils.walletResponse(201, true, "Wallet created successfully for userId: " + walletRequest.getUserId());

        } catch (Exception ex) {
            log.error("Error creating wallet: {}", ex.getMessage());
            return AppUtils.walletResponse(500, false, "Error creating wallet: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public WalletResponse fundWallet(WalletRequest walletRequest) {
        try {
            // Idempotency check
            if (transactionRepository.existsByTransactionRef(walletRequest.getTransactionRef())) {
                return AppUtils.walletResponse(200, true, "Transaction already processed");
            }

            // Rule 1: Wallet must exist
            checkIfWalletExist(walletRequest.getUserId());

            // Rule 2: Amount must be greater than 0
            if (walletRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return AppUtils.walletResponse(400, false, "Funding amount must be greater than 0");
            }

            // Fund the wallet
            Wallet wallet = walletRepository.findByUserId(walletRequest.getUserId())
                    .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
            wallet.setBalance(wallet.getBalance().add(walletRequest.getAmount()));
            walletRepository.save(wallet);

            // Log transaction
            transactionRepository.save(Transaction.builder()
                    .transactionId(UUID.randomUUID().toString())
                    .userId(walletRequest.getUserId())
                    .amount(walletRequest.getAmount())
                    .type("CREDIT")
                    .description("Wallet funded")
                    .transactionRef(walletRequest.getTransactionRef())
                    .build());

            log.info("Wallet funded for userId: {} amount: {}", walletRequest.getUserId(), walletRequest.getAmount());
            return AppUtils.walletResponse(200, true, "Wallet funded successfully. New balance: " + wallet.getBalance());

        } catch (WalletNotFoundException ex) {
            return AppUtils.walletResponse(404, false, ex.getMessage());
        }
    }

    @Override
    @Transactional
    public WalletResponse debitWallet(WalletRequest walletRequest) {
        try {
            // Idempotency check
            if (transactionRepository.existsByTransactionRef(walletRequest.getTransactionRef())) {
                return AppUtils.walletResponse(200, true, "Transaction already processed");
            }

            // Rule 1: Wallet must exist
            checkIfWalletExist(walletRequest.getUserId());

            // Rule 2: Amount must be greater than 0
            if (walletRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return AppUtils.walletResponse(400, false, "Debit amount must be greater than 0");
            }

            // Rule 3: Cannot debit beyond available balance
            Wallet wallet = walletRepository.findByUserId(walletRequest.getUserId())
                    .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

            if (walletRequest.getAmount().compareTo(wallet.getBalance()) > 0) {
                return AppUtils.walletResponse(400, false, "Insufficient funds. Available balance: " + wallet.getBalance());
            }

            // Debit the wallet
            wallet.setBalance(wallet.getBalance().subtract(walletRequest.getAmount()));
            walletRepository.save(wallet);

            // Log transaction
            transactionRepository.save(Transaction.builder()
                    .transactionId(UUID.randomUUID().toString())
                    .userId(walletRequest.getUserId())
                    .amount(walletRequest.getAmount())
                    .type("DEBIT")
                    .description("Wallet debited")
                    .transactionRef(walletRequest.getTransactionRef())
                    .build());

            log.info("Wallet debited for userId: {} amount: {}", walletRequest.getUserId(), walletRequest.getAmount());
            return AppUtils.walletResponse(200, true, "Debit successful. New balance: " + wallet.getBalance());

        } catch (WalletNotFoundException ex) {
            return AppUtils.walletResponse(404, false, ex.getMessage());
        }
    }

    @Override
    public WalletResponse getWalletDetails(WalletRequest walletRequest) {
        try {
            checkIfWalletExist(walletRequest.getUserId());

            Wallet wallet = walletRepository.findByUserId(walletRequest.getUserId())
                    .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

            return AppUtils.walletResponse(200, true, "Wallet Details - UserId: " + wallet.getUserId()
                    + " | WalletId: " + wallet.getId()
                    + " | Balance: " + wallet.getBalance());

        } catch (WalletNotFoundException ex) {
            return AppUtils.walletResponse(404, false, ex.getMessage());
        }
    }

    @Override
    public List<Transaction> getTransactionHistory(String userId) {
        return transactionRepository.findByUserId(userId);
    }

    private void checkIfWalletExist(String userId) throws WalletNotFoundException {
        if (!walletRepository.existsByUserId(userId)) {
            throw new WalletNotFoundException("Wallet not found for userId: " + userId);
        }
    }
}
