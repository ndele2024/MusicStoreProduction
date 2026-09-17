import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle
} from '@angular/material/dialog';
import { MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';

import { messageErreur } from '../_core/auth.interceptor';
import { NotificationService } from '../_services/notification.service';
import { UserConnectedService } from '../_services/user-connected.service';

@Component({
  selector: 'app-dialog-add-playlist',
  imports: [
    MatButtonModule,
    MatDialogActions,
    MatDialogClose,
    MatDialogTitle,
    MatDialogContent,
    MatFormField,
    MatInput,
    MatLabel,
    FormsModule,
    MatHint
  ],
  templateUrl: './dialog-add-playlist.component.html',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './dialog-add-playlist.component.scss'
})
export class DialogAddPlaylistComponent {
  private readonly dialogRef = inject(MatDialogRef<DialogAddPlaylistComponent>);
  private readonly bibliotheque = inject(UserConnectedService);
  private readonly notification = inject(NotificationService);

  readonly playlistName = signal('');
  readonly enCours = signal(false);

  readonly nomDejaPris = computed(() => {
    const nom = this.playlistName().trim();
    return nom !== '' && this.bibliotheque.isPlaylistNameExist(nom);
  });

  readonly peutValider = computed(() => this.playlistName().trim() !== '' && !this.nomDejaPris() && !this.enCours());

  addPlaylist(): void {
    if (!this.peutValider()) {
      return;
    }
    const nom = this.playlistName().trim();
    this.enCours.set(true);
    this.bibliotheque.addPlaylist(nom).subscribe({
      next: () => {
        this.enCours.set(false);
        this.notification.show(`${nom} a été ajoutée aux playlists`);
        this.dialogRef.close(true);
      },
      error: (erreur) => {
        this.enCours.set(false);
        this.notification.show(messageErreur(erreur, 'Création de la playlist impossible'));
      }
    });
  }

  handleEnter(code: string): void {
    if (code === 'Enter') {
      this.addPlaylist();
    }
  }
}
