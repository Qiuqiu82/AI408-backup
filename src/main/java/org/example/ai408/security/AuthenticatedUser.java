package org.example.ai408.security;

import java.security.Principal;

public record AuthenticatedUser(String id, String mobile, String nickname, String role) implements Principal {
    @Override
    public String getName() {
        return id;
    }
}
