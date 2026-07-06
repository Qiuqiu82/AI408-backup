package org.example.ai408.service;

import org.example.ai408.common.BusinessException;
import org.example.ai408.common.ErrorCode;
import org.example.ai408.domain.IdGenerator;
import org.example.ai408.domain.PracticeSessionEntity;
import org.example.ai408.domain.PracticeSessionQuestionEntity;
import org.example.ai408.domain.QuestionEntity;
import org.example.ai408.domain.UserQuestionStateEntity;
import org.example.ai408.dto.CommonDtos;
import org.example.ai408.repository.PracticeSessionQuestionRepository;
import org.example.ai408.repository.PracticeSessionRepository;
import org.example.ai408.repository.QuestionRepository;
import org.example.ai408.repository.UserQuestionStateRepository;
import org.example.ai408.util.JsonUtils;
import org.example.ai408.util.TimeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PracticeService {
    private final PracticeSessionRepository sessionRepository;
    private final PracticeSessionQuestionRepository sessionQuestionRepository;
    private final QuestionRepository questionRepository;
    private final UserQuestionStateRepository stateRepository;

    public PracticeService(
            PracticeSessionRepository sessionRepository,
            PracticeSessionQuestionRepository sessionQuestionRepository,
            QuestionRepository questionRepository,
            UserQuestionStateRepository stateRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.sessionQuestionRepository = sessionQuestionRepository;
        this.questionRepository = questionRepository;
        this.stateRepository = stateRepository;
    }

    @Transactional
    public CommonDtos.PracticeSessionDTO createSession(String userId, CommonDtos.PracticeSessionCreateRequest.Payload payload) {
        List<QuestionEntity> selectedQuestions = selectQuestions(userId, payload);
        if (selectedQuestions.isEmpty()) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }

        PracticeSessionEntity session = new PracticeSessionEntity();
        session.setId(IdGenerator.prefixed("sess"));
        session.setUserId(userId);
        session.setMode(payload.getMode());
        session.setSubjectCode(payload.getSubjectCode());
        session.setStatus("progressing");
        session.setTotalCount(selectedQuestions.size());
        session.setAnsweredCount(0);
        session.setCurrentQuestionId(selectedQuestions.get(0).getId());
        session.setQuestionIdsJson(JsonUtils.write(selectedQuestions.stream().map(QuestionEntity::getId).toList()));
        session.setStartedAt(TimeUtils.format(LocalDateTime.now()));
        session.setCorrectCount(0);
        session.setWrongCount(0);
        sessionRepository.save(session);

        List<PracticeSessionQuestionEntity> sessionQuestions = new ArrayList<>();
        for (int i = 0; i < selectedQuestions.size(); i++) {
            QuestionEntity question = selectedQuestions.get(i);
            PracticeSessionQuestionEntity entity = new PracticeSessionQuestionEntity();
            entity.setId(IdGenerator.prefixed("sq"));
            entity.setSessionId(session.getId());
            entity.setQuestionId(question.getId());
            entity.setOrderNo(i + 1);
            entity.setQuestionStatus("new");
            entity.setNewType(Boolean.TRUE.equals(question.getNewType()));
            entity.setStemImageUrl(Support.safe(question.getStemImageUrl()));
            entity.setAnswerJson("[]");
            entity.setIsCorrect(null);
            entity.setCorrectAnswerJson("[]");
            entity.setAnalysis("");
            entity.setStepStatusJson("[]");
            sessionQuestions.add(entity);
        }
        sessionQuestionRepository.saveAll(sessionQuestions);
        return toDto(session, sessionQuestions, selectedQuestions);
    }

    public CommonDtos.PracticeSessionDTO getSession(String userId, String sessionId) {
        PracticeSessionEntity session = loadSession(userId, sessionId);
        List<PracticeSessionQuestionEntity> sessionQuestions = sessionQuestionRepository.findBySessionIdOrderByOrderNoAsc(sessionId);
        List<QuestionEntity> questions = loadQuestions(sessionQuestions);
        ensureCurrentQuestion(session, sessionQuestions);
        return toDto(session, sessionQuestions, questions);
    }

    @Transactional
    public CommonDtos.SubmitAnswerDTO submitAnswer(String userId, String sessionId, CommonDtos.SubmitAnswerRequest.Payload payload) {
        PracticeSessionEntity session = loadSession(userId, sessionId);
        if ("finished".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.SESSION_FINISHED);
        }
        PracticeSessionQuestionEntity sessionQuestion = sessionQuestionRepository.findBySessionIdAndQuestionId(sessionId, payload.getQuestionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        if (sessionQuestion.getAnswerJson() != null && !"[]".equals(sessionQuestion.getAnswerJson()) && sessionQuestion.getIsCorrect() != null) {
            QuestionEntity question = questionRepository.findById(payload.getQuestionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
            return new CommonDtos.SubmitAnswerDTO(
                    question.getId(),
                    sessionQuestion.getIsCorrect(),
                    sessionQuestion.getQuestionStatus(),
                    Support.parseStringList(sessionQuestion.getCorrectAnswerJson()),
                    sessionQuestion.getAnalysis(),
                    Boolean.TRUE.equals(sessionQuestion.getIsCorrect()) ? Boolean.FALSE : Boolean.TRUE,
                    nextQuestionId(session, sessionQuestion.getOrderNo())
            );
        }

        QuestionEntity question = questionRepository.findById(payload.getQuestionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        List<String> userAnswer = payload.getAnswer() == null ? List.of() : normalizeAnswer(payload.getAnswer());
        List<String> correctAnswer = Support.parseStringList(question.getAnswerJson());
        boolean isCorrect = isCorrect(question.getQuestionType(), userAnswer, correctAnswer);
        sessionQuestion.setAnswerJson(JsonUtils.write(userAnswer));
        sessionQuestion.setCorrectAnswerJson(JsonUtils.write(correctAnswer));
        sessionQuestion.setAnalysis(Support.safe(question.getAnalysis()));
        sessionQuestion.setIsCorrect(isCorrect);
        sessionQuestion.setQuestionStatus(isCorrect ? "correct" : "wrong");
        sessionQuestion.setElapsedSeconds(payload.getElapsedSeconds());
        sessionQuestionRepository.save(sessionQuestion);

        upsertStateAfterAnswer(userId, question, isCorrect);
        refreshSessionProgress(session);

        return new CommonDtos.SubmitAnswerDTO(
                question.getId(),
                isCorrect,
                sessionQuestion.getQuestionStatus(),
                correctAnswer,
                Support.safe(question.getAnalysis()),
                !isCorrect,
                nextQuestionId(session, sessionQuestion.getOrderNo())
        );
    }

    @Transactional
    public CommonDtos.EssayStepDTO updateEssaySteps(String userId, String sessionId, CommonDtos.EssayStepsRequest.Payload payload) {
        PracticeSessionEntity session = loadSession(userId, sessionId);
        if ("finished".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.SESSION_FINISHED);
        }
        PracticeSessionQuestionEntity sessionQuestion = sessionQuestionRepository.findBySessionIdAndQuestionId(sessionId, payload.getQuestionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        QuestionEntity question = questionRepository.findById(payload.getQuestionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        List<Boolean> steps = payload.getSteps();
        List<Boolean> current = Support.parseBooleanList(sessionQuestion.getStepStatusJson());
        if (!current.isEmpty() && current.size() != steps.size()) {
            throw new BusinessException(ErrorCode.STEP_LENGTH_INVALID);
        }
        List<Boolean> normalized = new ArrayList<>(steps);
        sessionQuestion.setStepStatusJson(JsonUtils.write(normalized));
        boolean allDone = normalized.stream().allMatch(Boolean.TRUE::equals);
        if (allDone && !"correct".equals(sessionQuestion.getQuestionStatus())) {
            sessionQuestion.setQuestionStatus("correct");
            sessionQuestion.setIsCorrect(true);
            sessionQuestion.setCorrectAnswerJson(question.getAnswerJson());
            sessionQuestion.setAnalysis(Support.safe(question.getAnalysis()));
            upsertStateAfterAnswer(userId, question, true);
        } else if (!allDone && sessionQuestion.getQuestionStatus() == null) {
            sessionQuestion.setQuestionStatus("new");
        }
        sessionQuestionRepository.save(sessionQuestion);
        refreshSessionProgress(session);
        return new CommonDtos.EssayStepDTO(question.getId(), normalized, allDone, sessionQuestion.getQuestionStatus());
    }

    @Transactional
    public CommonDtos.FinishPracticeDTO finishSession(String userId, String sessionId) {
        PracticeSessionEntity session = loadSession(userId, sessionId);
        if ("finished".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.SESSION_FINISHED);
        }
        List<PracticeSessionQuestionEntity> sessionQuestions = sessionQuestionRepository.findBySessionIdOrderByOrderNoAsc(sessionId);
        int answeredCount = (int) sessionQuestions.stream().filter(this::isAnswered).count();
        int correctCount = (int) sessionQuestions.stream().filter(question -> Boolean.TRUE.equals(question.getIsCorrect())).count();
        int wrongCount = answeredCount - correctCount;
        int accuracy = answeredCount == 0 ? 0 : (int) Math.round(correctCount * 100.0 / answeredCount);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startedAt = TimeUtils.parse(session.getStartedAt());
        int durationSeconds = session.getDurationSeconds() != null
                ? session.getDurationSeconds()
                : (int) (startedAt == null ? 0 : Duration.between(startedAt, now).getSeconds());
        session.setStatus("finished");
        session.setAnsweredCount(answeredCount);
        session.setCorrectCount(correctCount);
        session.setWrongCount(wrongCount);
        session.setDurationSeconds(durationSeconds);
        session.setFinishedAt(TimeUtils.format(now));
        session.setReviewId(session.getReviewId() == null ? IdGenerator.prefixed("review") : session.getReviewId());
        sessionRepository.save(session);
        return new CommonDtos.FinishPracticeDTO(session.getId(), durationSeconds, answeredCount, correctCount, wrongCount, accuracy, session.getReviewId());
    }

    public CommonDtos.ReviewDTO reviewSession(String userId, String sessionId) {
        PracticeSessionEntity session = loadSession(userId, sessionId);
        List<PracticeSessionQuestionEntity> sessionQuestions = sessionQuestionRepository.findBySessionIdOrderByOrderNoAsc(sessionId);
        List<QuestionEntity> questions = loadQuestions(sessionQuestions);
        Map<String, QuestionEntity> questionMap = questions.stream()
                .collect(Collectors.toMap(QuestionEntity::getId, question -> question, (a, b) -> a, LinkedHashMap::new));
        int answeredCount = (int) sessionQuestions.stream().filter(this::isAnswered).count();
        int correctCount = (int) sessionQuestions.stream().filter(question -> Boolean.TRUE.equals(question.getIsCorrect())).count();
        int wrongCount = answeredCount - correctCount;
        int accuracy = answeredCount == 0 ? 0 : (int) Math.round(correctCount * 100.0 / answeredCount);
        int durationSeconds = session.getDurationSeconds() == null ? 0 : session.getDurationSeconds();

        List<String> wrongQuestionIds = sessionQuestions.stream()
                .filter(question -> Boolean.FALSE.equals(question.getIsCorrect()))
                .map(PracticeSessionQuestionEntity::getQuestionId)
                .toList();
        List<String> weakPoints = questions.stream()
                .filter(question -> wrongQuestionIds.contains(question.getId()))
                .flatMap(question -> Support.parseStringList(question.getTagsJson()).stream())
                .distinct()
                .limit(5)
                .toList();
        List<CommonDtos.ReviewQuestionDTO> wrongQuestions = sessionQuestions.stream()
                .filter(question -> Boolean.FALSE.equals(question.getIsCorrect()))
                .map(sessionQuestion -> toReviewQuestion(sessionQuestion, questionMap.get(sessionQuestion.getQuestionId())))
                .filter(Objects::nonNull)
                .toList();
        Map<String, CommonDtos.SubjectStatDTO> subjectStats = new LinkedHashMap<>();
        for (PracticeSessionQuestionEntity sessionQuestion : sessionQuestions) {
            QuestionEntity question = questionMap.get(sessionQuestion.getQuestionId());
            if (question == null) {
                continue;
            }
            CommonDtos.SubjectStatDTO stat = subjectStats.get(question.getSubjectCode());
            int correct = stat == null ? 0 : stat.correctCount();
            int wrong = stat == null ? 0 : stat.wrongCount();
            if (Boolean.TRUE.equals(sessionQuestion.getIsCorrect())) {
                correct++;
            } else if (Boolean.FALSE.equals(sessionQuestion.getIsCorrect())) {
                wrong++;
            }
            subjectStats.put(question.getSubjectCode(), new CommonDtos.SubjectStatDTO(question.getSubjectCode(), question.getSubjectName(), correct, wrong));
        }
        return new CommonDtos.ReviewDTO(
                accuracy,
                durationSeconds,
                answeredCount,
                wrongCount,
                wrongQuestionIds,
                wrongQuestions,
                weakPoints,
                new ArrayList<>(subjectStats.values())
        );
    }

    private List<QuestionEntity> selectQuestions(String userId, CommonDtos.PracticeSessionCreateRequest.Payload payload) {
        List<QuestionEntity> all = questionRepository.findAll().stream()
                .sorted(Comparator.comparing(QuestionEntity::getSortNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(QuestionEntity::getQuestionCode, Comparator.nullsLast(String::compareTo)))
                .toList();
        List<QuestionEntity> selected;
        if (payload.getQuestionIds() != null && !payload.getQuestionIds().isEmpty()) {
            Map<String, QuestionEntity> map = all.stream().collect(Collectors.toMap(QuestionEntity::getId, q -> q, (a, b) -> a));
            selected = payload.getQuestionIds().stream()
                    .map(map::get)
                    .filter(Objects::nonNull)
                    .toList();
        } else if ("wrongBook".equals(payload.getMode())) {
            selected = stateRepository.findByUserIdAndInWrongBookTrue(userId).stream()
                    .map(state -> questionRepository.findById(state.getQuestionId()).orElse(null))
                    .filter(Objects::nonNull)
                    .toList();
        } else if ("favorites".equals(payload.getMode())) {
            selected = stateRepository.findByUserIdAndFavoriteImportanceGreaterThan(userId, 0).stream()
                    .map(state -> questionRepository.findById(state.getQuestionId()).orElse(null))
                    .filter(Objects::nonNull)
                    .toList();
        } else {
            selected = all.stream()
                    .filter(question -> matchesSessionSubject(payload.getSubjectCode(), question))
                    .collect(Collectors.toList());
            if (selected.size() > 1) {
                java.util.Collections.shuffle(selected);
            }
        }
        int limit = payload.getLimit() == null || payload.getLimit() <= 0 ? 20 : payload.getLimit();
        if (selected.size() > limit) {
            selected = selected.subList(0, limit);
        }
        return selected;
    }

    private boolean matchesSessionSubject(String subjectCode, QuestionEntity question) {
        if (subjectCode == null || subjectCode.isBlank() || "MOCK".equals(subjectCode)) {
            return true;
        }
        return subjectCode.equals(question.getSubjectCode());
    }

    private PracticeSessionEntity loadSession(String userId, String sessionId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }

    private List<QuestionEntity> loadQuestions(List<PracticeSessionQuestionEntity> sessionQuestions) {
        Map<String, QuestionEntity> questionMap = questionRepository.findAll().stream()
                .collect(Collectors.toMap(QuestionEntity::getId, q -> q, (a, b) -> a, LinkedHashMap::new));
        return sessionQuestions.stream()
                .map(question -> questionMap.get(question.getQuestionId()))
                .filter(Objects::nonNull)
                .toList();
    }

    private void ensureCurrentQuestion(PracticeSessionEntity session, List<PracticeSessionQuestionEntity> sessionQuestions) {
        if (session.getCurrentQuestionId() != null) {
            return;
        }
        session.setCurrentQuestionId(sessionQuestions.stream()
                .filter(question -> !"correct".equals(question.getQuestionStatus()) && !"wrong".equals(question.getQuestionStatus()))
                .map(PracticeSessionQuestionEntity::getQuestionId)
                .findFirst()
                .orElse(sessionQuestions.isEmpty() ? null : sessionQuestions.get(0).getQuestionId()));
        sessionRepository.save(session);
    }

    private CommonDtos.PracticeSessionDTO toDto(
            PracticeSessionEntity session,
            List<PracticeSessionQuestionEntity> sessionQuestions,
            List<QuestionEntity> questions
    ) {
        Map<String, QuestionEntity> questionMap = questions.stream()
                .collect(Collectors.toMap(QuestionEntity::getId, q -> q, (a, b) -> a, LinkedHashMap::new));
        QuestionEntity current = questionMap.get(session.getCurrentQuestionId());
        if (current == null && !questions.isEmpty()) {
            current = questions.get(0);
        }
        List<CommonDtos.SessionQuestionBriefDTO> briefs = sessionQuestions.stream()
                .map(question -> Support.toBrief(question.getQuestionId(), question.getOrderNo(), question.getQuestionStatus(), question.getNewType()))
                .toList();
        return new CommonDtos.PracticeSessionDTO(
                session.getId(),
                session.getMode(),
                session.getStatus(),
                session.getSubjectCode(),
                session.getTotalCount() == null ? sessionQuestions.size() : session.getTotalCount(),
                session.getCurrentQuestionId(),
                session.getAnsweredCount() == null ? (int) sessionQuestions.stream().filter(this::isAnswered).count() : session.getAnsweredCount(),
                briefs,
                current == null ? null : Support.toCurrentQuestion(current)
        );
    }

    private boolean isAnswered(PracticeSessionQuestionEntity sessionQuestion) {
        return "correct".equals(sessionQuestion.getQuestionStatus()) || "wrong".equals(sessionQuestion.getQuestionStatus());
    }

    private CommonDtos.ReviewQuestionDTO toReviewQuestion(PracticeSessionQuestionEntity sessionQuestion, QuestionEntity question) {
        if (question == null) {
            return null;
        }
        List<String> userAnswer = Support.parseStringList(sessionQuestion.getAnswerJson());
        List<Boolean> stepStatus = Support.parseBooleanList(sessionQuestion.getStepStatusJson());
        List<String> correctAnswer = Support.parseStringList(sessionQuestion.getCorrectAnswerJson());
        if (correctAnswer.isEmpty()) {
            correctAnswer = Support.parseStringList(question.getAnswerJson());
        }
        String analysis = Support.safe(sessionQuestion.getAnalysis()).isBlank()
                ? Support.safe(question.getAnalysis())
                : Support.safe(sessionQuestion.getAnalysis());
        String stemImageUrl = Support.safe(sessionQuestion.getStemImageUrl()).isBlank()
                ? Support.safe(question.getStemImageUrl())
                : Support.safe(sessionQuestion.getStemImageUrl());
        return new CommonDtos.ReviewQuestionDTO(
                question.getId(),
                sessionQuestion.getOrderNo(),
                question.getSubjectCode(),
                question.getSubjectName(),
                question.getQuestionType(),
                question.getTitle(),
                question.getStem(),
                stemImageUrl,
                Support.parseOptions(question.getOptionsJson()),
                Support.parseStringList(question.getStepsJson()),
                Support.parseStringList(question.getTagsJson()),
                sessionQuestion.getNewType() != null ? sessionQuestion.getNewType() : question.getNewType(),
                userAnswer,
                stepStatus,
                correctAnswer,
                sessionQuestion.getIsCorrect(),
                analysis
        );
    }

    private void refreshSessionProgress(PracticeSessionEntity session) {
        List<PracticeSessionQuestionEntity> sessionQuestions = sessionQuestionRepository.findBySessionIdOrderByOrderNoAsc(session.getId());
        int answeredCount = (int) sessionQuestions.stream().filter(this::isAnswered).count();
        int correctCount = (int) sessionQuestions.stream().filter(question -> Boolean.TRUE.equals(question.getIsCorrect())).count();
        int wrongCount = answeredCount - correctCount;
        session.setAnsweredCount(answeredCount);
        session.setCorrectCount(correctCount);
        session.setWrongCount(wrongCount);
        session.setCurrentQuestionId(nextQuestionId(session, answeredCount > 0 ? sessionQuestions.stream().filter(this::isAnswered).reduce((first, second) -> second).map(PracticeSessionQuestionEntity::getOrderNo).orElse(1) : 1));
        sessionRepository.save(session);
    }

    private String nextQuestionId(PracticeSessionEntity session, int orderNo) {
        List<PracticeSessionQuestionEntity> sessionQuestions = sessionQuestionRepository.findBySessionIdOrderByOrderNoAsc(session.getId());
        return sessionQuestions.stream()
                .filter(question -> question.getOrderNo() > orderNo)
                .filter(question -> !"correct".equals(question.getQuestionStatus()) && !"wrong".equals(question.getQuestionStatus()))
                .map(PracticeSessionQuestionEntity::getQuestionId)
                .findFirst()
                .orElse(null);
    }

    private void upsertStateAfterAnswer(String userId, QuestionEntity question, boolean isCorrect) {
        UserQuestionStateEntity state = stateRepository.findByUserIdAndQuestionId(userId, question.getId())
                .orElseGet(() -> {
                    UserQuestionStateEntity entity = new UserQuestionStateEntity();
                    entity.setId(IdGenerator.prefixed("qs"));
                    entity.setUserId(userId);
                    entity.setQuestionId(question.getId());
                    entity.setQuestionStatus("new");
                    entity.setFavoriteImportance(0);
                    entity.setInWrongBook(false);
                    entity.setCorrectCount(0);
                    entity.setWrongCount(0);
                    entity.setEssayDone(false);
                    entity.setSelectedJson("[]");
                    entity.setStepStatusJson("[]");
                    return entity;
                });
        if (isCorrect) {
            state.setCorrectCount((state.getCorrectCount() == null ? 0 : state.getCorrectCount()) + 1);
            state.setQuestionStatus("correct");
            if (Boolean.TRUE.equals(state.getInWrongBook())) {
                state.setInWrongBook(false);
            }
        } else {
            state.setWrongCount((state.getWrongCount() == null ? 0 : state.getWrongCount()) + 1);
            state.setQuestionStatus("wrong");
            state.setInWrongBook(true);
            state.setLastWrongAt(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).toString());
        }
        stateRepository.save(state);
    }

    private boolean isCorrect(String questionType, List<String> userAnswer, List<String> correctAnswer) {
        if ("multiple".equals(questionType)) {
            return new LinkedHashSet<>(userAnswer).equals(new LinkedHashSet<>(correctAnswer));
        }
        if ("essay".equals(questionType)) {
            return new LinkedHashSet<>(userAnswer).equals(new LinkedHashSet<>(correctAnswer));
        }
        String actual = userAnswer.isEmpty() ? "" : userAnswer.get(0);
        String expected = correctAnswer.isEmpty() ? "" : correctAnswer.get(0);
        return expected.equalsIgnoreCase(actual);
    }

    private List<String> normalizeAnswer(List<String> answer) {
        return answer.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
