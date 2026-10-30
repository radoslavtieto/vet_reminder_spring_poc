package com.vetsync.pms.adapter.in.web.controller;

import com.vetsync.pms.adapter.in.web.dto.StaffDto;
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
public class StaffController {

    private final PmsApplicationService service;

    public StaffController(PmsApplicationService service) {
        this.service = service;
    }

    @GetMapping("/staff")
    public List<StaffDto.StaffView> staff(@PathVariable String region) {
        return service.staff(service.tenant(region));
    }

    @PostMapping("/staff")
    public ResponseEntity<StaffDto.StaffView> createStaff(@PathVariable String region,
                                                           @Valid @RequestBody StaffDto.StaffRequest request) {
        StaffDto.StaffView created = service.createStaff(service.tenant(region), request);
        return ControllerResponses.created("/api/v1/" + region + "/staff/" + created.id(), created);
    }

    @GetMapping("/staff/{id}")
    public StaffDto.StaffView staff(@PathVariable String region, @PathVariable long id) {
        return service.staff(service.tenant(region), id);
    }

    @PutMapping("/staff/{id}")
    public StaffDto.StaffView updateStaff(@PathVariable String region, @PathVariable long id,
                                          @Valid @RequestBody StaffDto.StaffRequest request) {
        return service.updateStaff(service.tenant(region), id, request);
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable String region, @PathVariable long id) {
        service.deleteStaff(service.tenant(region), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/clinics/{clinicId}/staff")
    public List<StaffDto.StaffView> clinicStaff(@PathVariable String region, @PathVariable long clinicId) {
        return service.staffForClinic(service.tenant(region), clinicId);
    }

    @PostMapping("/clinics/{clinicId}/staff")
    public ResponseEntity<StaffDto.StaffView> createClinicStaff(@PathVariable String region,
                                                                 @PathVariable long clinicId,
                                                                 @Valid @RequestBody StaffDto.StaffRequest request) {
        StaffDto.StaffView created = service.createStaff(service.tenant(region), clinicId, request);
        return ControllerResponses.created("/api/v1/" + region + "/staff/" + created.id(), created);
    }
}
