package org.example.ai408.repository;

import org.example.ai408.domain.PracticeSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PracticeSessionRepository extends JpaRepository<PracticeSessionEntity, String> {
    Optional<PracticeSessionEntity> findByIdAndUserId(String id, String userId);

    List<PracticeSessionEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}
