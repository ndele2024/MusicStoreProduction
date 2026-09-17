package com.musicstore.web;

import com.musicstore.dto.PlaylistDto;
import com.musicstore.dto.PlaylistRequest;
import com.musicstore.security.AppUserDetails;
import com.musicstore.service.PlaylistService;
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
@RequestMapping("/api/playlists")
@Tag(name = "Playlists")
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @GetMapping
    @Operation(summary = "Playlists de l'utilisateur connecté")
    public List<PlaylistDto> list(@AuthenticationPrincipal AppUserDetails principal) {
        return playlistService.findMine(principal.getUser());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une playlist")
    public PlaylistDto get(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        return playlistService.get(id, principal.getUser());
    }

    @PostMapping
    @Operation(summary = "Crée une playlist")
    public ResponseEntity<PlaylistDto> create(@Valid @RequestBody PlaylistRequest request,
                                              @AuthenticationPrincipal AppUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playlistService.create(request, principal.getUser()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Renomme une playlist")
    public PlaylistDto rename(@PathVariable UUID id,
                              @Valid @RequestBody PlaylistRequest request,
                              @AuthenticationPrincipal AppUserDetails principal) {
        return playlistService.rename(id, request, principal.getUser());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime une playlist")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails principal) {
        playlistService.delete(id, principal.getUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/titres/{titreId}")
    @Operation(summary = "Ajoute un titre à une playlist")
    public PlaylistDto addTitre(@PathVariable UUID id,
                                @PathVariable UUID titreId,
                                @AuthenticationPrincipal AppUserDetails principal) {
        return playlistService.addTitre(id, titreId, principal.getUser());
    }

    @DeleteMapping("/{id}/titres/{titreId}")
    @Operation(summary = "Retire un titre d'une playlist")
    public PlaylistDto removeTitre(@PathVariable UUID id,
                                   @PathVariable UUID titreId,
                                   @AuthenticationPrincipal AppUserDetails principal) {
        return playlistService.removeTitre(id, titreId, principal.getUser());
    }
}
