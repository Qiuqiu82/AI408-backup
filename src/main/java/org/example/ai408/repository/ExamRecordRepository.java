package org.example.ai408.repository;

import org.example.ai408.domain.ExamRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamRecordRepository extends JpaRepository<ExamRecordEntity, String> {
    List<ExamRecordEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<ExamRecordEntity> findByIdAndUserId(String id, String userId);
}
