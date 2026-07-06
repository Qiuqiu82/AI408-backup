package org.example.ai408.controller;

import jakarta.validation.Valid;
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
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<AuthDtos.UserDTO> me() {
        return ApiResponse.ok(userService.me());
    }

    @PatchMapping("/me")
    public ApiResponse<AuthDtos.UserDTO> updateMe(@Valid @RequestBody AuthDtos.UpdateUserRequest request) {
        AuthDtos.UpdateUserRequest.Payload payload = request.getData();
        return ApiResponse.ok(userService.updateMe(
                payload.getNickname(),
                payload.getAvatarUrl(),
                payload.getWrongBookAutoRemoveEnabled(),
                payload.getWrongBookAutoRemoveThreshold()
        ));
    }

    @GetMapping("/me/study-summary")
    public ApiResponse<org.example.ai408.dto.CommonDtos.StudySummaryDTO> studySummary() {
        return ApiResponse.ok(userService.studySummary());
    }
}
