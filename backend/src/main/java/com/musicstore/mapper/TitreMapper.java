package com.musicstore.mapper;

import com.musicstore.domain.Album;
import com.musicstore.domain.Titre;
import com.musicstore.domain.User;
import com.musicstore.dto.TitreDto;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class TitreMapper {

    public TitreDto toDto(Titre titre) {
        return toDto(titre, null);
    }

    public TitreDto toDto(Titre titre, Integer maNote) {
        User artist = titre.getArtist();
        Album album = titre.getAlbum();
        return new TitreDto(
                titre.getId(),
                titre.getName(),
                titre.getDescription(),
                titre.getGenre(),
                titre.getAnnee(),
                titre.getNomFichier(),
                titre.getNomImage(),
                titre.getMediaType().toApi(),
                titre.getStorageKey() == null ? null : "/api/media/" + titre.getId(),
                titre.getVues() == null ? 0L : titre.getVues(),
                titre.getNoteMoyenne(),
                titre.getNombreNotes() == null ? 0 : titre.getNombreNotes(),
                maNote,
                artist == null ? null : artist.getId(),
                artist == null ? null : artist.getFullName(),
                album == null ? null : album.getId(),
                album == null ? null : album.getName());
    }

    /** Variante de lot : évite une requête de note par titre lors du rendu d'une liste. */
    public List<TitreDto> toDtos(Collection<Titre> titres, Map<UUID, Integer> notesParTitre) {
        return titres.stream()
                .map(titre -> toDto(titre, notesParTitre.get(titre.getId())))
                .toList();
    }
}
