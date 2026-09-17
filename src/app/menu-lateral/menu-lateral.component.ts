import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { Playlist } from '../_model/model';
import { AuthentificationService } from '../_services/authentification.service';
import { DialoguesService } from '../_services/dialogues.service';
import { UserConnectedService } from '../_services/user-connected.service';
import { MarqueComponent } from '../_ui/marque.component';
import { PochetteComponent } from '../_ui/pochette.component';

@Component({
  selector: 'app-menu-lateral',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIcon, MatTooltip, RouterLink, RouterLinkActive, MarqueComponent, PochetteComponent],
  templateUrl: './menu-lateral.component.html',
  styleUrl: './menu-lateral.component.scss'
})
export class MenuLateralComponent {
  private readonly auth = inject(AuthentificationService);
  private readonly bibliotheque = inject(UserConnectedService);
  private readonly dialogues = inject(DialoguesService);

  /** Affiché comme tiroir mobile : un en-tête avec le logo et un bouton de fermeture apparaît. */
  readonly tiroir = input(false);
  readonly fermer = output<void>();

  readonly isConnected = this.auth.isAuthenticated;
  readonly isArtiste = this.auth.isArtiste;
  readonly nombreSauvegardes = this.bibliotheque.nombreSauvegardes;
  readonly nombrePlaylists = this.bibliotheque.nombrePlaylists;
  readonly nombreHistorique = this.bibliotheque.nombreHistorique;
  readonly playlists = this.bibliotheque.playlists;

  nouvellePlaylist(): void {
    this.fermer.emit();
    void this.dialogues.nouvellePlaylist();
  }

  ouvrirPlaylist(playlist: Playlist): void {
    this.fermer.emit();
    void this.dialogues.detail({ param: 'playlist', playlistId: playlist.id });
  }
}
