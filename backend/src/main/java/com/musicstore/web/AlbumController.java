package com.musicstore.web;

import com.musicstore.dto.AlbumDto;
import com.musicstore.dto.AlbumRequest;
import com.musicstore.dto.TitreDto;
import com.musicstore.security.AppUserDetails;
import com.musicstore.service.AlbumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/albums")
@Tag(name = "Albums")
public class AlbumController {

    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    @GetMapping
    @Operation(summary = "Liste des albums, du plus récent au plus ancien")
    public List<AlbumDto> list() {
        return albumService.findAll();
    }

    @GetMapping("/artiste/{artistId}")
    @Operation(summary = "Albums d'un artiste, brouillons compris lorsqu'il les consulte lui-même")
    public List<AlbumDto> byArtist(@PathVariable UUID artistId, @AuthenticationPrincipal AppUserDetails principal) {
        return albumService.byArtist(artistId, principal == null ? null : principal.getUser());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un album")
    public AlbumDto get(@PathVariable UUID id) {
        return albumService.get(id);
    }

    @GetMapping("/{id}/titres")
    @Operation(summary = "Titres contenus dans un album")
    public List<TitreDto> titres(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        return albumService.titresDe(id, principal == null ? null : principal.getUser());
    }

    @PostMapping
    @Operation(summary = "Crée un album (artiste)")
    public ResponseEntity<AlbumDto> create(@Valid @RequestBody AlbumRequest request,
                                           @AuthenticationPrincipal AppUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(albumService.create(request, principal.getUser()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifie un album dont on est l'auteur")
    public AlbumDto update(@PathVariable UUID id,
                           @Valid @RequestBody AlbumRequest request,
                           @AuthenticationPrincipal AppUserDetails principal) {
        return albumService.update(id, request, principal.getUser());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime un album dont on est l'auteur")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        albumService.delete(id, principal.getUser());
        return ResponseEntity.noContent().build();
    }
}
