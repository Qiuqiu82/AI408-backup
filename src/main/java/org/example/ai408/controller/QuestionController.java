package org.example.ai408.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.ai408.common.ApiResponse;
import org.example.ai408.common.PageResponse;
import org.example.ai408.dto.CommonDtos;
import org.example.ai408.service.QuestionService;
import org.example.ai408.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "题库", description = "公共科目、题目分页和题目详情")
public class QuestionController {
    private final QuestionService questionService;
    private final UserService userService;

    public QuestionController(QuestionService questionService, UserService userService) {
        this.questionService = questionService;
        this.userService = userService;
    }

    @GetMapping("/subjects")
    @Operation(summary = "查询科目列表")
    public ApiResponse<java.util.List<CommonDtos.SubjectDTO>> subjects() {
        return ApiResponse.ok(questionService.listSubjects(userService.currentUserEntity().getId()));
    }

    @PostMapping("/questions/page")
    @Operation(summary = "分页查询题目", description = "支持科目、关键词、题型、标签、错题和收藏状态等筛选条件。")
    public ApiResponse<PageResponse<CommonDtos.QuestionSummaryDTO>> pageQuestions(@Valid @RequestBody CommonDtos.QuestionPageRequest request) {
        return ApiResponse.ok(questionService.pageQuestions(userService.currentUserEntity().getId(), request.getData()));
    }

    @GetMapping("/questions/{id}")
    @Operation(summary = "查询题目详情", description = "根据 view 和 sessionId 返回题目内容及当前用户状态。")
    public ApiResponse<CommonDtos.QuestionDetailDTO> getQuestion(
            @PathVariable String id,
            @RequestParam(name = "view", defaultValue = "practice") String view,
            @RequestParam(name = "sessionId", required = false) String sessionId
    ) {
        return ApiResponse.ok(questionService.getQuestionDetail(userService.currentUserEntity().getId(), id, view, sessionId));
    }
}
