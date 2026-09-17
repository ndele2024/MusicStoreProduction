import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';
import { Router } from '@angular/router';

import { LectureEnCoursComponent } from '../_core/lecture-en-cours/lecture-en-cours.component';
import { AudiolecteurService } from '../_services/audiolecteur.service';
import { DureePipe } from '../_ui/format.pipes';
import { PochetteComponent } from '../_ui/pochette.component';

/**
 * Lecteur compact fixé en bas de l'écran, présent sur toutes les pages dès qu'un titre
 * est sélectionné. Il pilote le même service que la page lecteur : rien n'est relancé
 * en passant de l'un à l'autre.
 */
@Component({
  selector: 'app-mini-lecteur',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIcon, MatTooltip, PochetteComponent, LectureEnCoursComponent, DureePipe],
  templateUrl: './mini-lecteur.component.html',
  styleUrl: './mini-lecteur.component.scss'
})
export class MiniLecteurComponent {
  private readonly lecteur = inject(AudiolecteurService);
  private readonly router = inject(Router);

  readonly titre = this.lecteur.currentTrack;
  readonly isPlaying = this.lecteur.isPlaying;
  readonly currentTime = this.lecteur.currentTime;
  readonly duration = this.lecteur.duration;
  readonly progression = this.lecteur.progression;
  readonly volume = this.lecteur.volume;
  readonly muet = this.lecteur.muet;
  readonly aPrecedent = this.lecteur.aPrecedent;
  readonly aSuivant = this.lecteur.aSuivant;
  readonly erreur = this.lecteur.erreur;
  readonly estVideo = this.lecteur.estVideo;

  readonly iconeVolume = computed(() =>
    this.muet() || this.volume() === 0 ? 'volume_off' : this.volume() < 0.5 ? 'volume_down' : 'volume_up'
  );

  ouvrirLecteur(): void {
    void this.router.navigate(['/lecteur']);
  }

  /** La vidéo ne peut être lue que sur la page lecteur : le bouton y conduit. */
  lecturePause(): void {
    if (this.estVideo()) {
      this.ouvrirLecteur();
      return;
    }
    this.lecteur.togglePlayPause();
  }

  precedent(): void {
    this.lecteur.previousTrack();
  }

  suivant(): void {
    this.lecteur.nextTrack();
  }

  arreter(): void {
    this.lecteur.arreter();
  }

  fermer(): void {
    this.lecteur.fermer();
  }

  deplacer(event: Event): void {
    this.lecteur.seekAudio(Number.parseFloat((event.target as HTMLInputElement).value));
  }

  changerVolume(event: Event): void {
    this.lecteur.changeVolume(Number.parseFloat((event.target as HTMLInputElement).value));
  }

  basculerMuet(): void {
    this.lecteur.basculerMuet();
  }
}
