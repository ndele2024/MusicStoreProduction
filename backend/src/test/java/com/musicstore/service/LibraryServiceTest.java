package com.musicstore.service;

import com.musicstore.domain.Favorite;
import com.musicstore.domain.MediaType;
import com.musicstore.domain.Role;
import com.musicstore.domain.Titre;
import com.musicstore.domain.User;
import com.musicstore.exception.NotFoundException;
import com.musicstore.mapper.TitreMapper;
import com.musicstore.repository.FavoriteRepository;
import com.musicstore.repository.PlayHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private PlayHistoryRepository playHistoryRepository;

    @Mock
    private com.musicstore.repository.TitreRepository titreRepository;

    @Mock
    private TitreService titreService;

    @Spy
    private TitreMapper titreMapper = new TitreMapper();

    @InjectMocks
    private LibraryService libraryService;

    private User user;
    private Titre titre;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).fullName("Jean").userName("jean")
                .userEmail("jean@example.com").password("h").role(Role.AUDITEUR).build();
        titre = Titre.builder().id(UUID.randomUUID()).name("Shape of You").annee(2017)
                .mediaType(MediaType.AUDIO).vues(3L).sommeNotes(0L).nombreNotes(0).build();
    }

    @Test
    @DisplayName("sauvegarder un titre deja sauvegarde ne cree pas de doublon")
    void sauvegardeIdempotente() {
        when(titreService.requireTitre(titre.getId())).thenReturn(titre);
        when(favoriteRepository.existsByUserIdAndTitreId(user.getId(), titre.getId())).thenReturn(true);

        libraryService.ajouterSauvegarde(user, titre.getId());

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("un titre non encore sauvegarde est ajoute")
    void ajouteUneSauvegarde() {
        when(titreService.requireTitre(titre.getId())).thenReturn(titre);
        when(favoriteRepository.existsByUserIdAndTitreId(user.getId(), titre.getId())).thenReturn(false);

        var dto = libraryService.ajouterSauvegarde(user, titre.getId());

        verify(favoriteRepository).save(any(Favorite.class));
        assertThat(dto.id()).isEqualTo(titre.getId());
    }

    @Test
    @DisplayName("retirer une sauvegarde absente remonte une erreur 404")
    void refuseDeRetirerUneSauvegardeAbsente() {
        when(favoriteRepository.deleteByUserIdAndTitreId(user.getId(), titre.getId())).thenReturn(0L);

        assertThatThrownBy(() -> libraryService.retirerSauvegarde(user, titre.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("la liste des sauvegardes reprend les titres, les plus recents d'abord")
    void listeLesSauvegardes() {
        Favorite favori = Favorite.builder().user(user).titre(titre).build();
        when(favoriteRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(favori));
        when(titreService.notesUtilisateur(any(), any())).thenReturn(Map.of());

        assertThat(libraryService.sauvegardes(user))
                .extracting(t -> t.name())
                .containsExactly("Shape of You");
    }
}
