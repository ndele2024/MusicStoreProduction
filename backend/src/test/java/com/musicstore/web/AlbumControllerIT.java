package com.musicstore.web;

import com.musicstore.domain.Album;
import com.musicstore.domain.Role;
import com.musicstore.domain.Titre;
import com.musicstore.domain.User;
import com.musicstore.dto.AlbumRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AlbumControllerIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("la liste des albums porte les identifiants de ses titres")
    void listeDesAlbums() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        Album divide = creerAlbum("Divide", 2017, artiste);
        creerTitre("Shape of You", 2017, 0, artiste, divide);
        creerTitre("Perfect", 2017, 0, artiste, divide);

        mockMvc.perform(get("/api/albums"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Divide"))
                .andExpect(jsonPath("$[0].artisteNom").value("ed"))
                .andExpect(jsonPath("$[0].titres.length()").value(2));
    }

    @Test
    @DisplayName("un album ne compte et ne liste que ses titres lisibles, et disparait s'il n'en a aucun")
    void masqueLesTitresEtAlbumsSansMedia() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        Album divide = creerAlbum("Divide", 2017, artiste);
        creerTitre("Shape of You", 2017, 0, artiste, divide);
        creerTitreSansMedia("Perfect", 2017, 0, artiste, divide);
        Album vide = creerAlbum("Brouillon", 2025, artiste);
        creerTitreSansMedia("Maquette", 2025, 0, artiste, vide);

        mockMvc.perform(get("/api/albums"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Divide"))
                .andExpect(jsonPath("$[0].titres.length()").value(1));

        mockMvc.perform(get("/api/albums/" + divide.getId() + "/titres"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Shape of You"));
    }

    @Test
    @DisplayName("l'artiste voit ses albums encore vides, les visiteurs seulement les albums publies")
    void albumsDunArtiste() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        User auditeur = creerUtilisateur("jean@example.com", "jean", Role.AUDITEUR);
        Album publie = creerAlbum("Divide", 2017, artiste);
        creerTitre("Shape of You", 2017, 0, artiste, publie);
        creerAlbum("Brouillon", 2026, artiste);

        mockMvc.perform(get("/api/albums/artiste/" + artiste.getId()).header("Authorization", bearer(artiste)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/albums/artiste/" + artiste.getId()).header("Authorization", bearer(auditeur)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Divide"));
    }

    @Test
    @DisplayName("les titres d'un album sont listes avec leur detail")
    void titresDunAlbum() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        Album divide = creerAlbum("Divide", 2017, artiste);
        creerTitre("Shape of You", 2017, 12, artiste, divide);

        mockMvc.perform(get("/api/albums/" + divide.getId() + "/titres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Shape of You"))
                .andExpect(jsonPath("$[0].vues").value(12));
    }

    @Test
    @DisplayName("un artiste cree un album, un auditeur recoit 403")
    void creationReserveeAuxArtistes() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        User auditeur = creerUtilisateur("jean@example.com", "jean", Role.AUDITEUR);

        mockMvc.perform(post("/api/albums")
                        .header("Authorization", bearer(artiste))
                        .contentType("application/json")
                        .content(json(new AlbumRequest("Divide", 2017))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Divide"));

        mockMvc.perform(post("/api/albums")
                        .header("Authorization", bearer(auditeur))
                        .contentType("application/json")
                        .content(json(new AlbumRequest("Pirate", 2017))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("supprimer un album conserve ses titres, qui se retrouvent sans album")
    void suppressionConserveLesTitres() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);
        Album divide = creerAlbum("Divide", 2017, artiste);
        Titre titre = creerTitre("Shape of You", 2017, 0, artiste, divide);

        mockMvc.perform(delete("/api/albums/" + divide.getId()).header("Authorization", bearer(artiste)))
                .andExpect(status().isNoContent());

        assertThat(albumRepository.count()).isZero();
        assertThat(titreRepository.findById(titre.getId()).orElseThrow().getAlbum()).isNull();
    }

    @Test
    @DisplayName("une annee hors bornes est rejetee par la validation")
    void valideLannee() throws Exception {
        User artiste = creerUtilisateur("ed@example.com", "ed", Role.ARTISTE);

        mockMvc.perform(post("/api/albums")
                        .header("Authorization", bearer(artiste))
                        .contentType("application/json")
                        .content(json(new AlbumRequest("Trop vieux", 1200))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.annee").exists());
    }
}
