package org.example.ai408.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.ai408.common.ApiResponse;
import org.example.ai408.common.PageResponse;
import org.example.ai408.dto.CommonDtos;
import org.example.ai408.service.StateService;
import org.example.ai408.service.UserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "个人题目状态", description = "当前用户的错题本、收藏和题目状态")
public class StateController {
    private final StateService stateService;
    private final UserService userService;

    public StateController(StateService stateService, UserService userService) {
        this.stateService = stateService;
        this.userService = userService;
    }

    @PostMapping("/wrong-book/page")
    @Operation(summary = "分页查询错题本")
    public ApiResponse<PageResponse<CommonDtos.WrongBookRecordDTO>> wrongBook(@Valid @RequestBody CommonDtos.StatePageRequest request) {
        return ApiResponse.ok(stateService.pageWrongBook(userService.currentUserEntity().getId(), request.getData()));
    }

    @GetMapping("/wrong-book/stats")
    @Operation(summary = "查询错题统计")
    public ApiResponse<CommonDtos.WrongBookStatsDTO> wrongBookStats() {
        return ApiResponse.ok(stateService.wrongBookStats(userService.currentUserEntity().getId()));
    }

    @PostMapping("/favorites/page")
    @Operation(summary = "分页查询收藏题目")
    public ApiResponse<PageResponse<CommonDtos.FavoriteRecordDTO>> favorites(@Valid @RequestBody CommonDtos.StatePageRequest request) {
        return ApiResponse.ok(stateService.pageFavorites(userService.currentUserEntity().getId(), request.getData()));
    }

    @PatchMapping("/question-states/{questionId}")
    @Operation(summary = "更新题目状态", description = "更新收藏等级、个人笔记和错题本状态。")
    public ApiResponse<CommonDtos.StateUpdateResultDTO> patchState(@PathVariable String questionId, @Valid @RequestBody CommonDtos.QuestionStatePatchRequest request) {
        return ApiResponse.ok(stateService.patchQuestionState(userService.currentUserEntity().getId(), questionId, request.getData()));
    }

    @DeleteMapping("/wrong-book")
    @Operation(summary = "清空错题本")
    public ApiResponse<CommonDtos.ClearResultDTO> clearWrongBook() {
        return ApiResponse.ok(stateService.clearWrongBook(userService.currentUserEntity().getId()));
    }

    @DeleteMapping("/favorites")
    @Operation(summary = "清空收藏")
    public ApiResponse<CommonDtos.ClearResultDTO> clearFavorites() {
        return ApiResponse.ok(stateService.clearFavorites(userService.currentUserEntity().getId()));
    }
}
