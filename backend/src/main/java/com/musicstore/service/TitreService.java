package com.musicstore.service;

import com.musicstore.domain.Album;
import com.musicstore.domain.MediaType;
import com.musicstore.domain.PlayHistory;
import com.musicstore.domain.Rating;
import com.musicstore.domain.Role;
import com.musicstore.domain.Titre;
import com.musicstore.domain.User;
import com.musicstore.dto.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TitreService {

    private final TitreRepository titreRepository;
    private final AlbumRepository albumRepository;
    private final RatingRepository ratingRepository;
    private final PlayHistoryRepository playHistoryRepository;
    private final StorageService storageService;
    private final TitreMapper titreMapper;

    public TitreService(TitreRepository titreRepository,
                        AlbumRepository albumRepository,
                        RatingRepository ratingRepository,
                        PlayHistoryRepository playHistoryRepository,
                        StorageService storageService,
                        TitreMapper titreMapper) {
        this.titreRepository = titreRepository;
        this.albumRepository = albumRepository;
        this.ratingRepository = ratingRepository;
        this.playHistoryRepository = playHistoryRepository;
        this.storageService = storageService;
        this.titreMapper = titreMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<TitreDto> search(String query, Pageable pageable, User currentUser) {
        Page<Titre> page = titreRepository.search(query == null ? "" : query.trim(), pageable);
        Map<UUID, Integer> notes = notesUtilisateur(currentUser, page.getContent());
        return PageResponse.of(page, titre -> titreMapper.toDto(titre, notes.get(titre.getId())));
    }

    @Transactional(readOnly = true)
    public TitreDto get(UUID id, User currentUser) {
        Titre titre = requireTitre(id);
        return titreMapper.toDto(titre, noteDe(currentUser, id));
    }

    /** L'artiste et l'administrateur voient aussi les titres en attente de fichier, les autres non. */
    @Transactional(readOnly = true)
    public List<TitreDto> byArtist(UUID artistId, User currentUser) {
        boolean voitTout = currentUser != null
                && (currentUser.getId().equals(artistId) || currentUser.getRole() == Role.ADMIN);
        List<Titre> titres = voitTout
                ? titreRepository.findAllByArtistId(artistId)
                : titreRepository.findAllByArtistIdAndStorageKeyIsNotNull(artistId);
        return titreMapper.toDtos(titres, notesUtilisateur(currentUser, titres));
    }

    @Transactional
    public TitreDto create(TitreRequest request, User auteur) {
        Titre titre = Titre.builder()
                .name(request.name())
                .description(request.description())
                .genre(request.genre())
                .annee(request.annee())
                .mediaType(MediaType.fromApi(request.mediaType()))
                .artist(auteur)
                .album(resolveAlbum(request.albumId(), auteur))
                .build();
        return titreMapper.toDto(titreRepository.save(titre));
    }

    @Transactional
    public TitreDto update(UUID id, TitreRequest request, User auteur) {
        Titre titre = requireTitre(id);
        requireOwnership(titre, auteur);

        titre.setName(request.name());
        titre.setDescription(request.description());
        titre.setGenre(request.genre());
        titre.setAnnee(request.annee());
        titre.setMediaType(MediaType.fromApi(request.mediaType()));
        titre.setAlbum(resolveAlbum(request.albumId(), auteur));
        return titreMapper.toDto(titreRepository.save(titre));
    }

    @Transactional
    public void delete(UUID id, User auteur) {
        Titre titre = requireTitre(id);
        requireOwnership(titre, auteur);
        storageService.delete(titre.getStorageKey());
        titreRepository.delete(titre);
    }

    /** Depose ou remplace le fichier media du titre ; le type audio/video est deduit de l extension. */
    @Transactional
    public TitreDto uploadMedia(UUID id, MultipartFile file, User auteur) {
        Titre titre = requireTitre(id);
        requireOwnership(titre, auteur);

        String ancienneCle = titre.getStorageKey();
        String cle = storageService.store(file);
        String extension = StorageService.extensionOf(file.getOriginalFilename());

        titre.setStorageKey(cle);
        titre.setNomFichier(file.getOriginalFilename());
        titre.setContentType(file.getContentType());
        titre.setMediaType(StorageService.isVideoExtension(extension) ? MediaType.VIDEO : MediaType.AUDIO);
        Titre saved = titreRepository.save(titre);

        if (ancienneCle != null && !ancienneCle.equals(cle)) {
            storageService.delete(ancienneCle);
        }
        return titreMapper.toDto(saved);
    }

    /** Comptabilise une lecture : increment atomique du compteur de vues et ligne d historique. */
    @Transactional
    public TitreDto enregistrerLecture(UUID id, User auditeur) {
        Titre titre = requireTitre(id);
        titreRepository.incrementVues(id);
        playHistoryRepository.save(PlayHistory.builder().user(auditeur).titre(titre).build());

        // L entite chargee ne voit pas l UPDATE ci-dessus : on aligne la valeur retournee.
        titre.setVues(nz(titre.getVues()) + 1);
        return titreMapper.toDto(titre, noteDe(auditeur, id));
    }

    /** Enregistre ou met a jour la note de l utilisateur et tient a jour les agregats du titre. */
    @Transactional
    public TitreDto noter(UUID id, RatingRequest request, User votant) {
        Titre titre = requireTitre(id);
        int valeur = request.valeur();

        Rating rating = ratingRepository.findByUserIdAndTitreId(votant.getId(), id).orElse(null);
        if (rating == null) {
            ratingRepository.save(Rating.builder().user(votant).titre(titre).valeur(valeur).build());
            titre.setSommeNotes(nz(titre.getSommeNotes()) + valeur);
            titre.setNombreNotes(nz(titre.getNombreNotes()) + 1);
        } else {
            titre.setSommeNotes(nz(titre.getSommeNotes()) - rating.getValeur() + valeur);
            rating.setValeur(valeur);
            ratingRepository.save(rating);
        }
        return titreMapper.toDto(titreRepository.save(titre), valeur);
    }

    @Transactional(readOnly = true)
    public Titre requireTitre(UUID id) {
        return titreRepository.findById(id).orElseThrow(() -> NotFoundException.of("Titre", id));
    }

    /** Notes deja donnees par l utilisateur, en une seule requete pour toute la liste. */
    public Map<UUID, Integer> notesUtilisateur(User user, List<Titre> titres) {
        if (user == null || titres.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = titres.stream().map(Titre::getId).toList();
        return ratingRepository.findAllByUserIdAndTitreIds(user.getId(), ids).stream()
                .collect(Collectors.toMap(r -> r.getTitre().getId(), Rating::getValeur, (a, b) -> a, HashMap::new));
    }

    private Album resolveAlbum(UUID albumId, User auteur) {
        if (albumId == null) {
            return null;
        }
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> NotFoundException.of("Album", albumId));
        if (album.getArtist() != null
                && !album.getArtist().getId().equals(auteur.getId())
                && auteur.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Cet album appartient a un autre artiste");
        }
        return album;
    }

    private void requireOwnership(Titre titre, User auteur) {
        boolean proprietaire = titre.getArtist() != null && titre.getArtist().getId().equals(auteur.getId());
        if (!proprietaire && auteur.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Seul l artiste proprietaire peut modifier ce titre");
        }
    }

    private Integer noteDe(User user, UUID titreId) {
        if (user == null) {
            return null;
        }
        return ratingRepository.findByUserIdAndTitreId(user.getId(), titreId)
                .map(Rating::getValeur)
                .orElse(null);
    }

    private static long nz(Long value) {
        return value == null ? 0L : value;
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
