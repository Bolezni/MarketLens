package com.bolezni.mvp_test.authorization.api.service;

import com.bolezni.mvp_test.authorization.api.dto.AuthResponse;
import com.bolezni.mvp_test.authorization.api.dto.RefreshRequest;
import com.bolezni.mvp_test.authorization.store.RefreshTokenEntity;

public interface RefreshService {
    AuthResponse refresh(RefreshRequest request);

    void validateRefreshToken(String refreshToken);

    RefreshTokenEntity getRefreshTokenByTokenHash(String tokenHash);
}

