package com.vetsync.action.domain;

import java.util.UUID;

public record CareSubject(
        String tenant,
        UUID sourceId,
        long sourceVersion,
        String displayName,
        String ownerSourceId,
        String speciesCode,
        String status,
        boolean active
) implements ActionRecord {
}
