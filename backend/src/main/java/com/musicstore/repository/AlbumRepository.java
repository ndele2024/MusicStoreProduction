package com.musicstore.repository;

import com.musicstore.domain.Album;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlbumRepository extends JpaRepository<Album, UUID> {

    @EntityGraph(attributePaths = {"artist", "titres"})
    List<Album> findAllByOrderByAnneeDesc();

    @EntityGraph(attributePaths = {"artist", "titres"})
    Optional<Album> findWithTitresById(UUID id);

    @EntityGraph(attributePaths = {"artist", "titres"})
    List<Album> findAllByArtistIdOrderByAnneeDesc(UUID artistId);
}
