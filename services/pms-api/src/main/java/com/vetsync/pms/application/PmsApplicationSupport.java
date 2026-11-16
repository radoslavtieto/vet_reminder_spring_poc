package com.vetsync.pms.application;

import com.vetsync.pms.adapter.out.persistence.entity.BusinessEntity;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

final class PmsApplicationSupport {
    private PmsApplicationSupport() { }

    static <T extends BusinessEntity> T find(java.util.Optional<T> entity) {
        return entity.filter(value -> value.getDeletedAt() == null).orElseThrow(() -> missing("Resource not found"));
    }
    static <T extends BusinessEntity> List<T> active(List<T> entities) {
        return entities.stream().filter(entity -> entity.getDeletedAt() == null).toList();
    }
    static void softDelete(BusinessEntity entity, Clock clock) {
        entity.setDeletedAt(Instant.now(clock));
    }
    static String name(Enum<?> value) { return value == null ? null : value.name(); }
    static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
    static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
    static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw badRequest("Invalid " + type.getSimpleName() + " value: " + value);
        }
    }
    static PmsResourceNotFoundException missing(String message) {
        return new PmsResourceNotFoundException(message);
    }
    static PmsInvalidRequestException badRequest(String message) {
        return new PmsInvalidRequestException(message);
    }
}
