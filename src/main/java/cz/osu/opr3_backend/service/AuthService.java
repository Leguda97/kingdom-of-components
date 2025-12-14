package cz.osu.opr3_backend.service;

import cz.osu.opr3_backend.model.entity.User;
import cz.osu.opr3_backend.model.repo.UserRepository;
import cz.osu.opr3_backend.security.JwtService;
import cz.osu.opr3_backend.security.SecurityUtils;
import cz.osu.opr3_backend.web.dto.auth.AuthLoginRequest;
import cz.osu.opr3_backend.web.dto.auth.AuthRegisterRequest;
import cz.osu.opr3_backend.web.dto.auth.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(AuthRegisterRequest req) {
        String actor = SecurityUtils.usernameOrAnonymous();

        if (userRepository.existsByUsername(req.username())) {
            log.warn("AUDIT AUTH_REGISTER_DENIED actor={} reason=username_exists username={}", actor, req.username());
            throw new ConflictException("Username already exists");
        }
        if (userRepository.existsByEmail(req.email())) {
            log.warn("AUDIT AUTH_REGISTER_DENIED actor={} reason=email_exists email={}", actor, req.email());
            throw new ConflictException("Email already exists");
        }

        User user = User.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(User.Role.USER)
                .build();

        User saved = userRepository.save(user);

        log.info("AUDIT AUTH_REGISTER_OK actor={} userId={} username={} role={}",
                actor, saved.getId(), saved.getUsername(), saved.getRole());

        String token = jwtService.generateToken(saved.getUsername(), saved.getRole().name());
        return new AuthResponse(token, saved.getRole().name());
    }

    public AuthResponse login(AuthLoginRequest req) {
        String actor = SecurityUtils.usernameOrAnonymous();

        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> {
                    log.warn("AUDIT AUTH_LOGIN_FAIL actor={} reason=bad_credentials username={}", actor, req.username());
                    return new IllegalArgumentException("Invalid credentials");
                });

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            log.warn("AUDIT AUTH_LOGIN_FAIL actor={} reason=bad_credentials username={}", actor, req.username());
            throw new IllegalArgumentException("Invalid credentials");
        }

        log.info("AUDIT AUTH_LOGIN_OK actor={} username={} role={}", actor, user.getUsername(), user.getRole());

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getRole().name());
    }
}
