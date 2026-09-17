package com.musicstore.repository;

import com.musicstore.domain.Titre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TitreRepository extends JpaRepository<Titre, UUID> {

    /**
     * Recherche dans le catalogue public.
     *
     * <p>Seuls les titres dont le fichier a été déposé sont retournés : un titre sans média
     * ne peut pas être lu, il n'a donc rien à faire dans les listes des auditeurs.
     */
    @EntityGraph(attributePaths = {"artist", "album"})
    @Query("""
            select t from Titre t
            where t.storageKey is not null
              and (:q is null or :q = ''
               or lower(t.name) like lower(concat('%', :q, '%'))
               or lower(t.description) like lower(concat('%', :q, '%'))
               or lower(t.genre) like lower(concat('%', :q, '%'))
               or cast(t.annee as string) like concat('%', :q, '%'))
            """)
    Page<Titre> search(@Param("q") String q, Pageable pageable);

    @EntityGraph(attributePaths = {"artist", "album"})
    List<Titre> findAllByAlbumId(UUID albumId);

    @EntityGraph(attributePaths = {"artist", "album"})
    List<Titre> findAllByAlbumIdAndStorageKeyIsNotNull(UUID albumId);

    @EntityGraph(attributePaths = {"artist", "album"})
    List<Titre> findAllByArtistId(UUID artistId);

    @EntityGraph(attributePaths = {"artist", "album"})
    List<Titre> findAllByArtistIdAndStorageKeyIsNotNull(UUID artistId);

    /** Incrément atomique : évite la lecture-modification-écriture concurrente sur le compteur de vues. */
    @Modifying
    @Query("update Titre t set t.vues = t.vues + 1 where t.id = :id")
    int incrementVues(@Param("id") UUID id);
}
