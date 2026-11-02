package com.vetsync.action.domain;

import java.util.UUID;

public record Site(
        String tenant,
        UUID sourceId,
        long sourceVersion,
        String displayName,
        String city,
        boolean active
) implements ActionRecord {
}
