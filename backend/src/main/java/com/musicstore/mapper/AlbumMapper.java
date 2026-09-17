package com.musicstore.mapper;

import com.musicstore.domain.Album;
import com.musicstore.domain.Titre;
import com.musicstore.domain.User;
import com.musicstore.dto.AlbumDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AlbumMapper {

    public AlbumDto toDto(Album album) {
        User artist = album.getArtist();
        // Même règle que le catalogue : seuls les titres dont le fichier existe sont comptés.
        List<java.util.UUID> titreIds = album.getTitres() == null
                ? List.of()
                : album.getTitres().stream()
                        .filter(titre -> titre.getStorageKey() != null)
                        .map(Titre::getId)
                        .toList();
        return new AlbumDto(
                album.getId(),
                album.getName(),
                album.getAnnee(),
                titreIds,
                artist == null ? null : artist.getId(),
                artist == null ? null : artist.getFullName());
    }
}
