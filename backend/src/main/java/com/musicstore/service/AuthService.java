package com.musicstore.service;

import com.musicstore.domain.Role;
import com.musicstore.domain.User;
import com.musicstore.dto.AuthResponse;
import com.musicstore.dto.LoginRequest;
import com.musicstore.dto.RegisterRequest;
import com.musicstore.dto.UpdateProfileRequest;
import com.musicstore.dto.UserDto;
import com.musicstore.exception.ConflictException;
import com.musicstore.exception.NotFoundException;
import com.musicstore.mapper.UserMapper;
import com.musicstore.repository.UserRepository;
import com.musicstore.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUserEmailIgnoreCase(request.userEmail())) {
            throw new ConflictException("Cet email est déjà utilisé");
        }
        if (userRepository.existsByUserNameIgnoreCase(request.userName())) {
            throw new ConflictException("Ce nom d'utilisateur est déjà pris");
        }

        User user = User.builder()
                .fullName(request.fullName())
                .age(request.age())
                .sex(request.sex())
                .userName(request.userName())
                .userEmail(request.userEmail().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.fromApi(request.role()))
                .avatar("")
                .preferences(new ArrayList<>())
                .build();

        return buildResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUserEmailIgnoreCase(request.userEmail())
                // Message identique pour un email inconnu et un mot de passe faux : pas d'énumération de comptes.
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe invalide"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Email ou mot de passe invalide");
        }
        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return email != null && !email.isBlank() && userRepository.existsByUserEmailIgnoreCase(email);
    }

    @Transactional
    public UserDto updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("Utilisateur", userId));

        String nouvelEmail = request.userEmail().toLowerCase();
        if (!nouvelEmail.equalsIgnoreCase(user.getUserEmail())
                && userRepository.existsByUserEmailIgnoreCase(nouvelEmail)) {
            throw new ConflictException("Cet email est déjà utilisé");
        }

        user.setFullName(request.fullName());
        user.setUserEmail(nouvelEmail);
        user.setAge(request.age());
        user.setSex(request.sex());
        user.setAvatar(request.avatar());
        if (request.preferences() != null) {
            user.setPreferences(new ArrayList<>(request.preferences()));
        }
        return userMapper.toDto(userRepository.save(user));
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.expirationSeconds(),
                userMapper.toDto(user));
    }
}
