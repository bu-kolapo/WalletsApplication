package com.wallets.service;

import com.wallets.dto.request.CreateWalletRequest;
import com.wallets.dto.request.WalletRequest;
import com.wallets.dto.response.WalletResponse;
import com.wallets.model.Transaction;
import com.wallets.model.Wallet;

import java.util.List;

public interface WalletService {
    WalletResponse createWallet(CreateWalletRequest walletRequest);
    WalletResponse fundWallet(WalletRequest walletRequest, String idempotencyKey);
    WalletResponse debitWallet(WalletRequest walletRequest, String idempotencyKey);
    WalletResponse getWalletDetails(String userId);
    List<Transaction> getTransactionHistory(String userId);
    List<Wallet> getAllWallets();
}
