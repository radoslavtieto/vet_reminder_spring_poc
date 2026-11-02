package com.vetsync.action.domain;

import java.util.UUID;

public record Contact(
        String tenant,
        UUID sourceId,
        long sourceVersion,
        String displayName,
        String email,
        String phone,
        String preferredChannel,
        boolean consentGiven,
        boolean active
) implements ActionRecord {
}
