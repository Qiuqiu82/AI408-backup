package org.example.ai408.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SubjectCatalog {
    public record SubjectMeta(String subjectCode, String subjectName, String shortName) {
    }

    private static final List<SubjectMeta> SUBJECTS = List.of(
            new SubjectMeta("DS", "数据结构", "数据结构"),
            new SubjectMeta("CO", "计算机组成原理", "组成原理"),
            new SubjectMeta("OS", "操作系统", "操作系统"),
            new SubjectMeta("CN", "计算机网络", "计算机网络"),
            new SubjectMeta("MOCK", "整套模拟", "整套模拟")
    );

    private static final Map<String, SubjectMeta> SUBJECT_MAP = SUBJECTS.stream()
            .collect(java.util.stream.Collectors.toMap(SubjectMeta::subjectCode, subject -> subject, (a, b) -> a, LinkedHashMap::new));

    private SubjectCatalog() {
    }

    public static List<SubjectMeta> list() {
        return SUBJECTS;
    }

    public static SubjectMeta get(String subjectCode) {
        return SUBJECT_MAP.get(subjectCode);
    }
}
