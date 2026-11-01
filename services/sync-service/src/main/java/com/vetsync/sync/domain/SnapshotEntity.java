package com.vetsync.sync.domain;

import java.util.Arrays;
import java.util.Locale;

public enum SnapshotEntity {
    CLINIC("clinic", "clinic", SyncStream.MASTER_DATA),
    STAFF("staff", "staff", SyncStream.MASTER_DATA),
    CLIENT("client", "client", SyncStream.MASTER_DATA),
    SPECIES("species", "species", SyncStream.MASTER_DATA),
    BREED("breed", "breed", SyncStream.MASTER_DATA),
    PATIENT("patient", "patient", SyncStream.MASTER_DATA),
    VISIT("visit", "visit", SyncStream.VISITS),
    MEDICAL_RECORD_ENTRY("medical_record_entry", "medical_record_entry", SyncStream.VISITS),
    MEDICATION("medication", "medication", SyncStream.PRESCRIPTIONS),
    PRESCRIPTION("prescription", "prescription", SyncStream.PRESCRIPTIONS),
    PRESCRIPTION_ITEM("prescription_item", "prescription_item", SyncStream.PRESCRIPTIONS),
    INVOICE("invoice", "invoice", SyncStream.INVOICES),
    INVOICE_LINE("invoice_line", "invoice_line", SyncStream.INVOICES),
    PAYMENT("payment", "payment", SyncStream.INVOICES);

    private final String contractName;
    private final String tableName;
    private final SyncStream stream;

    SnapshotEntity(String contractName, String tableName, SyncStream stream) {
        this.contractName = contractName;
        this.tableName = tableName;
        this.stream = stream;
    }

    public String contractName() {
        return contractName;
    }

    public String tableName() {
        return tableName;
    }

    public String entityType() {
        return name();
    }

    public SyncStream stream() {
        return stream;
    }

    public static SnapshotEntity fromContractName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Snapshot entity must not be blank");
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(entity -> entity.contractName.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported snapshot entity: " + name));
    }
}
