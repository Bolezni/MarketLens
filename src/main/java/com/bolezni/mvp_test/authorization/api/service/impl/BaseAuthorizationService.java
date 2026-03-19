package com.bolezni.mvp_test.authorization.api.service.impl;

import com.bolezni.mvp_test.authorization.api.dto.AuthRequest;
import com.bolezni.mvp_test.authorization.api.dto.AuthResponse;
import com.bolezni.mvp_test.authorization.api.security.CustomUserDetails;
import com.bolezni.mvp_test.authorization.api.security.jwt.JwtProvider;
import com.bolezni.mvp_test.authorization.api.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BaseAuthorizationService implements AuthorizationService {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    @Override
    public AuthResponse authorize(AuthRequest authRequest) {
        String email = authRequest.email().trim().toLowerCase(Locale.ROOT);
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, authRequest.password())
            );
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String jwt = jwtProvider.buildToken(userDetails);
            String refreshToken = jwtProvider.buildRefreshToken(userDetails);
            return new AuthResponse(jwt, "Bearer", refreshToken);
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials", e);
        }
    }
}
