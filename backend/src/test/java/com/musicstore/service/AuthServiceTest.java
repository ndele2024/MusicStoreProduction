package com.musicstore.service;

import com.musicstore.domain.Role;
import com.musicstore.domain.User;
import com.musicstore.dto.AuthResponse;
import com.musicstore.dto.LoginRequest;
import com.musicstore.dto.RegisterRequest;
import com.musicstore.dto.UpdateProfileRequest;
import com.musicstore.exception.ConflictException;
import com.musicstore.exception.NotFoundException;
import com.musicstore.mapper.UserMapper;
import com.musicstore.repository.UserRepository;
import com.musicstore.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private AuthService authService;

    private static RegisterRequest inscription() {
        return new RegisterRequest("Jean Dupont", 25, "Homme", "jdupont", "Jean@Example.com", "Password1!", "auditeur");
    }

    private static User enBase(String email, String hash, Role role) {
        return User.builder()
                .id(UUID.randomUUID())
                .fullName("Jean Dupont")
                .userName("jdupont")
                .userEmail(email)
                .password(hash)
                .role(role)
                .build();
    }

    @Test
    @DisplayName("l'inscription hache le mot de passe, normalise l'email et retourne un jeton")
    void inscritUnUtilisateur() {
        when(userRepository.existsByUserEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByUserNameIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("hash-bcrypt");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jeton");
        when(jwtService.expirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.register(inscription());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User enregistre = captor.getValue();
        assertThat(enregistre.getPassword()).isEqualTo("hash-bcrypt");
        assertThat(enregistre.getUserEmail()).isEqualTo("jean@example.com");
        assertThat(enregistre.getRole()).isEqualTo(Role.AUDITEUR);

        assertThat(response.token()).isEqualTo("jeton");
        assertThat(response.expiresInSeconds()).isEqualTo(3600L);
        assertThat(response.user().userEmail()).isEqualTo("jean@example.com");
    }

    @Test
    @DisplayName("le role artiste demande par le formulaire est bien pris en compte")
    void inscritUnArtiste() {
        when(userRepository.existsByUserEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByUserNameIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jeton");

        AuthResponse response = authService.register(new RegisterRequest(
                "Ed Sheeran", 32, "Homme", "edsheeran", "ed@example.com", "Password1!", "artiste"));

        assertThat(response.user().role()).isEqualTo("artiste");
    }

    @Test
    @DisplayName("un email deja pris renvoie un conflit sans creer de compte")
    void refuseUnEmailDejaPris() {
        when(userRepository.existsByUserEmailIgnoreCase("Jean@Example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(inscription()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("email");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("un nom d'utilisateur deja pris renvoie un conflit")
    void refuseUnNomDejaPris() {
        when(userRepository.existsByUserEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByUserNameIgnoreCase("jdupont")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(inscription()))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("la connexion reussit quand le mot de passe correspond au hash")
    void connecteUnUtilisateur() {
        User user = enBase("jean@example.com", "hash", Role.AUDITEUR);
        when(userRepository.findByUserEmailIgnoreCase("jean@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1!", "hash")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jeton");

        AuthResponse response = authService.login(new LoginRequest("jean@example.com", "Password1!"));

        assertThat(response.token()).isEqualTo("jeton");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("un mot de passe errone et un email inconnu produisent le meme message")
    void refuseDesIdentifiantsInvalides() {
        User user = enBase("jean@example.com", "hash", Role.AUDITEUR);
        when(userRepository.findByUserEmailIgnoreCase("jean@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("mauvais", "hash")).thenReturn(false);
        when(userRepository.findByUserEmailIgnoreCase("inconnu@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("jean@example.com", "mauvais")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email ou mot de passe invalide");
        assertThatThrownBy(() -> authService.login(new LoginRequest("inconnu@example.com", "peu-importe")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email ou mot de passe invalide");
    }

    @Test
    @DisplayName("la mise a jour du profil refuse un email deja utilise par un autre compte")
    void refuseUnEmailDejaUtiliseALaMiseAJour() {
        User user = enBase("jean@example.com", "hash", Role.AUDITEUR);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByUserEmailIgnoreCase("alice@example.com")).thenReturn(true);

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Jean Dupont", "alice@example.com", 26, "Homme", "", List.of());

        assertThatThrownBy(() -> authService.updateProfile(user.getId(), request))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("conserver son propre email lors d'une mise a jour ne declenche pas de conflit")
    void autoriseLeMemeEmailALaMiseAJour() {
        User user = enBase("jean@example.com", "hash", Role.AUDITEUR);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = authService.updateProfile(user.getId(), new UpdateProfileRequest(
                "Jean D.", "Jean@example.com", 26, "Homme", "avatar.png", List.of("Pop")));

        assertThat(dto.fullName()).isEqualTo("Jean D.");
        assertThat(dto.userEmail()).isEqualTo("jean@example.com");
        assertThat(dto.preferences()).containsExactly("Pop");
    }

    @Test
    @DisplayName("mettre a jour un compte inexistant leve une erreur 404")
    void refuseUnCompteInexistant() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updateProfile(id, new UpdateProfileRequest(
                "X", "x@example.com", 20, "Homme", "", List.of())))
                .isInstanceOf(NotFoundException.class);
    }
}
