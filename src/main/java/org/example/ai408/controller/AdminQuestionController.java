package org.example.ai408.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "管理员题库", description = "管理员题库模板、异步导入任务和错误文件")
public class AdminQuestionController {
    private final ImportService importService;

    public AdminQuestionController(ImportService importService) {
        this.importService = importService;
    }

    @GetMapping("/template")
    @Operation(summary = "获取题库导入模板")
    public ApiResponse<CommonDtos.TemplateDTO> template() {
        return ApiResponse.ok(importService.template());
    }

    @PostMapping(value = "/import", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "提交题库导入", description = "上传 Excel/CSV 文件，支持 append 追加和 replace 替换。")
    public ApiResponse<CommonDtos.ImportJobDTO> importQuestions(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "importType", defaultValue = "append") String importType
    ) {
        return ApiResponse.ok(importService.submitImport(file, importType));
    }

    @GetMapping("/imports/{jobId}")
    @Operation(summary = "查询题库导入任务")
    public ApiResponse<CommonDtos.ImportJobDTO> importJob(@PathVariable String jobId) {
        return ApiResponse.ok(importService.getJob(jobId));
    }
}
