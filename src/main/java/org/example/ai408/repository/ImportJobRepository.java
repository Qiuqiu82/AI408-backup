package org.example.ai408.repository;

import org.example.ai408.domain.ImportJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJobEntity, String> {
}
