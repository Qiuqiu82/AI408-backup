package org.example.ai408.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai408_user_question_state")
public class UserQuestionStateEntity extends BaseEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "question_id", nullable = false, length = 64)
    private String questionId;

    @Column(name = "question_status", nullable = false, length = 20)
    private String questionStatus;

    @Column(name = "favorite_importance", nullable = false)
    private Integer favoriteImportance;

    @Lob
    private String note;

    @Column(name = "in_wrong_book", nullable = false)
    private Boolean inWrongBook;

    @Column(name = "last_wrong_at")
    private String lastWrongAt;

    @Column(name = "last_favorite_at")
    private String lastFavoriteAt;

    @Column(name = "correct_count", nullable = false)
    private Integer correctCount;

    @Column(name = "wrong_count", nullable = false)
    private Integer wrongCount;

    @Column(name = "essay_done", nullable = false)
    private Boolean essayDone;

    @Lob
    @Column(name = "selected_json")
    private String selectedJson;

    @Lob
    @Column(name = "step_status_json")
    private String stepStatusJson;

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

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getQuestionStatus() {
        return questionStatus;
    }

    public void setQuestionStatus(String questionStatus) {
        this.questionStatus = questionStatus;
    }

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

    public String getLastWrongAt() {
        return lastWrongAt;
    }

    public void setLastWrongAt(String lastWrongAt) {
        this.lastWrongAt = lastWrongAt;
    }

    public String getLastFavoriteAt() {
        return lastFavoriteAt;
    }

    public void setLastFavoriteAt(String lastFavoriteAt) {
        this.lastFavoriteAt = lastFavoriteAt;
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

    public Boolean getEssayDone() {
        return essayDone;
    }

    public void setEssayDone(Boolean essayDone) {
        this.essayDone = essayDone;
    }

    public String getSelectedJson() {
        return selectedJson;
    }

    public void setSelectedJson(String selectedJson) {
        this.selectedJson = selectedJson;
    }

    public String getStepStatusJson() {
        return stepStatusJson;
    }

    public void setStepStatusJson(String stepStatusJson) {
        this.stepStatusJson = stepStatusJson;
    }
}
