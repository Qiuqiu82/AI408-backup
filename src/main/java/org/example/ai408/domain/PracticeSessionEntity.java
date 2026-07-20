package org.example.ai408.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ai408_practice_session")
public class PracticeSessionEntity extends BaseEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, length = 20)
    private String mode;

    @Column(name = "subject_code", length = 20)
    private String subjectCode;

    @Column(name = "scope_type", length = 32)
    private String scopeType;

    @Column(name = "scope_key", length = 100)
    private String scopeKey;

    @Column(name = "scope_name", length = 255)
    private String scopeName;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Column(name = "answered_count", nullable = false)
    private Integer answeredCount;

    @Column(name = "current_question_id", length = 64)
    private String currentQuestionId;

    @Column(name = "question_ids_json")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String questionIdsJson;

    @Column(name = "review_id", length = 64)
    private String reviewId;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "wrong_count")
    private Integer wrongCount;

    @Column(name = "started_at", length = 32)
    private String startedAt;

    @Column(name = "finished_at", length = 32)
    private String finishedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

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

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getScopeKey() {
        return scopeKey;
    }

    public void setScopeKey(String scopeKey) {
        this.scopeKey = scopeKey;
    }

    public String getScopeName() {
        return scopeName;
    }

    public void setScopeName(String scopeName) {
        this.scopeName = scopeName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getAnsweredCount() {
        return answeredCount;
    }

    public void setAnsweredCount(Integer answeredCount) {
        this.answeredCount = answeredCount;
    }

    public String getCurrentQuestionId() {
        return currentQuestionId;
    }

    public void setCurrentQuestionId(String currentQuestionId) {
        this.currentQuestionId = currentQuestionId;
    }

    public String getQuestionIdsJson() {
        return questionIdsJson;
    }

    public void setQuestionIdsJson(String questionIdsJson) {
        this.questionIdsJson = questionIdsJson;
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(Integer correctCount) {
        this.correctCount = correctCount;
    }

    public Integer getWrongCount() {
        return wrongCount;
    }

    public void setWrongCount(Integer wrongCount) {
        this.wrongCount = wrongCount;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
    }
}
