package com.vetsync.pms.adapter.in.web.dto;

import com.vetsync.pms.adapter.out.persistence.entity.PmsUserEntity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDto {

    private AuthDto() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn,
                                String tenant, String role) {
    }

    public record CreateUserRequest(@NotBlank @Size(max = 254) String username,
                                    @NotBlank @Size(min = 12, max = 72) String password,
                                    @NotNull UserRole role, Long staffId) {
    }

    public record UserView(long id, String username, String role, Long staffId, boolean enabled) {
    }
}
