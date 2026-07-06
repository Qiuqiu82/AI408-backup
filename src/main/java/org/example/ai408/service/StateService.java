package org.example.ai408.service;

import org.example.ai408.common.BusinessException;
import org.example.ai408.common.ErrorCode;
import org.example.ai408.common.PageResponse;
import org.example.ai408.domain.QuestionEntity;
import org.example.ai408.domain.UserEntity;
import org.example.ai408.domain.UserQuestionStateEntity;
import org.example.ai408.dto.CommonDtos;
import org.example.ai408.repository.QuestionRepository;
import org.example.ai408.repository.UserRepository;
import org.example.ai408.repository.UserQuestionStateRepository;
import org.example.ai408.util.JsonUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class StateService {
    private final UserQuestionStateRepository stateRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    public StateService(UserQuestionStateRepository stateRepository, QuestionRepository questionRepository, UserRepository userRepository) {
        this.stateRepository = stateRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
    }

    public PageResponse<CommonDtos.WrongBookRecordDTO> pageWrongBook(String userId, CommonDtos.StatePageRequest.Payload payload) {
        return pageStates(userId, payload, true);
    }

    public CommonDtos.WrongBookStatsDTO wrongBookStats(String userId) {
        List<UserQuestionStateEntity> states = stateRepository.findByUserId(userId);
        String today = LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();
        List<UserQuestionStateEntity> wrongStates = states.stream()
                .filter(state -> Boolean.TRUE.equals(state.getInWrongBook()))
                .toList();
        List<String> wrongQuestionIds = wrongStates.stream()
                .map(UserQuestionStateEntity::getQuestionId)
                .toList();
        List<String> todayWrongQuestionIds = wrongStates.stream()
                .filter(state -> today.equals(state.getLastWrongAt()))
                .map(UserQuestionStateEntity::getQuestionId)
                .toList();
        int answeredCount = (int) states.stream()
                .filter(this::isAnswered)
                .count();
        int wrongRate = answeredCount == 0 ? 0 : (int) Math.round(wrongStates.size() * 100.0 / answeredCount);
        return new CommonDtos.WrongBookStatsDTO(
                wrongStates.size(),
                todayWrongQuestionIds.size(),
                answeredCount,
                wrongRate,
                wrongQuestionIds,
                todayWrongQuestionIds
        );
    }

    public PageResponse<CommonDtos.FavoriteRecordDTO> pageFavorites(String userId, CommonDtos.StatePageRequest.Payload payload) {
        return pageFavoritesInternal(userId, payload);
    }

    public CommonDtos.StateUpdateResultDTO patchQuestionState(String userId, String questionId, CommonDtos.QuestionStatePatchRequest.Payload payload) {
        QuestionEntity question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        UserQuestionStateEntity state = stateRepository.findByUserIdAndQuestionId(userId, questionId)
                .orElseGet(() -> createState(userId, questionId));
        if (payload.getFavoriteImportance() != null) {
            if (payload.getFavoriteImportance() < 0 || payload.getFavoriteImportance() > 2) {
                throw new BusinessException(ErrorCode.STATE_INVALID);
            }
            state.setFavoriteImportance(payload.getFavoriteImportance());
            if (payload.getFavoriteImportance() == 0) {
                state.setLastFavoriteAt(null);
            } else {
                state.setLastFavoriteAt(LocalDate.now(ZoneId.of("Asia/Shanghai")).toString());
            }
        }
        if (payload.getNote() != null) {
            state.setNote(payload.getNote());
        }
        if (payload.getInWrongBook() != null) {
            state.setInWrongBook(payload.getInWrongBook());
            if (!payload.getInWrongBook() && "wrong".equals(state.getQuestionStatus())) {
                state.setQuestionStatus(valueOrZero(state.getCorrectCount()) > 0 ? "correct" : "new");
            }
            if (!payload.getInWrongBook()) {
                state.setWrongBookResolveStreak(0);
            }
        }
        stateRepository.save(state);
        return new CommonDtos.StateUpdateResultDTO(questionId, state.getFavoriteImportance(), state.getInWrongBook(), state.getNote());
    }

    public CommonDtos.ClearResultDTO clearWrongBook(String userId) {
        List<UserQuestionStateEntity> states = stateRepository.findByUserId(userId);
        int cleared = 0;
        for (UserQuestionStateEntity state : states) {
            if (Boolean.TRUE.equals(state.getInWrongBook())) {
                cleared++;
                state.setInWrongBook(false);
                state.setWrongBookResolveStreak(0);
                if ("wrong".equals(state.getQuestionStatus())) {
                    state.setQuestionStatus(valueOrZero(state.getCorrectCount()) > 0 ? "correct" : "new");
                }
                stateRepository.save(state);
            }
        }
        return new CommonDtos.ClearResultDTO(cleared);
    }

    public CommonDtos.ClearResultDTO clearFavorites(String userId) {
        List<UserQuestionStateEntity> states = stateRepository.findByUserId(userId);
        int cleared = 0;
        for (UserQuestionStateEntity state : states) {
            if (valueOrZero(state.getFavoriteImportance()) > 0) {
                cleared++;
                state.setFavoriteImportance(0);
                state.setLastFavoriteAt(null);
                stateRepository.save(state);
            }
        }
        return new CommonDtos.ClearResultDTO(cleared);
    }

    public void applyAnswerResult(String userId, QuestionEntity question, boolean isCorrect) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        UserQuestionStateEntity state = stateRepository.findByUserIdAndQuestionId(userId, question.getId())
                .orElseGet(() -> createState(userId, question.getId()));
        if (isCorrect) {
            state.setCorrectCount(valueOrZero(state.getCorrectCount()) + 1);
            state.setQuestionStatus("correct");
            if (Boolean.TRUE.equals(state.getInWrongBook())) {
                int streak = valueOrZero(state.getWrongBookResolveStreak()) + 1;
                state.setWrongBookResolveStreak(streak);
                if (shouldAutoRemoveWrongBook(user, streak)) {
                    state.setInWrongBook(false);
                    state.setWrongBookResolveStreak(0);
                }
            } else {
                state.setWrongBookResolveStreak(0);
            }
        } else {
            state.setWrongCount(valueOrZero(state.getWrongCount()) + 1);
            state.setQuestionStatus("wrong");
            state.setInWrongBook(true);
            state.setWrongBookResolveStreak(0);
            state.setLastWrongAt(LocalDate.now(ZoneId.of("Asia/Shanghai")).toString());
        }
        stateRepository.save(state);
    }

    private PageResponse<CommonDtos.WrongBookRecordDTO> pageStates(String userId, CommonDtos.StatePageRequest.Payload payload, boolean wrongBook) {
        List<CommonDtos.StateRow> rows = buildRows(userId, payload).stream()
                .filter(row -> wrongBook ? row.inWrongBook : row.favoriteImportance > 0)
                .map(row -> row)
                .toList();
        int pageSize = payload.getPage() == null ? 20 : Math.max(1, payload.getPage().pageSize());
        int pageIndex = payload.getPage() == null ? 1 : Math.max(1, payload.getPage().pageIndex());
        int fromIndex = Math.min((pageIndex - 1) * pageSize, rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        List<CommonDtos.WrongBookRecordDTO> records = rows.subList(fromIndex, toIndex).stream()
                .map(row -> new CommonDtos.WrongBookRecordDTO(row.questionId, row.title, row.subjectName, row.tag, row.time))
                .toList();
        return new PageResponse<>(pageIndex, pageSize, Support.pageCount(rows.size(), pageSize), rows.size(), records);
    }

    private PageResponse<CommonDtos.FavoriteRecordDTO> pageFavoritesInternal(String userId, CommonDtos.StatePageRequest.Payload payload) {
        List<CommonDtos.StateRow> rows = buildRows(userId, payload).stream()
                .filter(row -> row.favoriteImportance > 0)
                .toList();
        int pageSize = payload.getPage() == null ? 20 : Math.max(1, payload.getPage().pageSize());
        int pageIndex = payload.getPage() == null ? 1 : Math.max(1, payload.getPage().pageIndex());
        int fromIndex = Math.min((pageIndex - 1) * pageSize, rows.size());
        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        List<CommonDtos.FavoriteRecordDTO> records = rows.subList(fromIndex, toIndex).stream()
                .map(row -> new CommonDtos.FavoriteRecordDTO(row.questionId, row.title, row.subjectName, row.favoriteImportance, row.time))
                .toList();
        return new PageResponse<>(pageIndex, pageSize, Support.pageCount(rows.size(), pageSize), rows.size(), records);
    }

    private List<CommonDtos.StateRow> buildRows(String userId, CommonDtos.StatePageRequest.Payload payload) {
        List<QuestionEntity> questions = questionRepository.findAll();
        var questionMap = questions.stream().collect(Collectors.toMap(QuestionEntity::getId, q -> q));
        List<UserQuestionStateEntity> states = stateRepository.findByUserId(userId);
        return states.stream()
                .map(state -> {
                    QuestionEntity question = questionMap.get(state.getQuestionId());
                    if (question == null) {
                        return null;
                    }
                    CommonDtos.StateRow row = new CommonDtos.StateRow();
                    row.questionId = question.getId();
                    row.title = question.getTitle();
                    row.subjectName = question.getSubjectName();
                    row.tag = Support.firstTag(question.getTagsJson(), question.getQuestionType());
                    row.time = state.getInWrongBook() != null && state.getInWrongBook() && state.getLastWrongAt() != null
                            ? state.getLastWrongAt()
                            : state.getLastFavoriteAt();
                    row.inWrongBook = Boolean.TRUE.equals(state.getInWrongBook());
                    row.favoriteImportance = valueOrZero(state.getFavoriteImportance());
                    return row;
                })
                .filter(Objects::nonNull)
                .filter(row -> matchesRow(row, payload))
                .sorted(Comparator.comparing((CommonDtos.StateRow row) -> row.time, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(row -> row.questionId))
                .toList();
    }

    private boolean matchesRow(CommonDtos.StateRow row, CommonDtos.StatePageRequest.Payload payload) {
        CommonDtos.StatePageRequest.Payload.Params params = payload.getParams();
        if (params == null) {
            return true;
        }
        if (params.getSubjectCode() != null && !params.getSubjectCode().isBlank()) {
            QuestionEntity question = questionRepository.findById(row.questionId).orElse(null);
            if (question == null || !params.getSubjectCode().equals(question.getSubjectCode())) {
                return false;
            }
        }
        if (params.getQuestionType() != null && !params.getQuestionType().isBlank()) {
            QuestionEntity question = questionRepository.findById(row.questionId).orElse(null);
            if (question == null || !params.getQuestionType().equals(question.getQuestionType())) {
                return false;
            }
        }
        if (params.getKeyword() != null && !params.getKeyword().isBlank()) {
            String keyword = params.getKeyword().trim().toLowerCase();
            String haystack = (safe(row.title) + " " + safe(row.subjectName) + " " + safe(row.tag) + " " + safe(row.questionId)).toLowerCase();
            if (!haystack.contains(keyword)) {
                return false;
            }
        }
        return true;
    }

    private UserQuestionStateEntity createState(String userId, String questionId) {
        UserQuestionStateEntity state = new UserQuestionStateEntity();
        state.setId(org.example.ai408.domain.IdGenerator.prefixed("qs"));
        state.setUserId(userId);
        state.setQuestionId(questionId);
        state.setQuestionStatus("new");
        state.setFavoriteImportance(0);
        state.setInWrongBook(false);
        state.setCorrectCount(0);
        state.setWrongCount(0);
        state.setWrongBookResolveStreak(0);
        state.setEssayDone(false);
        state.setSelectedJson("[]");
        state.setStepStatusJson("[]");
        return state;
    }

    private boolean shouldAutoRemoveWrongBook(UserEntity user, int streak) {
        boolean enabled = user != null && Boolean.TRUE.equals(user.getWrongBookAutoRemoveEnabled());
        int threshold = user == null || user.getWrongBookAutoRemoveThreshold() == null ? 1 : user.getWrongBookAutoRemoveThreshold();
        return enabled && streak >= threshold;
    }

    private boolean isAnswered(UserQuestionStateEntity state) {
        return "correct".equals(state.getQuestionStatus()) || "wrong".equals(state.getQuestionStatus()) || Boolean.TRUE.equals(state.getEssayDone());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    public static class StateRow {
        public String questionId;
        public String title;
        public String subjectName;
        public String tag;
        public String time;
        public boolean inWrongBook;
        public int favoriteImportance;
    }
}
