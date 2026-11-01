package com.vetsync.pms.adapter.in.web.controller;

import com.vetsync.pms.adapter.in.web.dto.ClinicDto;
import com.vetsync.pms.application.PmsApplicationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/{region}/clinics")
@SecurityRequirement(name = "bearerAuth")
public class ClinicController {

    private final PmsApplicationService service;

    public ClinicController(PmsApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ClinicDto.ClinicView> clinics(@PathVariable String region) {
        return service.clinics(service.tenant(region));
    }

    @PostMapping
    public ResponseEntity<ClinicDto.ClinicView> createClinic(@PathVariable String region,
                                                              @Valid @RequestBody ClinicDto.ClinicRequest request) {
        ClinicDto.ClinicView created = service.createClinic(service.tenant(region), request);
        return ControllerResponses.created("/api/v1/" + region + "/clinics/" + created.id(), created);
    }

    @GetMapping("/{id}")
    public ClinicDto.ClinicView clinic(@PathVariable String region, @PathVariable long id) {
        return service.clinic(service.tenant(region), id);
    }

    @PutMapping("/{id}")
    public ClinicDto.ClinicView updateClinic(@PathVariable String region, @PathVariable long id,
                                             @Valid @RequestBody ClinicDto.ClinicRequest request) {
        return service.updateClinic(service.tenant(region), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClinic(@PathVariable String region, @PathVariable long id) {
        service.deleteClinic(service.tenant(region), id);
        return ResponseEntity.noContent().build();
    }
}
