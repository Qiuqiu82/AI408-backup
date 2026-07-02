package org.example.ai408.controller;

import jakarta.validation.Valid;
import org.example.ai408.common.ApiResponse;
import org.example.ai408.common.PageResponse;
import org.example.ai408.dto.CommonDtos;
import org.example.ai408.service.PracticeService;
import org.example.ai408.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/practice/sessions")
public class PracticeController {
    private final PracticeService practiceService;
    private final UserService userService;

    public PracticeController(PracticeService practiceService, UserService userService) {
        this.practiceService = practiceService;
        this.userService = userService;
    }

    @PostMapping
    public ApiResponse<CommonDtos.PracticeSessionDTO> create(@Valid @RequestBody CommonDtos.PracticeSessionCreateRequest request) {
        return ApiResponse.ok(practiceService.createSession(userService.currentUserEntity().getId(), request.getData()));
    }

    @GetMapping("/{id}")
    public ApiResponse<CommonDtos.PracticeSessionDTO> get(@PathVariable String id) {
        return ApiResponse.ok(practiceService.getSession(userService.currentUserEntity().getId(), id));
    }

    @PostMapping("/{id}/answers")
    public ApiResponse<CommonDtos.SubmitAnswerDTO> submit(@PathVariable String id, @Valid @RequestBody CommonDtos.SubmitAnswerRequest request) {
        return ApiResponse.ok(practiceService.submitAnswer(userService.currentUserEntity().getId(), id, request.getData()));
    }

    @PatchMapping("/{id}/essay-steps")
    public ApiResponse<CommonDtos.EssayStepDTO> essaySteps(@PathVariable String id, @Valid @RequestBody CommonDtos.EssayStepsRequest request) {
        return ApiResponse.ok(practiceService.updateEssaySteps(userService.currentUserEntity().getId(), id, request.getData()));
    }

    @PostMapping("/{id}/finish")
    public ApiResponse<CommonDtos.FinishPracticeDTO> finish(@PathVariable String id) {
        return ApiResponse.ok(practiceService.finishSession(userService.currentUserEntity().getId(), id));
    }

    @GetMapping("/{id}/review")
    public ApiResponse<CommonDtos.ReviewDTO> review(@PathVariable String id) {
        return ApiResponse.ok(practiceService.reviewSession(userService.currentUserEntity().getId(), id));
    }
}
