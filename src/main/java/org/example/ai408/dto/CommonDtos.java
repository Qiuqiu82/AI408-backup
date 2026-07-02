package org.example.ai408.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class CommonDtos {
    private CommonDtos() {
    }

    public record PageDto(int pageSize, int pageIndex) {
    }

    public record PageRequestDto(PageDto page) {
    }

    public record PageResponseDto<T>(int pageIndex, int pageSize, int pageCount, long recordCount, List<T> records) {
    }

    public record OptionDTO(String key, String text) {
    }

    public record SubjectDTO(String subjectCode, String subjectName, String shortName, int totalCount, int doneCount, int wrongCount) {
    }

    public record StudySummaryDTO(int answeredCount, int correctCount, int wrongCount, int favoriteCount, int todayWrongCount, int todayFavoriteCount, int progressRate, int sessionSeconds) {
    }

    public record TemplateDTO(String templateUrl, String version) {
    }

    public record ImportJobDTO(String jobId, String status, Integer totalCount, Integer successCount, Integer failedCount, String errorFileUrl, String updatedAt) {
    }

    public record QuestionSummaryDTO(
            String id,
            String subjectCode,
            String questionType,
            String title,
            String tag,
            Boolean newType,
            String questionStatus,
            Integer favoriteImportance,
            Boolean inWrongBook
    ) {
    }

    public record QuestionDetailDTO(
            String id,
            String subjectCode,
            String subjectName,
            String questionType,
            String title,
            String stem,
            List<OptionDTO> options,
            List<String> answer,
            String analysis,
            List<String> steps,
            List<String> tags,
            Boolean newType,
            String note
    ) {
    }

    public record SessionQuestionBriefDTO(String questionId, int orderNo, String questionStatus, Boolean newType) {
    }

    public record PracticeCurrentQuestionDTO(String id, String questionType, String title) {
    }

    public record PracticeSessionDTO(
            String sessionId,
            String mode,
            String status,
            String subjectCode,
            int totalCount,
            String currentQuestionId,
            int answeredCount,
            List<SessionQuestionBriefDTO> questionBriefList,
            PracticeCurrentQuestionDTO currentQuestion
    ) {
    }

    public record SubmitAnswerDTO(
            String questionId,
            Boolean isCorrect,
            String questionStatus,
            List<String> correctAnswer,
            String analysis,
            Boolean inWrongBook,
            String nextQuestionId
    ) {
    }

    public record EssayStepDTO(
            String questionId,
            List<Boolean> stepStatus,
            Boolean allDone,
            String questionStatus
    ) {
    }

    public record FinishPracticeDTO(
            String sessionId,
            int durationSeconds,
            int answeredCount,
            int correctCount,
            int wrongCount,
            int accuracy,
            String reviewId
    ) {
    }

    public record SubjectStatDTO(String subjectCode, String subjectName, int correctCount, int wrongCount) {
    }

    public record ReviewDTO(
            int accuracy,
            int durationSeconds,
            int answeredCount,
            int wrongCount,
            List<String> wrongQuestionIds,
            List<String> weakPoints,
            List<SubjectStatDTO> subjectStats
    ) {
    }

    public record StateUpdateDTO(Integer favoriteImportance, String note, Boolean inWrongBook) {
    }

    public record StateUpdateResultDTO(String questionId, Integer favoriteImportance, Boolean inWrongBook, String note) {
    }

    public record ClearResultDTO(int clearedCount) {
    }

    public record WrongBookRecordDTO(String questionId, String title, String subjectName, String tag, String wrongAt) {
    }

    public record FavoriteRecordDTO(String questionId, String title, String subjectName, Integer favoriteImportance, String favoriteAt) {
    }

    public record AIExplainRequestDTO(String sessionId, String questionId, List<String> userAnswer) {
    }

    public record AIStreamEventDTO(String type, String content) {
    }

    public static class StateRow {
        public String questionId;
        public String title;
        public String subjectName;
        public String tag;
        public String time;
        public boolean inWrongBook;
        public int favoriteImportance;
    }

    public static class QuestionPageRequest {
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
            @Valid
            @NotNull
            private PageDto page;
            private Params params = new Params();

            public PageDto getPage() {
                return page;
            }

            public void setPage(PageDto page) {
                this.page = page;
            }

            public Params getParams() {
                return params;
            }

            public void setParams(Params params) {
                this.params = params;
            }

            public static class Params {
                private String subjectCode;
                private String keyword;
                private String questionType;
                private String tag;
                private Integer newType;
                private String questionStatus;
                private Integer inWrongBook;
                private Integer inFavorites;

                public String getSubjectCode() {
                    return subjectCode;
                }

                public void setSubjectCode(String subjectCode) {
                    this.subjectCode = subjectCode;
                }

                public String getKeyword() {
                    return keyword;
                }

                public void setKeyword(String keyword) {
                    this.keyword = keyword;
                }

                public String getQuestionType() {
                    return questionType;
                }

                public void setQuestionType(String questionType) {
                    this.questionType = questionType;
                }

                public String getTag() {
                    return tag;
                }

                public void setTag(String tag) {
                    this.tag = tag;
                }

                public Integer getNewType() {
                    return newType;
                }

                public void setNewType(Integer newType) {
                    this.newType = newType;
                }

                public String getQuestionStatus() {
                    return questionStatus;
                }

                public void setQuestionStatus(String questionStatus) {
                    this.questionStatus = questionStatus;
                }

                public Integer getInWrongBook() {
                    return inWrongBook;
                }

                public void setInWrongBook(Integer inWrongBook) {
                    this.inWrongBook = inWrongBook;
                }

                public Integer getInFavorites() {
                    return inFavorites;
                }

                public void setInFavorites(Integer inFavorites) {
                    this.inFavorites = inFavorites;
                }
            }
        }
    }

    public static class StatePageRequest {
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
            @Valid
            @NotNull
            private PageDto page;
            private Params params = new Params();

            public PageDto getPage() {
                return page;
            }

            public void setPage(PageDto page) {
                this.page = page;
            }

            public Params getParams() {
                return params;
            }

            public void setParams(Params params) {
                this.params = params;
            }

            public static class Params {
                private String subjectCode;
                private String questionType;
                private String keyword;
                private String groupBy;

                public String getSubjectCode() {
                    return subjectCode;
                }

                public void setSubjectCode(String subjectCode) {
                    this.subjectCode = subjectCode;
                }

                public String getQuestionType() {
                    return questionType;
                }

                public void setQuestionType(String questionType) {
                    this.questionType = questionType;
                }

                public String getKeyword() {
                    return keyword;
                }

                public void setKeyword(String keyword) {
                    this.keyword = keyword;
                }

                public String getGroupBy() {
                    return groupBy;
                }

                public void setGroupBy(String groupBy) {
                    this.groupBy = groupBy;
                }
            }
        }
    }

    public static class PracticeSessionCreateRequest {
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
            @NotBlank
            private String mode;
            private String subjectCode;
            private Integer limit;
            private List<String> questionIds;
            private String source;

            public String getMode() {
                return mode;
            }

            public void setMode(String mode) {
                this.mode = mode;
            }

            public String getSubjectCode() {
                return subjectCode;
            }

            public void setSubjectCode(String subjectCode) {
                this.subjectCode = subjectCode;
            }

            public Integer getLimit() {
                return limit;
            }

            public void setLimit(Integer limit) {
                this.limit = limit;
            }

            public List<String> getQuestionIds() {
                return questionIds;
            }

            public void setQuestionIds(List<String> questionIds) {
                this.questionIds = questionIds;
            }

            public String getSource() {
                return source;
            }

            public void setSource(String source) {
                this.source = source;
            }
        }
    }

    public static class SubmitAnswerRequest {
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
            @NotBlank
            private String questionId;
            @NotNull
            private List<String> answer;
            private Integer elapsedSeconds;

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

            public Integer getElapsedSeconds() {
                return elapsedSeconds;
            }

            public void setElapsedSeconds(Integer elapsedSeconds) {
                this.elapsedSeconds = elapsedSeconds;
            }
        }
    }

    public static class EssayStepsRequest {
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
            @NotBlank
            private String questionId;
            @NotNull
            private List<Boolean> steps;

            public String getQuestionId() {
                return questionId;
            }

            public void setQuestionId(String questionId) {
                this.questionId = questionId;
            }

            public List<Boolean> getSteps() {
                return steps;
            }

            public void setSteps(List<Boolean> steps) {
                this.steps = steps;
            }
        }
    }

    public static class QuestionStatePatchRequest {
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
            private Integer favoriteImportance;
            @Size(max = 5000)
            private String note;
            private Boolean inWrongBook;

            public Integer getFavoriteImportance() {
                return favoriteImportance;
            }

            public void setFavoriteImportance(Integer favoriteImportance) {
                this.favoriteImportance = favoriteImportance;
            }

            public String getNote() {
                return note;
            }

            public void setNote(String note) {
                this.note = note;
            }

            public Boolean getInWrongBook() {
                return inWrongBook;
            }

            public void setInWrongBook(Boolean inWrongBook) {
                this.inWrongBook = inWrongBook;
            }
        }
    }

    public static class AIExplainRequest {
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
            @NotBlank
            private String sessionId;
            @NotBlank
            private String questionId;
            @NotNull
            private List<String> userAnswer;

            public String getSessionId() {
                return sessionId;
            }

            public void setSessionId(String sessionId) {
                this.sessionId = sessionId;
            }

            public String getQuestionId() {
                return questionId;
            }

            public void setQuestionId(String questionId) {
                this.questionId = questionId;
            }

            public List<String> getUserAnswer() {
                return userAnswer;
            }

            public void setUserAnswer(List<String> userAnswer) {
                this.userAnswer = userAnswer;
            }
        }
    }

    public static class UserUpdateRequest {
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
            @Size(max = 50)
            private String nickname;
            @Size(max = 255)
            private String avatarUrl;

            public String getNickname() {
                return nickname;
            }

            public void setNickname(String nickname) {
                this.nickname = nickname;
            }

            public String getAvatarUrl() {
                return avatarUrl;
            }

            public void setAvatarUrl(String avatarUrl) {
                this.avatarUrl = avatarUrl;
            }
        }
    }
}
