import { computed, inject, Injectable, signal } from '@angular/core';

import { Titre } from '../_model/model';
import { TitreService } from './titre.service';
import { UserConnectedService } from './user-connected.service';

/**
 * Lecteur unique de l'application.
 *
 * L'audio est joué par un élément créé une seule fois, qui continue pendant la navigation.
 * La vidéo est jouée par la balise de la page lecteur, qui rapporte son état ici :
 * les cartes savent ainsi en permanence quel titre est en cours.
 */
@Injectable({ providedIn: 'root' })
export class AudiolecteurService {
  private readonly titreService = inject(TitreService);
  private readonly bibliotheque = inject(UserConnectedService);
  private readonly audioPlayer: HTMLAudioElement | null =
    typeof Audio === 'undefined' ? null : new Audio();

  private readonly _currentTrack = signal<Titre | null>(null);
  private readonly _playlist = signal<Titre[]>([]);
  private readonly _currentTime = signal(0);
  private readonly _duration = signal(0);
  private readonly _isPlaying = signal(false);
  private readonly _volume = signal(1);
  private readonly _erreur = signal<string | null>(null);
  private readonly _muet = signal(false);

  readonly currentTrack = this._currentTrack.asReadonly();
  readonly playlist = this._playlist.asReadonly();
  readonly currentTime = this._currentTime.asReadonly();
  readonly duration = this._duration.asReadonly();
  readonly isPlaying = this._isPlaying.asReadonly();
  readonly volume = this._volume.asReadonly();
  readonly erreur = this._erreur.asReadonly();
  readonly muet = this._muet.asReadonly();

  /** Avancement de 0 à 100, pour les barres de progression. */
  readonly progression = computed(() => {
    const duree = this._duration();
    return duree > 0 && Number.isFinite(duree) ? Math.min(100, (this._currentTime() / duree) * 100) : 0;
  });

  readonly aPrecedent = computed(() => this.indexCourant(this._playlist()) > 0);
  readonly aSuivant = computed(() => {
    const liste = this._playlist();
    const index = this.indexCourant(liste);
    return index >= 0 && index < liste.length - 1;
  });

  readonly estVideo = computed(() => this._currentTrack()?.mediaType === 'video');

  /** Position atteinte dans la vidéo, pour la retrouver en revenant sur la page lecteur. */
  positionVideo = 0;

  /** Piste dont la lecture a déjà été comptabilisée, pour ne pas gonfler les vues à chaque pause. */
  private lectureComptabilisee: string | null = null;

  constructor() {
    const lecteur = this.audioPlayer;
    if (!lecteur) {
      return;
    }
    lecteur.preload = 'metadata';
    lecteur.ontimeupdate = () => this._currentTime.set(lecteur.currentTime);
    lecteur.onloadedmetadata = () => this._duration.set(lecteur.duration);
    lecteur.onplay = () => this._isPlaying.set(true);
    lecteur.onpause = () => this._isPlaying.set(false);
    lecteur.onended = () => this._isPlaying.set(false);
    lecteur.onerror = () => {
      if (!lecteur.getAttribute('src')) {
        return;
      }
      this._isPlaying.set(false);
      this._erreur.set('Lecture impossible : le fichier de ce titre est indisponible');
    };
  }

  /** Vrai si ce titre est la piste courante et qu'il joue en ce moment. */
  enLecture(titreId: string): boolean {
    return this._isPlaying() && this._currentTrack()?.id === titreId;
  }

  /** Vrai si la piste en cours de lecture fait partie de la sélection donnée. */
  selectionEnLecture(titres: Titre[]): boolean {
    const courant = this._currentTrack();
    return this._isPlaying() && !!courant && titres.some((t) => t.id === courant.id);
  }

  /**
   * Point d'entrée des boutons « lire ».
   *
   * Sur la piste déjà sélectionnée, rien n'est relancé : une lecture en cours continue et une
   * pause reprend là où elle s'était arrêtée. Sinon la piste démarre avec sa sélection.
   */
  lire(titre: Titre, playlist: Titre[] = []): void {
    if (this._currentTrack()?.id === titre.id) {
      if (playlist.length > 0) {
        this._playlist.set(playlist);
      }
      if (!this.estVideo() && !this._isPlaying()) {
        this.togglePlayPause();
      }
      return;
    }
    this._playlist.set(playlist);
    this.selectTrack(titre);
  }

