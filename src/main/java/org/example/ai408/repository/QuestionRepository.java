package org.example.ai408.repository;

import org.example.ai408.domain.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<QuestionEntity, String> {
    Optional<QuestionEntity> findByQuestionCode(String questionCode);

    List<QuestionEntity> findBySubjectCodeOrderBySortNoAsc(String subjectCode);

    List<QuestionEntity> findBySubjectCodeInOrderBySortNoAsc(List<String> subjectCodes);
}
