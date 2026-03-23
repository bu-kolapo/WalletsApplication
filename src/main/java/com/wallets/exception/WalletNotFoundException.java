package com.wallets.exception;


public class WalletNotFoundException  extends RuntimeException {
    public WalletNotFoundException(String userId) {
        super("Wallet not found for userId: " + userId);
    }}
