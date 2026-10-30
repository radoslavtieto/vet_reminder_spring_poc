package com.vetsync.pms.adapter.out.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@MappedSuperclass
public abstract class AuditEntry {

    @Id
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "tenant_id", nullable = false, length = 2)
    private String tenantId;

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, length = 36)
    private String publicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private Operation operation;

    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSON")
    private JsonNode payload;

    protected AuditEntry() {
    }

    public enum Operation {
        INSERT, UPDATE, DELETE
    }

    public Long getAuditId() {
        return auditId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getPublicId() {
        return publicId;
    }

    public Operation getOperation() {
        return operation;
    }

    public long getRowVersion() {
        return rowVersion;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public JsonNode getPayload() {
        return payload;
    }
}
