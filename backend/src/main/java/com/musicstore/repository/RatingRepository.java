package com.musicstore.repository;

import com.musicstore.domain.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID> {

    Optional<Rating> findByUserIdAndTitreId(UUID userId, UUID titreId);

    @Query("select r from Rating r where r.user.id = :userId and r.titre.id in :titreIds")
    List<Rating> findAllByUserIdAndTitreIds(@Param("userId") UUID userId, @Param("titreIds") List<UUID> titreIds);
}
