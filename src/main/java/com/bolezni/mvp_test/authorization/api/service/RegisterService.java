package com.bolezni.mvp_test.authorization.api.service;

import com.bolezni.mvp_test.authorization.api.dto.RegisterRequest;
import com.bolezni.mvp_test.authorization.api.dto.RegisterResponse;

public interface RegisterService {
    RegisterResponse register(RegisterRequest registerRequest);
}
