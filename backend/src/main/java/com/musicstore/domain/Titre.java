package com.musicstore.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "titres")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Titre {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "genre", length = 60)
    private String genre;

    @Column(name = "annee", nullable = false)
    private Integer annee;

    /** Nom du fichier tel que déposé par l'artiste, utilisé pour le nom de téléchargement. */
    @Column(name = "nom_fichier", length = 255)
    private String nomFichier;

    @Column(name = "nom_image", length = 255)
    private String nomImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 10)
    private MediaType mediaType;

    /** Chemin relatif du média dans le répertoire de stockage, null tant qu'aucun fichier n'est déposé. */
    @Column(name = "storage_key", length = 255)
    private String storageKey;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "vues", nullable = false)
    @Builder.Default
    private Long vues = 0L;

    /** Somme des notes reçues : évite un GROUP BY à chaque lecture de la liste des titres. */
    @Column(name = "somme_notes", nullable = false)
    @Builder.Default
    private Long sommeNotes = 0L;

    @Column(name = "nombre_notes", nullable = false)
    @Builder.Default
    private Integer nombreNotes = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private User artist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (mediaType == null) {
            mediaType = MediaType.AUDIO;
        }
    }

    /** Moyenne arrondie sur 2 décimales, 0 si le titre n'a pas encore été noté. */
    public double getNoteMoyenne() {
        if (nombreNotes == null || nombreNotes == 0) {
            return 0d;
        }
        return Math.round((sommeNotes * 100d) / nombreNotes) / 100d;
    }
}
