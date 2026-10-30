package com.vetsync.pms.adapter.in.web.controller;

import org.springframework.http.ResponseEntity;

import java.net.URI;

final class ControllerResponses {

    private ControllerResponses() {
    }

    static <T> ResponseEntity<T> created(String location, T body) {
        return ResponseEntity.created(URI.create(location)).body(body);
    }
}
