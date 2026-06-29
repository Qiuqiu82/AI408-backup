package org.example.ai408.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai408_session_question")
public class PracticeSessionQuestionEntity extends BaseEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "question_id", nullable = false, length = 64)
    private String questionId;

    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

    @Column(name = "question_status", nullable = false, length = 20)
    private String questionStatus;

    @Column(name = "new_type", nullable = false)
    private Boolean newType;

    @Lob
    @Column(name = "answer_json")
    private String answerJson;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Lob
    @Column(name = "correct_answer_json")
    private String correctAnswerJson;

    @Lob
    private String analysis;

    @Lob
    @Column(name = "step_status_json")
    private String stepStatusJson;

    @Column(name = "elapsed_seconds")
    private Integer elapsedSeconds;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    public String getQuestionStatus() {
        return questionStatus;
    }

    public void setQuestionStatus(String questionStatus) {
        this.questionStatus = questionStatus;
    }

    public Boolean getNewType() {
        return newType;
    }

    public void setNewType(Boolean newType) {
        this.newType = newType;
    }

    public String getAnswerJson() {
        return answerJson;
    }

    public void setAnswerJson(String answerJson) {
        this.answerJson = answerJson;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean correct) {
        isCorrect = correct;
    }

    public String getCorrectAnswerJson() {
        return correctAnswerJson;
    }

    public void setCorrectAnswerJson(String correctAnswerJson) {
        this.correctAnswerJson = correctAnswerJson;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public String getStepStatusJson() {
        return stepStatusJson;
    }

    public void setStepStatusJson(String stepStatusJson) {
        this.stepStatusJson = stepStatusJson;
    }

    public Integer getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void setElapsedSeconds(Integer elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }
}
