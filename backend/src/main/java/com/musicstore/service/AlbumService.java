package com.musicstore.service;

import com.musicstore.domain.Album;
import com.musicstore.domain.Role;
import com.musicstore.domain.Titre;
import com.musicstore.domain.User;
import com.musicstore.dto.AlbumDto;
import com.musicstore.dto.AlbumRequest;
import com.musicstore.dto.TitreDto;
import com.musicstore.exception.ForbiddenException;
import com.musicstore.exception.NotFoundException;
import com.musicstore.mapper.AlbumMapper;
import com.musicstore.mapper.TitreMapper;
import com.musicstore.repository.AlbumRepository;
import com.musicstore.repository.TitreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final TitreRepository titreRepository;
    private final AlbumMapper albumMapper;
    private final TitreMapper titreMapper;
    private final TitreService titreService;

    public AlbumService(AlbumRepository albumRepository,
                        TitreRepository titreRepository,
                        AlbumMapper albumMapper,
                        TitreMapper titreMapper,
                        TitreService titreService) {
        this.albumRepository = albumRepository;
        this.titreRepository = titreRepository;
        this.albumMapper = albumMapper;
        this.titreMapper = titreMapper;
        this.titreService = titreService;
    }

    /** Un album sans aucun titre lisible n'est pas proposé aux auditeurs. */
    @Transactional(readOnly = true)
    public List<AlbumDto> findAll() {
        return albumRepository.findAllByOrderByAnneeDesc().stream()
                .map(albumMapper::toDto)
                .filter(album -> !album.titres().isEmpty())
                .toList();
    }

    /** L'artiste et l'administrateur voient aussi les albums encore vides, pour y ranger de nouveaux titres. */
    @Transactional(readOnly = true)
    public List<AlbumDto> byArtist(UUID artistId, User currentUser) {
        boolean voitTout = currentUser != null
                && (currentUser.getId().equals(artistId) || currentUser.getRole() == Role.ADMIN);
        return albumRepository.findAllByArtistIdOrderByAnneeDesc(artistId).stream()
                .map(albumMapper::toDto)
                .filter(album -> voitTout || !album.titres().isEmpty())
                .toList();
    }

    @Transactional(readOnly = true)
    public AlbumDto get(UUID id) {
        return albumMapper.toDto(requireAlbum(id));
    }

    /** Titres d'un album, avec la note de l'utilisateur courant lorsqu'il est connecté. */
    @Transactional(readOnly = true)
    public List<TitreDto> titresDe(UUID albumId, User currentUser) {
        requireAlbum(albumId);
        List<Titre> titres = titreRepository.findAllByAlbumIdAndStorageKeyIsNotNull(albumId);
        return titreMapper.toDtos(titres, titreService.notesUtilisateur(currentUser, titres));
    }

    @Transactional
    public AlbumDto create(AlbumRequest request, User auteur) {
        Album album = Album.builder()
                .name(request.name())
                .annee(request.annee())
                .artist(auteur)
                .build();
        return albumMapper.toDto(albumRepository.save(album));
    }

    @Transactional
    public AlbumDto update(UUID id, AlbumRequest request, User auteur) {
        Album album = requireAlbum(id);
        requireOwnership(album, auteur);
        album.setName(request.name());
        album.setAnnee(request.annee());
        return albumMapper.toDto(albumRepository.save(album));
    }

    /** Supprime l'album ; les titres sont conservés et se retrouvent hors album. */
    @Transactional
    public void delete(UUID id, User auteur) {
        Album album = requireAlbum(id);
        requireOwnership(album, auteur);
        titreRepository.findAllByAlbumId(id).forEach(titre -> titre.setAlbum(null));
        albumRepository.delete(album);
    }

    private Album requireAlbum(UUID id) {
        return albumRepository.findWithTitresById(id)
                .orElseThrow(() -> NotFoundException.of("Album", id));
    }

    private void requireOwnership(Album album, User auteur) {
        boolean proprietaire = album.getArtist() != null && album.getArtist().getId().equals(auteur.getId());
        if (!proprietaire && auteur.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Seul l'artiste propriétaire peut modifier cet album");
        }
    }
}
