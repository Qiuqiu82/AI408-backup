package org.example.ai408.repository;

import org.example.ai408.domain.ExamRecordQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamRecordQuestionRepository extends JpaRepository<ExamRecordQuestionEntity, String> {
    List<ExamRecordQuestionEntity> findByRecordIdOrderByOrderNoAsc(String recordId);
}
