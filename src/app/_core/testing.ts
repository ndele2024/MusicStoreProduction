import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { EnvironmentProviders, Provider } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { Album, Playlist, Titre, User } from '../_model/model';

/** Fournisseurs communs à tous les tests : HTTP simulé, routeur vide, animations neutralisées. */
export function providersDeTest(): (Provider | EnvironmentProviders)[] {
  return [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()];
}

export function titreDeTest(surcharge: Partial<Titre> = {}): Titre {
  return {
    id: '11111111-1111-1111-1111-111111111111',
    name: 'Shape of You',
    description: 'Hit song by Ed Sheeran',
    genre: 'Pop',
    annee: 2017,
    nomFichier: 'shape_of_you.mp3',
    nomImage: null,
    mediaType: 'audio',
    mediaUrl: '/api/media/11111111-1111-1111-1111-111111111111',
    vues: 42,
    noteMoyenne: 4,
    nombreNotes: 3,
    maNote: null,
    artisteId: '22222222-2222-2222-2222-222222222222',
    artisteNom: 'Ed Sheeran',
    albumId: null,
    albumNom: null,
    ...surcharge
  };
}

export function albumDeTest(surcharge: Partial<Album> = {}): Album {
  return {
    id: '33333333-3333-3333-3333-333333333333',
    name: 'Divide',
    annee: 2017,
    titres: ['11111111-1111-1111-1111-111111111111'],
    artisteId: '22222222-2222-2222-2222-222222222222',
    artisteNom: 'Ed Sheeran',
    ...surcharge
  };
}

export function playlistDeTest(surcharge: Partial<Playlist> = {}): Playlist {
  return {
    id: '44444444-4444-4444-4444-444444444444',
    nom: 'Route',
    titres: [],
    ...surcharge
  };
}

export function userDeTest(surcharge: Partial<User> = {}): User {
  return {
    id: '55555555-5555-5555-5555-555555555555',
    fullName: 'Jean Dupont',
    age: 25,
    sex: 'Homme',
    userName: 'jdupont',
    userEmail: 'jean@example.com',
    role: 'auditeur',
    avatar: '',
    preferences: [],
    ...surcharge
  };
}
