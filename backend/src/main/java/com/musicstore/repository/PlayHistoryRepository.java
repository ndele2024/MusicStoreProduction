package com.musicstore.repository;

import com.musicstore.domain.PlayHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PlayHistoryRepository extends JpaRepository<PlayHistory, UUID> {

    @EntityGraph(attributePaths = {"titre", "titre.artist", "titre.album"})
    Page<PlayHistory> findAllByUserIdOrderByPlayedAtDesc(UUID userId, Pageable pageable);

    /** Titres écoutés, chacun une seule fois, du plus récemment écouté au plus ancien. */
    @Query("""
            select h.titre.id from PlayHistory h
            where h.user.id = :userId
            group by h.titre.id
            order by max(h.playedAt) desc
            """)
    List<UUID> findTitreIdsEcoutes(@Param("userId") UUID userId);

    /** Retire toutes les écoutes d'un titre ; le compteur de vues du titre, lui, n'est pas touché. */
    @Modifying
    @Query("delete from PlayHistory h where h.user.id = :userId and h.titre.id = :titreId")
    int deleteAllByUserIdAndTitreId(@Param("userId") UUID userId, @Param("titreId") UUID titreId);

    @Modifying
    @Query("delete from PlayHistory h where h.user.id = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);
}
