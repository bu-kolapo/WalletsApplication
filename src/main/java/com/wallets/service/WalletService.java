package com.wallets.service;

import com.wallets.dto.WalletRequest;
import com.wallets.dto.WalletResponse;
import com.wallets.model.Transaction;

import java.util.List;

public interface WalletService {
    WalletResponse createWallet(WalletRequest walletRequest);
    WalletResponse fundWallet(WalletRequest walletRequest);
    WalletResponse debitWallet(WalletRequest walletRequest);
    WalletResponse getWalletDetails(WalletRequest walletRequest);
    List<Transaction> getTransactionHistory(String userId);
}
