package org.example.ai408.repository;

import org.example.ai408.domain.PracticeSessionQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PracticeSessionQuestionRepository extends JpaRepository<PracticeSessionQuestionEntity, String> {
    List<PracticeSessionQuestionEntity> findBySessionIdOrderByOrderNoAsc(String sessionId);

    Optional<PracticeSessionQuestionEntity> findBySessionIdAndQuestionId(String sessionId, String questionId);
}
