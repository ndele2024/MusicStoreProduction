package com.musicstore.config;

import com.musicstore.domain.Album;
import com.musicstore.domain.MediaType;
import com.musicstore.domain.Role;
import com.musicstore.domain.Titre;
import com.musicstore.domain.User;
import com.musicstore.repository.AlbumRepository;
import com.musicstore.repository.TitreRepository;
import com.musicstore.repository.UserRepository;
import com.musicstore.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Jeu de données de démonstration, repris du fichier {@code db.json} du frontend.
 *
 * <p>Ne s'exécute que sur une base vide : relancer l'application ne duplique rien et n'écrase
 * aucune donnée saisie depuis l'interface.
 */
@Configuration
@ConditionalOnProperty(prefix = "musicstore.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String MOT_DE_PASSE_DEMO = "Password1!";

    @Bean
    public ApplicationRunner seedDatabase(UserRepository userRepository,
                                          AlbumRepository albumRepository,
                                          TitreRepository titreRepository,
                                          StorageService storageService,
                                          PasswordEncoder passwordEncoder) {
        return args -> seed(userRepository, albumRepository, titreRepository, storageService, passwordEncoder);
    }

    @Transactional
    void seed(UserRepository userRepository,
              AlbumRepository albumRepository,
              TitreRepository titreRepository,
              StorageService storageService,
              PasswordEncoder passwordEncoder) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("Base vide : insertion du jeu de donnees de demonstration");

        User admin = userRepository.save(utilisateur(passwordEncoder, "Admin MusicStore", 30, "Non defini",
                "admin", "admin@musicstore.local", Role.ADMIN));
        User edSheeran = userRepository.save(utilisateur(passwordEncoder, "Ed Sheeran", 32, "Homme",
                "edsheeran", "ed@example.com", Role.ARTISTE));
        userRepository.save(utilisateur(passwordEncoder, "The Weeknd", 34, "Homme",
                "theweeknd", "weeknd@example.com", Role.ARTISTE));
        userRepository.save(utilisateur(passwordEncoder, "Jean Dupont", 25, "Homme",
                "jdupont", "jean@example.com", Role.AUDITEUR));
        userRepository.save(utilisateur(passwordEncoder, "Alice Martin", 22, "Femme",
                "aliceM", "alice@example.com", Role.AUDITEUR));

        Album divide = albumRepository.save(Album.builder().name("Divide").annee(2017).artist(edSheeran).build());

        // Seuls les titres accompagnes d'un fichier sont inseres : le catalogue n'affiche pas les autres.
        // Des titres supplementaires se deposent via l'API (voir scripts/seed-demo-media.py).
        String cleDemo = storageService.storeClasspathResource("seed-media/shape_of_you.mp3", "demo-shape-of-you.mp3");

        List<Titre> titres = new ArrayList<>();
        titres.add(Titre.builder()
                .name("Shape of You")
                .description("Hit song by Ed Sheeran")
                .genre("Pop")
                .annee(2017)
                .nomFichier("shape_of_you.mp3")
                .nomImage("shape_of_you.jpg")
                .contentType("audio/mpeg")
                .storageKey(cleDemo)
                .mediaType(MediaType.AUDIO)
                .vues(1_500_000L)
                .sommeNotes(17L)
                .nombreNotes(5)
                .artist(edSheeran)
                .album(divide)
                .build());
        titreRepository.saveAll(titres);

        log.info("Jeu de demonstration insere : {} utilisateurs, {} albums, {} titres. Mot de passe commun : {}",
                userRepository.count(), albumRepository.count(), titreRepository.count(), MOT_DE_PASSE_DEMO);
        log.info("Compte administrateur : {}", admin.getUserEmail());
    }

    private static User utilisateur(PasswordEncoder encoder,
                                    String fullName,
                                    int age,
                                    String sex,
                                    String userName,
                                    String email,
                                    Role role) {
        return User.builder()
                .fullName(fullName)
                .age(age)
                .sex(sex)
                .userName(userName)
                .userEmail(email)
                .password(encoder.encode(MOT_DE_PASSE_DEMO))
                .role(role)
                .avatar("")
                .preferences(new ArrayList<>())
                .build();
    }
}
