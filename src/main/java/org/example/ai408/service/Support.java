package org.example.ai408.service;

import org.example.ai408.common.BusinessException;
import org.example.ai408.common.ErrorCode;
import org.example.ai408.domain.QuestionEntity;
import org.example.ai408.domain.UserEntity;
import org.example.ai408.domain.UserQuestionStateEntity;
import org.example.ai408.dto.AuthDtos;
import org.example.ai408.dto.CommonDtos;
import org.example.ai408.util.JsonUtils;
import org.example.ai408.util.TimeUtils;

import java.util.Collections;
import java.util.List;

public final class Support {
    private Support() {
    }

    public static AuthDtos.UserDTO toUserDto(UserEntity user) {
        return new AuthDtos.UserDTO(
                user.getId(),
                user.getMobile(),
                user.getNickname(),
                user.getAvatarUrl() == null ? "" : user.getAvatarUrl(),
                user.getRole(),
                TimeUtils.format(user.getCreatedAt())
        );
    }

    public static CommonDtos.SubjectDTO toSubjectDto(String code, String name, String shortName, int totalCount, int doneCount, int wrongCount) {
        return new CommonDtos.SubjectDTO(code, name, shortName, totalCount, doneCount, wrongCount);
    }

    public static CommonDtos.QuestionSummaryDTO toQuestionSummary(QuestionEntity question, UserQuestionStateEntity state) {
        return new CommonDtos.QuestionSummaryDTO(
                question.getId(),
                question.getSubjectCode(),
                question.getQuestionType(),
                question.getTitle(),
                firstTag(question.getTagsJson(), question.getQuestionType()),
                question.getNewType(),
                state == null ? "new" : state.getQuestionStatus(),
                state == null || state.getFavoriteImportance() == null ? 0 : state.getFavoriteImportance(),
                state != null && Boolean.TRUE.equals(state.getInWrongBook())
        );
    }

    public static CommonDtos.QuestionDetailDTO toQuestionDetail(QuestionEntity question, boolean reviewView) {
        return toQuestionDetail(
                question,
                reviewView,
                safe(question.getStemImageUrl()),
                reviewView ? parseStringList(question.getAnswerJson()) : Collections.emptyList(),
                reviewView ? safe(question.getAnalysis()) : ""
        );
    }

    public static CommonDtos.QuestionDetailDTO toQuestionDetail(
            QuestionEntity question,
            boolean reviewView,
            String stemImageUrl,
            List<String> answer,
            String analysis
    ) {
        return new CommonDtos.QuestionDetailDTO(
                question.getId(),
                question.getSubjectCode(),
                question.getSubjectName(),
                question.getQuestionType(),
                question.getTitle(),
                question.getStem(),
                safe(stemImageUrl),
                reviewView ? parseOptions(question.getOptionsJson()) : parseOptions(question.getOptionsJson()),
                reviewView ? answer : Collections.emptyList(),
                reviewView ? safe(analysis) : "",
                parseStringList(question.getStepsJson()),
                parseStringList(question.getTagsJson()),
                question.getNewType(),
                safe(question.getNote())
        );
    }

    public static CommonDtos.SessionQuestionBriefDTO toBrief(String questionId, int orderNo, String status, Boolean newType) {
        return new CommonDtos.SessionQuestionBriefDTO(questionId, orderNo, status, newType);
    }

    public static CommonDtos.PracticeCurrentQuestionDTO toCurrentQuestion(QuestionEntity question) {
        return new CommonDtos.PracticeCurrentQuestionDTO(question.getId(), question.getQuestionType(), question.getTitle());
    }

    public static CommonDtos.ImportJobDTO toImportJobDto(org.example.ai408.domain.ImportJobEntity job) {
        return new CommonDtos.ImportJobDTO(
                job.getJobId(),
                job.getStatus(),
                job.getTotalCount(),
                job.getSuccessCount(),
                job.getFailedCount(),
                job.getErrorFileUrl(),
                TimeUtils.format(job.getUpdatedAt())
        );
    }

    public static List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        return JsonUtils.readStringList(json);
    }

    public static List<Boolean> parseBooleanList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        return JsonUtils.readBooleanList(json);
    }

    public static List<CommonDtos.OptionDTO> parseOptions(String json) {
        List<String> values = parseStringList(json);
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        return java.util.stream.IntStream.range(0, values.size())
                .mapToObj(i -> new CommonDtos.OptionDTO(String.valueOf((char) ('A' + i)), values.get(i)))
                .toList();
    }

    public static String firstTag(String tagsJson, String fallback) {
        List<String> tags = parseStringList(tagsJson);
        return tags.isEmpty() ? fallback : tags.get(0);
    }

    public static String safe(String value) {
        return value == null ? "" : value;
    }

    public static void require(boolean condition, ErrorCode errorCode) {
        if (!condition) {
            throw new BusinessException(errorCode);
        }
    }

    public static int pageCount(long recordCount, int pageSize) {
        if (pageSize <= 0) {
            return 0;
        }
        return (int) Math.ceil(recordCount * 1.0 / pageSize);
    }
}
