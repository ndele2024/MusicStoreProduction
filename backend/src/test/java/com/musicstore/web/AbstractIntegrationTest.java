package com.musicstore.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicstore.domain.Album;
import com.musicstore.domain.MediaType;
import com.musicstore.domain.Role;
import com.musicstore.domain.Titre;
import com.musicstore.domain.User;
import com.musicstore.repository.AlbumRepository;
import com.musicstore.repository.FavoriteRepository;
import com.musicstore.repository.PlayHistoryRepository;
import com.musicstore.repository.PlaylistRepository;
import com.musicstore.repository.RatingRepository;
import com.musicstore.repository.TitreRepository;
import com.musicstore.repository.UserRepository;
import com.musicstore.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

/**
 * Socle des tests d'intégration : contexte Spring complet, base H2 migrée par Flyway
 * et tables vidées avant chaque test pour que l'ordre d'exécution n'ait aucun effet.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    protected static final String MOT_DE_PASSE = "Password1!";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected TitreRepository titreRepository;

    @Autowired
    protected AlbumRepository albumRepository;

    @Autowired
    protected PlaylistRepository playlistRepository;

    @Autowired
    protected FavoriteRepository favoriteRepository;

    @Autowired
    protected RatingRepository ratingRepository;

    @Autowired
    protected PlayHistoryRepository playHistoryRepository;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void viderLaBase() {
        playHistoryRepository.deleteAll();
        ratingRepository.deleteAll();
        favoriteRepository.deleteAll();
        playlistRepository.deleteAll();
        titreRepository.deleteAll();
        albumRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected User creerUtilisateur(String email, String userName, Role role) {
        return userRepository.save(User.builder()
                .fullName(userName)
                .age(30)
                .sex("Non defini")
                .userName(userName)
                .userEmail(email)
                .password(passwordEncoder.encode(MOT_DE_PASSE))
                .role(role)
                .avatar("")
                .preferences(new ArrayList<>())
                .build());
    }

    protected Album creerAlbum(String nom, int annee, User artiste) {
        return albumRepository.save(Album.builder().name(nom).annee(annee).artist(artiste).build());
    }

    /** Titre publié : une clé de stockage est posée pour qu'il apparaisse dans le catalogue. */
    protected Titre creerTitre(String nom, int annee, long vues, User artiste, Album album) {
        Titre titre = creerTitreSansMedia(nom, annee, vues, artiste, album);
        titre.setStorageKey("fixture-" + titre.getId() + ".mp3");
        titre.setContentType("audio/mpeg");
        return titreRepository.save(titre);
    }

    /** Titre créé par l'artiste mais dont le fichier n'a pas encore été déposé. */
    protected Titre creerTitreSansMedia(String nom, int annee, long vues, User artiste, Album album) {
        return titreRepository.save(Titre.builder()
                .name(nom)
                .description("Description de " + nom)
                .genre("Pop")
                .annee(annee)
                .mediaType(MediaType.AUDIO)
                .vues(vues)
                .sommeNotes(0L)
                .nombreNotes(0)
                .artist(artiste)
                .album(album)
                .build());
    }

    /** En-tête d'autorisation prêt à l'emploi pour l'utilisateur donné. */
    protected String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
