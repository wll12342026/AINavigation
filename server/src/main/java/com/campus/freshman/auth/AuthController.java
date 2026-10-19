package com.campus.freshman.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AdminSessions sessions;
    public AuthController(AdminSessions sessions) { this.sessions=sessions; }
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        String password=System.getenv().getOrDefault("CAMPUS_ADMIN_PASSWORD","admin123");
        if (!"admin".equals(request.username()) || !password.equals(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        return new LoginResponse(sessions.issue(), "admin", "林管理员", "ADMIN", LocalDateTime.now().plusHours(8));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String token, String username, String displayName, String role, LocalDateTime expiresAt) {}
}
