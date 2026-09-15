package com.queueapi.service;

import com.queueapi.dto.request.LoginRequest;
import com.queueapi.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
}
