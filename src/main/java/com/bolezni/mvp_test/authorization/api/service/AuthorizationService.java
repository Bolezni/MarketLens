package com.bolezni.mvp_test.authorization.api.service;

import com.bolezni.mvp_test.authorization.api.dto.AuthRequest;
import com.bolezni.mvp_test.authorization.api.dto.AuthResponse;

public interface AuthorizationService {
    AuthResponse authorize(AuthRequest authRequest);
}
