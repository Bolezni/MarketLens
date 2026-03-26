package com.bolezni.mvp_test.authorization.api.service;

import com.bolezni.mvp_test.authorization.api.dto.LogoutRequest;

public interface LogoutService {
    void logout(LogoutRequest request);
}
