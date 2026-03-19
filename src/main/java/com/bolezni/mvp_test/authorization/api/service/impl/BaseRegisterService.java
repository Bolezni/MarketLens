package com.bolezni.mvp_test.authorization.api.service.impl;

import com.bolezni.mvp_test.authorization.api.dto.RegisterRequest;
import com.bolezni.mvp_test.authorization.api.dto.RegisterResponse;
import com.bolezni.mvp_test.authorization.api.service.RegisterService;
import com.bolezni.mvp_test.authorization.store.UserEntity;
import com.bolezni.mvp_test.authorization.store.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaseRegisterService implements RegisterService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest) {
        if (registerRequest == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is missing");
        }

        String email = registerRequest.email().trim().toLowerCase(Locale.ROOT);

        if (!registerRequest.password().equals(registerRequest.passwordConfirmation())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password confirmation does not match");
        }

        UserEntity user = UserEntity.builder()
                .email(email)
                .password(passwordEncoder.encode(registerRequest.password()))
                .plan("Base")
                .build();

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists", e);
        }

        return new RegisterResponse("create new user");
    }
}
