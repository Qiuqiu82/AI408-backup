package org.example.ai408.service;

import org.example.ai408.common.BusinessException;
import org.example.ai408.common.ErrorCode;
import org.example.ai408.domain.ExamRecordEntity;
import org.example.ai408.domain.ExamRecordQuestionEntity;
import org.example.ai408.domain.IdGenerator;
import org.example.ai408.domain.QuestionEntity;
import org.example.ai408.domain.UserQuestionStateEntity;
import org.example.ai408.dto.CommonDtos;
import org.example.ai408.dto.ExamDtos;
import org.example.ai408.repository.ExamRecordQuestionRepository;
import org.example.ai408.repository.ExamRecordRepository;
import org.example.ai408.repository.QuestionRepository;
import org.example.ai408.repository.UserQuestionStateRepository;
import org.example.ai408.util.JsonUtils;
import org.example.ai408.util.TimeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ExamService {
    private static final int EXAM_DURATION_SECONDS = 180 * 60;
    private static final int DEFAULT_LIMIT = 25;
    private static final int MAX_LIMIT = 50;

    private final QuestionRepository questionRepository;
    private final UserQuestionStateRepository stateRepository;
    private final StateService stateService;
    private final ExamRecordRepository examRecordRepository;
    private final ExamRecordQuestionRepository examRecordQuestionRepository;

    public ExamService(
            QuestionRepository questionRepository,
            UserQuestionStateRepository stateRepository,
            StateService stateService,
            ExamRecordRepository examRecordRepository,
            ExamRecordQuestionRepository examRecordQuestionRepository
    ) {
        this.questionRepository = questionRepository;
        this.stateRepository = stateRepository;
        this.stateService = stateService;
        this.examRecordRepository = examRecordRepository;
        this.examRecordQuestionRepository = examRecordQuestionRepository;
    }

    public ExamDtos.ExamPaperDTO createPaper(String userId, ExamDtos.ExamPaperCreateRequest.Payload payload) {
        List<QuestionEntity> selectedQuestions = selectQuestions(payload == null ? null : payload.getLimit());
        if (selectedQuestions.isEmpty()) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }

        Map<String, UserQuestionStateEntity> stateMap = stateRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserQuestionStateEntity::getQuestionId, state -> state, (a, b) -> a));

        List<ExamDtos.ExamPaperQuestionDTO> questions = new ArrayList<>();
        for (int i = 0; i < selectedQuestions.size(); i++) {
            QuestionEntity question = selectedQuestions.get(i);
            UserQuestionStateEntity state = stateMap.get(question.getId());
            questions.add(new ExamDtos.ExamPaperQuestionDTO(
                    question.getId(),
                    i + 1,
                    question.getSubjectCode(),
                    question.getSubjectName(),
                    question.getQuestionType(),
                    question.getTitle(),
                    Support.safe(question.getStem()),
                    Support.safe(question.getStemImageUrl()),
                    Support.parseOptions(question.getOptionsJson()),
                    Support.parseStringList(question.getStepsJson()),
                    Support.parseStringList(question.getTagsJson()),
                    question.getNewType(),
                    state == null || state.getFavoriteImportance() == null ? 0 : state.getFavoriteImportance()
            ));
        }

        return new ExamDtos.ExamPaperDTO(
                IdGenerator.prefixed("paper"),
                EXAM_DURATION_SECONDS,
                TimeUtils.format(LocalDateTime.now()),
                questions.size(),
                questions.get(0).questionId(),
                questions
        );
    }

    @Transactional
    public ExamDtos.ExamRecordSummaryDTO submitPaper(String userId, ExamDtos.ExamSubmitRequest.Payload payload) {
        if (payload == null || payload.getQuestionIds() == null || payload.getQuestionIds().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        List<String> orderedQuestionIds = payload.getQuestionIds().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (orderedQuestionIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        Map<String, QuestionEntity> questionMap = questionRepository.findAllById(orderedQuestionIds).stream()
                .collect(Collectors.toMap(QuestionEntity::getId, question -> question));
        if (questionMap.size() != orderedQuestionIds.size()) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }

        Map<String, ExamDtos.ExamSubmitRequest.AnswerItem> answerMap = payload.getAnswers().stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getQuestionId() != null && !item.getQuestionId().isBlank())
                .collect(Collectors.toMap(
                        item -> item.getQuestionId().trim(),
                        item -> item,
                        (first, second) -> second,
                        LinkedHashMap::new
                ));

        ExamRecordEntity record = new ExamRecordEntity();
        record.setId(IdGenerator.prefixed("exam"));
        record.setUserId(userId);

        List<ExamRecordQuestionEntity> recordQuestions = new ArrayList<>();
        int answeredCount = 0;
        int correctCount = 0;
        for (int i = 0; i < orderedQuestionIds.size(); i++) {
            QuestionEntity question = questionMap.get(orderedQuestionIds.get(i));
            ExamDtos.ExamSubmitRequest.AnswerItem answerItem = answerMap.get(question.getId());
            List<String> userAnswer = normalizeAnswer(answerItem == null ? List.of() : answerItem.getAnswer());
            List<Boolean> stepStatus = normalizeStepStatus(answerItem == null ? List.of() : answerItem.getStepStatus());
            boolean answered = isAnswered(question, userAnswer, stepStatus);
            boolean isCorrect = isCorrect(question, userAnswer, stepStatus);
            if (answered) {
                answeredCount++;
            }
            if (isCorrect) {
                correctCount++;
            }

            stateService.applyAnswerResult(userId, question, isCorrect);

            ExamRecordQuestionEntity entity = new ExamRecordQuestionEntity();
            entity.setId(IdGenerator.prefixed("eq"));
            entity.setRecordId(record.getId());
            entity.setQuestionId(question.getId());
            entity.setOrderNo(i + 1);
            entity.setSubjectCode(question.getSubjectCode());
            entity.setSubjectName(question.getSubjectName());
            entity.setQuestionType(question.getQuestionType());
            entity.setTitle(question.getTitle());
            entity.setStem(question.getStem());
            entity.setStemImageUrl(question.getStemImageUrl());
            entity.setOptionsJson(question.getOptionsJson());
            entity.setStepsJson(question.getStepsJson());
            entity.setTagsJson(question.getTagsJson());
            entity.setNewType(question.getNewType());
            entity.setUserAnswerJson(JsonUtils.write(userAnswer));
            entity.setStepStatusJson(JsonUtils.write(stepStatus));
            entity.setCorrectAnswerJson(question.getAnswerJson());
            entity.setIsCorrect(isCorrect);
            entity.setAnalysis(Support.safe(question.getAnalysis()));
            recordQuestions.add(entity);
        }

        int totalCount = orderedQuestionIds.size();
        int wrongCount = totalCount - correctCount;
        int score = totalCount == 0 ? 0 : (int) Math.round(correctCount * 100.0 / totalCount);
        record.setScore(score);
        record.setTotalCount(totalCount);
        record.setAnsweredCount(answeredCount);
        record.setCorrectCount(correctCount);
        record.setWrongCount(wrongCount);
        record.setDurationSeconds(clampDuration(payload.getDurationSeconds()));
        record.setSubmittedAt(TimeUtils.format(LocalDateTime.now()));

        examRecordRepository.save(record);
        examRecordQuestionRepository.saveAll(recordQuestions);
        return toSummary(record);
    }

    public List<ExamDtos.ExamRecordSummaryDTO> listRecords(String userId) {
        return examRecordRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    public ExamDtos.ExamRecordDetailDTO getRecordDetail(String userId, String recordId) {
        ExamRecordEntity record = examRecordRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        List<ExamRecordQuestionEntity> recordQuestions = examRecordQuestionRepository.findByRecordIdOrderByOrderNoAsc(recordId);
        List<ExamDtos.ExamQuestionReviewDTO> wrongQuestions = recordQuestions.stream()
                .filter(question -> !Boolean.TRUE.equals(question.getIsCorrect()))
                .map(this::toReviewQuestion)
                .toList();
        List<String> wrongQuestionIds = wrongQuestions.stream()
                .map(ExamDtos.ExamQuestionReviewDTO::questionId)
                .toList();
        return new ExamDtos.ExamRecordDetailDTO(
                record.getId(),
                valueOrZero(record.getScore()),
                valueOrZero(record.getTotalCount()),
                valueOrZero(record.getAnsweredCount()),
                valueOrZero(record.getCorrectCount()),
                valueOrZero(record.getWrongCount()),
                valueOrZero(record.getDurationSeconds()),
                record.getSubmittedAt(),
                wrongQuestionIds,
                wrongQuestions
        );
    }

    private List<QuestionEntity> selectQuestions(Integer requestedLimit) {
        List<QuestionEntity> allQuestions = questionRepository.findAll().stream()
                .sorted(Comparator.comparing(QuestionEntity::getSortNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(QuestionEntity::getQuestionCode, Comparator.nullsLast(String::compareTo)))
                .toList();
        List<QuestionEntity> pool = allQuestions.stream()
                .filter(question -> !"MOCK".equalsIgnoreCase(question.getSubjectCode()))
                .collect(Collectors.toCollection(ArrayList::new));
        if (pool.isEmpty()) {
            pool = new ArrayList<>(allQuestions);
        }
        Collections.shuffle(pool);
        int limit = requestedLimit == null || requestedLimit <= 0 ? DEFAULT_LIMIT : Math.min(MAX_LIMIT, requestedLimit);
        if (pool.size() > limit) {
            return new ArrayList<>(pool.subList(0, limit));
        }
        return pool;
    }

    private boolean isAnswered(QuestionEntity question, List<String> userAnswer, List<Boolean> stepStatus) {
        if ("essay".equals(question.getQuestionType())) {
            return stepStatus.stream().anyMatch(Boolean.TRUE::equals) || !userAnswer.isEmpty();
        }
        return !userAnswer.isEmpty();
    }

    private boolean isCorrect(QuestionEntity question, List<String> userAnswer, List<Boolean> stepStatus) {
        List<String> correctAnswer = Support.parseStringList(question.getAnswerJson());
        if ("multiple".equals(question.getQuestionType())) {
            return new LinkedHashSet<>(userAnswer).equals(new LinkedHashSet<>(correctAnswer));
        }
        if ("essay".equals(question.getQuestionType())) {
            List<String> steps = Support.parseStringList(question.getStepsJson());
            if (!stepStatus.isEmpty()) {
                return stepStatus.size() == steps.size() && stepStatus.stream().allMatch(Boolean.TRUE::equals);
            }
            return new LinkedHashSet<>(userAnswer).equals(new LinkedHashSet<>(correctAnswer));
        }
        String actual = userAnswer.isEmpty() ? "" : userAnswer.get(0);
        String expected = correctAnswer.isEmpty() ? "" : correctAnswer.get(0);
        return expected.equalsIgnoreCase(actual);
    }

    private List<String> normalizeAnswer(List<String> answers) {
        if (answers == null) {
            return List.of();
        }
        return answers.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    private List<Boolean> normalizeStepStatus(List<Boolean> stepStatus) {
        if (stepStatus == null) {
            return List.of();
        }
        return stepStatus.stream()
                .map(Boolean.TRUE::equals)
                .toList();
    }

    private int clampDuration(Integer durationSeconds) {
        if (durationSeconds == null || durationSeconds < 0) {
            return 0;
        }
        return Math.min(EXAM_DURATION_SECONDS, durationSeconds);
    }

    private ExamDtos.ExamRecordSummaryDTO toSummary(ExamRecordEntity record) {
        return new ExamDtos.ExamRecordSummaryDTO(
                record.getId(),
                valueOrZero(record.getScore()),
                valueOrZero(record.getTotalCount()),
                valueOrZero(record.getAnsweredCount()),
                valueOrZero(record.getCorrectCount()),
                valueOrZero(record.getWrongCount()),
                valueOrZero(record.getDurationSeconds()),
                record.getSubmittedAt()
        );
    }

    private ExamDtos.ExamQuestionReviewDTO toReviewQuestion(ExamRecordQuestionEntity question) {
        return new ExamDtos.ExamQuestionReviewDTO(
                question.getQuestionId(),
                valueOrZero(question.getOrderNo()),
                question.getSubjectCode(),
                question.getSubjectName(),
                question.getQuestionType(),
                question.getTitle(),
                Support.safe(question.getStem()),
                Support.safe(question.getStemImageUrl()),
                Support.parseOptions(question.getOptionsJson()),
                Support.parseStringList(question.getStepsJson()),
                Support.parseStringList(question.getTagsJson()),
                question.getNewType(),
                Support.parseStringList(question.getUserAnswerJson()),
                Support.parseBooleanList(question.getStepStatusJson()),
                Support.parseStringList(question.getCorrectAnswerJson()),
                question.getIsCorrect(),
                Support.safe(question.getAnalysis())
        );
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
