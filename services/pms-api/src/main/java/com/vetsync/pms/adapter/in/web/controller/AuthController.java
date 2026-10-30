package com.vetsync.pms.adapter.in.web.controller;

import com.vetsync.pms.adapter.in.web.dto.AuthDto;
import com.vetsync.pms.application.PmsApplicationService;
import com.vetsync.pms.application.PmsAuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/{region}")
public class AuthController {

    private final PmsApplicationService service;
    private final PmsAuthenticationService authentication;

    public AuthController(PmsApplicationService service, PmsAuthenticationService authentication) {
        this.service = service;
        this.authentication = authentication;
    }

    @PostMapping("/auth/login")
    public AuthDto.TokenResponse login(@PathVariable String region,
                                       @Valid @RequestBody AuthDto.LoginRequest request) {
        return authentication.login(service.tenant(region), request.username(), request.password());
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDto.UserView createUser(@PathVariable String region,
                                       @Valid @RequestBody AuthDto.CreateUserRequest request) {
        return authentication.createUser(service.tenant(region), request);
    }
}
