package com.vetsync.pms.adapter.in.web.controller;

import com.vetsync.pms.adapter.in.web.dto.CatalogDto;
import com.vetsync.pms.application.PmsApplicationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/{region}")
@SecurityRequirement(name = "bearerAuth")
public class CatalogController {

    private final PmsApplicationService service;

    public CatalogController(PmsApplicationService service) {
        this.service = service;
    }

    @GetMapping("/species")
    public List<CatalogDto.SpeciesView> species(@PathVariable String region) {
        return service.species(service.tenant(region));
    }

    @GetMapping("/species/{speciesId}/breeds")
    public List<CatalogDto.BreedView> breeds(@PathVariable String region, @PathVariable long speciesId) {
        return service.breeds(service.tenant(region), speciesId);
    }

    @GetMapping("/medications")
    public List<CatalogDto.MedicationView> medications(@PathVariable String region) {
        return service.medications(service.tenant(region));
    }
}
