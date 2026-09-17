import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';

import { environment } from '../../environments/environment';
import { Album, Titre } from '../_model/model';

@Injectable({ providedIn: 'root' })
export class AlbumService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/albums`;

  /** Les titres d'un album sont réutilisés par la carte et par la fenêtre de détail : le cache évite un aller-retour. */
  private readonly cacheTitres = new Map<string, Observable<Titre[]>>();

  getAlbums(): Observable<Album[]> {
    return this.http.get<Album[]>(this.base);
  }

  /** Albums d'un artiste ; consultés par l'artiste lui-même, ils incluent les albums encore vides. */
  getAlbumsArtiste(artisteId: string): Observable<Album[]> {
    return this.http.get<Album[]>(`${this.base}/artiste/${artisteId}`);
  }

  creer(name: string, annee: number): Observable<Album> {
    return this.http.post<Album>(this.base, { name, annee });
  }

  /** À appeler après une modification du catalogue, pour que les fenêtres d'album se rechargent. */
  viderCache(): void {
    this.cacheTitres.clear();
  }

  getAlbum(id: string): Observable<Album> {
    return this.http.get<Album>(`${this.base}/${id}`);
  }

  getTitresAlbum(id: string): Observable<Titre[]> {
    let requete = this.cacheTitres.get(id);
    if (!requete) {
      requete = this.http.get<Titre[]>(`${this.base}/${id}/titres`).pipe(shareReplay({ bufferSize: 1, refCount: false }));
      this.cacheTitres.set(id, requete);
    }
    return requete;
  }
}
