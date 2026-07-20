package org.example.ai408.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public static class SendCodeRequest {
        @Valid
        @NotNull
        @JsonProperty("data")
        private Payload data;

        public Payload getData() {
            return data;
        }

        public void setData(Payload data) {
            this.data = data;
        }

        public static class Payload {
            @Email
            @Size(max = 120)
            private String email;

            @Size(max = 20)
            private String mobile;

            @Size(max = 20)
            private String scene = "login";

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

            public String getMobile() {
                return mobile;
            }

            public void setMobile(String mobile) {
                this.mobile = mobile;
            }

            public String getScene() {
                return scene;
            }

            public void setScene(String scene) {
                this.scene = scene;
            }
        }
    }

    public static class LoginRequest {
        @Valid
        @NotNull
        @JsonProperty("data")
        private Payload data;

        public Payload getData() {
            return data;
        }

        public void setData(Payload data) {
            this.data = data;
        }

        public static class Payload {
            @Email
            @Size(max = 120)
            private String email;

            @Size(max = 20)
            private String mobile;

            @NotBlank
            @Size(max = 12)
            private String code;

            @Size(max = 100)
            private String deviceId;

            @Size(max = 20)
            private String clientType = "web";

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

            public String getMobile() {
                return mobile;
            }

            public void setMobile(String mobile) {
                this.mobile = mobile;
            }

            public String getCode() {
                return code;
            }

            public void setCode(String code) {
                this.code = code;
            }

            public String getDeviceId() {
                return deviceId;
            }

            public void setDeviceId(String deviceId) {
                this.deviceId = deviceId;
            }

            public String getClientType() {
                return clientType;
            }

            public void setClientType(String clientType) {
                this.clientType = clientType;
            }
        }
    }

    public static class PasswordLoginRequest {
        @Valid
        @NotNull
        @JsonProperty("data")
        private Payload data;

        public Payload getData() {
            return data;
        }

        public void setData(Payload data) {
            this.data = data;
        }

        public static class Payload {
            @Email
            @NotBlank
            @Size(max = 120)
            private String email;

            @NotBlank
            @Size(max = 64)
            private String password;

            @Size(max = 100)
            private String deviceId;

            @Size(max = 20)
            private String clientType = "web";

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public String getDeviceId() {
                return deviceId;
            }

            public void setDeviceId(String deviceId) {
                this.deviceId = deviceId;
            }

            public String getClientType() {
                return clientType;
            }

            public void setClientType(String clientType) {
                this.clientType = clientType;
            }
        }
    }

    public static class RefreshRequest {
        @Valid
        @NotNull
        @JsonProperty("data")
        private Payload data;

        public Payload getData() {
            return data;
        }

        public void setData(Payload data) {
            this.data = data;
        }

        public static class Payload {
            @NotBlank
            private String refreshToken;

            public String getRefreshToken() {
                return refreshToken;
            }

            public void setRefreshToken(String refreshToken) {
                this.refreshToken = refreshToken;
            }
        }
    }

    public record SendCodeResponse(int expireSeconds) {
    }

    public record AuthTokens(String accessToken, String refreshToken, long expiresIn, UserDTO user) {
    }

    public record UserDTO(
            String id,
            String mobile,
            String email,
            String nickname,
            String avatarUrl,
            String role,
            String createdAt,
            Boolean hasPassword,
            Boolean wrongBookAutoRemoveEnabled,
            Integer wrongBookAutoRemoveThreshold
    ) {
    }

    public static class UpdateUserRequest {
        @Valid
        @NotNull
        @JsonProperty("data")
        private Payload data;

        public Payload getData() {
            return data;
        }

        public void setData(Payload data) {
            this.data = data;
        }

        public static class Payload {
            @Size(max = 50)
            private String nickname;

            @Size(max = 255)
            private String avatarUrl;

            private Boolean wrongBookAutoRemoveEnabled;

            private Integer wrongBookAutoRemoveThreshold;

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
    }

    public static class UpdatePasswordRequest {
        @Valid
        @NotNull
        @JsonProperty("data")
        private Payload data;

        public Payload getData() {
            return data;
        }

        public void setData(Payload data) {
            this.data = data;
        }

        public static class Payload {
            @Size(max = 64)
            private String currentPassword;

            @NotBlank
            @Size(min = 8, max = 64)
            private String newPassword;

            public String getCurrentPassword() {
                return currentPassword;
            }

            public void setCurrentPassword(String currentPassword) {
                this.currentPassword = currentPassword;
            }

            public String getNewPassword() {
                return newPassword;
            }

            public void setNewPassword(String newPassword) {
                this.newPassword = newPassword;
            }
        }
    }
}
