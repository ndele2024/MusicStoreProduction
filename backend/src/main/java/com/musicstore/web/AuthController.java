package com.musicstore.web;

import com.musicstore.dto.AuthResponse;
import com.musicstore.dto.LoginRequest;
import com.musicstore.dto.RegisterRequest;
import com.musicstore.dto.UpdateProfileRequest;
import com.musicstore.dto.UserDto;
import com.musicstore.mapper.UserMapper;
import com.musicstore.security.AppUserDetails;
import com.musicstore.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification")
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;

    public AuthController(AuthService authService, UserMapper userMapper) {
        this.authService = authService;
        this.userMapper = userMapper;
    }

    @PostMapping("/register")
    @Operation(summary = "Crée un compte auditeur ou artiste et retourne un jeton")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Authentifie un utilisateur et retourne un jeton")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/email-disponible")
    @Operation(summary = "Indique si un email est déjà pris, pour la validation du formulaire d'inscription")
    public Map<String, Boolean> emailDisponible(@RequestParam String email) {
        return Map.of("exists", authService.emailExists(email));
    }

    @GetMapping("/me")
    @Operation(summary = "Profil de l'utilisateur authentifié")
    public UserDto me(@AuthenticationPrincipal AppUserDetails principal) {
        return userMapper.toDto(principal.getUser());
    }

    @PutMapping("/me")
    @Operation(summary = "Met à jour le profil de l'utilisateur authentifié")
    public UserDto updateMe(@AuthenticationPrincipal AppUserDetails principal,
                            @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(principal.getId(), request);
    }
}
