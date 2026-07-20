package org.example.ai408.repository;

import org.example.ai408.domain.LoginCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface LoginCodeRepository extends JpaRepository<LoginCodeEntity, String> {
    Optional<LoginCodeEntity> findTopByEmailAndSceneAndUsedFalseOrderByCreatedAtDesc(String email, String scene);

    long countByRequestIpAndSceneAndCreatedAtAfter(String requestIp, String scene, LocalDateTime createdAt);

    long countByEmailAndSceneAndCreatedAtAfter(String email, String scene, LocalDateTime createdAt);
}
