package org.example.ai408.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.ai408.common.ApiResponse;
import org.example.ai408.dto.ExamDtos;
import org.example.ai408.service.ExamService;
import org.example.ai408.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exams")
@Tag(name = "模拟考试", description = "试卷生成、交卷、考试记录和错题回顾")
public class ExamController {
    private final ExamService examService;
    private final UserService userService;

    public ExamController(ExamService examService, UserService userService) {
        this.examService = examService;
        this.userService = userService;
    }

    @PostMapping("/papers")
    @Operation(summary = "生成模拟试卷", description = "生成当前用户的模拟试卷，默认 25 题，后端最多允许 50 题。")
    public ApiResponse<ExamDtos.ExamPaperDTO> createPaper(@Valid @RequestBody ExamDtos.ExamPaperCreateRequest request) {
        return ApiResponse.ok(examService.createPaper(userService.currentUserEntity().getId(), request.getData()));
    }

    @PostMapping("/records")
    @Operation(summary = "提交模拟考试")
    public ApiResponse<ExamDtos.ExamRecordSummaryDTO> submitRecord(@Valid @RequestBody ExamDtos.ExamSubmitRequest request) {
        return ApiResponse.ok(examService.submitPaper(userService.currentUserEntity().getId(), request.getData()));
    }

    @GetMapping("/records")
    @Operation(summary = "查询考试记录列表")
    public ApiResponse<List<ExamDtos.ExamRecordSummaryDTO>> listRecords() {
        return ApiResponse.ok(examService.listRecords(userService.currentUserEntity().getId()));
    }

    @GetMapping("/records/{id}")
    @Operation(summary = "查询考试记录详情")
    public ApiResponse<ExamDtos.ExamRecordDetailDTO> getRecord(@PathVariable String id) {
        return ApiResponse.ok(examService.getRecordDetail(userService.currentUserEntity().getId(), id));
    }
}
