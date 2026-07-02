package org.example.ai408.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
            @NotBlank
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误")
            private String mobile;

            @Size(max = 20)
            private String scene = "login";

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
            @NotBlank
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误")
            private String mobile;

            @NotBlank
            private String code;

            @Size(max = 100)
            private String deviceId;

            @Size(max = 20)
            private String clientType = "web";

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

    public record UserDTO(String id, String mobile, String nickname, String avatarUrl, String role, String createdAt) {
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
        }
    }
}
