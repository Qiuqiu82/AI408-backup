package org.example.ai408.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai408_user")
public class UserEntity extends BaseEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(unique = true, length = 20)
    private String mobile;

    @Column(unique = true, length = 120)
    private String email;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "refresh_token_hash", length = 128)
    private String refreshTokenHash;

    @Column(name = "refresh_token_expires_at")
    private LocalDateTime refreshTokenExpiresAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "wrong_book_auto_remove_enabled", nullable = false)
    private Boolean wrongBookAutoRemoveEnabled;

    @Column(name = "wrong_book_auto_remove_threshold", nullable = false)
    private Integer wrongBookAutoRemoveThreshold;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public void setRefreshTokenHash(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    public LocalDateTime getRefreshTokenExpiresAt() {
        return refreshTokenExpiresAt;
    }

    public void setRefreshTokenExpiresAt(LocalDateTime refreshTokenExpiresAt) {
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Boolean getWrongBookAutoRemoveEnabled() {
        return wrongBookAutoRemoveEnabled;
    }

    public void setWrongBookAutoRemoveEnabled(Boolean wrongBookAutoRemoveEnabled) {
        this.wrongBookAutoRemoveEnabled = wrongBookAutoRemoveEnabled;
    }

    public Integer getWrongBookAutoRemoveThreshold() {
        return wrongBookAutoRemoveThreshold;
    }

    public void setWrongBookAutoRemoveThreshold(Integer wrongBookAutoRemoveThreshold) {
        this.wrongBookAutoRemoveThreshold = wrongBookAutoRemoveThreshold;
    }
}
