package com.musicstore.web;

import com.musicstore.domain.Titre;
import com.musicstore.exception.NotFoundException;
import com.musicstore.service.StorageService;
import com.musicstore.service.TitreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import com.musicstore.exception.StorageException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Diffusion des fichiers audio et vidéo.
 *
 * <p>Les requêtes {@code Range} sont honorées : sans elles, le navigateur ne peut pas se déplacer
 * dans la piste ni démarrer une vidéo avant la fin du téléchargement.
 */
@RestController
@RequestMapping("/api/media")
@Tag(name = "Média")
public class MediaController {

    /** Taille maximale d'un morceau renvoyé pour une requête partielle : 1 Mio. */
    private static final long TAILLE_CHUNK = 1024L * 1024;

    private final TitreService titreService;
    private final StorageService storageService;

    public MediaController(TitreService titreService, StorageService storageService) {
        this.titreService = titreService;
        this.storageService = storageService;
    }

    @GetMapping("/{titreId}")
    @Operation(summary = "Diffuse le média d'un titre, avec reprise par plage d'octets")
    public ResponseEntity<Resource> stream(@PathVariable UUID titreId, HttpServletRequest request) {
        Titre titre = titreService.requireTitre(titreId);
        if (titre.getStorageKey() == null) {
            throw new NotFoundException("Aucun média associé au titre " + titreId);
        }

        Resource resource = storageService.loadAsResource(titre.getStorageKey());
        long longueur = storageService.sizeOf(titre.getStorageKey());
        MediaType contentType = resolveContentType(titre);

        List<HttpRange> ranges = HttpRange.parseRanges(request.getHeader(HttpHeaders.RANGE));
        if (ranges.isEmpty()) {
            // Réponse complète : le fichier est diffusé en flux, sans passer par la mémoire.
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .contentLength(longueur)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                    .body(resource);
        }

        // Un seul intervalle est servi à la fois : c'est ce que demandent les lecteurs des navigateurs.
        HttpRange range = ranges.getFirst();
        long debut = range.getRangeStart(longueur);
        long fin = Math.min(range.getRangeEnd(longueur), debut + TAILLE_CHUNK - 1);
        byte[] morceau = lire(resource, debut, (int) (fin - debut + 1));

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(contentType)
                .contentLength(morceau.length)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE, "bytes %d-%d/%d".formatted(debut, debut + morceau.length - 1, longueur))
                .body(new ByteArrayResource(morceau));
    }

    /** Le morceau est plafonné à {@value #TAILLE_CHUNK} octets, il tient donc en mémoire sans risque. */
    private static byte[] lire(Resource resource, long debut, int taille) {
        try (InputStream in = resource.getInputStream()) {
            in.skipNBytes(debut);
            return in.readNBytes(taille);
        } catch (IOException e) {
            throw new StorageException("Lecture partielle impossible", e);
        }
    }

    private static MediaType resolveContentType(Titre titre) {
        if (titre.getContentType() != null && !titre.getContentType().isBlank()) {
            try {
                return MediaType.parseMediaType(titre.getContentType());
            } catch (org.springframework.http.InvalidMediaTypeException ignored) {
                // Type enregistré illisible : on retombe sur le type déduit de l'extension.
            }
        }
        String extension = StorageService.extensionOf(titre.getNomFichier());
        return switch (extension) {
            case "mp3" -> MediaType.parseMediaType("audio/mpeg");
            case "wav" -> MediaType.parseMediaType("audio/wav");
            case "ogg" -> MediaType.parseMediaType("audio/ogg");
            case "m4a", "aac" -> MediaType.parseMediaType("audio/mp4");
            case "flac" -> MediaType.parseMediaType("audio/flac");
            case "mp4", "mov" -> MediaType.parseMediaType("video/mp4");
            case "webm" -> MediaType.parseMediaType("video/webm");
            case "ogv" -> MediaType.parseMediaType("video/ogg");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
