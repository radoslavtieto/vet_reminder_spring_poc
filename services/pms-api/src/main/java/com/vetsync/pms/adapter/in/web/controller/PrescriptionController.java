package com.vetsync.pms.adapter.in.web.controller;

import com.vetsync.pms.adapter.in.web.dto.PrescriptionDto;
import com.vetsync.pms.application.PmsApplicationService;
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
@RequestMapping("/api/v1/{region}")
public class PrescriptionController {

    private final PmsApplicationService service;

    public PrescriptionController(PmsApplicationService service) {
        this.service = service;
    }

    @GetMapping("/prescriptions")
    public List<PrescriptionDto.PrescriptionView> prescriptions(@PathVariable String region) {
        return service.prescriptions(service.tenant(region));
    }

    @PostMapping("/prescriptions")
    public ResponseEntity<PrescriptionDto.PrescriptionView> createPrescription(@PathVariable String region,
                                                                                @Valid @RequestBody
                                                                                PrescriptionDto.PrescriptionRequest request) {
        PrescriptionDto.PrescriptionView created = service.createPrescription(service.tenant(region), request);
        return ControllerResponses.created("/api/v1/" + region + "/prescriptions/" + created.id(), created);
    }

    @GetMapping("/prescriptions/{id}")
    public PrescriptionDto.PrescriptionView prescription(@PathVariable String region, @PathVariable long id) {
        return service.prescription(service.tenant(region), id);
    }

    @PutMapping("/prescriptions/{id}")
    public PrescriptionDto.PrescriptionView updatePrescription(@PathVariable String region, @PathVariable long id,
                                                                 @Valid @RequestBody
                                                                 PrescriptionDto.PrescriptionRequest request) {
        return service.updatePrescription(service.tenant(region), id, request);
    }

    @DeleteMapping("/prescriptions/{id}")
    public ResponseEntity<Void> deletePrescription(@PathVariable String region, @PathVariable long id) {
        service.deletePrescription(service.tenant(region), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/visits/{visitId}/prescriptions")
    public List<PrescriptionDto.PrescriptionView> visitPrescriptions(@PathVariable String region,
                                                                      @PathVariable long visitId) {
        return service.prescriptionsForVisit(service.tenant(region), visitId);
    }

    @PostMapping("/visits/{visitId}/prescriptions")
    public ResponseEntity<PrescriptionDto.PrescriptionView> createVisitPrescription(@PathVariable String region,
                                                                                      @PathVariable long visitId,
                                                                                      @Valid @RequestBody
                                                                                      PrescriptionDto.PrescriptionRequest request) {
        PrescriptionDto.PrescriptionView created =
                service.createPrescriptionForVisit(service.tenant(region), visitId, request);
        return ControllerResponses.created("/api/v1/" + region + "/prescriptions/" + created.id(), created);
    }
}
