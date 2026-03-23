package com.wallets.utils;

import com.wallets.dto.WalletResponse;

public class AppUtils {

    public static WalletResponse walletResponse(int responseCode, boolean success, String message){
        WalletResponse response = new WalletResponse();
        response.setResponseCode(responseCode);
        response.setSuccess(success);
        response.setMessage(message);
        return response;
    }
}
