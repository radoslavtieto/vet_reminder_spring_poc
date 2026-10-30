package com.vetsync.pms.config;

import com.vetsync.pms.adapter.out.persistence.entity.PmsUserEntity;
import com.vetsync.pms.adapter.out.persistence.entity.PmsUserEntity.UserRole;
import com.vetsync.pms.adapter.out.persistence.repository.PmsUserRepository;
import com.vetsync.pms.application.PmsAuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PmsAdminBootstrap implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(PmsAdminBootstrap.class);
    private static final List<String> TENANTS = List.of("SW", "SE", "SC", "NE", "NW", "NC");

    private final Environment environment;
    private final PmsUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public PmsAdminBootstrap(Environment environment, PmsUserRepository users,
                             PasswordEncoder passwordEncoder) {
        this.environment = environment;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String tenant : TENANTS) {
            if (users.existsByTenantId(tenant)) {
                continue;
            }
            String username = environment.getProperty("PMS_BOOTSTRAP_ADMIN_" + tenant + "_USERNAME", "").trim();
            String password = environment.getProperty("PMS_BOOTSTRAP_ADMIN_" + tenant + "_PASSWORD", "");
            if (username.isEmpty() && password.isEmpty()) {
                logger.warn("No bootstrap administrator configured for tenant {}", tenant);
                continue;
            }
            if (username.isEmpty() || password.isEmpty() || password.length() < 12) {
                throw new IllegalStateException("Configure both bootstrap username and a 12-character password for "
                        + tenant);
            }
            users.save(new PmsUserEntity(tenant, PmsAuthenticationService.normalizeUsername(username),
                    passwordEncoder.encode(password), UserRole.ADMIN, null));
            logger.info("Bootstrapped initial administrator for tenant {}", tenant);
        }
    }
}
