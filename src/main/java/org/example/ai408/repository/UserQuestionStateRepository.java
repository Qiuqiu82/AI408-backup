package org.example.ai408.repository;

import org.example.ai408.domain.UserQuestionStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserQuestionStateRepository extends JpaRepository<UserQuestionStateEntity, String> {
    Optional<UserQuestionStateEntity> findByUserIdAndQuestionId(String userId, String questionId);

    List<UserQuestionStateEntity> findByUserId(String userId);

    List<UserQuestionStateEntity> findByUserIdAndInWrongBookTrue(String userId);

    List<UserQuestionStateEntity> findByUserIdAndFavoriteImportanceGreaterThan(String userId, Integer importance);
}
