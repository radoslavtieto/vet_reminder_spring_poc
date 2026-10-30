package com.vetsync.pms.config;

import com.vetsync.pms.adapter.out.persistence.entity.PmsUserEntity;
import com.vetsync.pms.adapter.out.persistence.repository.PmsUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PmsAdminBootstrapTests {

    private final Environment environment = mock(Environment.class);
    private final PmsUserRepository users = mock(PmsUserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void createsOnlyConfiguredAdminsForEmptyTenantsAndHashesTheirPasswords() {
        when(users.existsByTenantId(any())).thenAnswer(call -> call.getArgument(0).equals("SE"));
        when(environment.getProperty(anyString(), anyString())).thenReturn("");
        when(environment.getProperty("PMS_BOOTSTRAP_ADMIN_SW_USERNAME", "")).thenReturn("Owner@Example.invalid");
        when(environment.getProperty("PMS_BOOTSTRAP_ADMIN_SW_PASSWORD", "")).thenReturn("strong-password");
        when(passwordEncoder.encode("strong-password")).thenReturn("hashed-password");
        when(users.save(any(PmsUserEntity.class))).thenAnswer(call -> call.getArgument(0));

        new PmsAdminBootstrap(environment, users, passwordEncoder).run(new DefaultApplicationArguments());

        verify(users).save(argThat(user -> user.getTenantId().equals("SW")
                && user.getUsername().equals("owner@example.invalid")
                && user.getPasswordHash().equals("hashed-password")
                && user.getRole() == PmsUserEntity.UserRole.ADMIN));
        verify(users, never()).save(argThat(user -> user.getTenantId().equals("SE")));
        verify(users, times(6)).existsByTenantId(any());
    }

    @Test
    void rejectsIncompleteOrWeakInitialCredentialsInsteadOfCreatingUnusableAccount() {
        when(users.existsByTenantId("SW")).thenReturn(false);
        when(environment.getProperty("PMS_BOOTSTRAP_ADMIN_SW_USERNAME", "")).thenReturn("admin");
        when(environment.getProperty("PMS_BOOTSTRAP_ADMIN_SW_PASSWORD", "")).thenReturn("short");

        assertThatThrownBy(() -> new PmsAdminBootstrap(environment, users, passwordEncoder)
                .run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("12-character password");

        verify(users, never()).save(any());
    }

    @Test
    void doesNotBootstrapAccountsWhenCredentialsAreNotConfigured() {
        when(users.existsByTenantId(any())).thenReturn(false);
        when(environment.getProperty(anyString(), anyString())).thenReturn("");

        new PmsAdminBootstrap(environment, users, passwordEncoder).run(new DefaultApplicationArguments());

        verify(users, never()).save(any());
    }
}
