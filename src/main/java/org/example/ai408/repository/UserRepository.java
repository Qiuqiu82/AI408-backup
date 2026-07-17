package org.example.ai408.repository;

import org.example.ai408.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByMobile(String mobile);

    Optional<UserEntity> findByEmail(String email);
}
