package vn.codegym.clinicbook.controller.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.codegym.clinicbook.dto.ApiResponses.AuthResponse;
import vn.codegym.clinicbook.dto.LoginRequest;
import vn.codegym.clinicbook.dto.RegisterRequest;
import vn.codegym.clinicbook.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
    private final AuthService authService;

    public AuthApiController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.registerPatient(request);
        LoginRequest login = new LoginRequest();
        login.setUsername(request.getUsername().trim());
        login.setPassword(request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.login(login));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
