package com.bolezni.mvp_test.authorization.api.service;

import com.bolezni.mvp_test.authorization.api.dto.AuthResponse;
import com.bolezni.mvp_test.authorization.api.dto.RefreshRequest;

public interface RefreshService {
    AuthResponse refresh(RefreshRequest request);
}

