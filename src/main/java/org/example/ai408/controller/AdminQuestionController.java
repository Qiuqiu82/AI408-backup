package org.example.ai408.controller;

import jakarta.validation.Valid;
import org.example.ai408.common.ApiResponse;
import org.example.ai408.dto.CommonDtos;
import org.example.ai408.service.ImportService;
import org.example.ai408.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/questions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuestionController {
    private final ImportService importService;

    public AdminQuestionController(ImportService importService) {
        this.importService = importService;
    }

    @GetMapping("/template")
    public ApiResponse<CommonDtos.TemplateDTO> template() {
        return ApiResponse.ok(importService.template());
    }

    @PostMapping(value = "/import", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CommonDtos.ImportJobDTO> importQuestions(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "importType", defaultValue = "append") String importType
    ) {
        return ApiResponse.ok(importService.submitImport(file, importType));
    }

    @GetMapping("/imports/{jobId}")
    public ApiResponse<CommonDtos.ImportJobDTO> importJob(@PathVariable String jobId) {
        return ApiResponse.ok(importService.getJob(jobId));
    }
}
