package org.example.ai408.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ExamDtos {
    private ExamDtos() {
    }

    public record ExamPaperQuestionDTO(
            String questionId,
            int orderNo,
            String subjectCode,
            String subjectName,
            String questionType,
            String title,
            String stem,
            String stemImageUrl,
            List<CommonDtos.OptionDTO> options,
            List<String> steps,
            List<String> tags,
            Boolean newType,
            Integer favoriteImportance
    ) {
    }

    public record ExamPaperDTO(
            String paperId,
            int durationSeconds,
            String generatedAt,
            int totalCount,
            String currentQuestionId,
            List<ExamPaperQuestionDTO> questions
    ) {
    }

    public record ExamRecordSummaryDTO(
            String recordId,
            int score,
            int totalCount,
            int answeredCount,
            int correctCount,
            int wrongCount,
            int durationSeconds,
            String submittedAt
    ) {
    }

    public record ExamQuestionReviewDTO(
            String questionId,
            int orderNo,
            String subjectCode,
            String subjectName,
            String questionType,
            String title,
            String stem,
            String stemImageUrl,
            List<CommonDtos.OptionDTO> options,
            List<String> steps,
            List<String> tags,
            Boolean newType,
            List<String> userAnswer,
            List<Boolean> stepStatus,
            List<String> correctAnswer,
            Boolean isCorrect,
            String analysis
    ) {
    }

    public record ExamRecordDetailDTO(
            String recordId,
            int score,
            int totalCount,
            int answeredCount,
            int correctCount,
            int wrongCount,
            int durationSeconds,
            String submittedAt,
            List<String> wrongQuestionIds,
            List<ExamQuestionReviewDTO> wrongQuestions
    ) {
    }

    public static class ExamPaperCreateRequest {
        @Valid
        @NotNull
        @JsonProperty("data")
        private Payload data = new Payload();

        public Payload getData() {
            return data;
        }

        public void setData(Payload data) {
            this.data = data;
        }

        public static class Payload {
            private Integer limit;

            public Integer getLimit() {
                return limit;
            }

            public void setLimit(Integer limit) {
                this.limit = limit;
            }
        }
    }

    public static class ExamSubmitRequest {
        @Valid
        @NotNull
        @JsonProperty("data")
        private Payload data;

        public Payload getData() {
            return data;
        }

        public void setData(Payload data) {
            this.data = data;
        }

        public static class Payload {
            @NotNull
            private List<String> questionIds = new ArrayList<>();
            private Integer durationSeconds;
            @NotNull
            private List<AnswerItem> answers = new ArrayList<>();

            public List<String> getQuestionIds() {
                return questionIds;
            }

            public void setQuestionIds(List<String> questionIds) {
                this.questionIds = questionIds;
            }

            public Integer getDurationSeconds() {
                return durationSeconds;
            }

            public void setDurationSeconds(Integer durationSeconds) {
                this.durationSeconds = durationSeconds;
            }

            public List<AnswerItem> getAnswers() {
                return answers;
            }

            public void setAnswers(List<AnswerItem> answers) {
                this.answers = answers;
            }
        }

        public static class AnswerItem {
            @NotNull
            private String questionId;
            private List<String> answer = new ArrayList<>();
            private List<Boolean> stepStatus = new ArrayList<>();

            public String getQuestionId() {
                return questionId;
            }

            public void setQuestionId(String questionId) {
                this.questionId = questionId;
            }

            public List<String> getAnswer() {
                return answer;
            }

            public void setAnswer(List<String> answer) {
                this.answer = answer;
            }

            public List<Boolean> getStepStatus() {
                return stepStatus;
            }

            public void setStepStatus(List<Boolean> stepStatus) {
                this.stepStatus = stepStatus;
            }
        }
    }
}
