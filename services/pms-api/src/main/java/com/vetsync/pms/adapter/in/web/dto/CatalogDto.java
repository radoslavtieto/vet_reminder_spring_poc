package com.vetsync.pms.adapter.in.web.dto;

public final class CatalogDto {

    private CatalogDto() {
    }

    public record SpeciesView(long id, String publicId, String tenant, String code, String name) {
    }

    public record BreedView(long id, String publicId, String tenant,
                            long speciesId, String code, String name) {
    }

    public record MedicationView(long id, String publicId, String tenant,
                                 String code, String name, String form, String strength,
                                 boolean controlled) {
    }
}
