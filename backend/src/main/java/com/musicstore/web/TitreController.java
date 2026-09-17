package com.musicstore.web;

import com.musicstore.domain.User;
import com.musicstore.dto.PageResponse;
import com.musicstore.dto.RatingRequest;
import com.musicstore.dto.TitreDto;
import com.musicstore.dto.TitreRequest;
import com.musicstore.security.AppUserDetails;
import com.musicstore.service.TitreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/titres")
@Tag(name = "Titres")
public class TitreController {

    /** Garde-fou contre une page trop grande demandée par un client. */
    private static final int TAILLE_PAGE_MAX = 100;

    private final TitreService titreService;

    public TitreController(TitreService titreService) {
        this.titreService = titreService;
    }

    @GetMapping
    @Operation(summary = "Liste paginée et filtrable des titres")
    public PageResponse<TitreDto> list(@RequestParam(name = "q", required = false) String query,
                                       @RequestParam(name = "page", defaultValue = "0") int page,
                                       @RequestParam(name = "size", defaultValue = "24") int size,
                                       @RequestParam(name = "sort", defaultValue = "recent") String sort,
                                       @AuthenticationPrincipal AppUserDetails principal) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, TAILLE_PAGE_MAX), sortOf(sort));
        return titreService.search(query, pageable, userOrNull(principal));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un titre")
    public TitreDto get(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        return titreService.get(id, userOrNull(principal));
    }

    @GetMapping("/artiste/{artistId}")
    @Operation(summary = "Titres publiés par un artiste")
    public List<TitreDto> byArtist(@PathVariable UUID artistId, @AuthenticationPrincipal AppUserDetails principal) {
        return titreService.byArtist(artistId, userOrNull(principal));
    }

    @PostMapping
    @Operation(summary = "Publie un nouveau titre (artiste)")
    public ResponseEntity<TitreDto> create(@Valid @RequestBody TitreRequest request,
                                           @AuthenticationPrincipal AppUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(titreService.create(request, principal.getUser()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifie un titre dont on est l'auteur")
    public TitreDto update(@PathVariable UUID id,
                           @Valid @RequestBody TitreRequest request,
                           @AuthenticationPrincipal AppUserDetails principal) {
        return titreService.update(id, request, principal.getUser());
    }

    @PostMapping(path = "/{id}/media", consumes = "multipart/form-data")
    @Operation(summary = "Dépose le fichier audio ou vidéo du titre")
    public TitreDto uploadMedia(@PathVariable UUID id,
                                @RequestPart("file") MultipartFile file,
                                @AuthenticationPrincipal AppUserDetails principal) {
        return titreService.uploadMedia(id, file, principal.getUser());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime un titre dont on est l'auteur")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        titreService.delete(id, principal.getUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/lectures")
    @Operation(summary = "Comptabilise une lecture et alimente l'historique")
    public TitreDto lecture(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        return titreService.enregistrerLecture(id, principal.getUser());
    }

    @PutMapping("/{id}/note")
    @Operation(summary = "Note le titre de 1 à 5 étoiles")
    public TitreDto noter(@PathVariable UUID id,
                          @Valid @RequestBody RatingRequest request,
                          @AuthenticationPrincipal AppUserDetails principal) {
        return titreService.noter(id, request, principal.getUser());
    }

    private static User userOrNull(AppUserDetails principal) {
        return principal == null ? null : principal.getUser();
    }

    /** Les tris exposés correspondent aux sections de la page d'accueil : nouveautés et populaires. */
    private static Sort sortOf(String sort) {
        return switch (sort == null ? "" : sort.toLowerCase()) {
            case "populaire", "vues" -> Sort.by(Sort.Direction.DESC, "vues");
            case "note" -> Sort.by(Sort.Direction.DESC, "sommeNotes");
            case "nom" -> Sort.by(Sort.Direction.ASC, "name");
            default -> Sort.by(Sort.Direction.DESC, "annee").and(Sort.by(Sort.Direction.DESC, "createdAt"));
        };
    }
}
