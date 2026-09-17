import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';

import { messageErreur } from '../_core/auth.interceptor';
import { LectureEnCoursComponent } from '../_core/lecture-en-cours/lecture-en-cours.component';
import { Album, Titre } from '../_model/model';
import { AlbumService } from '../_services/album.service';
import { AudiolecteurService } from '../_services/audiolecteur.service';
import { AuthentificationService } from '../_services/authentification.service';
import { NotificationService } from '../_services/notification.service';
import { UserConnectedService } from '../_services/user-connected.service';
import { EtoilesComponent } from '../_ui/etoiles.component';
import { VuesPipe } from '../_ui/format.pipes';
import { PochetteComponent } from '../_ui/pochette.component';

export interface PlaylistDetailData {
  param: string;
  albumId?: string;
  playlistId?: string;
}

/** Fenêtre de détail d'un album ou d'une playlist : liste des titres et actions groupées. */
@Component({
  selector: 'app-playlist-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule, MatIcon, MatTooltip, LectureEnCoursComponent, PochetteComponent, EtoilesComponent, VuesPipe],
  templateUrl: './playlist-detail.component.html',
  styleUrl: './playlist-detail.component.scss'
})
export class PlaylistDetailComponent implements OnInit {
  private readonly bibliotheque = inject(UserConnectedService);
  private readonly albumService = inject(AlbumService);
  private readonly notification = inject(NotificationService);
  private readonly auth = inject(AuthentificationService);
  private readonly router = inject(Router);
  private readonly lecteur = inject(AudiolecteurService);
  readonly dialogRef = inject(MatDialogRef<PlaylistDetailComponent>);
  readonly data = inject<PlaylistDetailData>(MAT_DIALOG_DATA);

  readonly param = this.data.param;
  readonly estAlbum = computed(() => this.param === 'album');

  private readonly album = signal<Album | null>(null);
  private readonly titresAlbum = signal<Titre[]>([]);
  readonly chargement = signal(this.data.param === 'album');

  /** Une playlist est lue en direct depuis la bibliothèque : un retrait se reflète aussitôt. */
  private readonly playlist = computed(() =>
    this.bibliotheque.playlists().find((p) => p.id === this.data.playlistId) ?? null
  );

  readonly titres = computed(() => (this.estAlbum() ? this.titresAlbum() : (this.playlist()?.titres ?? [])));
  readonly nom = computed(() => (this.estAlbum() ? this.album()?.name : this.playlist()?.nom) ?? '');
  readonly graine = computed(() => this.data.albumId ?? this.data.playlistId ?? this.nom());
  readonly sousTitre = computed(() => {
    const nombre = `${this.titres().length} titre${this.titres().length > 1 ? 's' : ''}`;
    if (this.estAlbum()) {
      return [this.album()?.artisteNom, this.album()?.annee, nombre].filter(Boolean).join(' · ');
    }
    return nombre;
  });

  readonly selectionEnLecture = computed(() => this.lecteur.selectionEnLecture(this.titres()));

  ngOnInit(): void {
    if (!this.estAlbum() || !this.data.albumId) {
      return;
    }
    const albumId = this.data.albumId;
    this.albumService.getAlbum(albumId).subscribe({
      next: (album) => this.album.set(album),
      error: () => this.album.set(null)
    });
    this.albumService.getTitresAlbum(albumId).subscribe({
      next: (titres) => {
        this.titresAlbum.set(titres);
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false)
    });
  }

  enLecture(titre: Titre): boolean {
    return this.lecteur.enLecture(titre.id);
  }

  isInSauvegarde(titreId: string): boolean {
    return this.bibliotheque.isTitleInSauvegarde(titreId);
  }

  sauvegarderTitre(titre: Titre): void {
    if (!this.exigerConnexion()) {
      return;
    }
    this.bibliotheque.addSauvegarde(titre).subscribe({
      next: () => this.notification.show(`« ${titre.name} » sauvegardé`),
      error: (erreur) => this.notification.show(messageErreur(erreur, 'Sauvegarde impossible'))
    });
  }

  sauvegarderTitres(): void {
    if (!this.exigerConnexion()) {
      return;
    }
    const aSauvegarder = this.titres().filter((t) => !this.bibliotheque.isTitleInSauvegarde(t.id));
    const requete = aSauvegarder.length === 0 ? of([]) : forkJoin(aSauvegarder.map((t) => this.bibliotheque.addSauvegarde(t)));
    requete.subscribe({
      next: () => this.notification.show(`Les titres de « ${this.nom()} » ont été sauvegardés`),
      error: (erreur) => this.notification.show(messageErreur(erreur, 'Sauvegarde impossible'))
    });
  }

  /** Retire un titre de la playlist affichée ; indisponible sur un album, qui appartient à l'artiste. */
  retirerDeLaPlaylist(titre: Titre): void {
    const playlistId = this.data.playlistId;
    if (!playlistId) {
      return;
    }
    this.bibliotheque.deleteTitleToPlaylist(playlistId, titre).subscribe({
      next: (playlist) => this.notification.show(`« ${titre.name} » retiré de ${playlist.nom}`),
      error: (erreur) => this.notification.show(messageErreur(erreur, 'Retrait impossible'))
    });
  }

  openLecteur(titre?: Titre): void {
    if (!this.exigerConnexion()) {
      return;
    }
    const liste = this.titres();
    // Sans titre précis et avec une piste de la sélection déjà en cours, on revient au lecteur sans rien relancer.
    const depart = titre ?? (this.selectionEnLecture() ? this.lecteur.currentTrack() : liste[0]);
    if (!depart) {
      this.notification.show('Cette sélection ne contient aucun titre');
      return;
    }
    this.lecteur.lire(depart, liste);
    this.dialogRef.close();
    void this.router.navigate(['/lecteur']);
  }

  private exigerConnexion(): boolean {
    if (this.auth.isAuthenticated()) {
      return true;
    }
    this.notification.show('Connectez-vous ou créez un compte pour continuer');
    this.dialogRef.close();
    void this.router.navigate(['/login']);
    return false;
  }
}
