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
    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是 AI408 刷题系统里的 AI 讲解老师。
            请使用中文，像经验丰富的 408 老师带学生复盘一样进行讲解。
            输出必须使用以下四个小标题：
            一、解题思路
            二、正确答案
            三、易错点
            四、知识点总结

            要求：
            - 先讲思路，再讲答案和总结。
            - 可以参考题库解析，但不要直接照抄、不要复读原文。
            - 如果用户答错，要明确指出错因；如果用户答对，要说明为什么这个答案成立。
            - 如果用户未作答，要按“未作答”处理。
            - 如果题目包含图片，要结合图片信息讲解。
            - 语言简洁、自然、适合 408 备考。
            """;

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
                generateExplanation(emitter, question, sessionQuestion, payload.getUserAnswer());
                emitter.send(SseEmitter.event().name("done").data(new CommonDtos.AIStreamEventDTO("done", "")));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                sendError(emitter, userFriendlyMessage(exception));
            } catch (Exception exception) {
                sendError(emitter, userFriendlyMessage(exception));
            } finally {
                emitter.complete();
            }
        });
        return emitter;
    }

    private void generateExplanation(
            SseEmitter emitter,
            QuestionEntity question,
            PracticeSessionQuestionEntity sessionQuestion,
            List<String> userAnswer
    ) throws IOException, InterruptedException {
        if (appProperties.ai().mockEnabled()) {
            emitText(emitter, buildMockExplanation(question, sessionQuestion, userAnswer));
            return;
        }

        String stemImageUrl = resolveStemImageUrl(question, sessionQuestion);
        boolean hasImage = !stemImageUrl.isBlank();
        requireApiKey();
        if (hasImage) {
            resolveVisionModel();
        }
        streamFromDashScope(emitter, question, sessionQuestion, userAnswer, stemImageUrl, hasImage);
    }

    private void streamFromDashScope(
            SseEmitter emitter,
            QuestionEntity question,
            PracticeSessionQuestionEntity sessionQuestion,
            List<String> userAnswer,
            String stemImageUrl,
            boolean hasImage
    ) throws IOException, InterruptedException {
        String prompt = buildPrompt(question, sessionQuestion, userAnswer, hasImage);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", hasImage ? resolveVisionModel() : resolveTextModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", resolveSystemPrompt()),
                Map.of("role", "user", "content", hasImage ? buildVisionContent(prompt, stemImageUrl) : prompt)
        ));
        body.put("stream", true);
        body.put("temperature", 0.35);

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
            throw new IllegalStateException(buildUpstreamErrorMessage(response.statusCode(), errorBody));
        }

        boolean emittedAnyChunk = false;
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
                JsonNode errorNode = node.path("error");
                if (!errorNode.isMissingNode() && !errorNode.isNull()) {
                    throw new IllegalStateException(buildUpstreamErrorMessage(200, errorNode.toString()));
                }
                JsonNode choices = node.path("choices");
                if (!choices.isArray() || choices.isEmpty()) {
                    continue;
                }
                JsonNode choice = choices.get(0);
                String chunk = extractChunk(choice.path("delta").path("content"));
                if (chunk.isBlank()) {
                    chunk = extractChunk(choice.path("message").path("content"));
                }
                if (chunk.isBlank()) {
                    continue;
                }
                emittedAnyChunk = true;
                emitter.send(SseEmitter.event().name("delta").data(new CommonDtos.AIStreamEventDTO("delta", chunk)));
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Qwen 返回了无法解析的流式数据，请稍后重试。", exception);
        }

        if (!emittedAnyChunk) {
            throw new IllegalStateException("Qwen 未返回有效讲解内容，请稍后重试。");
        }
    }

    private void emitText(SseEmitter emitter, String text) throws IOException, InterruptedException {
        for (String part : splitText(text, 28)) {
            emitter.send(SseEmitter.event().name("delta").data(new CommonDtos.AIStreamEventDTO("delta", part)));
            Thread.sleep(35L);
        }
    }

    private String buildMockExplanation(QuestionEntity question, PracticeSessionQuestionEntity sessionQuestion, List<String> userAnswer) {
        return String.join("\n\n",
                "一、解题思路\n先抓住题干的核心考点，再对照选项逐一排除，优先解释为什么正确答案成立。",
                "二、正确答案\n" + resolveCorrectAnswerText(question),
                "三、易错点\n你的作答情况：" + resolveUserAnswerText(question, sessionQuestion, userAnswer) + "。如果和标准答案不一致，重点复盘概念边界与干扰项。",
                "四、知识点总结\n" + resolveAnalysis(question, sessionQuestion)
        );
    }

    private String buildPrompt(
            QuestionEntity question,
            PracticeSessionQuestionEntity sessionQuestion,
            List<String> userAnswer,
            boolean hasImage
    ) {
        return """
                请根据下面这道 408 题目生成一段新的讲解。
                你可以参考“题库解析”，但只能把它当作内部参考资料，不要直接照抄，不要复读原文。
                讲解必须用中文，且必须使用以下四个小标题：
                一、解题思路
                二、正确答案
                三、易错点
                四、知识点总结

                题型：%s
                题目标题：%s
                题干：
                %s

                题目配图：%s

                选项：
                %s

                用户答案：%s
                标准答案：%s
                当前判定：%s

                题库解析（仅供内部参考，不要直接复述）：
                %s

                请重新组织为老师讲题风格。
                如果用户答错，请明确指出错因；如果用户答对，请说明为什么该答案成立。
                """.formatted(
                Support.safe(question.getQuestionType()),
                Support.safe(question.getTitle()),
                Support.safe(question.getStem()),
                hasImage ? "有，请结合随附图片一起讲解。" : "无",
                resolveOptionsText(question),
                resolveUserAnswerText(question, sessionQuestion, userAnswer),
                resolveCorrectAnswerText(question),
                resolveJudgementText(sessionQuestion),
                resolveAnalysis(question, sessionQuestion)
        );
    }

    private List<Map<String, Object>> buildVisionContent(String prompt, String stemImageUrl) {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", prompt));
        content.add(Map.of("type", "image_url", "image_url", Map.of("url", stemImageUrl)));
        return content;
    }

    private void requireApiKey() {
        if (appProperties.ai().apiKey() == null || appProperties.ai().apiKey().isBlank()) {
            throw new IllegalStateException("未配置 DashScope API Key，请在 Spring Boot 运行配置的 Environment variables 中设置 DASHSCOPE_API_KEY。");
        }
    }

    private String resolveTextModel() {
        if (appProperties.ai().model() == null || appProperties.ai().model().isBlank()) {
            return "qwen-plus";
        }
        return appProperties.ai().model();
    }

    private String resolveVisionModel() {
        if (appProperties.ai().visionModel() == null || appProperties.ai().visionModel().isBlank()) {
            throw new IllegalStateException("当前题目包含图片，但未配置 AI408_QWEN_VISION_MODEL，暂时无法生成图片题讲解。");
        }
        return appProperties.ai().visionModel();
    }

    private String resolveSystemPrompt() {
        String configured = appProperties.ai().systemPrompt();
        if (configured == null || configured.isBlank()) {
            return DEFAULT_SYSTEM_PROMPT;
        }
        return configured;
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
        String analysis = Support.safe(question.getAnalysis());
        return analysis.isBlank() ? "题库中未提供解析，请结合题意、选项和标准答案自行归纳知识点。" : analysis;
    }

    private String resolveOptionsText(QuestionEntity question) {
        List<String> options = Support.parseOptions(question.getOptionsJson()).stream()
                .map(option -> option.key() + ". " + option.text())
                .toList();
        return options.isEmpty() ? "无" : String.join("\n", options);
    }

    private String resolveUserAnswerText(
            QuestionEntity question,
            PracticeSessionQuestionEntity sessionQuestion,
            List<String> userAnswer
    ) {
        List<String> answers = userAnswer == null ? List.of() : userAnswer.stream()
                .filter(item -> item != null && !item.isBlank())
                .toList();
        if (answers.isEmpty() && sessionQuestion != null) {
            answers = Support.parseStringList(sessionQuestion.getAnswerJson());
        }
        if (!answers.isEmpty()) {
            return String.join(" / ", answers);
        }
        if ("essay".equalsIgnoreCase(question.getQuestionType()) && sessionQuestion != null) {
            List<Boolean> steps = Support.parseBooleanList(sessionQuestion.getStepStatusJson());
            if (!steps.isEmpty()) {
                long completed = steps.stream().filter(Boolean.TRUE::equals).count();
                return "未填写答案，当前步骤完成情况：" + completed + "/" + steps.size();
            }
        }
        return "未作答";
    }

    private String resolveCorrectAnswerText(QuestionEntity question) {
        List<String> answers = Support.parseStringList(question.getAnswerJson());
        if (!answers.isEmpty()) {
            return String.join(" / ", answers);
        }
        List<String> steps = Support.parseStringList(question.getStepsJson());
        if (!steps.isEmpty()) {
            return "参考步骤：" + String.join("；", steps);
        }
        return "题库中未提供标准答案。";
    }

    private String resolveJudgementText(PracticeSessionQuestionEntity sessionQuestion) {
        if (sessionQuestion == null) {
            return "未判定";
        }
        if (Boolean.TRUE.equals(sessionQuestion.getIsCorrect())) {
            return "答对";
        }
        if (Boolean.FALSE.equals(sessionQuestion.getIsCorrect())) {
            return "答错";
        }
        return "未判定";
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

    private String buildUpstreamErrorMessage(int statusCode, String rawBody) {
        String summary;
        if (statusCode == 401 || statusCode == 403) {
            summary = "DashScope 鉴权失败，请检查 DASHSCOPE_API_KEY 是否正确。";
        } else if (statusCode == 429) {
            summary = "DashScope 请求过于频繁，请稍后重试。";
        } else if (statusCode >= 500) {
            summary = "DashScope 服务暂时不可用，请稍后重试。";
        } else {
            summary = "Qwen 调用失败，请稍后重试。";
        }
        String detail = extractErrorMessage(rawBody);
        return detail.isBlank() ? summary : summary + " 上游信息：" + detail;
    }

    private String extractErrorMessage(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return "";
        }
        String normalized = rawBody.replaceAll("\\s+", " ").trim();
        try {
            JsonNode node = JsonUtils.MAPPER.readTree(rawBody);
            String message = node.path("error").path("message").asText("");
            if (message.isBlank()) {
                message = node.path("message").asText("");
            }
            if (message.isBlank()) {
                message = node.path("msg").asText("");
            }
            if (!message.isBlank()) {
                normalized = message.trim();
            }
        } catch (Exception ignored) {
        }
        return normalized.length() > 240 ? normalized.substring(0, 240) : normalized;
    }

    private String userFriendlyMessage(Exception exception) {
        if (exception instanceof IllegalStateException && exception.getMessage() != null && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }
        if (exception instanceof IOException) {
            return "Qwen 流式响应中断，请稍后重试。";
        }
        if (exception instanceof InterruptedException) {
            return "AI 讲解已中断，请稍后重试。";
        }
        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }
        return "AI 讲解生成失败，请稍后重试。";
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(new CommonDtos.AIStreamEventDTO("error", message)));
        } catch (IOException ignored) {
        }
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
