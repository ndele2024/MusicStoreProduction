package com.musicstore.service;

import com.musicstore.config.StorageProperties;
import com.musicstore.exception.BadRequestException;
import com.musicstore.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageServiceTest {

    @TempDir
    Path racine;

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new StorageService(new StorageProperties(racine.toString(), 1024, false));
        storageService.init();
    }

    private static MockMultipartFile fichier(String nom, int octets) {
        return new MockMultipartFile("file", nom, "audio/mpeg", new byte[octets]);
    }

    @Test
    @DisplayName("un fichier depose recoit une cle generee et reste lisible")
    void enregistreUnFichier() {
        String cle = storageService.store(new MockMultipartFile(
                "file", "ma piste.mp3", "audio/mpeg", "contenu".getBytes(StandardCharsets.UTF_8)));

        assertThat(cle).endsWith(".mp3").doesNotContain("ma piste");
        assertThat(storageService.sizeOf(cle)).isEqualTo(7);
        assertThat(storageService.loadAsResource(cle).exists()).isTrue();
    }

    @Test
    @DisplayName("une extension inconnue est rejetee")
    void refuseUneExtensionInconnue() {
        assertThatThrownBy(() -> storageService.store(fichier("script.exe", 10)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Extension");
    }

    @Test
    @DisplayName("un fichier vide est rejete")
    void refuseUnFichierVide() {
        assertThatThrownBy(() -> storageService.store(fichier("piste.mp3", 0)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("un fichier au-dela de la taille maximale est rejete")
    void refuseUnFichierTropGros() {
        assertThatThrownBy(() -> storageService.store(fichier("piste.mp3", 2048)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("volumineux");
    }

    @Test
    @DisplayName("une cle qui remonte hors du repertoire racine est rejetee")
    void bloqueLaTraverseeDeRepertoire() {
        assertThatThrownBy(() -> storageService.loadAsResource("../secret.txt"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("invalide");
    }

    @Test
    @DisplayName("supprimer une cle absente ou nulle ne leve rien")
    void supprimeSansErreur() {
        storageService.delete(null);
        storageService.delete("inexistant.mp3");

        String cle = storageService.store(fichier("piste.mp3", 10));
        storageService.delete(cle);
        assertThat(Files.exists(racine.resolve(cle))).isFalse();
    }

    @Test
    @DisplayName("les extensions video sont reconnues comme telles")
    void distingueAudioEtVideo() {
        assertThat(StorageService.isVideoExtension("mp4")).isTrue();
        assertThat(StorageService.isVideoExtension("webm")).isTrue();
        assertThat(StorageService.isVideoExtension("mp3")).isFalse();
        assertThat(StorageService.extensionOf("Clip.FINAL.MP4")).isEqualTo("mp4");
        assertThat(StorageService.extensionOf("sans-extension")).isEmpty();
        assertThat(StorageService.extensionOf(null)).isEmpty();
    }
}
