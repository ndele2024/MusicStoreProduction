package com.musicstore.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Une ligne par lecture : alimente l'historique et le compteur de vues. */
@Entity
@Table(name = "play_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayHistory {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "titre_id", nullable = false)
    private Titre titre;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;

    @PrePersist
    void onCreate() {
        if (playedAt == null) {
            playedAt = Instant.now();
        }
    }
}
