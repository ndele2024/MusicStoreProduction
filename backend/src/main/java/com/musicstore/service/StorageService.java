package com.musicstore.service;

import com.musicstore.config.StorageProperties;
import com.musicstore.exception.BadRequestException;
import com.musicstore.exception.StorageException;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Écrit et relit les fichiers média sur le disque, à plat sous le répertoire configuré. */
@Service
public class StorageService {

    private static final Set<String> EXTENSIONS_AUDIO = Set.of("mp3", "wav", "ogg", "m4a", "flac", "aac");
    private static final Set<String> EXTENSIONS_VIDEO = Set.of("mp4", "webm", "ogv", "mov", "mkv");

    private final StorageProperties properties;
    private final Path root;

    public StorageService(StorageProperties properties) {
        this.properties = properties;
        this.root = Paths.get(properties.location()).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new StorageException("Impossible de créer le répertoire de stockage " + root, e);
        }
    }

    public Path getRoot() {
        return root;
    }

    /**
     * Enregistre le fichier sous un nom généré et retourne la clé de stockage.
     * Le nom d'origine n'est jamais réutilisé comme chemin, ce qui écarte les traversées de répertoire.
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier média est vide");
        }
        if (file.getSize() > properties.maxFileSizeBytes()) {
            throw new BadRequestException("Fichier trop volumineux (maximum " + properties.maxFileSizeBytes() + " octets)");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!EXTENSIONS_AUDIO.contains(extension) && !EXTENSIONS_VIDEO.contains(extension)) {
            throw new BadRequestException("Extension de fichier non supportée : " + extension);
        }

        String key = UUID.randomUUID() + "." + extension;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(key), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Échec de l'enregistrement du média", e);
        }
        return key;
    }

    /** Copie un média embarqué dans le jar vers le stockage, utilisé par le jeu de données de démonstration. */
    public String storeClasspathResource(String classpathLocation, String targetKey) {
        Path target = root.resolve(targetKey);
        if (Files.exists(target)) {
            return targetKey;
        }
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        if (!resource.exists()) {
            return null;
        }
        try (InputStream in = resource.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return targetKey;
        } catch (IOException e) {
            throw new StorageException("Échec de la copie du média de démonstration " + classpathLocation, e);
        }
    }

    public Resource loadAsResource(String key) {
        Path file = resolve(key);
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new StorageException("Média illisible : " + key);
            }
            return resource;
        } catch (IOException e) {
            throw new StorageException("Média illisible : " + key, e);
        }
    }

    public long sizeOf(String key) {
        try {
            return Files.size(resolve(key));
        } catch (IOException e) {
            throw new StorageException("Taille du média indisponible : " + key, e);
        }
    }

    public void delete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new StorageException("Échec de la suppression du média " + key, e);
        }
    }

    /** Rejette toute clé qui sortirait du répertoire racine. */
    private Path resolve(String key) {
        Path file = root.resolve(key).normalize();
        if (!file.startsWith(root)) {
            throw new StorageException("Clé de média invalide : " + key);
        }
        return file;
    }

    public static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean isVideoExtension(String extension) {
        return EXTENSIONS_VIDEO.contains(extension);
    }
}
