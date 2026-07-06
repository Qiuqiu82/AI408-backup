package org.example.ai408.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ai408_exam_record_question")
public class ExamRecordQuestionEntity extends BaseEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "record_id", nullable = false, length = 64)
    private String recordId;

    @Column(name = "question_id", nullable = false, length = 64)
    private String questionId;

    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

    @Column(name = "subject_code", nullable = false, length = 20)
    private String subjectCode;

    @Column(name = "subject_name", nullable = false, length = 50)
    private String subjectName;

    @Column(name = "question_type", nullable = false, length = 20)
    private String questionType;

    @Column(nullable = false, length = 255)
    private String title;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String stem;

    @Column(name = "stem_image_url", length = 500)
    private String stemImageUrl;

    @Column(name = "options_json")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String optionsJson;

    @Column(name = "steps_json")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String stepsJson;

    @Column(name = "tags_json")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String tagsJson;

    @Column(name = "new_type")
    private Boolean newType;

    @Column(name = "user_answer_json")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String userAnswerJson;

    @Column(name = "step_status_json")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String stepStatusJson;

    @Column(name = "correct_answer_json")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String correctAnswerJson;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String analysis;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
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

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStem() {
        return stem;
    }

    public void setStem(String stem) {
        this.stem = stem;
    }

    public String getStemImageUrl() {
        return stemImageUrl;
    }

    public void setStemImageUrl(String stemImageUrl) {
        this.stemImageUrl = stemImageUrl;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }

    public String getStepsJson() {
        return stepsJson;
    }

    public void setStepsJson(String stepsJson) {
        this.stepsJson = stepsJson;
    }

    public String getTagsJson() {
        return tagsJson;
    }

    public void setTagsJson(String tagsJson) {
        this.tagsJson = tagsJson;
    }

    public Boolean getNewType() {
        return newType;
    }

    public void setNewType(Boolean newType) {
        this.newType = newType;
    }

    public String getUserAnswerJson() {
        return userAnswerJson;
    }

    public void setUserAnswerJson(String userAnswerJson) {
        this.userAnswerJson = userAnswerJson;
    }

    public String getStepStatusJson() {
        return stepStatusJson;
    }

    public void setStepStatusJson(String stepStatusJson) {
        this.stepStatusJson = stepStatusJson;
    }

    public String getCorrectAnswerJson() {
        return correctAnswerJson;
    }

    public void setCorrectAnswerJson(String correctAnswerJson) {
        this.correctAnswerJson = correctAnswerJson;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean correct) {
        isCorrect = correct;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }
}
