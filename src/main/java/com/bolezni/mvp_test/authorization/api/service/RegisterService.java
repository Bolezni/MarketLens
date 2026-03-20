package com.bolezni.mvp_test.authorization.api.service;

import com.bolezni.mvp_test.authorization.api.dto.RegisterRequest;
import com.bolezni.mvp_test.authorization.api.dto.RegisterResponse;

public interface RegisterService {

    /**
     * Метод для регистрации пользователя.
     * При ошибке выброситься исключение ResponseStatusException
     * @param registerRequest принимает email, password и passwordConfirmation
     * @return cтатус, что пользователь создан
     */
    RegisterResponse register(RegisterRequest registerRequest);
}
