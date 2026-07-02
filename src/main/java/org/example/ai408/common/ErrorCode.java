package org.example.ai408.common;

public enum ErrorCode {
    BAD_REQUEST("40000", "请求参数错误"),
    MOBILE_INVALID("40001", "手机号格式错误"),
    CODE_INVALID("40002", "验证码错误或已过期"),
    LOGIN_FAILED("40003", "登录失败"),
    UNAUTHORIZED("40100", "未登录"),
    TOKEN_EXPIRED("40101", "登录已过期"),
    REFRESH_TOKEN_INVALID("40102", "刷新令牌无效"),
    FORBIDDEN("40300", "无权限访问"),
    NOT_FOUND("40400", "资源不存在"),
    IMPORT_JOB_NOT_FOUND("40401", "导入任务不存在"),
    QUESTION_NOT_FOUND("40402", "题目不存在"),
    SESSION_NOT_FOUND("40403", "练习会话不存在"),
    REVIEW_NOT_FOUND("40404", "复盘结果不存在"),
    USER_NOT_FOUND("40405", "用户不存在"),
    VALIDATION_FAILED("42200", "参数校验失败"),
    IMPORT_FORMAT_INVALID("42202", "文件格式不正确"),
    ANSWER_FORMAT_INVALID("42203", "答案格式不正确"),
    STEP_LENGTH_INVALID("42204", "步骤数量不匹配"),
    STATE_INVALID("42205", "状态参数不合法"),
    FILE_EMPTY("42206", "文件为空"),
    CONFLICT("40900", "资源冲突"),
    SESSION_FINISHED("40901", "练习会话已结束"),
    AI_PROVIDER_ERROR("50300", "AI 服务调用失败"),
    INTERNAL_ERROR("999", "处理失败");

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
