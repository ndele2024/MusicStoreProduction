package com.musicstore.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "playlists", uniqueConstraints = @UniqueConstraint(name = "uk_playlist_owner_nom", columnNames = {"owner_id", "nom"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Playlist {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nom", nullable = false, length = 120)
    private String nom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /** Ordre d'ajout conservé via la colonne {@code position} de la table de jointure. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "playlist_titres",
            joinColumns = @JoinColumn(name = "playlist_id"),
            inverseJoinColumns = @JoinColumn(name = "titre_id"))
    @OrderColumn(name = "ordre")
    @Builder.Default
    private List<Titre> titres = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
