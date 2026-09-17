/**
 * Modèles alignés sur les DTO exposés par l'API Spring Boot.
 *
 * Ce sont des interfaces et non des classes : les objets viennent tels quels du JSON,
 * il n'y a donc rien à instancier côté client.
 */

export type Role = 'auditeur' | 'artiste' | 'admin';

export type MediaKind = 'audio' | 'video';

export interface Titre {
  id: string;
  name: string;
  description: string;
  genre: string;
  annee: number;
  nomFichier?: string | null;
  nomImage?: string | null;
  mediaType: MediaKind;
  /** Chemin relatif du flux, `null` tant qu'aucun fichier n'a été déposé par l'artiste. */
  mediaUrl?: string | null;
  vues: number;
  noteMoyenne: number;
  nombreNotes: number;
  /** Note donnée par l'utilisateur courant, absente s'il n'a pas voté ou n'est pas connecté. */
  maNote?: number | null;
  artisteId?: string | null;
  artisteNom?: string | null;
  albumId?: string | null;
  albumNom?: string | null;
}

export interface Album {
  id: string;
  name: string;
  annee: number;
  /** Identifiants des titres de l'album. */
  titres: string[];
  artisteId?: string | null;
  artisteNom?: string | null;
}

export interface Playlist {
  id: string;
  nom: string;
  titres: Titre[];
}

export interface User {
  id: string;
  fullName: string;
  age?: number | null;
  sex?: string | null;
  userName: string;
  userEmail: string;
  role: Role;
  avatar?: string | null;
  preferences: string[];
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInSeconds: number;
  user: User;
}

export interface HistoriqueEntry {
  titre: Titre;
  playedAt: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

/** Métadonnées saisies par l'artiste ; le type audio ou vidéo est déduit du fichier déposé. */
export interface TitrePayload {
  name: string;
  description: string;
  genre: string;
  annee: number;
  mediaType: MediaKind;
  albumId: string | null;
}

export interface RegisterPayload {
  fullName: string;
  age: number;
  sex: string;
  userName: string;
  userEmail: string;
  password: string;
  role: Role;
}

export interface UpdateProfilePayload {
  fullName: string;
  userEmail: string;
  age?: number | null;
  sex?: string | null;
  avatar?: string | null;
  preferences?: string[];
}

/** Réponse d'erreur normalisée du backend. */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors: Record<string, string>;
}
