package com.ga.auth_service.controller;

import com.ga.auth_service.model.User;
import com.ga.auth_service.model.request.LoginRequest;
import com.ga.auth_service.model.response.LoginResponse;
import com.ga.auth_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public User createUser(@Valid @RequestBody User user) {
        return userService.createUser(user);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(
            @RequestBody LoginRequest request) {

        return userService.loginUser(request);
    }
}