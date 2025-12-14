package cz.osu.opr3_backend.web;

import cz.osu.opr3_backend.service.AuthService;
import cz.osu.opr3_backend.web.dto.auth.AuthLoginRequest;
import cz.osu.opr3_backend.web.dto.auth.AuthRegisterRequest;
import cz.osu.opr3_backend.web.dto.auth.AuthResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody AuthRegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthLoginRequest req) {
        return authService.login(req);
    }
}
