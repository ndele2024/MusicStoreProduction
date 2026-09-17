package com.musicstore.repository;

import com.musicstore.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUserEmailIgnoreCase(String userEmail);

    boolean existsByUserEmailIgnoreCase(String userEmail);

    boolean existsByUserNameIgnoreCase(String userName);
}
