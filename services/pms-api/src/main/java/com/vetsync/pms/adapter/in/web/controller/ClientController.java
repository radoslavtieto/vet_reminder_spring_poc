package com.vetsync.pms.adapter.in.web.controller;

import com.vetsync.pms.adapter.in.web.dto.ClientDto;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/{region}/clients")
public class ClientController {

    private final PmsApplicationService service;

    public ClientController(PmsApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ClientDto.ClientView> clients(@PathVariable String region,
                                         @RequestParam(required = false) String q) {
        return service.clients(service.tenant(region), q);
    }

    @PostMapping
    public ResponseEntity<ClientDto.ClientView> createClient(@PathVariable String region,
                                                              @Valid @RequestBody ClientDto.ClientRequest request) {
        ClientDto.ClientView created = service.createClient(service.tenant(region), request);
        return ControllerResponses.created("/api/v1/" + region + "/clients/" + created.id(), created);
    }

    @GetMapping("/{id}")
    public ClientDto.ClientView client(@PathVariable String region, @PathVariable long id) {
        return service.client(service.tenant(region), id);
    }

    @PutMapping("/{id}")
    public ClientDto.ClientView updateClient(@PathVariable String region, @PathVariable long id,
                                             @Valid @RequestBody ClientDto.ClientRequest request) {
        return service.updateClient(service.tenant(region), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable String region, @PathVariable long id) {
        service.deleteClient(service.tenant(region), id);
        return ResponseEntity.noContent().build();
    }
}
