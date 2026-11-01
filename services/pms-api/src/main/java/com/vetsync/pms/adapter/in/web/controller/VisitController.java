package com.vetsync.pms.adapter.in.web.controller;

import com.vetsync.pms.adapter.in.web.dto.VisitDto;
import com.vetsync.pms.application.PmsApplicationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/{region}/visits")
@SecurityRequirement(name = "bearerAuth")
public class VisitController {

    private final PmsApplicationService service;

    public VisitController(PmsApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<VisitDto.VisitView> visits(@PathVariable String region) {
        return service.visits(service.tenant(region));
    }

    @PostMapping
    public ResponseEntity<VisitDto.VisitView> createVisit(@PathVariable String region,
                                                           @Valid @RequestBody VisitDto.VisitRequest request) {
        VisitDto.VisitView created = service.createVisit(service.tenant(region), request);
        return ControllerResponses.created("/api/v1/" + region + "/visits/" + created.id(), created);
    }

    @GetMapping("/{id}")
    public VisitDto.VisitView visit(@PathVariable String region, @PathVariable long id) {
        return service.visit(service.tenant(region), id);
    }

    @PutMapping("/{id}")
    public VisitDto.VisitView updateVisit(@PathVariable String region, @PathVariable long id,
                                          @Valid @RequestBody VisitDto.VisitRequest request) {
        return service.updateVisit(service.tenant(region), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVisit(@PathVariable String region, @PathVariable long id) {
        service.deleteVisit(service.tenant(region), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public VisitDto.VisitView transitionStatus(@PathVariable String region, @PathVariable long id,
                                               @Valid @RequestBody
                                               VisitDto.StatusTransitionRequest request) {
        return service.transitionVisit(service.tenant(region), id, request.status());
    }
}
