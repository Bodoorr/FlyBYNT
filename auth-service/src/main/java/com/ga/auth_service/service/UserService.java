package com.ga.auth_service.service;

import com.ga.auth_service.model.User;
import com.ga.auth_service.model.request.LoginRequest;
import com.ga.auth_service.model.response.LoginResponse;
import com.ga.auth_service.repository.UserRepository;
import com.ga.auth_service.security.JWTUtils;
import com.ga.auth_service.security.MyUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    public User createUser(User user) {

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email already exists");
        }

        user.setId(null);
        user.setCreatedAt(null);
        user.setUpdatedAt(null);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    public ResponseEntity<LoginResponse> loginUser(LoginRequest request) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            MyUserDetails userDetails =
                    (MyUserDetails) authentication.getPrincipal();

            String token = jwtUtils.generateJwtToken(userDetails);

            return ResponseEntity.ok(new LoginResponse(token));

        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
    }
}