package com.vetsync.action.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.vetsync.contracts.SyncEnvelope;
import com.vetsync.contracts.SyncOperation;
import com.vetsync.action.domain.ActionRecord;
import com.vetsync.action.domain.CareSubject;
import com.vetsync.action.domain.Contact;
import com.vetsync.action.domain.Site;

import java.util.Locale;

public class SyncEnvelopeMapper {
    public ActionRecord map(SyncEnvelope event) {
        if (!"master-data".equals(event.stream())) {
            throw new IllegalArgumentException("Unsupported stream: " + event.stream());
        }
        if (event.operation() != SyncOperation.UPSERT && event.operation() != SyncOperation.DELETE) {
            throw new IllegalArgumentException("Unsupported master-data operation: " + event.operation());
        }
        boolean active = event.operation() != SyncOperation.DELETE
                && !event.payload().hasNonNull("deleted_at");
        return switch (event.entityType().toUpperCase(Locale.ROOT)) {
            case "CLINIC" -> new Site(
                    event.tenant(), event.entityId(), event.sourceVersion(),
                    requiredText(event.payload(), "name"), optionalText(event.payload(), "city"),
                    active && requiredBoolean(event.payload(), "active"));
            case "CLIENT" -> new Contact(
                    event.tenant(), event.entityId(), event.sourceVersion(),
                    requiredText(event.payload(), "first_name") + " " + requiredText(event.payload(), "last_name"),
                    optionalText(event.payload(), "email"),
                    optionalText(event.payload(), "phone"),
                    optionalText(event.payload(), "preferred_contact"),
                    event.payload().hasNonNull("gdpr_consent_at"),
                    active);
            case "PATIENT" -> new CareSubject(
                    event.tenant(), event.entityId(), event.sourceVersion(),
                    requiredText(event.payload(), "name"),
                    requiredText(event.payload(), "owner_public_id"),
                    requiredText(event.payload(), "species_code"),
                    requiredText(event.payload(), "status"),
                    active);
            default -> throw new IllegalArgumentException("Unsupported master-data entity: " + event.entityType());
        };
    }

    private String requiredText(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Required payload field is missing: " + field);
        }
        return value.asText();
    }

    private String optionalText(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private boolean requiredBoolean(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || !value.isBoolean()) {
            throw new IllegalArgumentException("Required boolean payload field is missing: " + field);
        }
        return value.booleanValue();
    }
}
