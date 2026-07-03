package org.example.ai408.service;

import org.example.ai408.common.BusinessException;
import org.example.ai408.common.ErrorCode;
import org.example.ai408.common.PageResponse;
import org.example.ai408.domain.PracticeSessionEntity;
import org.example.ai408.domain.PracticeSessionQuestionEntity;
import org.example.ai408.domain.QuestionEntity;
import org.example.ai408.domain.UserQuestionStateEntity;
import org.example.ai408.dto.CommonDtos;
import org.example.ai408.repository.PracticeSessionQuestionRepository;
import org.example.ai408.repository.PracticeSessionRepository;
import org.example.ai408.repository.QuestionRepository;
import org.example.ai408.repository.UserQuestionStateRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final UserQuestionStateRepository stateRepository;
    private final PracticeSessionRepository sessionRepository;
    private final PracticeSessionQuestionRepository sessionQuestionRepository;

    public QuestionService(
            QuestionRepository questionRepository,
            UserQuestionStateRepository stateRepository,
            PracticeSessionRepository sessionRepository,
            PracticeSessionQuestionRepository sessionQuestionRepository
    ) {
        this.questionRepository = questionRepository;
        this.stateRepository = stateRepository;
        this.sessionRepository = sessionRepository;
        this.sessionQuestionRepository = sessionQuestionRepository;
    }

    public List<CommonDtos.SubjectDTO> listSubjects(String userId) {
        List<QuestionEntity> questions = questionRepository.findAll();
        Map<String, UserQuestionStateEntity> stateMap = stateRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserQuestionStateEntity::getQuestionId, state -> state, (a, b) -> a));
        return SubjectCatalog.list().stream()
                .map(subject -> {
                    List<QuestionEntity> subjectQuestions = questions.stream()
                            .filter(question -> subject.subjectCode().equals(question.getSubjectCode()))
                            .toList();
                    int doneCount = (int) subjectQuestions.stream()
                            .filter(question -> isAnswered(stateMap.get(question.getId())))
                            .count();
                    int wrongCount = (int) subjectQuestions.stream()
                            .filter(question -> Boolean.TRUE.equals(stateMap.get(question.getId()) == null ? Boolean.FALSE : stateMap.get(question.getId()).getInWrongBook()))
                            .count();
                    return Support.toSubjectDto(
                            subject.subjectCode(),
                            subject.subjectName(),
                            subject.shortName(),
                            subjectQuestions.size(),
                            doneCount,
                            wrongCount
                    );
                })
                .toList();
    }

    public PageResponse<CommonDtos.QuestionSummaryDTO> pageQuestions(String userId, CommonDtos.QuestionPageRequest.Payload payload) {
        List<QuestionEntity> questions = questionRepository.findAll();
        Map<String, UserQuestionStateEntity> stateMap = stateRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserQuestionStateEntity::getQuestionId, state -> state, (a, b) -> a));
        List<QuestionEntity> filtered = questions.stream()
                .filter(question -> matchesQuestion(question, stateMap.get(question.getId()), payload))
                .sorted(Comparator.comparing(QuestionEntity::getSortNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(QuestionEntity::getQuestionCode, Comparator.nullsLast(String::compareTo)))
                .toList();
        int pageSize = payload.getPage() == null ? 20 : Math.max(1, payload.getPage().pageSize());
        int pageIndex = payload.getPage() == null ? 1 : Math.max(1, payload.getPage().pageIndex());
        int fromIndex = Math.min((pageIndex - 1) * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<CommonDtos.QuestionSummaryDTO> records = filtered.subList(fromIndex, toIndex).stream()
                .map(question -> Support.toQuestionSummary(question, stateMap.get(question.getId())))
                .toList();
        return new PageResponse<>(
                pageIndex,
                pageSize,
                Support.pageCount(filtered.size(), pageSize),
                filtered.size(),
                records
        );
    }

    public CommonDtos.QuestionDetailDTO getQuestionDetail(String userId, String id, String view, String sessionId) {
        QuestionEntity question = questionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        boolean reviewView = "review".equalsIgnoreCase(view);
        if (sessionId == null || sessionId.isBlank()) {
            return Support.toQuestionDetail(question, reviewView);
        }

        PracticeSessionEntity session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        PracticeSessionQuestionEntity sessionQuestion = sessionQuestionRepository
                .findBySessionIdAndQuestionId(session.getId(), id)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        String stemImageUrl = Support.safe(sessionQuestion.getStemImageUrl()).isBlank()
                ? Support.safe(question.getStemImageUrl())
                : Support.safe(sessionQuestion.getStemImageUrl());
        List<String> answer = reviewView && sessionQuestion.getCorrectAnswerJson() != null && !sessionQuestion.getCorrectAnswerJson().isBlank()
                ? Support.parseStringList(sessionQuestion.getCorrectAnswerJson())
                : (reviewView ? Support.parseStringList(question.getAnswerJson()) : List.of());
        String analysis = reviewView && sessionQuestion.getAnalysis() != null && !sessionQuestion.getAnalysis().isBlank()
                ? sessionQuestion.getAnalysis()
                : (reviewView ? Support.safe(question.getAnalysis()) : "");
        return Support.toQuestionDetail(question, reviewView, stemImageUrl, answer, analysis);
    }

    public List<QuestionEntity> findAllQuestions() {
        return questionRepository.findAll();
    }

    public QuestionEntity findById(String id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
    }

    public List<QuestionEntity> selectQuestionsForSession(String mode, String subjectCode, Integer limit, List<String> questionIds) {
        List<QuestionEntity> allQuestions = questionRepository.findAll();
        List<QuestionEntity> selected;
        if (questionIds != null && !questionIds.isEmpty()) {
            Map<String, QuestionEntity> map = allQuestions.stream().collect(Collectors.toMap(QuestionEntity::getId, q -> q, (a, b) -> a));
            selected = questionIds.stream()
                    .map(map::get)
                    .filter(Objects::nonNull)
                    .toList();
        } else if ("wrongBook".equals(mode) || "favorites".equals(mode)) {
            selected = allQuestions;
        } else if (subjectCode != null && !subjectCode.isBlank() && !"MOCK".equals(subjectCode)) {
            selected = allQuestions.stream()
                    .filter(question -> subjectCode.equals(question.getSubjectCode()))
                    .collect(Collectors.toCollection(ArrayList::new));
        } else {
            selected = new ArrayList<>(allQuestions);
        }
        if (selected.isEmpty()) {
            return selected;
        }
        if (questionIds == null || questionIds.isEmpty()) {
            java.util.Collections.shuffle(selected);
        }
        int finalLimit = limit == null || limit <= 0 ? 20 : limit;
        if (selected.size() > finalLimit) {
            selected = selected.subList(0, finalLimit);
        }
        return selected;
    }

    private boolean matchesQuestion(QuestionEntity question, UserQuestionStateEntity state, CommonDtos.QuestionPageRequest.Payload payload) {
        CommonDtos.QuestionPageRequest.Payload.Params params = payload.getParams();
        if (params != null) {
            if (params.getSubjectCode() != null && !params.getSubjectCode().isBlank() && !params.getSubjectCode().equals(question.getSubjectCode())) {
                return false;
            }
            if (params.getQuestionType() != null && !params.getQuestionType().isBlank() && !params.getQuestionType().equals(question.getQuestionType())) {
                return false;
            }
            if (params.getKeyword() != null && !params.getKeyword().isBlank()) {
                String keyword = params.getKeyword().trim().toLowerCase();
                String haystack = String.join(" ", List.of(
                        safe(question.getTitle()),
                        safe(question.getStem()),
                        safe(question.getSubjectName()),
                        safe(question.getQuestionCode()),
                        safe(question.getQuestionType())
                )).toLowerCase();
                if (!haystack.contains(keyword)) {
                    return false;
                }
            }
            if (params.getTag() != null && !params.getTag().isBlank()) {
                List<String> tags = Support.parseStringList(question.getTagsJson());
                if (tags.stream().noneMatch(tag -> tag.contains(params.getTag()))) {
                    return false;
                }
            }
            if (params.getNewType() != null) {
                boolean newType = Boolean.TRUE.equals(question.getNewType());
                if ((params.getNewType() == 1) != newType) {
                    return false;
                }
            }
            if (params.getQuestionStatus() != null && !params.getQuestionStatus().isBlank()) {
                String status = state == null ? "new" : state.getQuestionStatus();
                if (!params.getQuestionStatus().equals(status)) {
                    return false;
                }
            }
            if (params.getInWrongBook() != null) {
                boolean inWrongBook = state != null && Boolean.TRUE.equals(state.getInWrongBook());
                if ((params.getInWrongBook() == 1) != inWrongBook) {
                    return false;
                }
            }
            if (params.getInFavorites() != null) {
                boolean inFavorites = state != null && state.getFavoriteImportance() != null && state.getFavoriteImportance() > 0;
                if ((params.getInFavorites() == 1) != inFavorites) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isAnswered(UserQuestionStateEntity state) {
        return state != null && ("correct".equals(state.getQuestionStatus()) || "wrong".equals(state.getQuestionStatus()) || Boolean.TRUE.equals(state.getEssayDone()));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
