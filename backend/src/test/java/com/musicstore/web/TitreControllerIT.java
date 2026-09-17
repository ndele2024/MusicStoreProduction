package com.musicstore.web;

import com.musicstore.domain.Album;
import com.musicstore.domain.Role;
import com.musicstore.domain.Titre;
import com.musicstore.domain.User;
import com.musicstore.dto.RatingRequest;
import com.musicstore.dto.TitreRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TitreControllerIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("le catalogue est consultable sans compte")
    void catalogueLibre() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        creerTitre("Shape of You", 2017, 1500, artiste, null);
        creerTitre("Blinding Lights", 2019, 1800, artiste, null);

        mockMvc.perform(get("/api/titres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("un titre sans fichier depose n'apparait ni dans le catalogue ni dans la recherche")
    void masqueLesTitresSansMedia() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        creerTitre("Publie", 2020, 10, artiste, null);
        creerTitreSansMedia("En attente", 2024, 999, artiste, null);

        mockMvc.perform(get("/api/titres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Publie"));

        mockMvc.perform(get("/api/titres").param("sort", "populaire"))
                .andExpect(jsonPath("$.content[0].name").value("Publie"));

        mockMvc.perform(get("/api/titres").param("q", "attente"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("l'artiste voit ses titres en attente de fichier, les autres visiteurs non")
    void titresEnAttenteVisiblesParLeurAuteur() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        User auditeur = creerUtilisateur("jean@example.com", "jean", Role.AUDITEUR);
        creerTitre("Publie", 2020, 0, artiste, null);
        creerTitreSansMedia("En attente", 2024, 0, artiste, null);

        mockMvc.perform(get("/api/titres/artiste/" + artiste.getId()).header("Authorization", bearer(artiste)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/titres/artiste/" + artiste.getId()).header("Authorization", bearer(auditeur)))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/titres/artiste/" + artiste.getId()))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("la recherche filtre sur le nom, le genre, la description et l'annee")
    void recherche() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        creerTitre("Shape of You", 2017, 10, artiste, null);
        creerTitre("Blinding Lights", 2019, 20, artiste, null);

        mockMvc.perform(get("/api/titres").param("q", "shape"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Shape of You"));

        mockMvc.perform(get("/api/titres").param("q", "2019"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Blinding Lights"));

        mockMvc.perform(get("/api/titres").param("q", "introuvable"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("le tri par popularite place le titre le plus vu en tete")
    void triParPopularite() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        creerTitre("Peu vu", 2020, 5, artiste, null);
        creerTitre("Tres vu", 2015, 900, artiste, null);

        mockMvc.perform(get("/api/titres").param("sort", "populaire"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Tres vu"));

        mockMvc.perform(get("/api/titres").param("sort", "recent"))
                .andExpect(jsonPath("$.content[0].name").value("Peu vu"));
    }

    @Test
    @DisplayName("un artiste publie un titre, un auditeur ne le peut pas")
    void publicationReserveeAuxArtistes() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        User auditeur = creerUtilisateur("jean@example.com", "jean", Role.AUDITEUR);
        Album album = creerAlbum("Divide", 2017, artiste);

        mockMvc.perform(post("/api/titres")
                        .header("Authorization", bearer(artiste))
                        .contentType("application/json")
                        .content(json(new TitreRequest("Perfect", "Ballade", "Pop", 2017, "audio", album.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Perfect"))
                .andExpect(jsonPath("$.albumNom").value("Divide"));

        mockMvc.perform(post("/api/titres")
                        .header("Authorization", bearer(auditeur))
                        .contentType("application/json")
                        .content(json(new TitreRequest("Pirate", "x", "Pop", 2017, "audio", null))))
                .andExpect(status().isForbidden());

        assertThat(titreRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("un artiste ne peut ni modifier ni supprimer le titre d'un autre")
    void isolationEntreArtistes() throws Exception {
        User ed = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        User weeknd = creerUtilisateur("weeknd@example.com", "weeknd", Role.ARTISTE);
        Titre titre = creerTitre("Shape of You", 2017, 0, ed, null);

        mockMvc.perform(put("/api/titres/" + titre.getId())
                        .header("Authorization", bearer(weeknd))
                        .contentType("application/json")
                        .content(json(new TitreRequest("Detourne", "x", "Pop", 2017, "audio", null))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/titres/" + titre.getId())
                        .header("Authorization", bearer(weeknd)))
                .andExpect(status().isForbidden());

        assertThat(titreRepository.findById(titre.getId()).orElseThrow().getName()).isEqualTo("Shape of You");
    }

    @Test
    @DisplayName("le media depose est diffuse en entier puis par plage d'octets")
    void depotEtDiffusionDuMedia() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        User auditeur = creerUtilisateur("jean@example.com", "jean", Role.AUDITEUR);
        Titre titre = creerTitre("Shape of You", 2017, 0, artiste, null);

        byte[] contenu = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);
        mockMvc.perform(multipart("/api/titres/" + titre.getId() + "/media")
                        .file(new MockMultipartFile("file", "piste.mp3", "audio/mpeg", contenu))
                        .header("Authorization", bearer(artiste)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaUrl").value("/api/media/" + titre.getId()))
                .andExpect(jsonPath("$.mediaType").value("audio"));

        mockMvc.perform(get("/api/media/" + titre.getId()).header("Authorization", bearer(auditeur)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "audio/mpeg"));

        mockMvc.perform(get("/api/media/" + titre.getId())
                        .header("Authorization", bearer(auditeur))
                        .header(HttpHeaders.RANGE, "bytes=4-9"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 4-9/16"));

        // Le streaming reste ferme aux visiteurs anonymes.
        mockMvc.perform(get("/api/media/" + titre.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un fichier d'extension interdite est refuse")
    void refuseUnMediaNonSupporte() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        Titre titre = creerTitre("Shape of You", 2017, 0, artiste, null);

        mockMvc.perform(multipart("/api/titres/" + titre.getId() + "/media")
                        .file(new MockMultipartFile("file", "malware.exe", "application/octet-stream", new byte[]{1, 2}))
                        .header("Authorization", bearer(artiste)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("une lecture incremente les vues et alimente l'historique")
    void comptabiliseUneLecture() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        User auditeur = creerUtilisateur("jean@example.com", "jean", Role.AUDITEUR);
        Titre titre = creerTitre("Shape of You", 2017, 41, artiste, null);

        mockMvc.perform(post("/api/titres/" + titre.getId() + "/lectures")
                        .header("Authorization", bearer(auditeur)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vues").value(42));

        mockMvc.perform(get("/api/me/historique").header("Authorization", bearer(auditeur)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titre.name").value("Shape of You"));

        assertThat(titreRepository.findById(titre.getId()).orElseThrow().getVues()).isEqualTo(42L);

        // Sans compte, la lecture n'est pas comptabilisee.
        mockMvc.perform(post("/api/titres/" + titre.getId() + "/lectures"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("deux notes donnent la moyenne, renoter ne compte pas deux fois")
    void moyenneDesNotes() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        User jean = creerUtilisateur("jean@example.com", "jean", Role.AUDITEUR);
        User alice = creerUtilisateur("alice@example.com", "alice", Role.AUDITEUR);
        Titre titre = creerTitre("Shape of You", 2017, 0, artiste, null);

        mockMvc.perform(put("/api/titres/" + titre.getId() + "/note")
                        .header("Authorization", bearer(jean))
                        .contentType("application/json")
                        .content(json(new RatingRequest(4))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreNotes").value(1))
                .andExpect(jsonPath("$.noteMoyenne").value(4.0));

        mockMvc.perform(put("/api/titres/" + titre.getId() + "/note")
                        .header("Authorization", bearer(alice))
                        .contentType("application/json")
                        .content(json(new RatingRequest(2))))
                .andExpect(jsonPath("$.nombreNotes").value(2))
                .andExpect(jsonPath("$.noteMoyenne").value(3.0));

        mockMvc.perform(put("/api/titres/" + titre.getId() + "/note")
                        .header("Authorization", bearer(alice))
                        .contentType("application/json")
                        .content(json(new RatingRequest(5))))
                .andExpect(jsonPath("$.nombreNotes").value(2))
                .andExpect(jsonPath("$.noteMoyenne").value(4.5))
                .andExpect(jsonPath("$.maNote").value(5));

        // La note personnelle suit l'utilisateur qui consulte le titre.
        mockMvc.perform(get("/api/titres/" + titre.getId()).header("Authorization", bearer(jean)))
                .andExpect(jsonPath("$.maNote").value(4));
        mockMvc.perform(get("/api/titres/" + titre.getId()))
                .andExpect(jsonPath("$.maNote").doesNotExist());
    }

    @Test
    @DisplayName("une note hors de l'intervalle 1-5 est rejetee")
    void refuseUneNoteInvalide() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        User jean = creerUtilisateur("jean@example.com", "jean", Role.AUDITEUR);
        Titre titre = creerTitre("Shape of You", 2017, 0, artiste, null);

        mockMvc.perform(put("/api/titres/" + titre.getId() + "/note")
                        .header("Authorization", bearer(jean))
                        .contentType("application/json")
                        .content(json(new RatingRequest(9))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.valeur").exists());
    }

    @Test
    @DisplayName("un identifiant inconnu remonte une erreur 404 structuree")
    void titreInconnu() throws Exception {
        mockMvc.perform(get("/api/titres/" + java.util.UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").exists());
    }
}
