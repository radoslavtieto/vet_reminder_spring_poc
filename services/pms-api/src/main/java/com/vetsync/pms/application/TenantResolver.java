package com.vetsync.pms.application;

import java.util.Map;

public final class TenantResolver {

    private static final Map<String, String> TENANTS = Map.of(
            "sw", "SW",
            "se", "SE",
            "sc", "SC",
            "ne", "NE",
            "nw", "NW",
            "nc", "NC");

    private TenantResolver() {
    }

    public static String resolve(String region) {
        String tenant = TENANTS.get(region);
        if (tenant == null) {
            throw new PmsResourceNotFoundException("Unknown region");
        }
        return tenant;
    }
}
