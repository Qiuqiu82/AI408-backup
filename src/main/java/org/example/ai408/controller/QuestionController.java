package org.example.ai408.controller;

import jakarta.validation.Valid;
import org.example.ai408.common.ApiResponse;
import org.example.ai408.common.PageResponse;
import org.example.ai408.dto.CommonDtos;
import org.example.ai408.security.SecurityUtils;
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
public class QuestionController {
    private final QuestionService questionService;
    private final UserService userService;

    public QuestionController(QuestionService questionService, UserService userService) {
        this.questionService = questionService;
        this.userService = userService;
    }

    @GetMapping("/subjects")
    public ApiResponse<java.util.List<CommonDtos.SubjectDTO>> subjects() {
        return ApiResponse.ok(questionService.listSubjects(userService.currentUserEntity().getId()));
    }

    @PostMapping("/questions/page")
    public ApiResponse<PageResponse<CommonDtos.QuestionSummaryDTO>> pageQuestions(@Valid @RequestBody CommonDtos.QuestionPageRequest request) {
        return ApiResponse.ok(questionService.pageQuestions(userService.currentUserEntity().getId(), request.getData()));
    }

    @GetMapping("/questions/{id}")
    public ApiResponse<CommonDtos.QuestionDetailDTO> getQuestion(@PathVariable String id, @RequestParam(name = "view", defaultValue = "practice") String view) {
        return ApiResponse.ok(questionService.getQuestionDetail(id, view));
    }
}
