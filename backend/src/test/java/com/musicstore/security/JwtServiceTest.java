package com.musicstore.security;

import com.musicstore.domain.Role;
import com.musicstore.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "secret-de-test-musicstore-suffisamment-long-pour-hs256";

    private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, 3600, "musicstore"));

    private static User utilisateur() {
        return User.builder()
                .id(UUID.randomUUID())
                .fullName("Jean Dupont")
                .userName("jdupont")
                .userEmail("jean@example.com")
                .password("hash")
                .role(Role.AUDITEUR)
                .build();
    }

    @Test
    @DisplayName("un jeton emis est relu avec le meme identifiant et les memes claims")
    void genereEtRelitUnJeton() {
        User user = utilisateur();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUserId(token)).contains(user.getId());
        assertThat(jwtService.parse(token)).hasValueSatisfying(claims -> {
            assertThat(claims.get("email")).isEqualTo("jean@example.com");
            assertThat(claims.get("role")).isEqualTo("AUDITEUR");
            assertThat(claims.getIssuer()).isEqualTo("musicstore");
        });
    }

    @Test
    @DisplayName("un jeton signe avec une autre cle est rejete")
    void rejetteUneSignatureEtrangere() {
        String tokenEtranger = new JwtService(new JwtProperties(
                "une-autre-cle-de-test-tout-aussi-longue-pour-hs256", 3600, "musicstore"))
                .generateToken(utilisateur());

        assertThat(jwtService.parse(tokenEtranger)).isEmpty();
        assertThat(jwtService.extractUserId(tokenEtranger)).isEmpty();
    }

    @Test
    @DisplayName("un jeton emis par un autre emetteur est rejete")
    void rejetteUnAutreEmetteur() {
        String token = new JwtService(new JwtProperties(SECRET, 3600, "autre-service")).generateToken(utilisateur());

        assertThat(jwtService.parse(token)).isEmpty();
    }

    @Test
    @DisplayName("un jeton expire est rejete")
    void rejetteUnJetonExpire() throws InterruptedException {
        JwtService court = new JwtService(new JwtProperties(SECRET, 1, "musicstore"));
        String token = court.generateToken(utilisateur());

        // La tolerance d'horloge par defaut de jjwt est nulle : une seconde suffit a expirer le jeton.
        Thread.sleep(1_500);

        assertThat(court.parse(token)).isEmpty();
    }

    @Test
    @DisplayName("un jeton absent ou malforme ne fait pas echouer la lecture")
    void toleredesJetonsInvalides() {
        assertThat(jwtService.parse(null)).isEmpty();
        assertThat(jwtService.parse("   ")).isEmpty();
        assertThat(jwtService.parse("pas.un.jwt")).isEmpty();
    }

    @Test
    @DisplayName("une cle de moins de 32 octets est refusee au demarrage")
    void refuseUneCleTropCourte() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("trop-court", 3600, "musicstore")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 octets");
    }
}
