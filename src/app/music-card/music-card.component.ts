import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIcon } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltip } from '@angular/material/tooltip';
import { Router } from '@angular/router';

import { messageErreur } from '../_core/auth.interceptor';
import { LectureEnCoursComponent } from '../_core/lecture-en-cours/lecture-en-cours.component';
import { Playlist, Titre } from '../_model/model';
import { AudiolecteurService } from '../_services/audiolecteur.service';
import { AuthentificationService } from '../_services/authentification.service';
import { DialoguesService } from '../_services/dialogues.service';
import { NotificationService } from '../_services/notification.service';
import { TitreService } from '../_services/titre.service';
import { UserConnectedService } from '../_services/user-connected.service';
import { EtoilesComponent } from '../_ui/etoiles.component';
import { VuesPipe } from '../_ui/format.pipes';
import { PochetteComponent } from '../_ui/pochette.component';

/** Contexte d'affichage de la carte : il détermine les actions proposées. */
export type ContexteCarte = 'autre' | 'sauvegarde' | 'historique';

@Component({
  selector: 'app-music-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatIcon,
    MatMenuModule,
    MatDividerModule,
    MatTooltip,
    LectureEnCoursComponent,
    EtoilesComponent,
    PochetteComponent,
    VuesPipe
  ],
  templateUrl: './music-card.component.html',
  styleUrl: './music-card.component.scss'
})
export class MusicCardComponent {
  private readonly auth = inject(AuthentificationService);
  private readonly bibliotheque = inject(UserConnectedService);
  private readonly titreService = inject(TitreService);
  private readonly lecteur = inject(AudiolecteurService);
  private readonly notification = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly dialogues = inject(DialoguesService);

  readonly titre = input.required<Titre>();
  readonly param = input<ContexteCarte | string>('autre');

  /** Version rafraîchie après une note, sans attendre un rechargement du parent. */
  private readonly aJour = signal<Titre | null>(null);

  readonly titreAffiche = computed<Titre>(() => {
    const local = this.aJour();
    const source = this.titre();
    return local && local.id === source.id ? local : source;
  });

  readonly playlists = this.bibliotheque.playlists;
  readonly isConnected = this.auth.isAuthenticated;
  readonly enLecture = computed(() => this.lecteur.enLecture(this.titreAffiche().id));
  readonly estSauvegarde = computed(() => this.bibliotheque.isTitleInSauvegarde(this.titreAffiche().id));
  readonly icone = computed(() => (this.titreAffiche().mediaType === 'video' ? 'movie' : 'music_note'));

  openDialog(): void {
    if (!this.exigerConnexion()) {
      return;
    }
    void this.dialogues.nouvellePlaylist();
  }

  openLecteur(): void {
    if (!this.exigerConnexion()) {
      return;
    }
    // Sur le titre déjà en cours, lire() ne relance rien : on revient simplement au lecteur.
    this.lecteur.lire(this.titreAffiche());
    void this.router.navigate(['/lecteur']);
  }

  sauvegarderTitre(): void {
    if (!this.exigerConnexion()) {
      return;
    }
    const titre = this.titreAffiche();
    this.bibliotheque.addSauvegarde(titre).subscribe({
      next: () => this.notification.show(`« ${titre.name} » sauvegardé`),
      error: (erreur) => this.notification.show(messageErreur(erreur, 'Sauvegarde impossible'))
    });
  }

  deleteToSauvegarde(): void {
    const titre = this.titreAffiche();
    this.bibliotheque.deleteSauvegardeUser(titre.id).subscribe({
      next: () => this.notification.show(`« ${titre.name} » retiré des sauvegardes`),
      error: (erreur) => this.notification.show(messageErreur(erreur, 'Suppression impossible'))
    });
  }

  retirerDeHistorique(): void {
    const titre = this.titreAffiche();
    this.bibliotheque.retirerDeHistorique(titre.id).subscribe({
      next: () => this.notification.show(`« ${titre.name} » retiré de l'historique`),
      error: (erreur) => this.notification.show(messageErreur(erreur, "Retrait de l'historique impossible"))
    });
  }

  addToPlaylist(playlist: Playlist): void {
    const titre = this.titreAffiche();
    this.bibliotheque.addTitleToPlaylist(playlist.id, titre).subscribe({
      next: () => this.notification.show(`« ${titre.name} » ajouté à ${playlist.nom}`),
      error: (erreur) => this.notification.show(messageErreur(erreur, 'Ajout à la playlist impossible'))
    });
  }

  isInPlaylist(playlist: Playlist): boolean {
    return playlist.titres.some((t) => t.id === this.titreAffiche().id);
  }

  /** Enregistre la note de l'utilisateur : la moyenne affichée est celle renvoyée par le serveur. */
  noter(valeur: number): void {
    if (!this.exigerConnexion()) {
      return;
    }
    this.titreService.noter(this.titreAffiche().id, valeur).subscribe({
      next: (titre) => {
        this.aJour.set(titre);
        this.notification.show(`Note de ${valeur} étoile${valeur > 1 ? 's' : ''} enregistrée`);
      },
      error: (erreur) => this.notification.show(messageErreur(erreur, 'Note non enregistrée'))
    });
  }

  private exigerConnexion(): boolean {
    if (this.isConnected()) {
      return true;
    }
    this.notification.show('Connectez-vous ou créez un compte pour continuer');
    void this.router.navigate(['/login']);
    return false;
  }
}
