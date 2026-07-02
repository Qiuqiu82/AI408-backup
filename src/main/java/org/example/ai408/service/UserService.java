package org.example.ai408.service;

import org.example.ai408.common.BusinessException;
import org.example.ai408.common.ErrorCode;
import org.example.ai408.domain.PracticeSessionEntity;
import org.example.ai408.domain.QuestionEntity;
import org.example.ai408.domain.UserEntity;
import org.example.ai408.domain.UserQuestionStateEntity;
import org.example.ai408.dto.AuthDtos;
import org.example.ai408.dto.CommonDtos;
import org.example.ai408.repository.PracticeSessionRepository;
import org.example.ai408.repository.QuestionRepository;
import org.example.ai408.repository.UserQuestionStateRepository;
import org.example.ai408.repository.UserRepository;
import org.example.ai408.security.AuthenticatedUser;
import org.example.ai408.security.SecurityUtils;
import org.example.ai408.util.TimeUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserQuestionStateRepository stateRepository;
    private final PracticeSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;

    public UserService(
            UserRepository userRepository,
            UserQuestionStateRepository stateRepository,
            PracticeSessionRepository sessionRepository,
            QuestionRepository questionRepository
    ) {
        this.userRepository = userRepository;
        this.stateRepository = stateRepository;
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
    }

    public AuthDtos.UserDTO me() {
        return Support.toUserDto(currentUserEntity());
    }

    public AuthDtos.UserDTO updateMe(String nickname, String avatarUrl) {
        UserEntity user = currentUserEntity();
        if (nickname != null) {
            user.setNickname(nickname.trim());
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl.trim());
        }
        return Support.toUserDto(userRepository.save(user));
    }

    public CommonDtos.StudySummaryDTO studySummary() {
        UserEntity user = currentUserEntity();
        List<UserQuestionStateEntity> states = stateRepository.findByUserId(user.getId());
        long answeredCount = states.stream()
                .filter(state -> "correct".equals(state.getQuestionStatus()) || "wrong".equals(state.getQuestionStatus()) || Boolean.TRUE.equals(state.getEssayDone()))
                .count();
        int correctCount = states.stream().mapToInt(state -> valueOrZero(state.getCorrectCount())).sum();
        int wrongCount = states.stream().mapToInt(state -> valueOrZero(state.getWrongCount())).sum();
        long favoriteCount = states.stream().filter(state -> valueOrZero(state.getFavoriteImportance()) > 0).count();
        String today = LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();
        long todayWrongCount = states.stream().filter(state -> today.equals(state.getLastWrongAt())).count();
        long todayFavoriteCount = states.stream().filter(state -> today.equals(state.getLastFavoriteAt())).count();
        int totalCount = (int) questionRepository.count();
        int progressRate = totalCount == 0 ? 0 : (int) Math.round(answeredCount * 100.0 / totalCount);
        int sessionSeconds = sessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(session -> "finished".equals(session.getStatus()))
                .mapToInt(session -> valueOrZero(session.getDurationSeconds()))
                .sum();
        return new CommonDtos.StudySummaryDTO(
                (int) answeredCount,
                correctCount,
                wrongCount,
                (int) favoriteCount,
                (int) todayWrongCount,
                (int) todayFavoriteCount,
                progressRate,
                sessionSeconds
        );
    }

    public UserEntity currentUserEntity() {
        AuthenticatedUser principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userRepository.findById(principal.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public UserEntity currentUserEntity(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
