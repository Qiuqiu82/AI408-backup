package org.example.ai408.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "练习", description = "练习会话、答题、步骤题、结束和复盘")
public class PracticeController {
    private final PracticeService practiceService;
    private final UserService userService;

    public PracticeController(PracticeService practiceService, UserService userService) {
        this.practiceService = practiceService;
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "创建练习会话", description = "按科目、模式、题量或指定题目创建个人练习会话。")
    public ApiResponse<CommonDtos.PracticeSessionDTO> create(@Valid @RequestBody CommonDtos.PracticeSessionCreateRequest request) {
        return ApiResponse.ok(practiceService.createSession(userService.currentUserEntity().getId(), request.getData()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取练习会话")
    public ApiResponse<CommonDtos.PracticeSessionDTO> get(@PathVariable String id) {
        return ApiResponse.ok(practiceService.getSession(userService.currentUserEntity().getId(), id));
    }

    @PostMapping("/{id}/answers")
    @Operation(summary = "提交练习答案", description = "提交单选或多选答案并返回判题结果、正确答案和解析。")
    public ApiResponse<CommonDtos.SubmitAnswerDTO> submit(@PathVariable String id, @Valid @RequestBody CommonDtos.SubmitAnswerRequest request) {
        return ApiResponse.ok(practiceService.submitAnswer(userService.currentUserEntity().getId(), id, request.getData()));
    }

    @PatchMapping("/{id}/essay-steps")
    @Operation(summary = "更新步骤题完成状态")
    public ApiResponse<CommonDtos.EssayStepDTO> essaySteps(@PathVariable String id, @Valid @RequestBody CommonDtos.EssayStepsRequest request) {
        return ApiResponse.ok(practiceService.updateEssaySteps(userService.currentUserEntity().getId(), id, request.getData()));
    }

    @PostMapping("/{id}/finish")
    @Operation(summary = "结束练习")
    public ApiResponse<CommonDtos.FinishPracticeDTO> finish(@PathVariable String id) {
        return ApiResponse.ok(practiceService.finishSession(userService.currentUserEntity().getId(), id));
    }

    @GetMapping("/{id}/review")
    @Operation(summary = "获取练习复盘")
    public ApiResponse<CommonDtos.ReviewDTO> review(@PathVariable String id) {
        return ApiResponse.ok(practiceService.reviewSession(userService.currentUserEntity().getId(), id));
    }
}
