package org.example.ai408.controller;

import jakarta.validation.Valid;
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
public class ExamController {
    private final ExamService examService;
    private final UserService userService;

    public ExamController(ExamService examService, UserService userService) {
        this.examService = examService;
        this.userService = userService;
    }

    @PostMapping("/papers")
    public ApiResponse<ExamDtos.ExamPaperDTO> createPaper(@Valid @RequestBody ExamDtos.ExamPaperCreateRequest request) {
        return ApiResponse.ok(examService.createPaper(userService.currentUserEntity().getId(), request.getData()));
    }

    @PostMapping("/records")
    public ApiResponse<ExamDtos.ExamRecordSummaryDTO> submitRecord(@Valid @RequestBody ExamDtos.ExamSubmitRequest request) {
        return ApiResponse.ok(examService.submitPaper(userService.currentUserEntity().getId(), request.getData()));
    }

    @GetMapping("/records")
    public ApiResponse<List<ExamDtos.ExamRecordSummaryDTO>> listRecords() {
        return ApiResponse.ok(examService.listRecords(userService.currentUserEntity().getId()));
    }

    @GetMapping("/records/{id}")
    public ApiResponse<ExamDtos.ExamRecordDetailDTO> getRecord(@PathVariable String id) {
        return ApiResponse.ok(examService.getRecordDetail(userService.currentUserEntity().getId(), id));
    }
}
