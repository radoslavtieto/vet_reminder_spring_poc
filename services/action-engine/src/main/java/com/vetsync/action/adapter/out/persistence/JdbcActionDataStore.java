package com.vetsync.action.adapter.out.persistence;

import com.vetsync.action.application.ActionDataStore;
import com.vetsync.action.domain.ActionRecord;
import com.vetsync.action.domain.CareSubject;
import com.vetsync.action.domain.Contact;
import com.vetsync.action.domain.Site;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class JdbcActionDataStore implements ActionDataStore {
    private final JdbcTemplate jdbcTemplate;

    public JdbcActionDataStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean claimEvent(UUID eventId, String tenant) {
        return jdbcTemplate.update("""
                INSERT INTO processed_event (event_id, tenant, processed_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (event_id) DO NOTHING
                """, eventId, tenant) == 1;
    }

    @Override
    public void upsert(ActionRecord record) {
        if (record instanceof Site site) {
            upsertSite(site);
        } else if (record instanceof Contact contact) {
            upsertContact(contact);
        } else if (record instanceof CareSubject subject) {
            upsertCareSubject(subject);
        } else {
            throw new IllegalArgumentException("Unsupported action-engine record type: " + record.getClass());
        }
    }

    private void upsertSite(Site site) {
        jdbcTemplate.update("""
                INSERT INTO site (tenant, source_id, source_version, display_name, city, active, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (tenant, source_id) DO UPDATE SET
                    source_version = EXCLUDED.source_version,
                    display_name = EXCLUDED.display_name,
                    city = EXCLUDED.city,
                    active = EXCLUDED.active,
                    updated_at = CURRENT_TIMESTAMP
                WHERE site.source_version < EXCLUDED.source_version
                """, site.tenant(), site.sourceId(), site.sourceVersion(), site.displayName(), site.city(),
                site.active());
    }

    private void upsertContact(Contact contact) {
        jdbcTemplate.update("""
                INSERT INTO contact
                    (tenant, source_id, source_version, display_name, email, phone, preferred_channel,
                     consent_given, active, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (tenant, source_id) DO UPDATE SET
                    source_version = EXCLUDED.source_version,
                    display_name = EXCLUDED.display_name,
                    email = EXCLUDED.email,
                    phone = EXCLUDED.phone,
                    preferred_channel = EXCLUDED.preferred_channel,
                    consent_given = EXCLUDED.consent_given,
                    active = EXCLUDED.active,
                    updated_at = CURRENT_TIMESTAMP
                WHERE contact.source_version < EXCLUDED.source_version
                """, contact.tenant(), contact.sourceId(), contact.sourceVersion(), contact.displayName(),
                contact.email(), contact.phone(), contact.preferredChannel(), contact.consentGiven(), contact.active());
    }

    private void upsertCareSubject(CareSubject subject) {
        jdbcTemplate.update("""
                INSERT INTO care_subject
                    (tenant, source_id, source_version, display_name, owner_source_id, species_code, status, active, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (tenant, source_id) DO UPDATE SET
                    source_version = EXCLUDED.source_version,
                    display_name = EXCLUDED.display_name,
                    owner_source_id = EXCLUDED.owner_source_id,
                    species_code = EXCLUDED.species_code,
                    status = EXCLUDED.status,
                    active = EXCLUDED.active,
                    updated_at = CURRENT_TIMESTAMP
                WHERE care_subject.source_version < EXCLUDED.source_version
                """, subject.tenant(), subject.sourceId(), subject.sourceVersion(), subject.displayName(),
                subject.ownerSourceId(), subject.speciesCode(), subject.status(), subject.active());
    }
}
