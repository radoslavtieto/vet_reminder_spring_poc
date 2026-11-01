package com.vetsync.sync.application;

import com.vetsync.sync.domain.TenantCode;

import java.time.Instant;

public interface LeaseRepository {

    boolean tryAcquire(
            TenantCode tenant,
            String scope,
            String owner,
            Instant now,
            Instant expiresAt);

    boolean renew(
            TenantCode tenant,
            String scope,
            String owner,
            Instant now,
            Instant expiresAt);

    void release(TenantCode tenant, String scope, String owner);
}
