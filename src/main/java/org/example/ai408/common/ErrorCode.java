package org.example.ai408.common;

public enum ErrorCode {
    BAD_REQUEST("40000", "Bad request"),
    MOBILE_INVALID("40001", "Invalid mobile number"),
    CODE_INVALID("40002", "Invalid or expired code"),
    UNAUTHORIZED("40100", "Unauthorized"),
    TOKEN_EXPIRED("40101", "Token expired"),
    FORBIDDEN("40300", "Forbidden"),
    NOT_FOUND("40400", "Not found"),
    QUESTION_NOT_FOUND("40402", "Question not found"),
    SESSION_NOT_FOUND("40403", "Session not found"),
    REVIEW_NOT_FOUND("40404", "Review not found"),
    VALIDATION_FAILED("42200", "Validation failed"),
    IMPORT_FORMAT_INVALID("42202", "Invalid file format"),
    ANSWER_FORMAT_INVALID("42203", "Invalid answer format"),
    STEP_LENGTH_INVALID("42204", "Step length mismatch"),
    STATE_INVALID("42205", "Invalid state"),
    CONFLICT("40900", "Conflict"),
    SESSION_FINISHED("40901", "Session finished"),
    INTERNAL_ERROR("999", "Internal error");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
