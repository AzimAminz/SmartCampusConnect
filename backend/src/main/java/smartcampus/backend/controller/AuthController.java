package smartcampus.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smartcampus.backend.model.User;
import smartcampus.backend.model.UserSession;
import smartcampus.backend.repository.UserRepository;
import smartcampus.backend.repository.UserSessionRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Auth REST API
 *
 * No password required — login by userId (matric no. or "ADMIN").
 *
 * POST /api/auth/login   — Login, returns token
 * GET  /api/auth/me      — Who am I? (requires X-Auth-Token header)
 * POST /api/auth/logout  — Logout (deletes session)
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private UserSessionRepository userSessionRepository;

    // -------------------------------------------------------------------------
    // POST /api/auth/login
    // Body: { "userId": "B032310001" }   or   { "userId": "ADMIN" }
    // -------------------------------------------------------------------------
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "userId is required"));
        }

        Optional<User> userOpt = userRepository.findByUserId(userId.trim().toUpperCase()
                // Keep original casing if not ADMIN
                .equals("ADMIN") ? "ADMIN" : userId.trim());

        // Try original case if upper didn't match
        if (!userOpt.isPresent() && !userId.trim().equalsIgnoreCase("ADMIN")) {
            userOpt = userRepository.findByUserId(userId.trim());
        }

        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found. Please check your matric number / user ID."));
        }

        User user = userOpt.get();

        // Generate new UUID token
        String token = UUID.randomUUID().toString();
        UserSession session = new UserSession(token, user.getUserId(), user.getRole(), user.getFullName());
        userSessionRepository.save(session);

        return ResponseEntity.ok(Map.of(
                "token",    token,
                "userId",   user.getUserId(),
                "role",     user.getRole().name(),
                "fullName", user.getFullName(),
                "expiresAt", session.getExpiresAt().toString(),
                "message",  "Login successful. Welcome, " + user.getFullName() + "!"
        ));
    }

    // -------------------------------------------------------------------------
    // GET /api/auth/me
    // Header: X-Auth-Token: <token>
    // -------------------------------------------------------------------------
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing X-Auth-Token header. Please login first."));
        }

        Optional<UserSession> sessionOpt = userSessionRepository.findByToken(token);
        if (!sessionOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token. Please login again."));
        }

        UserSession session = sessionOpt.get();
        if (session.isExpired()) {
            userSessionRepository.deleteByToken(token);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Session expired. Please login again."));
        }

        return ResponseEntity.ok(Map.of(
                "userId",   session.getUserId(),
                "role",     session.getRole().name(),
                "fullName", session.getFullName(),
                "expiresAt", session.getExpiresAt().toString()
        ));
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/logout
    // Header: X-Auth-Token: <token>
    // -------------------------------------------------------------------------
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing X-Auth-Token header."));
        }
        userSessionRepository.deleteByToken(token);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    // -------------------------------------------------------------------------
    // Helper: Resolve session from token (shared by other controllers)
    // -------------------------------------------------------------------------
    public static ResponseEntity<?> unauthorizedResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Unauthorized. Please login and provide X-Auth-Token header."));
    }
}
