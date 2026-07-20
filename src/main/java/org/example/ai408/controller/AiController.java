package org.example.ai408.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.ai408.dto.CommonDtos;
import org.example.ai408.service.AiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI 讲解", description = "题目 AI 流式讲解")
public class AiController {
    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping(value = "/explanations/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式生成题目讲解", description = "通过 SSE 返回 delta、done、error 事件。")
    public SseEmitter stream(@Valid @RequestBody CommonDtos.AIExplainRequest request) {
        return aiService.streamExplanation(request.getData());
    }
}
