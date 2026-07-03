package org.example.ai408.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.ai408.common.BusinessException;
import org.example.ai408.common.ErrorCode;
import org.example.ai408.config.AppProperties;
import org.example.ai408.domain.PracticeSessionEntity;
import org.example.ai408.domain.PracticeSessionQuestionEntity;
import org.example.ai408.domain.QuestionEntity;
import org.example.ai408.dto.CommonDtos;
import org.example.ai408.repository.PracticeSessionQuestionRepository;
import org.example.ai408.repository.PracticeSessionRepository;
import org.example.ai408.repository.QuestionRepository;
import org.example.ai408.security.AuthenticatedUser;
import org.example.ai408.security.SecurityUtils;
import org.example.ai408.util.JsonUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class AiService {
    private final AppProperties appProperties;
    private final PracticeSessionRepository sessionRepository;
    private final PracticeSessionQuestionRepository sessionQuestionRepository;
    private final QuestionRepository questionRepository;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public AiService(
            AppProperties appProperties,
            PracticeSessionRepository sessionRepository,
            PracticeSessionQuestionRepository sessionQuestionRepository,
            QuestionRepository questionRepository
    ) {
        this.appProperties = appProperties;
        this.sessionRepository = sessionRepository;
        this.sessionQuestionRepository = sessionQuestionRepository;
        this.questionRepository = questionRepository;
    }

    public SseEmitter streamExplanation(CommonDtos.AIExplainRequest.Payload payload) {
        AuthenticatedUser principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        PracticeSessionEntity session = sessionRepository.findByIdAndUserId(payload.getSessionId(), principal.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        PracticeSessionQuestionEntity sessionQuestion = sessionQuestionRepository
                .findBySessionIdAndQuestionId(session.getId(), payload.getQuestionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        QuestionEntity question = questionRepository.findById(payload.getQuestionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        CompletableFuture.runAsync(() -> {
            try {
                if (appProperties.ai().mockEnabled()
                        || appProperties.ai().apiKey() == null
                        || appProperties.ai().apiKey().isBlank()) {
                    emitText(emitter, buildMockExplanation(question, sessionQuestion, payload.getUserAnswer()));
                } else {
                    streamFromDashScope(emitter, question, sessionQuestion, payload.getUserAnswer());
                }
                emitter.send(SseEmitter.event().name("done").data(new CommonDtos.AIStreamEventDTO("done", "")));
                emitter.complete();
            } catch (Exception exception) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(new CommonDtos.AIStreamEventDTO("error", exception.getMessage())));
                } catch (IOException ignored) {
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    private void streamFromDashScope(
            SseEmitter emitter,
            QuestionEntity question,
            PracticeSessionQuestionEntity sessionQuestion,
            List<String> userAnswer
    ) throws IOException, InterruptedException {
        String prompt = buildPrompt(question, sessionQuestion, userAnswer);
        String stemImageUrl = resolveStemImageUrl(question, sessionQuestion);
        boolean hasImage = !stemImageUrl.isBlank();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", hasImage ? resolveVisionModel() : appProperties.ai().model());
        body.put("messages", List.of(
                Map.of("role", "system", "content", appProperties.ai().systemPrompt()),
                Map.of("role", "user", "content", hasImage ? buildVisionContent(prompt, stemImageUrl) : prompt)
        ));
        body.put("stream", true);
        body.put("temperature", 0.2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl(appProperties.ai().baseUrl()) + "/chat/completions"))
                .header("Authorization", "Bearer " + appProperties.ai().apiKey())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtils.write(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() >= 300) {
            String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new IllegalStateException("dashscope status=" + response.statusCode() + ", body=" + errorBody);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isBlank()) {
                    continue;
                }
                if ("[DONE]".equalsIgnoreCase(data)) {
                    break;
                }
                JsonNode node = JsonUtils.MAPPER.readTree(data);
                JsonNode choices = node.path("choices");
                if (!choices.isArray() || choices.isEmpty()) {
                    continue;
                }
                JsonNode choice = choices.get(0);
                JsonNode contentNode = choice.path("delta").path("content");
                String chunk = extractChunk(contentNode);
                if (chunk.isBlank()) {
                    chunk = extractChunk(choice.path("message").path("content"));
                }
                if (!chunk.isBlank()) {
                    emitter.send(SseEmitter.event().name("delta").data(new CommonDtos.AIStreamEventDTO("delta", chunk)));
                }
            }
        }
    }

    private void emitText(SseEmitter emitter, String text) throws IOException, InterruptedException {
        for (String part : splitText(text, 28)) {
            emitter.send(SseEmitter.event().name("delta").data(new CommonDtos.AIStreamEventDTO("delta", part)));
            Thread.sleep(35L);
        }
    }

    private String buildMockExplanation(QuestionEntity question, PracticeSessionQuestionEntity sessionQuestion, List<String> userAnswer) {
        List<String> options = Support.parseOptions(question.getOptionsJson()).stream()
                .map(option -> option.key() + ". " + option.text())
                .toList();
        List<String> answer = Support.parseStringList(question.getAnswerJson());
        List<String> actualAnswer = userAnswer == null ? List.of() : userAnswer;
        List<String> lines = new ArrayList<>();
        lines.add("题型：" + question.getQuestionType());
        lines.add("题目：" + question.getTitle());
        lines.add("选项：" + String.join(" | ", options));
        if (!resolveStemImageUrl(question, sessionQuestion).isBlank()) {
            lines.add("配图：这道题包含题干配图，正式环境会调用视觉模型结合图片讲解。");
        }
        lines.add("你的答案：" + String.join(" / ", actualAnswer));
        lines.add("标准答案：" + String.join(" / ", answer));
        lines.add("答案解析：" + resolveAnalysis(question, sessionQuestion));
        lines.add("一句话总结：先抓题眼，再对照选项逐个排除。");
        return String.join("\n", lines);
    }

    private String buildPrompt(QuestionEntity question, PracticeSessionQuestionEntity sessionQuestion, List<String> userAnswer) {
        String imageLine = resolveStemImageUrl(question, sessionQuestion).isBlank()
                ? "题目配图：无"
                : "题目配图：请结合随附图片一起讲解";
        return String.join("\n",
                "请针对这道 408 题目做讲解。",
                "要求：使用中文，结构清晰，简洁直接，优先输出关键思路、标准答案、易错点和一句话总结。",
                "题目类型：" + question.getQuestionType(),
                "题目标题：" + question.getTitle(),
                "题干：" + Support.safe(question.getStem()),
                imageLine,
                "选项：" + Support.parseOptions(question.getOptionsJson()),
                "用户答案：" + String.join(" / ", userAnswer == null ? List.of() : userAnswer),
                "标准答案：" + Support.parseStringList(question.getAnswerJson()),
                "题目解析：" + resolveAnalysis(question, sessionQuestion),
                "当前判定：" + sessionQuestion.getQuestionStatus()
        );
    }

    private List<Map<String, Object>> buildVisionContent(String prompt, String stemImageUrl) {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", prompt));
        content.add(Map.of("type", "image_url", "image_url", Map.of("url", stemImageUrl)));
        return content;
    }

    private String resolveVisionModel() {
        if (appProperties.ai().visionModel() == null || appProperties.ai().visionModel().isBlank()) {
            throw new IllegalStateException("当前题目包含图片，但未配置 AI408_QWEN_VISION_MODEL，暂时无法生成视觉讲解。");
        }
        return appProperties.ai().visionModel();
    }

    private String resolveStemImageUrl(QuestionEntity question, PracticeSessionQuestionEntity sessionQuestion) {
        String sessionImage = sessionQuestion == null ? "" : Support.safe(sessionQuestion.getStemImageUrl());
        if (!sessionImage.isBlank()) {
            return sessionImage;
        }
        return Support.safe(question.getStemImageUrl());
    }

    private String resolveAnalysis(QuestionEntity question, PracticeSessionQuestionEntity sessionQuestion) {
        if (sessionQuestion != null && sessionQuestion.getAnalysis() != null && !sessionQuestion.getAnalysis().isBlank()) {
            return sessionQuestion.getAnalysis();
        }
        return Support.safe(question.getAnalysis());
    }

    private String extractChunk(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                String text = item.path("text").asText("");
                if (text.isBlank()) {
                    text = item.path("content").asText("");
                }
                builder.append(text);
            }
            return builder.toString();
        }
        return contentNode.path("text").asText("");
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private List<String> splitText(String text, int size) {
        List<String> parts = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return parts;
        }
        for (int i = 0; i < text.length(); i += size) {
            parts.add(text.substring(i, Math.min(text.length(), i + size)));
        }
        return parts;
    }
}
