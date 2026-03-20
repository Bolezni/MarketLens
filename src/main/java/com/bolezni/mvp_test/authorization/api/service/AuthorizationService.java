package com.bolezni.mvp_test.authorization.api.service;

import com.bolezni.mvp_test.authorization.api.dto.AuthRequest;
import com.bolezni.mvp_test.authorization.api.dto.AuthResponse;

public interface AuthorizationService {

    /**
     * Метод для авторизации пользователя, проверяющий на входе
     * email и password.
     * @param authRequest принимающий email и password
     * @return объект с тремя полями основной токен, тип токена и
     * токен для обновления основного
     */
    AuthResponse authorize(AuthRequest authRequest);
}
