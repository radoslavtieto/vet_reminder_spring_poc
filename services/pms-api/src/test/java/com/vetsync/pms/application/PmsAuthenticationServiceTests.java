package com.vetsync.pms.application;

import com.vetsync.pms.adapter.in.web.dto.AuthDto.CreateUserRequest;
import com.vetsync.pms.adapter.out.persistence.entity.PmsUserEntity;
import com.vetsync.pms.adapter.out.persistence.entity.PmsUserEntity.UserRole;
import com.vetsync.pms.adapter.out.persistence.entity.StaffEntity;
import com.vetsync.pms.adapter.out.persistence.entity.ClinicEntity;
import com.vetsync.pms.adapter.out.persistence.repository.PmsUserRepository;
import com.vetsync.pms.adapter.out.persistence.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PmsAuthenticationServiceTests {

    private static final Instant NOW = Instant.parse("2026-10-30T12:00:00Z");

    private final PmsUserRepository users = mock(PmsUserRepository.class);
    private final StaffRepository staff = mock(StaffRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtEncoder jwtEncoder = mock(JwtEncoder.class);
    private PmsAuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new PmsAuthenticationService(users, staff, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC),
                jwtEncoder, 900, "test-issuer");
    }

    @Test
    void loginNormalizesUsernameAndIssuesTenantRoleAndExpiryClaims() {
        PmsUserEntity user = user("SW", "admin@example.invalid", UserRole.ADMIN, null);
        when(users.findByTenantIdAndUsername("SW", "admin@example.invalid")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("strong-password", "encoded-password")).thenReturn(true);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(
                Jwt.withTokenValue("signed-token").header("alg", "HS256").subject("admin@example.invalid")
                        .issuedAt(NOW).expiresAt(NOW.plusSeconds(900))
                        .claim("tenant", "SW").claim("role", "ADMIN").build());

        var response = service.login("SW", " ADMIN@example.invalid ", "strong-password");

        assertThat(response.accessToken()).isEqualTo("signed-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(response.tenant()).isEqualTo("SW");
        assertThat(response.role()).isEqualTo("ADMIN");
        var encoded = org.mockito.ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(encoded.capture());
        assertThat(encoded.getValue().getClaims().getClaim("iss").toString()).isEqualTo("test-issuer");
        Instant issuedAt = encoded.getValue().getClaims().getIssuedAt();
        Instant expiresAt = encoded.getValue().getClaims().getExpiresAt();
        assertThat(issuedAt.toEpochMilli()).isEqualTo(NOW.toEpochMilli());
        assertThat(expiresAt.toEpochMilli()).isEqualTo(NOW.plusSeconds(900).toEpochMilli());
        assertThat((String) encoded.getValue().getClaims().getClaim("tenant")).isEqualTo("SW");
        assertThat((String) encoded.getValue().getClaims().getClaim("role")).isEqualTo("ADMIN");
    }

    @Test
    void loginRejectsUnknownUsersAndWrongPasswordsWithSameError() {
        when(users.findByTenantIdAndUsername("SW", "missing@example.invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("SW", "missing@example.invalid", "bad-password"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");

        PmsUserEntity user = user("SW", "known@example.invalid", UserRole.ADMIN, null);
        when(users.findByTenantIdAndUsername("SW", "known@example.invalid")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad-password", "encoded-password")).thenReturn(false);
        assertThatThrownBy(() -> service.login("SW", "known@example.invalid", "bad-password"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");
        verifyNoInteractions(jwtEncoder);
    }

    @Test
    void loginRejectsDisabledUsersBeforeCheckingTheirPassword() {
        PmsUserEntity user = user("SW", "disabled@example.invalid", UserRole.ADMIN, null);
        ReflectionTestUtils.setField(user, "enabled", false);
        when(users.findByTenantIdAndUsername("SW", "disabled@example.invalid")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login("SW", "disabled@example.invalid", "strong-password"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");

        verifyNoInteractions(passwordEncoder, jwtEncoder);
    }

    @Test
    void createUserHashesPasswordAndRequiresActiveStaffForClinicalRoles() {
        StaffEntity vet = new StaffEntity("SW", clinic(), StaffEntity.StaffRole.VET, "Synthetic Vet",
                "LIC-TEST", true);
        ReflectionTestUtils.setField(vet, "id", 7L);
        when(users.existsByTenantIdAndUsername("SW", "vet@example.invalid")).thenReturn(false);
        when(staff.findByTenantIdAndId("SW", 7L)).thenReturn(Optional.of(vet));
        when(passwordEncoder.encode("strong-password")).thenReturn("hashed-password");
        when(users.save(any(PmsUserEntity.class))).thenAnswer(invocation -> {
            PmsUserEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 11L);
            return entity;
        });

        var created = service.createUser("SW",
                new CreateUserRequest(" VET@example.invalid ", "strong-password", UserRole.VET, 7L));

        assertThat(created.id()).isEqualTo(11L);
        assertThat(created.username()).isEqualTo("vet@example.invalid");
        assertThat(created.role()).isEqualTo("VET");
        assertThat(created.staffId()).isEqualTo(7L);
        verify(passwordEncoder).encode("strong-password");
        verify(users).save(argThat(entity -> entity.getPasswordHash().equals("hashed-password")
                && entity.getTenantId().equals("SW")));
    }

    @Test
    void createUserRejectsDuplicateNamesAndUnmatchedClinicalStaff() {
        when(users.existsByTenantIdAndUsername("SW", "admin@example.invalid")).thenReturn(true);
        assertThatThrownBy(() -> service.createUser("SW",
                new CreateUserRequest("ADMIN@example.invalid", "strong-password", UserRole.ADMIN, null)))
                .isInstanceOf(PmsInvalidRequestException.class)
                .hasMessage("Username already exists in this tenant");

        when(users.existsByTenantIdAndUsername("SW", "vet@example.invalid")).thenReturn(false);
        when(staff.findByTenantIdAndId("SW", 8L)).thenReturn(Optional.of(
                new StaffEntity("SW", clinic(), StaffEntity.StaffRole.NURSE, "Synthetic Nurse", null, true)));
        assertThatThrownBy(() -> service.createUser("SW",
                new CreateUserRequest("vet@example.invalid", "strong-password", UserRole.VET, 8L)))
                .isInstanceOf(PmsInvalidRequestException.class)
                .hasMessage("User role must match the staff role");
    }

    @Test
    void createUserRequiresStaffOnlyForClinicalRolesAndRejectsMissingStaff() {
        when(users.existsByTenantIdAndUsername(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.createUser("SW",
                new CreateUserRequest("vet@example.invalid", "strong-password", UserRole.VET, null)))
                .isInstanceOf(PmsInvalidRequestException.class)
                .hasMessage("VET and NURSE users require a matching staff record");

        when(staff.findByTenantIdAndId("SW", 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createUser("SW",
                new CreateUserRequest("nurse@example.invalid", "strong-password", UserRole.NURSE, 99L)))
                .isInstanceOf(PmsResourceNotFoundException.class)
                .hasMessage("Staff record not found");
    }

    @Test
    void createUserRejectsInactiveStaff() {
        when(users.existsByTenantIdAndUsername("SW", "nurse@example.invalid")).thenReturn(false);
        StaffEntity inactiveNurse = new StaffEntity("SW", clinic(), StaffEntity.StaffRole.NURSE,
                "Synthetic Nurse", null, false);
        when(staff.findByTenantIdAndId("SW", 9L)).thenReturn(Optional.of(inactiveNurse));

        assertThatThrownBy(() -> service.createUser("SW",
                new CreateUserRequest("nurse@example.invalid", "strong-password", UserRole.NURSE, 9L)))
                .isInstanceOf(PmsResourceNotFoundException.class)
                .hasMessage("Staff record not found");
    }

    @Test
    void constructorRejectsTokenLifetimeOutsideSafeBounds() {
        assertThatThrownBy(() -> new PmsAuthenticationService(users, staff, passwordEncoder,
                Clock.fixed(NOW, ZoneOffset.UTC), jwtEncoder, 59, "test-issuer"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("PMS_JWT_TTL_SECONDS must be between 60 and 86400");
        assertThatThrownBy(() -> new PmsAuthenticationService(users, staff, passwordEncoder,
                Clock.fixed(NOW, ZoneOffset.UTC), jwtEncoder, 86_401, "test-issuer"))
                .isInstanceOf(IllegalStateException.class);
    }

    private static PmsUserEntity user(String tenant, String username, UserRole role, StaffEntity staff) {
        PmsUserEntity user = new PmsUserEntity(tenant, username, "encoded-password", role, staff);
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private static ClinicEntity clinic() {
        return new ClinicEntity("SW", "Synthetic Clinic", "Sample City", null, null, null, true);
    }
}
