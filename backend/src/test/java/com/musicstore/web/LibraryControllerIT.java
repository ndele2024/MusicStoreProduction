package com.musicstore.web;

import com.musicstore.domain.Role;
import com.musicstore.domain.Titre;
import com.musicstore.domain.User;
import com.musicstore.dto.PlaylistRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Playlists et sauvegardes : contenu correct et cloisonnement entre comptes. */
class LibraryControllerIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("cycle complet d'une sauvegarde : ajout, liste, retrait")
    void cycleDeSauvegarde() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        User jean = creerUtilisateur("jean@example.com", "jean", Role.AUDITEUR);
        Titre titre = creerTitre("Shape of You", 2017, 0, artiste, null);

        mockMvc.perform(post("/api/me/sauvegardes/" + titre.getId()).header("Authorization", bearer(jean)))
                .andExpect(status().isCreated());

        // Rejouer l'ajout ne cree pas de doublon.
        mockMvc.perform(post("/api/me/sauvegardes/" + titre.getId()).header("Authorization", bearer(jean)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/me/sauvegardes").header("Authorization", bearer(jean)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Shape of You"));

        mockMvc.perform(get("/api/me/sauvegardes/ids").header("Authorization", bearer(jean)))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete("/api/me/sauvegardes/" + titre.getId()).header("Authorization", bearer(jean)))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/me/sauvegardes/" + titre.getId()).header("Authorization", bearer(jean)))
                .andExpect(status().isNotFound());

        assertThat(favoriteRepository.count()).isZero();
    }

    @Test
    @DisplayName("les sauvegardes d'un compte sont invisibles pour un autre")
    void sauvegardesCloisonnees() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        User jean = creerUtilisateur("jean@example.com", "jean", Role.AUDITEUR);
        User alice = creerUtilisateur("alice@example.com", "alice", Role.AUDITEUR);
        Titre titre = creerTitre("Shape of You", 2017, 0, artiste, null);

        mockMvc.perform(post("/api/me/sauvegardes/" + titre.getId()).header("Authorization", bearer(jean)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/me/sauvegardes").header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("cycle complet d'une playlist : creation, ajout de titre, retrait, suppression")
    void cycleDePlaylist() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        User jean = creerUtilisateur("jean@example.com", "jean", Role.AUDITEUR);
        Titre titre = creerTitre("Shape of You", 2017, 0, artiste, null);

        String reponse = mockMvc.perform(post("/api/playlists")
                        .header("Authorization", bearer(jean))
                        .contentType("application/json")
                        .content(json(new PlaylistRequest("Route"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Route"))
                .andReturn().getResponse().getContentAsString();
        String playlistId = objectMapper.readTree(reponse).get("id").asText();

        mockMvc.perform(post("/api/playlists/" + playlistId + "/titres/" + titre.getId())
                        .header("Authorization", bearer(jean)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titres.length()").value(1));

        mockMvc.perform(post("/api/playlists/" + playlistId + "/titres/" + titre.getId())
                        .header("Authorization", bearer(jean)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/playlists/" + playlistId)
                        .header("Authorization", bearer(jean))
                        .contentType("application/json")
                        .content(json(new PlaylistRequest("Trajet"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Trajet"));

        mockMvc.perform(delete("/api/playlists/" + playlistId + "/titres/" + titre.getId())
                        .header("Authorization", bearer(jean)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titres.length()").value(0));

        mockMvc.perform(delete("/api/playlists/" + playlistId).header("Authorization", bearer(jean)))
                .andExpect(status().isNoContent());

        assertThat(playlistRepository.count()).isZero();
        // La suppression de la playlist ne touche pas au catalogue.
        assertThat(titreRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("deux playlists du meme nom pour un meme compte sont refusees")
    void nomDePlaylistUnique() throws Exception {
        User jean = creerUtilisateur("jean@example.com", "jean", Role.AUDITEUR);

        mockMvc.perform(post("/api/playlists")
                        .header("Authorization", bearer(jean))
                        .contentType("application/json")
                        .content(json(new PlaylistRequest("Route"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/playlists")
                        .header("Authorization", bearer(jean))
                        .contentType("application/json")
                        .content(json(new PlaylistRequest("route"))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("un autre compte peut reutiliser le meme nom mais ne voit pas la playlist")
    void playlistsCloisonnees() throws Exception {
        User jean = creerUtilisateur("jean@example.com", "jean", Role.AUDITEUR);
        User alice = creerUtilisateur("alice@example.com", "alice", Role.AUDITEUR);

        String reponse = mockMvc.perform(post("/api/playlists")
                        .header("Authorization", bearer(jean))
                        .contentType("application/json")
                        .content(json(new PlaylistRequest("Route"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String playlistId = objectMapper.readTree(reponse).get("id").asText();

        mockMvc.perform(post("/api/playlists")
                        .header("Authorization", bearer(alice))
                        .contentType("application/json")
                        .content(json(new PlaylistRequest("Route"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/playlists").header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/playlists/" + playlistId).header("Authorization", bearer(alice)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/playlists/" + playlistId).header("Authorization", bearer(alice)))
                .andExpect(status().isNotFound());

        assertThat(playlistRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("l'historique en titres ne liste chaque titre qu'une fois, le dernier ecoute en tete")
    void historiqueDedoublonne() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        User jean = creerUtilisateur("jean@example.com", "jean", Role.AUDITEUR);
        Titre premier = creerTitre("Premier", 2020, 0, artiste, null);
        Titre second = creerTitre("Second", 2021, 0, artiste, null);

        for (Titre titre : new Titre[] {premier, premier, second, premier}) {
            mockMvc.perform(post("/api/titres/" + titre.getId() + "/lectures").header("Authorization", bearer(jean)))
                    .andExpect(status().isOk());
            Thread.sleep(5);
        }

        mockMvc.perform(get("/api/me/historique/titres").header("Authorization", bearer(jean)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Premier"))
                .andExpect(jsonPath("$[1].name").value("Second"));
    }

    @Test
    @DisplayName("retirer un titre de l'historique efface ses ecoutes sans toucher aux vues")
    void retireUnTitreDeLHistorique() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        User jean = creerUtilisateur("jean@example.com", "jean", Role.AUDITEUR);
        Titre garde = creerTitre("Garde", 2020, 0, artiste, null);
        Titre retire = creerTitre("Retire", 2021, 0, artiste, null);

        for (Titre titre : new Titre[] {retire, garde, retire}) {
            mockMvc.perform(post("/api/titres/" + titre.getId() + "/lectures").header("Authorization", bearer(jean)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(delete("/api/me/historique/titres/" + retire.getId()).header("Authorization", bearer(jean)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/me/historique/titres").header("Authorization", bearer(jean)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Garde"));
        assertThat(titreRepository.findById(retire.getId()).orElseThrow().getVues()).isEqualTo(2L);

        mockMvc.perform(delete("/api/me/historique/titres/" + retire.getId()).header("Authorization", bearer(jean)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("vider l'historique et les sauvegardes ne concerne que le compte connecte")
    void videHistoriqueEtSauvegardes() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        User jean = creerUtilisateur("jean@example.com", "jean", Role.AUDITEUR);
        User alice = creerUtilisateur("alice@example.com", "alice", Role.AUDITEUR);
        Titre titre = creerTitre("Titre", 2020, 0, artiste, null);

        for (User user : new User[] {jean, alice}) {
            mockMvc.perform(post("/api/titres/" + titre.getId() + "/lectures").header("Authorization", bearer(user)))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/me/sauvegardes/" + titre.getId()).header("Authorization", bearer(user)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(delete("/api/me/historique").header("Authorization", bearer(jean)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/me/sauvegardes").header("Authorization", bearer(jean)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/me/historique/titres").header("Authorization", bearer(jean)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/me/sauvegardes").header("Authorization", bearer(jean)))
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/me/historique/titres").header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/me/sauvegardes").header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$.length()").value(1));

        // Vider un historique deja vide reste sans erreur.
        mockMvc.perform(delete("/api/me/historique").header("Authorization", bearer(jean)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("la bibliotheque est inaccessible sans jeton")
    void bibliothequeProtegee() throws Exception {
        mockMvc.perform(get("/api/me/sauvegardes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me/historique")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/playlists")).andExpect(status().isUnauthorized());
    }
}
