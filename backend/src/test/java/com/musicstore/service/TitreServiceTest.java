package com.musicstore.service;

import com.musicstore.domain.MediaType;
import com.musicstore.domain.Rating;
import com.musicstore.domain.Role;
import com.musicstore.domain.Titre;
import com.musicstore.domain.User;
import com.musicstore.dto.RatingRequest;
import com.musicstore.dto.TitreDto;
import com.musicstore.dto.TitreRequest;
import com.musicstore.exception.ForbiddenException;
import com.musicstore.exception.NotFoundException;
import com.musicstore.mapper.TitreMapper;
import com.musicstore.repository.AlbumRepository;
import com.musicstore.repository.PlayHistoryRepository;
import com.musicstore.repository.RatingRepository;
import com.musicstore.repository.TitreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TitreServiceTest {

    @Mock
    private TitreRepository titreRepository;

    @Mock
    private AlbumRepository albumRepository;

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private PlayHistoryRepository playHistoryRepository;

    @Mock
    private StorageService storageService;

    @Spy
    private TitreMapper titreMapper = new TitreMapper();

    @InjectMocks
    private TitreService titreService;

    private User artiste;
    private User auditeur;
    private Titre titre;

    @BeforeEach
    void setUp() {
        artiste = User.builder().id(UUID.randomUUID()).fullName("Ed Sheeran").userName("ed")
                .userEmail("ed@example.com").password("h").role(Role.ARTISTE).build();
        auditeur = User.builder().id(UUID.randomUUID()).fullName("Jean").userName("jean")
                .userEmail("jean@example.com").password("h").role(Role.AUDITEUR).build();
        titre = Titre.builder()
                .id(UUID.randomUUID())
                .name("Shape of You")
                .description("Hit")
                .genre("Pop")
                .annee(2017)
                .mediaType(MediaType.AUDIO)
                .vues(10L)
                .sommeNotes(8L)
                .nombreNotes(2)
                .artist(artiste)
                .build();
    }

    @Test
    @DisplayName("une premiere note ajoute la valeur et incremente le nombre de votes")
    void enregistreUnePremiereNote() {
        when(titreRepository.findById(titre.getId())).thenReturn(Optional.of(titre));
        when(ratingRepository.findByUserIdAndTitreId(auditeur.getId(), titre.getId())).thenReturn(Optional.empty());
        when(titreRepository.save(any(Titre.class))).thenAnswer(i -> i.getArgument(0));

        TitreDto dto = titreService.noter(titre.getId(), new RatingRequest(5), auditeur);

        verify(ratingRepository).save(any(Rating.class));
        assertThat(titre.getSommeNotes()).isEqualTo(13L);
        assertThat(titre.getNombreNotes()).isEqualTo(3);
        assertThat(dto.noteMoyenne()).isEqualTo(4.33);
        assertThat(dto.maNote()).isEqualTo(5);
    }

    @Test
    @DisplayName("renoter remplace l'ancienne valeur sans compter un vote supplementaire")
    void metAJourUneNoteExistante() {
        Rating existante = Rating.builder().user(auditeur).titre(titre).valeur(3).build();
        when(titreRepository.findById(titre.getId())).thenReturn(Optional.of(titre));
        when(ratingRepository.findByUserIdAndTitreId(auditeur.getId(), titre.getId())).thenReturn(Optional.of(existante));
        when(titreRepository.save(any(Titre.class))).thenAnswer(i -> i.getArgument(0));

        TitreDto dto = titreService.noter(titre.getId(), new RatingRequest(5), auditeur);

        assertThat(titre.getSommeNotes()).isEqualTo(10L);
        assertThat(titre.getNombreNotes()).isEqualTo(2);
        assertThat(existante.getValeur()).isEqualTo(5);
        assertThat(dto.noteMoyenne()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("enregistrer une lecture incremente les vues et ecrit une ligne d'historique")
    void comptabiliseUneLecture() {
        when(titreRepository.findById(titre.getId())).thenReturn(Optional.of(titre));
        when(ratingRepository.findByUserIdAndTitreId(auditeur.getId(), titre.getId())).thenReturn(Optional.empty());

        TitreDto dto = titreService.enregistrerLecture(titre.getId(), auditeur);

        verify(titreRepository).incrementVues(titre.getId());
        verify(playHistoryRepository).save(any());
        assertThat(dto.vues()).isEqualTo(11L);
    }

    @Test
    @DisplayName("un artiste ne peut pas modifier le titre d'un autre artiste")
    void refuseLaModificationParUnTiers() {
        User autre = User.builder().id(UUID.randomUUID()).fullName("Autre").userName("autre")
                .userEmail("autre@example.com").password("h").role(Role.ARTISTE).build();
        when(titreRepository.findById(titre.getId())).thenReturn(Optional.of(titre));

        TitreRequest request = new TitreRequest("Nouveau", "d", "Pop", 2020, "audio", null);

        assertThatThrownBy(() -> titreService.update(titre.getId(), request, autre))
                .isInstanceOf(ForbiddenException.class);
        verify(titreRepository, never()).save(any());
    }

    @Test
    @DisplayName("un administrateur peut supprimer le titre d'un artiste et son fichier")
    void autoriseLaSuppressionParUnAdministrateur() {
        titre.setStorageKey("fichier.mp3");
        User admin = User.builder().id(UUID.randomUUID()).fullName("Admin").userName("admin")
                .userEmail("admin@example.com").password("h").role(Role.ADMIN).build();
        when(titreRepository.findById(titre.getId())).thenReturn(Optional.of(titre));

        titreService.delete(titre.getId(), admin);

        verify(storageService).delete("fichier.mp3");
        verify(titreRepository).delete(titre);
    }

    @Test
    @DisplayName("un titre inexistant remonte une erreur 404")
    void signaleUnTitreInexistant() {
        UUID id = UUID.randomUUID();
        when(titreRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> titreService.get(id, null)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("la creation associe le titre a son auteur et retourne un media absent")
    void creeUnTitreSansMedia() {
        when(titreRepository.save(any(Titre.class))).thenAnswer(i -> {
            Titre t = i.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TitreDto dto = titreService.create(new TitreRequest("Perfect", "Ballade", "Pop", 2017, "audio", null), artiste);

        assertThat(dto.artisteId()).isEqualTo(artiste.getId());
        assertThat(dto.mediaUrl()).isNull();
        assertThat(dto.mediaType()).isEqualTo("audio");
        assertThat(dto.nombreNotes()).isZero();
    }

    @Test
    @DisplayName("un titre jamais note affiche une moyenne de zero plutot qu'une division par zero")
    void toleredesTitresJamaisNotes() {
        Titre vierge = Titre.builder().id(UUID.randomUUID()).name("X").annee(2024)
                .mediaType(MediaType.AUDIO).vues(0L).sommeNotes(0L).nombreNotes(0).build();

        assertThat(vierge.getNoteMoyenne()).isZero();
    }
}
