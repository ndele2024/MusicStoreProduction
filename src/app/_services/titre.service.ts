import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { Page, Titre, TitrePayload } from '../_model/model';
import { AuthentificationService } from './authentification.service';

export type TriTitres = 'recent' | 'populaire' | 'note' | 'nom';

export interface RechercheTitres {
  q?: string;
  page?: number;
  size?: number;
  sort?: TriTitres;
}

@Injectable({ providedIn: 'root' })
export class TitreService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthentificationService);
  private readonly base = `${environment.apiUrl}/titres`;

  getTitres(recherche: RechercheTitres = {}): Observable<Page<Titre>> {
    let params = new HttpParams();
    if (recherche.q) {
      params = params.set('q', recherche.q);
    }
    params = params
      .set('page', String(recherche.page ?? 0))
      .set('size', String(recherche.size ?? 24))
      .set('sort', recherche.sort ?? 'recent');
    return this.http.get<Page<Titre>>(this.base, { params });
  }

  getTitre(id: string): Observable<Titre> {
    return this.http.get<Titre>(`${this.base}/${id}`);
  }

  /** Titres d'un artiste ; consultés par l'artiste lui-même, ils incluent ceux sans fichier. */
  getTitresArtiste(artisteId: string): Observable<Titre[]> {
    return this.http.get<Titre[]>(`${this.base}/artiste/${artisteId}`);
  }

  creer(payload: TitrePayload): Observable<Titre> {
    return this.http.post<Titre>(this.base, payload);
  }

  modifier(id: string, payload: TitrePayload): Observable<Titre> {
    return this.http.put<Titre>(`${this.base}/${id}`, payload);
  }

  supprimer(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  /** Dépose ou remplace le fichier audio ou vidéo ; c'est ce dépôt qui rend le titre visible. */
  deposerMedia(id: string, fichier: File): Observable<Titre> {
    const formulaire = new FormData();
    formulaire.append('file', fichier, fichier.name);
    return this.http.post<Titre>(`${this.base}/${id}/media`, formulaire);
  }

  /** Comptabilise une lecture côté serveur et retourne le titre avec son compteur à jour. */
  enregistrerLecture(id: string): Observable<Titre> {
    return this.http.post<Titre>(`${this.base}/${id}/lectures`, {});
  }

  noter(id: string, valeur: number): Observable<Titre> {
    return this.http.put<Titre>(`${this.base}/${id}/note`, { valeur });
  }

  /**
   * URL de lecture directe pour les balises `audio` et `video`.
   *
   * Ces balises ne permettent pas d'ajouter un en-tête `Authorization` : le jeton
   * voyage donc en paramètre de requête, ce que l'API accepte pour cette seule route.
   */
  mediaUrl(titre: Titre): string | null {
    if (!titre.mediaUrl) {
      return null;
    }
    const token = this.auth.token();
    return token ? `${titre.mediaUrl}?token=${encodeURIComponent(token)}` : titre.mediaUrl;
  }
}
