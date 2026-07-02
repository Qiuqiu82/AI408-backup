package org.example.ai408.controller;

import jakarta.validation.Valid;
import org.example.ai408.common.ApiResponse;
import org.example.ai408.common.PageResponse;
import org.example.ai408.dto.CommonDtos;
import org.example.ai408.service.StateService;
import org.example.ai408.service.UserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class StateController {
    private final StateService stateService;
    private final UserService userService;

    public StateController(StateService stateService, UserService userService) {
        this.stateService = stateService;
        this.userService = userService;
    }

    @PostMapping("/wrong-book/page")
    public ApiResponse<PageResponse<CommonDtos.WrongBookRecordDTO>> wrongBook(@Valid @RequestBody CommonDtos.StatePageRequest request) {
        return ApiResponse.ok(stateService.pageWrongBook(userService.currentUserEntity().getId(), request.getData()));
    }

    @PostMapping("/favorites/page")
    public ApiResponse<PageResponse<CommonDtos.FavoriteRecordDTO>> favorites(@Valid @RequestBody CommonDtos.StatePageRequest request) {
        return ApiResponse.ok(stateService.pageFavorites(userService.currentUserEntity().getId(), request.getData()));
    }

    @PatchMapping("/question-states/{questionId}")
    public ApiResponse<CommonDtos.StateUpdateResultDTO> patchState(@PathVariable String questionId, @Valid @RequestBody CommonDtos.QuestionStatePatchRequest request) {
        return ApiResponse.ok(stateService.patchQuestionState(userService.currentUserEntity().getId(), questionId, request.getData()));
    }

    @DeleteMapping("/wrong-book")
    public ApiResponse<CommonDtos.ClearResultDTO> clearWrongBook() {
        return ApiResponse.ok(stateService.clearWrongBook(userService.currentUserEntity().getId()));
    }

    @DeleteMapping("/favorites")
    public ApiResponse<CommonDtos.ClearResultDTO> clearFavorites() {
        return ApiResponse.ok(stateService.clearFavorites(userService.currentUserEntity().getId()));
    }
}
