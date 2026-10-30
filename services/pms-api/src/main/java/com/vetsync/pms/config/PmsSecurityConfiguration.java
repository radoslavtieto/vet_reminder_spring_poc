package com.vetsync.pms.config;

import com.vetsync.pms.application.TenantResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.Base64;

@Configuration
public class PmsSecurityConfiguration {

    @Bean
    SecretKey jwtSecretKey(@Value("${pms.security.jwt-secret}") String encodedSecret) {
        try {
            byte[] secret = Base64.getDecoder().decode(encodedSecret);
            if (secret.length < 32) {
                throw new IllegalStateException("PMS_JWT_SECRET must encode at least 32 random bytes");
            }
            return new SecretKeySpec(secret, "HmacSHA256");
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("PMS_JWT_SECRET must be valid Base64", ex);
        }
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey,
                          @Value("${PMS_JWT_ISSUER:vetsync-pms-api}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> java.util.List.of(
                new SimpleGrantedAuthority("ROLE_" + jwt.getClaimAsString("role"))));
        return converter;
    }

    @Bean
    TenantScopeFilter tenantScopeFilter() {
        return new TenantScopeFilter();
    }

    @Bean
    FilterRegistrationBean<TenantScopeFilter> tenantScopeFilterRegistration(TenantScopeFilter filter) {
        FilterRegistrationBean<TenantScopeFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    SecurityFilterChain pmsSecurityFilterChain(HttpSecurity http,
                                               TenantScopeFilter tenantScopeFilter,
                                               JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/*/auth/login", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/*/clinics/**")
                        .hasAnyRole("ADMIN", "VET", "NURSE", "RECEPTION")
                        .requestMatchers("/api/v1/*/clinics/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/*/staff/**")
                        .hasAnyRole("ADMIN", "VET", "NURSE")
                        .requestMatchers("/api/v1/*/staff/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/*/clients/**")
                        .hasAnyRole("ADMIN", "VET", "NURSE", "RECEPTION")
                        .requestMatchers("/api/v1/*/clients/**").hasAnyRole("ADMIN", "RECEPTION")
                        .requestMatchers("/api/v1/*/appointments/**").hasAnyRole("ADMIN", "RECEPTION")
                        .requestMatchers("/api/v1/*/patients/**", "/api/v1/*/visits/**",
                                "/api/v1/*/prescriptions/**", "/api/v1/*/species/**",
                                "/api/v1/*/medications/**")
                        .hasAnyRole("ADMIN", "VET", "NURSE")
                        .requestMatchers("/api/v1/*/invoices/**").hasAnyRole("ADMIN", "RECEPTION")
                        .requestMatchers("/api/v1/*/users/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .addFilterAfter(tenantScopeFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    static final class TenantScopeFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            String uri = request.getRequestURI();
            if (uri.startsWith("/api/v1/") && !uri.endsWith("/auth/login")
                    && SecurityContextHolder.getContext().getAuthentication()
                    instanceof JwtAuthenticationToken authentication) {
                String[] segments = uri.split("/");
                String region = segments.length > 3 ? segments[3] : "";
                String requestedTenant;
                try {
                    requestedTenant = TenantResolver.resolve(region);
                } catch (RuntimeException ex) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                Jwt jwt = authentication.getToken();
                if (!requestedTenant.equals(jwt.getClaimAsString("tenant"))) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            }
            filterChain.doFilter(request, response);
        }
    }
}
