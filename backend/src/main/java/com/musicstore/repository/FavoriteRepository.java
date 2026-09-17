package com.musicstore.repository;

import com.musicstore.domain.Favorite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    @EntityGraph(attributePaths = {"titre", "titre.artist", "titre.album"})
    List<Favorite> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndTitreId(UUID userId, UUID titreId);

    long deleteByUserIdAndTitreId(UUID userId, UUID titreId);

    @Query("select f.titre.id from Favorite f where f.user.id = :userId")
    List<UUID> findTitreIdsByUserId(@Param("userId") UUID userId);

    /** Suppression en une requête, sans charger chaque sauvegarde en mémoire. */
    @Modifying
    @Query("delete from Favorite f where f.user.id = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);
}
