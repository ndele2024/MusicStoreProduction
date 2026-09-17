import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { environment } from '../../environments/environment';
import { AuthResponse, RegisterPayload, UpdateProfilePayload, User } from '../_model/model';

const CLE_JETON = 'musicstore.token';
const CLE_UTILISATEUR = 'musicstore.user';
const CLE_LANGUE = 'musicstore.language';

/**
 * Authentification par jeton JWT.
 *
 * L'état de session vit dans des signaux : les composants s'y abonnent sans polling
 * et le stockage local ne sert qu'à survivre à un rechargement de page.
 */
@Injectable({ providedIn: 'root' })
export class AuthentificationService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/auth`;

  private readonly _token = signal<string | null>(lireChaine(CLE_JETON));
  private readonly _user = signal<User | null>(lireJson<User>(CLE_UTILISATEUR));
  private readonly _language = signal<string>(lireChaine(CLE_LANGUE) ?? 'Francais');

  readonly token = this._token.asReadonly();
  readonly user = this._user.asReadonly();
  readonly language = this._language.asReadonly();
  readonly isAuthenticated = computed(() => this._token() !== null && this._user() !== null);
  readonly isArtiste = computed(() => {
    const role = this._user()?.role;
    return role === 'artiste' || role === 'admin';
  });

  login(userEmail: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.base}/login`, { userEmail, password })
      .pipe(tap((reponse) => this.ouvrirSession(reponse)));
  }

  register(payload: RegisterPayload): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.base}/register`, payload)
      .pipe(tap((reponse) => this.ouvrirSession(reponse)));
  }

  emailExists(email: string): Observable<{ exists: boolean }> {
    return this.http.get<{ exists: boolean }>(`${this.base}/email-disponible`, { params: { email } });
  }

  /** Recharge le profil depuis l'API, par exemple après un rechargement de page. */
  refreshProfile(): Observable<User> {
    return this.http.get<User>(`${this.base}/me`).pipe(tap((user) => this.majUtilisateur(user)));
  }

  updateProfile(payload: UpdateProfilePayload): Observable<User> {
    return this.http.put<User>(`${this.base}/me`, payload).pipe(tap((user) => this.majUtilisateur(user)));
  }

  logout(): void {
    this._token.set(null);
    this._user.set(null);
    localStorage.removeItem(CLE_JETON);
    localStorage.removeItem(CLE_UTILISATEUR);
  }

  setLanguage(langue: string): void {
    this._language.set(langue);
    localStorage.setItem(CLE_LANGUE, langue);
  }

  private ouvrirSession(reponse: AuthResponse): void {
    this._token.set(reponse.token);
    this._user.set(reponse.user);
    localStorage.setItem(CLE_JETON, reponse.token);
    localStorage.setItem(CLE_UTILISATEUR, JSON.stringify(reponse.user));
  }

  private majUtilisateur(user: User): void {
    this._user.set(user);
    localStorage.setItem(CLE_UTILISATEUR, JSON.stringify(user));
  }
}

function lireChaine(cle: string): string | null {
  try {
    return localStorage.getItem(cle);
  } catch {
    // Navigation privée ou stockage bloqué : la session reste simplement en mémoire.
    return null;
  }
}

function lireJson<T>(cle: string): T | null {
  const brut = lireChaine(cle);
  if (!brut) {
    return null;
  }
  try {
    return JSON.parse(brut) as T;
  } catch {
    return null;
  }
}
