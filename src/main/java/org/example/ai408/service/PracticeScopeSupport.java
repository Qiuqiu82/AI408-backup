package org.example.ai408.service;

import org.example.ai408.domain.QuestionEntity;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PracticeScopeSupport {
    static final String PAPER = "paper";
    static final String KNOWLEDGE_POINT = "knowledgePoint";
    static final String SUBJECT = "subject";
    static final String CUSTOM = "custom";

    private static final Pattern YEAR_PATTERN = Pattern.compile("(?<!\\d)(20\\d{2})(?!\\d)");
    private static final Set<String> STRUCTURAL_TAGS = Set.of("single", "multiple", "essay");

    private PracticeScopeSupport() {
    }

    static String normalizeType(String scopeType) {
        return scopeType == null ? "" : scopeType.trim();
    }

    static String paperYear(QuestionEntity question) {
        String year = findYear(question.getSource());
        return year == null ? findYear(question.getQuestionCode()) : year;
    }

    static boolean matchesKnowledgePoint(QuestionEntity question, String scopeKey) {
        if (scopeKey == null || scopeKey.isBlank()) {
            return false;
        }
        return Support.parseStringList(question.getTagsJson()).stream()
                .anyMatch(tag -> scopeKey.equalsIgnoreCase(tag));
    }

    static List<String> knowledgePoints(QuestionEntity question) {
        return Support.parseStringList(question.getTagsJson()).stream()
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .filter(tag -> !STRUCTURAL_TAGS.contains(tag.toLowerCase(Locale.ROOT)))
                .distinct()
                .toList();
    }

    private static String findYear(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Matcher matcher = YEAR_PATTERN.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }
}
