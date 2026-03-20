package com.bolezni.mvp_test.authorization.api.controller;

import com.bolezni.mvp_test.authorization.api.dto.AuthRequest;
import com.bolezni.mvp_test.authorization.api.dto.AuthResponse;
import com.bolezni.mvp_test.authorization.api.service.AuthorizationService;
import com.bolezni.mvp_test.authorization.api.dto.RefreshRequest;
import com.bolezni.mvp_test.authorization.api.service.RefreshService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthorizationService authorizationService;
    private final RefreshService refreshService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        AuthResponse response = authorizationService.authorize(authRequest);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(refreshService.refresh(request));
    }
}
