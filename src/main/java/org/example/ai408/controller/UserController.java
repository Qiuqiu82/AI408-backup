package org.example.ai408.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.ai408.common.ApiResponse;
import org.example.ai408.dto.AuthDtos;
import org.example.ai408.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户", description = "当前用户资料、密码和学习摘要")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户")
    public ApiResponse<AuthDtos.UserDTO> me() {
        return ApiResponse.ok(userService.me());
    }

    @PatchMapping("/me")
    @Operation(summary = "更新当前用户", description = "更新昵称、头像和错题自动移除设置。")
    public ApiResponse<AuthDtos.UserDTO> updateMe(@Valid @RequestBody AuthDtos.UpdateUserRequest request) {
        AuthDtos.UpdateUserRequest.Payload payload = request.getData();
        return ApiResponse.ok(userService.updateMe(
                payload.getNickname(),
                payload.getAvatarUrl(),
                payload.getWrongBookAutoRemoveEnabled(),
                payload.getWrongBookAutoRemoveThreshold()
        ));
    }

    @PatchMapping("/me/password")
    @Operation(summary = "设置或修改密码", description = "首次设置无需旧密码；验证码登录会话可直接重置，密码登录会话修改已有密码时需要旧密码。")
    public ApiResponse<AuthDtos.UserDTO> updatePassword(@Valid @RequestBody AuthDtos.UpdatePasswordRequest request) {
        AuthDtos.UpdatePasswordRequest.Payload payload = request.getData();
        return ApiResponse.ok(userService.updatePassword(
                payload.getCurrentPassword(),
                payload.getNewPassword()
        ));
    }

    @GetMapping("/me/study-summary")
    @Operation(summary = "获取学习摘要")
    public ApiResponse<org.example.ai408.dto.CommonDtos.StudySummaryDTO> studySummary() {
        return ApiResponse.ok(userService.studySummary());
    }
}
