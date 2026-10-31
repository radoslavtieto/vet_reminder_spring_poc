package com.vetsync.action.domain;

import java.util.UUID;

public sealed interface ActionRecord permits Site, Contact, CareSubject {
    String tenant();

    UUID sourceId();

    long sourceVersion();

    boolean active();
}
