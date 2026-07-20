package org.example.ai408.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.ai408.common.ApiResponse;
import org.example.ai408.dto.AuthDtos;
import org.example.ai408.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证", description = "邮箱验证码、邮箱密码登录和 Token 刷新")
@SecurityRequirements
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/send-code")
    @Operation(summary = "发送邮箱验证码", description = "向邮箱发送登录验证码。邀请制开启时，仅允许白名单邮箱调用。")
    public ApiResponse<AuthDtos.SendCodeResponse> sendCode(@Valid @RequestBody AuthDtos.SendCodeRequest request) {
        AuthDtos.SendCodeRequest.Payload payload = request.getData();
        return ApiResponse.ok(authService.sendCode(
                payload.getEmail() == null ? payload.getMobile() : payload.getEmail(),
                payload.getScene()
        ));
    }

    @PostMapping("/login")
    @Operation(summary = "邮箱验证码登录", description = "使用邮箱和验证码登录；首次登录或未设置密码的用户需要随后设置密码。")
    public ApiResponse<AuthDtos.AuthTokens> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        AuthDtos.LoginRequest.Payload payload = request.getData();
        return ApiResponse.ok(authService.login(
                payload.getEmail() == null ? payload.getMobile() : payload.getEmail(),
                payload.getCode(),
                payload.getDeviceId(),
                payload.getClientType()
        ));
    }

    @PostMapping("/password-login")
    @Operation(summary = "邮箱密码登录", description = "使用已设置密码的邮箱登录。账号不存在、未设置密码或密码错误统一返回 LOGIN_FAILED。")
    public ApiResponse<AuthDtos.AuthTokens> passwordLogin(@Valid @RequestBody AuthDtos.PasswordLoginRequest request) {
        AuthDtos.PasswordLoginRequest.Payload payload = request.getData();
        return ApiResponse.ok(authService.passwordLogin(
                payload.getEmail(),
                payload.getPassword(),
                payload.getDeviceId(),
                payload.getClientType()
        ));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新 Token", description = "使用 refreshToken 获取新的 accessToken 和 refreshToken。")
    public ApiResponse<AuthDtos.AuthTokens> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.getData().getRefreshToken()));
    }
}
