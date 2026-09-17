import { Routes } from '@angular/router';

import { artisteGuard, authGuard } from './_guard/auth.guard';

/** Données de route lues par la coquille et par la page de contenu. */
export interface DonneesRoute {
  /** Section affichée par la page de contenu. */
  section?: Section;
  /** Vrai pour les pages plein écran, sans barre ni menu : connexion et inscription. */
  pleinEcran?: boolean;
}

export type Section = 'accueil' | 'nouveautes' | 'recherche' | 'historique' | 'sauvegardes' | 'playlists';

const contenu = () => import('./main-content/main-content.component').then((m) => m.MainContentComponent);

/**
 * Chaque section a sa propre adresse : le bouton retour, les favoris du navigateur et le
 * rechargement de page fonctionnent. Toutes les pages sont chargées à la demande.
 */
export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', loadComponent: contenu, data: { section: 'accueil' }, title: 'MusicSpace' },
  { path: 'nouveautes', loadComponent: contenu, data: { section: 'nouveautes' }, title: 'Nouveautés · MusicSpace' },
  { path: 'recherche', loadComponent: contenu, data: { section: 'recherche' }, title: 'Recherche · MusicSpace' },
  {
    path: 'historique',
    loadComponent: contenu,
    data: { section: 'historique' },
    title: 'Historique · MusicSpace',
    canActivate: [authGuard]
  },
  {
    path: 'sauvegardes',
    loadComponent: contenu,
    data: { section: 'sauvegardes' },
    title: 'Sauvegardes · MusicSpace',
    canActivate: [authGuard]
  },
  {
    path: 'playlists',
    loadComponent: contenu,
    data: { section: 'playlists' },
    title: 'Playlists · MusicSpace',
    canActivate: [authGuard]
  },
  {
    path: 'lecteur',
    loadComponent: () => import('./lecteur-audio/lecteur-audio.component').then((m) => m.LecteurAudioComponent),
    title: 'Lecteur · MusicSpace',
    canActivate: [authGuard]
  },
  // Ancienne adresse du lecteur, conservée pour les liens existants.
  { path: 'lecteur_Audio', redirectTo: 'lecteur' },
  {
    path: 'mes-titres',
    loadComponent: () => import('./mes-titres/mes-titres.component').then((m) => m.MesTitresComponent),
    title: 'Gérer vos titres · MusicSpace',
    canActivate: [artisteGuard]
  },
  {
    path: 'profil',
    loadComponent: () => import('./profil/profil.component').then((m) => m.ProfilComponent),
    title: 'Mon profil · MusicSpace',
    canActivate: [authGuard]
  },
  {
    path: 'login',
    loadComponent: () => import('./connexion/connexion.component').then((m) => m.ConnexionComponent),
    data: { pleinEcran: true },
    title: 'Connexion · MusicSpace'
  },
  {
    path: 'signin',
    loadComponent: () => import('./inscription/inscription.component').then((m) => m.InscriptionComponent),
    data: { pleinEcran: true },
    title: 'Inscription · MusicSpace'
  },
  {
    path: '**',
    loadComponent: () => import('./page404/page404.component').then((m) => m.Page404Component),
    title: 'Page introuvable · MusicSpace'
  }
];
