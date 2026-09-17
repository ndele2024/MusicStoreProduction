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
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "age")
    private Integer age;

    @Column(name = "sex", length = 30)
    private String sex;

    @Column(name = "user_name", nullable = false, unique = true, length = 60)
    private String userName;

    @Column(name = "user_email", nullable = false, unique = true, length = 180)
    private String userEmail;

    /** Hash BCrypt : le mot de passe en clair ne quitte jamais la couche web. */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "avatar", length = 255)
    private String avatar;

    // Chargée avec l'utilisateur : le principal authentifié est sérialisé hors transaction.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_preferences", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "preference", length = 80)
    @Builder.Default
    private List<String> preferences = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (role == null) {
            role = Role.AUDITEUR;
        }
    }
}
