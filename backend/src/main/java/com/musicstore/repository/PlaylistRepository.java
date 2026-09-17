package com.musicstore.repository;

import com.musicstore.domain.Playlist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {

    @EntityGraph(attributePaths = {"titres", "titres.artist"})
    List<Playlist> findAllByOwnerIdOrderByCreatedAtAsc(UUID ownerId);

    @EntityGraph(attributePaths = {"titres", "titres.artist"})
    Optional<Playlist> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByOwnerIdAndNomIgnoreCase(UUID ownerId, String nom);
}
