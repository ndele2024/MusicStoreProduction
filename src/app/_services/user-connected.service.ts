import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { environment } from '../../environments/environment';
import { Playlist, Titre } from '../_model/model';
import { AuthentificationService } from './authentification.service';

/**
 * Bibliothèque de l'utilisateur connecté : sauvegardes et playlists.
 *
 * Le service tient un cache en signaux pour que les cartes affichent l'état des boutons
 * sans interroger l'API à chaque rendu ; chaque écriture met le cache à jour.
 */
@Injectable({ providedIn: 'root' })
export class UserConnectedService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthentificationService);
  private readonly baseMoi = `${environment.apiUrl}/me`;
  private readonly basePlaylists = `${environment.apiUrl}/playlists`;

  private readonly _sauvegardes = signal<Titre[]>([]);
  private readonly _playlists = signal<Playlist[]>([]);
  private readonly _historique = signal<Titre[]>([]);

  readonly sauvegardes = this._sauvegardes.asReadonly();
  readonly playlists = this._playlists.asReadonly();
  readonly nombreSauvegardes = computed(() => this._sauvegardes().length);
  readonly nombrePlaylists = computed(() => this._playlists().length);
  /** Titres écoutés, sans doublon, le plus récent en tête. */
  readonly historique = this._historique.asReadonly();
  readonly nombreHistorique = computed(() => this._historique().length);

  /** Identifiants sauvegardés, sous forme d'ensemble pour un test en temps constant. */
  private readonly idsSauvegardes = computed(() => new Set(this._sauvegardes().map((t) => t.id)));

  /** Charge sauvegardes et playlists ; à appeler à la connexion et au démarrage. */
  chargerBibliotheque(): void {
    if (!this.auth.isAuthenticated()) {
      this.vider();
      return;
    }
    this.http.get<Titre[]>(`${this.baseMoi}/sauvegardes`).subscribe({
      next: (titres) => this._sauvegardes.set(titres),
      error: () => this._sauvegardes.set([])
    });
    this.http.get<Playlist[]>(this.basePlaylists).subscribe({
      next: (playlists) => this._playlists.set(playlists),
      error: () => this._playlists.set([])
    });
    this.chargerHistorique();
  }

  vider(): void {
    this._sauvegardes.set([]);
    this._playlists.set([]);
    this._historique.set([]);
  }

  // --- Sauvegardes ---------------------------------------------------------

  isTitleInSauvegarde(titreId: string): boolean {
    return this.idsSauvegardes().has(titreId);
  }

  addSauvegarde(titre: Titre): Observable<Titre> {
    return this.http.post<Titre>(`${this.baseMoi}/sauvegardes/${titre.id}`, {}).pipe(
      tap((enregistre) => {
        if (!this.isTitleInSauvegarde(enregistre.id)) {
          this._sauvegardes.update((liste) => [enregistre, ...liste]);
        }
      })
    );
  }

  deleteSauvegardeUser(titreId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseMoi}/sauvegardes/${titreId}`).pipe(
      tap(() => this._sauvegardes.update((liste) => liste.filter((t) => t.id !== titreId)))
    );
  }

  viderSauvegardes(): Observable<void> {
    return this.http.delete<void>(`${this.baseMoi}/sauvegardes`).pipe(
      tap(() => this._sauvegardes.set([]))
    );
  }

  // --- Playlists -----------------------------------------------------------

  isPlaylistNameExist(nom: string): boolean {
    const cible = nom.trim().toLowerCase();
    return this._playlists().some((p) => p.nom.toLowerCase() === cible);
  }

  addPlaylist(nom: string): Observable<Playlist> {
    return this.http.post<Playlist>(this.basePlaylists, { nom }).pipe(
      tap((playlist) => this._playlists.update((liste) => [...liste, playlist]))
    );
  }

  deletePlaylist(playlistId: string): Observable<void> {
    return this.http.delete<void>(`${this.basePlaylists}/${playlistId}`).pipe(
      tap(() => this._playlists.update((liste) => liste.filter((p) => p.id !== playlistId)))
    );
  }

  addTitleToPlaylist(playlistId: string, titre: Titre): Observable<Playlist> {
    return this.http.post<Playlist>(`${this.basePlaylists}/${playlistId}/titres/${titre.id}`, {}).pipe(
      tap((playlist) => this.remplacerPlaylist(playlist))
    );
  }

  deleteTitleToPlaylist(playlistId: string, titre: Titre): Observable<Playlist> {
    return this.http.delete<Playlist>(`${this.basePlaylists}/${playlistId}/titres/${titre.id}`).pipe(
      tap((playlist) => this.remplacerPlaylist(playlist))
    );
  }

  // --- Historique ----------------------------------------------------------

  chargerHistorique(): void {
    if (!this.auth.isAuthenticated()) {
      return;
    }
    this.http.get<Titre[]>(`${this.baseMoi}/historique/titres`).subscribe({
      next: (titres) => this._historique.set(titres),
      error: () => this._historique.set([])
    });
  }

  /** Retire un titre de l'historique ; ses vues restent comptabilisées côté serveur. */
  retirerDeHistorique(titreId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseMoi}/historique/titres/${titreId}`).pipe(
      tap(() => this._historique.update((liste) => liste.filter((t) => t.id !== titreId)))
    );
  }

  viderHistorique(): Observable<void> {
    return this.http.delete<void>(`${this.baseMoi}/historique`).pipe(
      tap(() => this._historique.set([]))
    );
  }

  /** Place le titre en tête de l'historique local, sans attendre un rechargement. */
  noterEcoute(titre: Titre): void {
    this._historique.update((liste) => [titre, ...liste.filter((t) => t.id !== titre.id)]);
  }

  private remplacerPlaylist(playlist: Playlist): void {
    this._playlists.update((liste) => liste.map((p) => (p.id === playlist.id ? playlist : p)));
  }
}
