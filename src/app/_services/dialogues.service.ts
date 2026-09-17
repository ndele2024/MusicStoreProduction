import { inject, Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';

import type { PlaylistDetailData } from '../playlist-detail/playlist-detail.component';

/**
 * Ouverture des fenêtres de dialogue partagées.
 *
 * Les composants sont importés au moment de l'ouverture : le menu et les cartes, présents
 * dès le premier affichage, n'embarquent ainsi ni les formulaires ni la liste de détail.
 */
@Injectable({ providedIn: 'root' })
export class DialoguesService {
  private readonly dialog = inject(MatDialog);

  async nouvellePlaylist(): Promise<void> {
    const { DialogAddPlaylistComponent } = await import('../dialog-add-playlist/dialog-add-playlist.component');
    this.dialog.open(DialogAddPlaylistComponent, { width: '420px', maxWidth: '95vw' });
  }

  async detail(data: PlaylistDetailData): Promise<void> {
    const { PlaylistDetailComponent } = await import('../playlist-detail/playlist-detail.component');
    this.dialog.open(PlaylistDetailComponent, {
      data,
      width: '760px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      panelClass: 'dialogue-plein-ecran-mobile'
    });
  }
}
