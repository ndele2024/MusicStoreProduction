package com.musicstore.web;

import com.musicstore.dto.HistoriqueEntryDto;
import com.musicstore.dto.PageResponse;
import com.musicstore.dto.TitreDto;
import com.musicstore.security.AppUserDetails;
import com.musicstore.service.LibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Bibliothèque de l'utilisateur connecté : sauvegardes et historique. */
@RestController
@RequestMapping("/api/me")
@Tag(name = "Bibliothèque")
public class LibraryController {

    private static final int TAILLE_PAGE_MAX = 100;

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/sauvegardes")
    @Operation(summary = "Titres sauvegardés")
    public List<TitreDto> sauvegardes(@AuthenticationPrincipal AppUserDetails principal) {
        return libraryService.sauvegardes(principal.getUser());
    }

    @GetMapping("/sauvegardes/ids")
    @Operation(summary = "Identifiants des titres sauvegardés, pour l'état des boutons de la liste")
    public List<UUID> idsSauvegardes(@AuthenticationPrincipal AppUserDetails principal) {
        return libraryService.idsSauvegardes(principal.getUser());
    }

    @PostMapping("/sauvegardes/{titreId}")
    @Operation(summary = "Sauvegarde un titre")
    public ResponseEntity<TitreDto> ajouter(@PathVariable UUID titreId,
                                            @AuthenticationPrincipal AppUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(libraryService.ajouterSauvegarde(principal.getUser(), titreId));
    }

    @DeleteMapping("/sauvegardes/{titreId}")
    @Operation(summary = "Retire un titre des sauvegardes")
    public ResponseEntity<Void> retirer(@PathVariable UUID titreId,
                                        @AuthenticationPrincipal AppUserDetails principal) {
        libraryService.retirerSauvegarde(principal.getUser(), titreId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sauvegardes")
    @Operation(summary = "Vide toutes les sauvegardes")
    public ResponseEntity<Void> viderSauvegardes(@AuthenticationPrincipal AppUserDetails principal) {
        libraryService.viderSauvegardes(principal.getUser());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/historique/titres/{titreId}")
    @Operation(summary = "Retire un titre de l'historique, sans modifier son nombre de vues")
    public ResponseEntity<Void> retirerDeHistorique(@PathVariable UUID titreId,
                                                    @AuthenticationPrincipal AppUserDetails principal) {
        libraryService.retirerDeHistorique(principal.getUser(), titreId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/historique")
    @Operation(summary = "Vide tout l'historique de lecture")
    public ResponseEntity<Void> viderHistorique(@AuthenticationPrincipal AppUserDetails principal) {
        libraryService.viderHistorique(principal.getUser());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/historique/titres")
    @Operation(summary = "Titres écoutés, sans doublon, du plus récent au plus ancien")
    public List<TitreDto> titresEcoutes(@AuthenticationPrincipal AppUserDetails principal) {
        return libraryService.titresEcoutes(principal.getUser());
    }

    @GetMapping("/historique")
    @Operation(summary = "Historique de lecture, de la plus récente à la plus ancienne")
    public PageResponse<HistoriqueEntryDto> historique(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "30") int size,
                                                       @AuthenticationPrincipal AppUserDetails principal) {
        return libraryService.historique(
                principal.getUser(),
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, TAILLE_PAGE_MAX)));
    }
}
