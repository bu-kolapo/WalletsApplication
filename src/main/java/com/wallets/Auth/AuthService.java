package com.wallets.Auth;


    public interface AuthService {
        AuthResponse register(RegisterRequest request);
        AuthResponse login(LoginRequest request);
    }

