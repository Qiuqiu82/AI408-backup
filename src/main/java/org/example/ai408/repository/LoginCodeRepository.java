package org.example.ai408.repository;

import org.example.ai408.domain.LoginCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginCodeRepository extends JpaRepository<LoginCodeEntity, String> {
    Optional<LoginCodeEntity> findTopByEmailAndSceneAndUsedFalseOrderByCreatedAtDesc(String email, String scene);
}
