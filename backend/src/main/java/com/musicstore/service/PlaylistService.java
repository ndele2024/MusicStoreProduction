package com.musicstore.service;

import com.musicstore.domain.Playlist;
import com.musicstore.domain.Titre;
import com.musicstore.domain.User;
import com.musicstore.dto.PlaylistDto;
import com.musicstore.dto.PlaylistRequest;
import com.musicstore.exception.BadRequestException;
import com.musicstore.exception.ConflictException;
import com.musicstore.exception.NotFoundException;
import com.musicstore.mapper.TitreMapper;
import com.musicstore.repository.PlaylistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Playlists personnelles : toutes les opérations sont bornées au propriétaire connecté. */
@Service
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final TitreService titreService;
    private final TitreMapper titreMapper;

    public PlaylistService(PlaylistRepository playlistRepository,
                           TitreService titreService,
                           TitreMapper titreMapper) {
        this.playlistRepository = playlistRepository;
        this.titreService = titreService;
        this.titreMapper = titreMapper;
    }

    @Transactional(readOnly = true)
    public List<PlaylistDto> findMine(User owner) {
        return playlistRepository.findAllByOwnerIdOrderByCreatedAtAsc(owner.getId()).stream()
                .map(playlist -> toDto(playlist, owner))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlaylistDto get(UUID id, User owner) {
        return toDto(require(id, owner), owner);
    }

    @Transactional
    public PlaylistDto create(PlaylistRequest request, User owner) {
        String nom = request.nom().trim();
        if (playlistRepository.existsByOwnerIdAndNomIgnoreCase(owner.getId(), nom)) {
            throw new ConflictException("Une playlist porte déjà ce nom");
        }
        Playlist playlist = Playlist.builder().nom(nom).owner(owner).build();
        return toDto(playlistRepository.save(playlist), owner);
    }

    @Transactional
    public PlaylistDto rename(UUID id, PlaylistRequest request, User owner) {
        Playlist playlist = require(id, owner);
        String nom = request.nom().trim();
        if (!nom.equalsIgnoreCase(playlist.getNom())
                && playlistRepository.existsByOwnerIdAndNomIgnoreCase(owner.getId(), nom)) {
            throw new ConflictException("Une playlist porte déjà ce nom");
        }
        playlist.setNom(nom);
        return toDto(playlistRepository.save(playlist), owner);
    }

    @Transactional
    public void delete(UUID id, User owner) {
        playlistRepository.delete(require(id, owner));
    }

    @Transactional
    public PlaylistDto addTitre(UUID playlistId, UUID titreId, User owner) {
        Playlist playlist = require(playlistId, owner);
        if (playlist.getTitres().stream().anyMatch(t -> t.getId().equals(titreId))) {
            throw new BadRequestException("Ce titre est déjà dans la playlist");
        }
        playlist.getTitres().add(titreService.requireTitre(titreId));
        return toDto(playlistRepository.save(playlist), owner);
    }

    @Transactional
    public PlaylistDto removeTitre(UUID playlistId, UUID titreId, User owner) {
        Playlist playlist = require(playlistId, owner);
        boolean retire = playlist.getTitres().removeIf(t -> t.getId().equals(titreId));
        if (!retire) {
            throw NotFoundException.of("Titre dans la playlist", titreId);
        }
        return toDto(playlistRepository.save(playlist), owner);
    }

    private Playlist require(UUID id, User owner) {
        return playlistRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> NotFoundException.of("Playlist", id));
    }

    private PlaylistDto toDto(Playlist playlist, User owner) {
        List<Titre> titres = playlist.getTitres();
        return new PlaylistDto(
                playlist.getId(),
                playlist.getNom(),
                titreMapper.toDtos(titres, titreService.notesUtilisateur(owner, titres)));
    }
}
