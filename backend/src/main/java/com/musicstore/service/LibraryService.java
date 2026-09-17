package com.musicstore.service;

import com.musicstore.domain.Favorite;
import com.musicstore.domain.Titre;
import com.musicstore.domain.User;
import com.musicstore.dto.HistoriqueEntryDto;
import com.musicstore.dto.PageResponse;
import com.musicstore.dto.TitreDto;
import com.musicstore.exception.NotFoundException;
import com.musicstore.mapper.TitreMapper;
import com.musicstore.repository.FavoriteRepository;
import com.musicstore.repository.PlayHistoryRepository;
import com.musicstore.repository.TitreRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Bibliothèque personnelle : sauvegardes et historique de lecture. */
@Service
public class LibraryService {

    private final FavoriteRepository favoriteRepository;
    private final PlayHistoryRepository playHistoryRepository;
    private final TitreRepository titreRepository;
    private final TitreService titreService;
    private final TitreMapper titreMapper;

    public LibraryService(FavoriteRepository favoriteRepository,
                          PlayHistoryRepository playHistoryRepository,
                          TitreRepository titreRepository,
                          TitreService titreService,
                          TitreMapper titreMapper) {
        this.favoriteRepository = favoriteRepository;
        this.playHistoryRepository = playHistoryRepository;
        this.titreRepository = titreRepository;
        this.titreService = titreService;
        this.titreMapper = titreMapper;
    }

    @Transactional(readOnly = true)
    public List<TitreDto> sauvegardes(User user) {
        List<Titre> titres = favoriteRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(Favorite::getTitre)
                .toList();
        return titreMapper.toDtos(titres, titreService.notesUtilisateur(user, titres));
    }

    /** Ajout idempotent : réenregistrer une sauvegarde existante ne crée pas de doublon. */
    @Transactional
    public TitreDto ajouterSauvegarde(User user, UUID titreId) {
        Titre titre = titreService.requireTitre(titreId);
        if (!favoriteRepository.existsByUserIdAndTitreId(user.getId(), titreId)) {
            favoriteRepository.save(Favorite.builder().user(user).titre(titre).build());
        }
        return titreMapper.toDto(titre);
    }

    @Transactional
    public void retirerSauvegarde(User user, UUID titreId) {
        long supprimes = favoriteRepository.deleteByUserIdAndTitreId(user.getId(), titreId);
        if (supprimes == 0) {
            throw NotFoundException.of("Sauvegarde", titreId);
        }
    }

    /** Vide toutes les sauvegardes de l'utilisateur ; renvoie le nombre de titres retirés. */
    @Transactional
    public int viderSauvegardes(User user) {
        return favoriteRepository.deleteAllByUserId(user.getId());
    }

    /**
     * Retire un titre de l'historique.
     *
     * <p>Les vues déjà comptabilisées restent acquises à l'artiste : effacer sa trace d'écoute
     * ne doit pas faire baisser la popularité d'un titre.
     */
    @Transactional
    public void retirerDeHistorique(User user, UUID titreId) {
        if (playHistoryRepository.deleteAllByUserIdAndTitreId(user.getId(), titreId) == 0) {
            throw NotFoundException.of("Titre dans l'historique", titreId);
        }
    }

    @Transactional
    public int viderHistorique(User user) {
        return playHistoryRepository.deleteAllByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public List<UUID> idsSauvegardes(User user) {
        return favoriteRepository.findTitreIdsByUserId(user.getId());
    }

    /** Historique dédoublonné : chaque titre écouté une fois, les plus récents d'abord. */
    @Transactional(readOnly = true)
    public List<TitreDto> titresEcoutes(User user) {
        List<UUID> ids = playHistoryRepository.findTitreIdsEcoutes(user.getId());
        Map<UUID, Titre> parId = titreRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Titre::getId, Function.identity()));
        List<Titre> titres = ids.stream().map(parId::get).filter(Objects::nonNull).toList();
        return titreMapper.toDtos(titres, titreService.notesUtilisateur(user, titres));
    }

    @Transactional(readOnly = true)
    public PageResponse<HistoriqueEntryDto> historique(User user, Pageable pageable) {
        return PageResponse.of(
                playHistoryRepository.findAllByUserIdOrderByPlayedAtDesc(user.getId(), pageable),
                entry -> new HistoriqueEntryDto(titreMapper.toDto(entry.getTitre()), entry.getPlayedAt()));
    }
}
