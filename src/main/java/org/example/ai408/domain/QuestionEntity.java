package org.example.ai408.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ai408_question")
public class QuestionEntity extends BaseEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "question_code", unique = true, length = 64)
    private String questionCode;

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

    @Column(name = "options_json")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String optionsJson;

    @Column(name = "answer_json")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String answerJson;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String analysis;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String note;

    @Column(name = "tags_json")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String tagsJson;

    @Column(name = "new_type")
    private Boolean newType;

    @Column(name = "steps_json")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String stepsJson;

    private Integer difficulty;

    @Column(name = "sort_no")
    private Integer sortNo;

    @Column(length = 50)
    private String source;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuestionCode() {
        return questionCode;
    }

    public void setQuestionCode(String questionCode) {
        this.questionCode = questionCode;
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

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }

    public String getAnswerJson() {
        return answerJson;
    }

    public void setAnswerJson(String answerJson) {
        this.answerJson = answerJson;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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

    public String getStepsJson() {
        return stepsJson;
    }

    public void setStepsJson(String stepsJson) {
        this.stepsJson = stepsJson;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
