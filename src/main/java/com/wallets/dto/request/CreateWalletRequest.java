package com.wallets.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// For createWallet - only needs userId
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWalletRequest {

    @NotBlank(message = "UserId cannot be blank")
    private String userId;
}