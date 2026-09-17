import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';

import { messageErreur } from '../_core/auth.interceptor';
import { LectureEnCoursComponent } from '../_core/lecture-en-cours/lecture-en-cours.component';
import { Album, Playlist, Titre } from '../_model/model';
import { AlbumService } from '../_services/album.service';
import { AudiolecteurService } from '../_services/audiolecteur.service';
import { AuthentificationService } from '../_services/authentification.service';
import { DialoguesService } from '../_services/dialogues.service';
import { NotificationService } from '../_services/notification.service';
import { UserConnectedService } from '../_services/user-connected.service';
import { PochetteComponent } from '../_ui/pochette.component';

@Component({
  selector: 'app-playlist-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIcon, MatTooltip, LectureEnCoursComponent, PochetteComponent],
  templateUrl: './playlist-card.component.html',
  styleUrl: './playlist-card.component.scss'
})
export class PlaylistCardComponent {
  private readonly bibliotheque = inject(UserConnectedService);
  private readonly albumService = inject(AlbumService);
  private readonly notification = inject(NotificationService);
  private readonly auth = inject(AuthentificationService);
  private readonly router = inject(Router);
  private readonly lecteur = inject(AudiolecteurService);
  private readonly dialogues = inject(DialoguesService);

  readonly playlist = input<Playlist>();
  readonly album = input<Album>();
  readonly param = input.required<string>();

  readonly estAlbum = computed(() => this.param() === 'album');
  readonly titre = computed(() => (this.estAlbum() ? this.album()?.name : this.playlist()?.nom) ?? '');
  readonly sousTitre = computed(() => (this.estAlbum() ? this.album()?.annee : null));
  readonly nombreTitres = computed(() =>
    this.estAlbum() ? (this.album()?.titres.length ?? 0) : (this.playlist()?.titres.length ?? 0)
  );
  readonly graine = computed(() => (this.estAlbum() ? this.album()?.id : this.playlist()?.id) ?? this.titre());
  readonly description = computed(() => {
    const nombre = `${this.nombreTitres()} titre${this.nombreTitres() > 1 ? 's' : ''}`;
    if (this.estAlbum()) {
      return [this.album()?.artisteNom, this.album()?.annee, nombre].filter(Boolean).join(' · ');
    }
    return `Playlist · ${nombre}`;
  });

  /** La piste qui joue appartient-elle à cette playlist ou à cet album ? */
  readonly enLecture = computed(() => {
    if (!this.lecteur.isPlaying()) {
      return false;
    }
    if (!this.estAlbum()) {
      return this.lecteur.selectionEnLecture(this.playlist()?.titres ?? []);
    }
    const courant = this.lecteur.currentTrack();
    return !!courant && !!this.album() && courant.albumId === this.album()?.id;
  });

  deletePlaylist(): void {
    const playlist = this.playlist();
    if (!playlist || !confirm(`Supprimer la playlist « ${playlist.nom} » ?`)) {
      return;
    }
    this.bibliotheque.deletePlaylist(playlist.id).subscribe({
      next: () => this.notification.show(`Playlist « ${playlist.nom} » supprimée`),
      error: (erreur) => this.notification.show(messageErreur(erreur, 'Suppression impossible'))
    });
  }

  /** Sauvegarde en une fois tous les titres de l'album qui ne le sont pas déjà. */
  saveAlbum(): void {
    const album = this.album();
    if (!album || !this.exigerConnexion()) {
      return;
    }
    this.albumService
      .getTitresAlbum(album.id)
      .pipe(
        switchMap((titres) => {
          const aSauvegarder = titres.filter((t) => !this.bibliotheque.isTitleInSauvegarde(t.id));
          return aSauvegarder.length === 0
            ? of([])
            : forkJoin(aSauvegarder.map((t) => this.bibliotheque.addSauvegarde(t)));
        })
      )
      .subscribe({
        next: () => this.notification.show(`Les titres de « ${album.name} » ont été sauvegardés`),
        error: (erreur) => this.notification.show(messageErreur(erreur, 'Sauvegarde impossible'))
      });
  }

  goToDetail(): void {
    void this.dialogues.detail({ param: this.param(), albumId: this.album()?.id, playlistId: this.playlist()?.id });
  }

  openLecteur(): void {
    if (!this.exigerConnexion()) {
      return;
    }
    if (this.enLecture()) {
      void this.router.navigate(['/lecteur']);
      return;
    }
    if (!this.estAlbum()) {
      this.lancer(this.playlist()?.titres ?? []);
      return;
    }
    const album = this.album();
    if (!album) {
      return;
    }
    this.albumService.getTitresAlbum(album.id).subscribe({
      next: (titres) => this.lancer(titres),
      error: (erreur) => this.notification.show(messageErreur(erreur, "Lecture de l'album impossible"))
    });
  }

  private lancer(titres: Titre[]): void {
    if (titres.length === 0) {
      this.notification.show('Cette sélection ne contient aucun titre');
      return;
    }
    this.lecteur.lire(titres[0], titres);
    void this.router.navigate(['/lecteur']);
  }

  private exigerConnexion(): boolean {
    if (this.auth.isAuthenticated()) {
      return true;
    }
    this.notification.show('Connectez-vous ou créez un compte pour continuer');
    void this.router.navigate(['/login']);
    return false;
  }
}
