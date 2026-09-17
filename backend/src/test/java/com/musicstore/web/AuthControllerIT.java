package com.musicstore.web;

import com.musicstore.domain.Role;
import com.musicstore.domain.User;
import com.musicstore.dto.LoginRequest;
import com.musicstore.dto.RegisterRequest;
import com.musicstore.dto.UpdateProfileRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("inscription puis appel de /me avec le jeton retourne le profil cree")
    void inscriptionPuisProfil() throws Exception {
        String reponse = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(json(new RegisterRequest("Jean Dupont", 25, "Homme", "jdupont",
                                "Jean@Example.com", MOT_DE_PASSE, "auditeur"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.role").value("auditeur"))
                .andExpect(jsonPath("$.user.userEmail").value("jean@example.com"))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(reponse).get("token").asText();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("jdupont"));

        // Le mot de passe est stocké haché, jamais en clair.
        User enBase = userRepository.findByUserEmailIgnoreCase("jean@example.com").orElseThrow();
        assertThat(enBase.getPassword()).isNotEqualTo(MOT_DE_PASSE).startsWith("$2");
    }

    @Test
    @DisplayName("la reponse d'inscription ne contient jamais le mot de passe")
    void neDivulguePasLeMotDePasse() throws Exception {
        String reponse = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(json(new RegisterRequest("Jean", 25, "Homme", "jdupont",
                                "jean@example.com", MOT_DE_PASSE, "auditeur"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(reponse).doesNotContain("password").doesNotContain(MOT_DE_PASSE);
    }

    @Test
    @DisplayName("un mot de passe trop faible est refuse avec le detail du champ")
    void refuseUnMotDePasseFaible() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(json(new RegisterRequest("Jean", 25, "Homme", "jdupont",
                                "jean@example.com", "motdepasse", "auditeur"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password", notNullValue()));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    @DisplayName("un email deja pris renvoie 409")
    void refuseUnEmailDuplique() throws Exception {
        creerUtilisateur("jean@example.com", "jdupont", Role.AUDITEUR);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(json(new RegisterRequest("Autre", 30, "Femme", "autre",
                                "JEAN@example.com", MOT_DE_PASSE, "auditeur"))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("la connexion retourne un jeton, un mauvais mot de passe retourne 401")
    void connexion() throws Exception {
        creerUtilisateur("jean@example.com", "jdupont", Role.AUDITEUR);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(new LoginRequest("jean@example.com", MOT_DE_PASSE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(new LoginRequest("jean@example.com", "Mauvais1!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou mot de passe invalide"));
    }

    @Test
    @DisplayName("un endpoint protege repond 401 en JSON sans jeton, et avec un jeton invalide")
    void protegeLesEndpoints() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentification requise"));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer pas.un.jeton"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("la disponibilite d'un email est consultable sans etre connecte")
    void verifieLaDisponibiliteDunEmail() throws Exception {
        creerUtilisateur("jean@example.com", "jdupont", Role.AUDITEUR);

        mockMvc.perform(get("/api/auth/email-disponible").param("email", "jean@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

        mockMvc.perform(get("/api/auth/email-disponible").param("email", "libre@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    @Test
    @DisplayName("la mise a jour du profil persiste les changements")
    void metAJourLeProfil() throws Exception {
        User user = creerUtilisateur("jean@example.com", "jdupont", Role.AUDITEUR);

        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", bearer(user))
                        .contentType("application/json")
                        .content(json(new UpdateProfileRequest("Jean D.", "jean.d@example.com", 26,
                                "Homme", "avatar.png", List.of("Pop", "Rock")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Jean D."))
                .andExpect(jsonPath("$.preferences.length()").value(2));

        assertThat(userRepository.findById(user.getId()).orElseThrow().getUserEmail())
                .isEqualTo("jean.d@example.com");
    }
}
