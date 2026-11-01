package com.vetsync.sync.domain;

import java.util.Arrays;

public enum SyncStream {
    MASTER_DATA("master-data", true),
    VISITS("visits", true),
    PRESCRIPTIONS("prescriptions", true),
    INVOICES("invoices", true),
    PRESCRIPTION_EXPIRY("prescription-expiry", false),
    UNPAID_INVOICES("unpaid-invoices", false);

    private final String contractName;
    private final boolean auditStream;

    SyncStream(String contractName, boolean auditStream) {
        this.contractName = contractName;
        this.auditStream = auditStream;
    }

    public String contractName() {
        return contractName;
    }

    public boolean isAuditStream() {
        return auditStream;
    }

    public static SyncStream fromContractName(String contractName) {
        return Arrays.stream(values())
                .filter(stream -> stream.contractName.equals(contractName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported sync stream: " + contractName));
    }
}
