package com.vetsync.pms.application;

import com.vetsync.pms.adapter.in.web.dto.AuthDto.CreateUserRequest;
import com.vetsync.pms.adapter.in.web.dto.AuthDto.TokenResponse;
import com.vetsync.pms.adapter.in.web.dto.AuthDto.UserView;
import com.vetsync.pms.adapter.out.persistence.entity.PmsUserEntity;
import com.vetsync.pms.adapter.out.persistence.entity.PmsUserEntity.UserRole;
import com.vetsync.pms.adapter.out.persistence.entity.StaffEntity;
import com.vetsync.pms.adapter.out.persistence.repository.PmsUserRepository;
import com.vetsync.pms.adapter.out.persistence.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;

@Service
@Transactional
public class PmsAuthenticationService {

    private final PmsUserRepository users;
    private final StaffRepository staff;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final JwtEncoder jwtEncoder;
    private final Duration tokenLifetime;
    private final String issuer;

    public PmsAuthenticationService(PmsUserRepository users, StaffRepository staff,
                                    PasswordEncoder passwordEncoder, Clock clock,
                                    JwtEncoder jwtEncoder,
                                    @Value("${PMS_JWT_TTL_SECONDS:900}") long tokenLifetimeSeconds,
                                    @Value("${PMS_JWT_ISSUER:vetsync-pms-api}") String issuer) {
        this.users = users;
        this.staff = staff;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.jwtEncoder = jwtEncoder;
        if (tokenLifetimeSeconds < 60 || tokenLifetimeSeconds > 86_400) {
            throw new IllegalStateException("PMS_JWT_TTL_SECONDS must be between 60 and 86400");
        }
        this.tokenLifetime = Duration.ofSeconds(tokenLifetimeSeconds);
        this.issuer = issuer;
    }

    public TokenResponse login(String tenant, String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        PmsUserEntity user = users.findByTenantIdAndUsername(tenant, normalizedUsername)
                .filter(PmsUserEntity::isEnabled)
                .filter(account -> passwordEncoder.matches(password, account.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        var issuedAt = clock.instant();
        var expiresAt = issuedAt.plus(tokenLifetime);
        JwtClaimsSet.Builder claimBuilder = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getUsername())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("tenant", user.getTenantId())
                .claim("role", user.getRole().name());
        if (user.getStaff() != null) {
            claimBuilder.claim("staffId", user.getStaff().getId());
        }
        JwtClaimsSet claims = claimBuilder.build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(),
                claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", tokenLifetime.toSeconds(),
                user.getTenantId(), user.getRole().name());
    }

    public UserView createUser(String tenant, CreateUserRequest request) {
        String username = normalizeUsername(request.username());
        if (users.existsByTenantIdAndUsername(tenant, username)) {
            throw new PmsInvalidRequestException("Username already exists in this tenant");
        }

        StaffEntity staffMember = null;
        boolean clinicalRole = request.role() == UserRole.VET || request.role() == UserRole.NURSE;
        if (clinicalRole != (request.staffId() != null)) {
            throw new PmsInvalidRequestException("VET and NURSE users require a matching staff record");
        }
        if (clinicalRole) {
            staffMember = staff.findByTenantIdAndId(tenant, request.staffId())
                    .filter(entity -> entity.getDeletedAt() == null && entity.isActive())
                    .orElseThrow(() -> new PmsResourceNotFoundException("Staff record not found"));
            StaffEntity.StaffRole expected = request.role() == UserRole.VET
                    ? StaffEntity.StaffRole.VET : StaffEntity.StaffRole.NURSE;
            if (staffMember.getRole() != expected) {
                throw new PmsInvalidRequestException("User role must match the staff role");
            }
        }
        PmsUserEntity saved = users.save(new PmsUserEntity(tenant, username,
                passwordEncoder.encode(request.password()), request.role(), staffMember));
        return view(saved);
    }

    public static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private static UserView view(PmsUserEntity user) {
        return new UserView(user.getId(), user.getUsername(), user.getRole().name(),
                user.getStaff() == null ? null : user.getStaff().getId(), user.isEnabled());
    }
}
