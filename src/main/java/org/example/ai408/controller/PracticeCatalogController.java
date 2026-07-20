package org.example.ai408.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.ai408.common.ApiResponse;
import org.example.ai408.dto.CommonDtos;
import org.example.ai408.service.PracticeCatalogService;
import org.example.ai408.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/practice")
@Tag(name = "练习目录", description = "按套卷、知识点等范围查询可练习题目集合")
public class PracticeCatalogController {
    private final PracticeCatalogService catalogService;
    private final UserService userService;

    public PracticeCatalogController(PracticeCatalogService catalogService, UserService userService) {
        this.catalogService = catalogService;
        this.userService = userService;
    }

    @GetMapping("/scopes")
    @Operation(summary = "查询练习范围", description = "当前支持 paper，knowledgePoint 契约已预留。")
    public ApiResponse<List<CommonDtos.PracticeScopeDTO>> scopes(
            @RequestParam(defaultValue = "paper") String type
    ) {
        return ApiResponse.ok(catalogService.listScopes(userService.currentUserEntity().getId(), type));
    }
}
