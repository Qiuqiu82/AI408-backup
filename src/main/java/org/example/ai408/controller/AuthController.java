package org.example.ai408.controller;

import jakarta.validation.Valid;
import org.example.ai408.common.ApiResponse;
import org.example.ai408.dto.AuthDtos;
import org.example.ai408.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/send-code")
    public ApiResponse<AuthDtos.SendCodeResponse> sendCode(@Valid @RequestBody AuthDtos.SendCodeRequest request) {
        return ApiResponse.ok(authService.sendCode(request.getData().getMobile()));
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.AuthTokens> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        AuthDtos.LoginRequest.Payload payload = request.getData();
        return ApiResponse.ok(authService.login(payload.getMobile(), payload.getCode(), payload.getDeviceId(), payload.getClientType()));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthDtos.AuthTokens> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.getData().getRefreshToken()));
    }
}
