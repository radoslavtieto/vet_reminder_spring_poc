package com.vetsync.action.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vetsync.contracts.SyncEnvelope;
import com.vetsync.contracts.SyncOperation;
import com.vetsync.action.domain.Contact;
import com.vetsync.action.domain.CareSubject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SyncEnvelopeMapperTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SyncEnvelopeMapper mapper = new SyncEnvelopeMapper();

    @Test
    void mapsClientPayloadToAnEngineOwnedContact() {
        ObjectNode payload = objectMapper.createObjectNode()
                .put("first_name", "Synthetic")
                .put("last_name", "Client")
                .put("email", "client@example.test")
                .put("phone", "+359000000000")
                .put("preferred_contact", "EMAIL")
                .put("gdpr_consent_at", "2026-10-01T10:00:00Z");

        Contact contact = (Contact) mapper.map(envelope("CLIENT", SyncOperation.UPSERT, payload));

        assertThat(contact.tenant()).isEqualTo("SW");
        assertThat(contact.displayName()).isEqualTo("Synthetic Client");
        assertThat(contact.email()).isEqualTo("client@example.test");
        assertThat(contact.consentGiven()).isTrue();
        assertThat(contact.active()).isTrue();
    }

    @Test
    void mapsPatientOwnerAndSoftDeleteWithoutDependingOnPmsTypes() {
        ObjectNode payload = objectMapper.createObjectNode()
                .put("name", "Synthetic Pet")
                .put("owner_public_id", "a6caec5a-8dc6-4091-888d-0b41de51d4a0")
                .put("species_code", "DOG")
                .put("status", "ACTIVE");

        CareSubject subject = (CareSubject) mapper.map(envelope("PATIENT", SyncOperation.DELETE, payload));

        assertThat(subject.displayName()).isEqualTo("Synthetic Pet");
        assertThat(subject.ownerSourceId()).isEqualTo("a6caec5a-8dc6-4091-888d-0b41de51d4a0");
        assertThat(subject.speciesCode()).isEqualTo("DOG");
        assertThat(subject.active()).isFalse();
    }

    @Test
    void rejectsUnmappedEntityTypesInsteadOfSilentlyDroppingThem() {
        assertThatThrownBy(() -> mapper.map(envelope(
                "PRESCRIPTION", SyncOperation.UPSERT, objectMapper.createObjectNode())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PRESCRIPTION");
    }

    private SyncEnvelope envelope(String entityType, SyncOperation operation, ObjectNode payload) {
        UUID entityId = UUID.fromString("6a7c7f1b-a61f-4f4b-96de-6748647735c1");
        return new SyncEnvelope(1, UUID.randomUUID(), UUID.randomUUID(), "SW", "master-data",
                entityType, entityId, operation, 3,
                Instant.parse("2026-10-31T12:00:00Z"), Instant.parse("2026-10-31T12:00:01Z"), payload);
    }
}
