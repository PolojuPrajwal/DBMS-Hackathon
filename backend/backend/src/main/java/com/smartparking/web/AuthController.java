package com.smartparking.web;

import com.smartparking.model.AppUser;
import com.smartparking.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    public record RegisterReq(String username, String password) {}

    @PostMapping("/register")
    Map<String, String> register(@RequestBody RegisterReq r) {
        if (r.username() == null || r.username().isBlank() || r.password() == null || r.password().length() < 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username required and password needs 6+ characters");
        if (users.findByUsername(r.username()).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Username taken");
        users.save(new AppUser(null, r.username(), encoder.encode(r.password()), "USER")); // self-registration is always USER
        return Map.of("username", r.username(), "role", "USER");
    }

    @GetMapping("/me")
    Map<String, String> me(Authentication auth) {
        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        return Map.of("username", auth.getName(), "role", role);
    }
}
