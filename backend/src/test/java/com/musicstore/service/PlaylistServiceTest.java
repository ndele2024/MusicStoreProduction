package com.musicstore.service;

import com.musicstore.domain.MediaType;
import com.musicstore.domain.Playlist;
import com.musicstore.domain.Role;
import com.musicstore.domain.Titre;
import com.musicstore.domain.User;
import com.musicstore.dto.PlaylistDto;
import com.musicstore.dto.PlaylistRequest;
import com.musicstore.exception.BadRequestException;
import com.musicstore.exception.ConflictException;
import com.musicstore.exception.NotFoundException;
import com.musicstore.mapper.TitreMapper;
import com.musicstore.repository.PlaylistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private TitreService titreService;

    @Spy
    private TitreMapper titreMapper = new TitreMapper();

    @InjectMocks
    private PlaylistService playlistService;

    private User owner;
    private Playlist playlist;
    private Titre titre;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(UUID.randomUUID()).fullName("Jean").userName("jean")
                .userEmail("jean@example.com").password("h").role(Role.AUDITEUR).build();
        titre = Titre.builder().id(UUID.randomUUID()).name("Shape of You").annee(2017)
                .mediaType(MediaType.AUDIO).vues(0L).sommeNotes(0L).nombreNotes(0).build();
        playlist = Playlist.builder()
                .id(UUID.randomUUID())
                .nom("Route")
                .owner(owner)
                .titres(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("la creation refuse un nom deja utilise par le meme proprietaire")
    void refuseUnNomDuplique() {
        when(playlistRepository.existsByOwnerIdAndNomIgnoreCase(owner.getId(), "Route")).thenReturn(true);

        assertThatThrownBy(() -> playlistService.create(new PlaylistRequest("  Route  "), owner))
                .isInstanceOf(ConflictException.class);
        verify(playlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("le nom est detoure des espaces avant enregistrement")
    void nettoieLeNom() {
        when(playlistRepository.existsByOwnerIdAndNomIgnoreCase(owner.getId(), "Route")).thenReturn(false);
        when(playlistRepository.save(any(Playlist.class))).thenAnswer(i -> {
            Playlist p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PlaylistDto dto = playlistService.create(new PlaylistRequest("  Route  "), owner);

        assertThat(dto.nom()).isEqualTo("Route");
        assertThat(dto.titres()).isEmpty();
    }

    @Test
    @DisplayName("ajouter deux fois le meme titre est refuse")
    void refuseUnTitreDejaPresent() {
        playlist.getTitres().add(titre);
        when(playlistRepository.findByIdAndOwnerId(playlist.getId(), owner.getId())).thenReturn(Optional.of(playlist));

        assertThatThrownBy(() -> playlistService.addTitre(playlist.getId(), titre.getId(), owner))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("un titre ajoute apparait dans la playlist retournee")
    void ajouteUnTitre() {
        when(playlistRepository.findByIdAndOwnerId(playlist.getId(), owner.getId())).thenReturn(Optional.of(playlist));
        when(titreService.requireTitre(titre.getId())).thenReturn(titre);
        when(titreService.notesUtilisateur(any(), any())).thenReturn(java.util.Map.of());
        when(playlistRepository.save(playlist)).thenReturn(playlist);

        PlaylistDto dto = playlistService.addTitre(playlist.getId(), titre.getId(), owner);

        assertThat(dto.titres()).extracting(t -> t.name()).containsExactly("Shape of You");
    }

    @Test
    @DisplayName("retirer un titre absent remonte une erreur 404")
    void refuseDeRetirerUnTitreAbsent() {
        when(playlistRepository.findByIdAndOwnerId(playlist.getId(), owner.getId())).thenReturn(Optional.of(playlist));

        assertThatThrownBy(() -> playlistService.removeTitre(playlist.getId(), titre.getId(), owner))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("une playlist appartenant a un autre utilisateur est invisible")
    void isoleLesPlaylistsParProprietaire() {
        User autre = User.builder().id(UUID.randomUUID()).fullName("Alice").userName("alice")
                .userEmail("alice@example.com").password("h").role(Role.AUDITEUR).build();
        when(playlistRepository.findByIdAndOwnerId(playlist.getId(), autre.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.get(playlist.getId(), autre))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("renommer vers un nom deja pris est refuse, garder le sien est accepte")
    void gereLeRenommage() {
        when(playlistRepository.findByIdAndOwnerId(playlist.getId(), owner.getId())).thenReturn(Optional.of(playlist));
        when(playlistRepository.existsByOwnerIdAndNomIgnoreCase(owner.getId(), "Sport")).thenReturn(true);

        assertThatThrownBy(() -> playlistService.rename(playlist.getId(), new PlaylistRequest("Sport"), owner))
                .isInstanceOf(ConflictException.class);

        when(titreService.notesUtilisateur(any(), any())).thenReturn(java.util.Map.of());
        when(playlistRepository.save(playlist)).thenReturn(playlist);
        assertThat(playlistService.rename(playlist.getId(), new PlaylistRequest("route"), owner).nom())
                .isEqualTo("route");
    }

    @Test
    @DisplayName("la liste personnelle ne remonte que les playlists du proprietaire")
    void listeSesPlaylists() {
        when(playlistRepository.findAllByOwnerIdOrderByCreatedAtAsc(owner.getId())).thenReturn(List.of(playlist));
        when(titreService.notesUtilisateur(any(), any())).thenReturn(java.util.Map.of());

        assertThat(playlistService.findMine(owner)).hasSize(1);
    }
}