  selectTrack(track: Titre): void {
    this._currentTrack.set(track);
    this._erreur.set(null);
    this._currentTime.set(0);
    this._duration.set(0);
    this.positionVideo = 0;

    const source = this.titreService.mediaUrl(track);
    if (!source) {
      this.arreterAudio();
      this._erreur.set("Aucun fichier n'a encore été déposé pour ce titre");
      return;
    }

    if (track.mediaType === 'video') {
      // La balise vidéo de la page lecteur prend le relais : l'audio ne doit pas jouer en double.
      this.arreterAudio();
    } else if (this.audioPlayer) {
      this.audioPlayer.src = source;
      void this.audioPlayer.play().catch(() => this._isPlaying.set(false));
    }
    this.comptabiliserLecture(track);
  }

  /** État rapporté par la balise vidéo de la page lecteur. */
  signalerVideo(enLecture: boolean, position?: number): void {
    if (!this.estVideo()) {
      return;
    }
    this._isPlaying.set(enLecture);
    if (position !== undefined) {
      this.positionVideo = position;
    }
  }

  togglePlayPause(): void {
    if (!this.audioPlayer || !this._currentTrack() || this.estVideo()) {
      return;
    }
    if (this.audioPlayer.paused) {
      void this.audioPlayer.play().catch(() => this._isPlaying.set(false));
    } else {
      this.audioPlayer.pause();
    }
  }

  /**
   * Arrête la lecture : pause et retour au début. La piste reste sélectionnée,
   * si bien qu'un appui sur lecture la relance depuis le départ.
   */
  arreter(): void {
    if (this.audioPlayer && !this.estVideo()) {
      this.audioPlayer.pause();
      if (this.audioPlayer.getAttribute('src')) {
        this.audioPlayer.currentTime = 0;
      }
    }
    this._currentTime.set(0);
    this.positionVideo = 0;
    this._isPlaying.set(false);
  }

  seekAudio(valeur: number): void {
    if (this.audioPlayer && Number.isFinite(valeur)) {
      this.audioPlayer.currentTime = valeur;
    }
  }

  changeVolume(valeur: number): void {
    const borne = Math.min(Math.max(valeur, 0), 1);
    if (this.audioPlayer) {
      this.audioPlayer.volume = borne;
      this.audioPlayer.muted = false;
    }
    this._volume.set(borne);
    this._muet.set(false);
  }

  basculerMuet(): void {
    const muet = !this._muet();
    if (this.audioPlayer) {
      this.audioPlayer.muted = muet;
    }
    this._muet.set(muet);
  }

  /** Ferme le lecteur : la lecture s'arrête et le mini-lecteur disparaît. */
  fermer(): void {
    this.arreterAudio();
    this._currentTrack.set(null);
    this._playlist.set([]);
    this._currentTime.set(0);
    this._duration.set(0);
    this._erreur.set(null);
    this.positionVideo = 0;
    this.lectureComptabilisee = null;
  }

  previousTrack(): void {
    const liste = this._playlist();
    const index = this.indexCourant(liste);
    if (index > 0) {
      this.selectTrack(liste[index - 1]);
    }
  }

  nextTrack(): void {
    const liste = this._playlist();
    const index = this.indexCourant(liste);
    if (index >= 0 && index < liste.length - 1) {
      this.selectTrack(liste[index + 1]);
    }
  }

  repeatTrack(): void {
    if (!this.audioPlayer || this.estVideo()) {
      return;
    }
    this.audioPlayer.currentTime = 0;
    void this.audioPlayer.play().catch(() => this._isPlaying.set(false));
  }

  /** Coupe la lecture si le titre vient d'être supprimé du catalogue. */
  arreterSi(titreId: string): void {
    if (this._currentTrack()?.id !== titreId) {
      return;
    }
    this.arreterAudio();
    this._currentTrack.set(null);
    this._playlist.update((liste) => liste.filter((t) => t.id !== titreId));
  }

  /** Remplace le titre courant après une note ou une lecture, sans relancer le flux. */
  rafraichirTitreCourant(titre: Titre): void {
    if (this._currentTrack()?.id === titre.id) {
      this._currentTrack.set(titre);
    }
  }

  private arreterAudio(): void {
    if (this.audioPlayer) {
      this.audioPlayer.pause();
      this.audioPlayer.removeAttribute('src');
      this.audioPlayer.load();
    }
    this._isPlaying.set(false);
  }

  private indexCourant(playlist: Titre[]): number {
    const id = this._currentTrack()?.id;
    return id ? playlist.findIndex((t) => t.id === id) : -1;
  }

  private comptabiliserLecture(track: Titre): void {
    if (this.lectureComptabilisee === track.id) {
      return;
    }
    this.lectureComptabilisee = track.id;
    this.titreService.enregistrerLecture(track.id).subscribe({
      next: (aJour) => {
        this.rafraichirTitreCourant(aJour);
        this.bibliotheque.noterEcoute(aJour);
      },
      // Une lecture non comptabilisée ne doit jamais interrompre l'écoute.
      error: () => undefined
    });
  }
}
