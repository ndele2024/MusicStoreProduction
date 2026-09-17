import { ChangeDetectionStrategy, Component, computed, ElementRef, inject, OnDestroy, viewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';

import { messageErreur } from '../_core/auth.interceptor';
import { LectureEnCoursComponent } from '../_core/lecture-en-cours/lecture-en-cours.component';
import { Titre } from '../_model/model';
import { AudiolecteurService } from '../_services/audiolecteur.service';
import { NotificationService } from '../_services/notification.service';
import { TitreService } from '../_services/titre.service';
import { EtoilesComponent } from '../_ui/etoiles.component';
import { DureePipe, VuesPipe } from '../_ui/format.pipes';
import { PochetteComponent } from '../_ui/pochette.component';

/**
 * Page du lecteur. Elle affiche l'état tenu par le service et ne démarre rien elle-même :
 * les boutons « lire » choisissent la piste avant d'y naviguer, si bien que revenir ici
 * ne relance jamais le titre en cours.
 */
@Component({
  selector: 'app-lecteur-audio',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatIcon,
    MatTooltip,
    RouterLink,
    PochetteComponent,
    EtoilesComponent,
    LectureEnCoursComponent,
    DureePipe,
    VuesPipe
  ],
  templateUrl: './lecteur-audio.component.html',
  styleUrl: './lecteur-audio.component.scss'
})
export class LecteurAudioComponent implements OnDestroy {
  private readonly lecteur = inject(AudiolecteurService);
  private readonly titreService = inject(TitreService);
  private readonly notification = inject(NotificationService);

  readonly titre = this.lecteur.currentTrack;
  readonly playlist = this.lecteur.playlist;
  readonly currentTime = this.lecteur.currentTime;
  readonly duration = this.lecteur.duration;
  readonly isPlaying = this.lecteur.isPlaying;
  readonly volume = this.lecteur.volume;
  readonly muet = this.lecteur.muet;
  readonly erreur = this.lecteur.erreur;
  readonly progression = this.lecteur.progression;
  readonly aPrecedent = this.lecteur.aPrecedent;
  readonly aSuivant = this.lecteur.aSuivant;

  readonly hasPlaylist = computed(() => this.playlist().length > 1);
  readonly iconeVolume = computed(() =>
    this.muet() || this.volume() === 0 ? 'volume_off' : this.volume() < 0.5 ? 'volume_down' : 'volume_up'
  );

  private readonly video = viewChild<ElementRef<HTMLVideoElement>>('video');

  /** Source du lecteur vidéo, `null` pour une piste audio ou un titre sans fichier. */
  readonly videoUrl = computed(() => {
    const courant = this.titre();
    return courant && courant.mediaType === 'video' ? this.titreService.mediaUrl(courant) : null;
  });

  ngOnDestroy(): void {
    // La balise vidéo disparaît avec la page : la lecture s'arrête, la position est conservée.
    this.lecteur.signalerVideo(false);
  }

  onVideoPrete(event: Event): void {
    const video = event.target as HTMLVideoElement;
    if (this.lecteur.positionVideo > 0 && this.lecteur.positionVideo < video.duration) {
      video.currentTime = this.lecteur.positionVideo;
    }
    void video.play().catch(() => this.lecteur.signalerVideo(false));
  }

  onVideoEtat(event: Event): void {
    const video = event.target as HTMLVideoElement;
    this.lecteur.signalerVideo(!video.paused && !video.ended, video.currentTime);
  }

  changeVolume(event: Event): void {
    this.lecteur.changeVolume(Number.parseFloat((event.target as HTMLInputElement).value));
  }

  basculerMuet(): void {
    this.lecteur.basculerMuet();
  }

  seekAudio(event: Event): void {
    this.lecteur.seekAudio(Number.parseFloat((event.target as HTMLInputElement).value));
  }

  selectTrack(track: Titre): void {
    this.lecteur.lire(track);
  }

  estCourant(track: Titre): boolean {
    return this.titre()?.id === track.id;
  }

  arreter(): void {
    const video = this.video()?.nativeElement;
    if (video) {
      video.pause();
      video.currentTime = 0;
    }
    this.lecteur.arreter();
  }

  togglePlayPause(): void {
    this.lecteur.togglePlayPause();
  }

  previousTrack(): void {
    this.lecteur.previousTrack();
  }

  nextTrack(): void {
    this.lecteur.nextTrack();
  }

  repeatTrack(): void {
    this.lecteur.repeatTrack();
  }

  noter(valeur: number): void {
    const courant = this.titre();
    if (!courant) {
      return;
    }
    this.titreService.noter(courant.id, valeur).subscribe({
      next: (aJour) => {
        this.lecteur.rafraichirTitreCourant(aJour);
        this.notification.show(`Note de ${valeur} étoile${valeur > 1 ? 's' : ''} enregistrée`);
      },
      error: (erreur) => this.notification.show(messageErreur(erreur, 'Note non enregistrée'))
    });
  }
}
