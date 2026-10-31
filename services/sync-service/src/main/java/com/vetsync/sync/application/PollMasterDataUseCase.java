package com.vetsync.sync.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsync.contracts.SyncEnvelope;
import com.vetsync.contracts.SyncOperation;
import com.vetsync.sync.domain.AuditChange;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PollMasterDataUseCase {
    private static final String STREAM = "master-data";

    private final AuditChangeSource source;
    private final SyncProgressStore progress;
    private final SyncEventPublisher publisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final List<String> tenants;
    private final Duration safetyLag;
    private final Duration overlap;
    private final int batchSize;

    public PollMasterDataUseCase(
            AuditChangeSource source,
            SyncProgressStore progress,
            SyncEventPublisher publisher,
            ObjectMapper objectMapper,
            Clock clock,
            List<String> tenants,
            Duration safetyLag,
            Duration overlap,
            int batchSize
    ) {
        if (safetyLag.isNegative() || overlap.isNegative() || batchSize < 1) {
            throw new IllegalArgumentException("Polling lag/overlap and batch size are invalid");
        }
        this.source = source;
        this.progress = progress;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.tenants = List.copyOf(tenants);
        this.safetyLag = safetyLag;
        this.overlap = overlap;
        this.batchSize = batchSize;
    }

    public int poll() {
        Instant safeBefore = clock.instant().minus(safetyLag);
        Instant overlapFrom = safeBefore.minus(overlap);
        int published = 0;
        for (String tenant : tenants) {
            long lastAuditId = progress.lastAuditId(tenant, STREAM);
            List<AuditChange> changes = source.readMasterData(
                    tenant, lastAuditId, overlapFrom, safeBefore, batchSize);
            for (AuditChange change : changes) {
                publisher.publish(envelopeFor(change));
                progress.advance(tenant, STREAM, change.auditId());
                published++;
            }
        }
        return published;
    }

    SyncEnvelope envelopeFor(AuditChange change) {
        SyncOperation operation = switch (change.operation().toUpperCase(Locale.ROOT)) {
            case "INSERT", "UPDATE" -> SyncOperation.UPSERT;
            case "DELETE" -> SyncOperation.DELETE;
            default -> throw new IllegalArgumentException("Unsupported audit operation: " + change.operation());
        };
        String identity = change.tenant() + ":" + change.entityType() + ":" + change.publicId() + ":"
                + change.rowVersion();
        UUID eventId = UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
        return new SyncEnvelope(
                1,
                eventId,
                eventId,
                change.tenant(),
                STREAM,
                change.entityType().toUpperCase(Locale.ROOT),
                change.publicId(),
                operation,
                change.rowVersion(),
                change.changedAt(),
                clock.instant(),
                objectMapper.valueToTree(change.payload())
        );
    }
}
