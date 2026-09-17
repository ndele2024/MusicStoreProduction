import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { playlistDeTest, providersDeTest, titreDeTest, userDeTest } from '../_core/testing';
import { AuthentificationService } from './authentification.service';
import { UserConnectedService } from './user-connected.service';

describe('UserConnectedService', () => {
  let service: UserConnectedService;
  let auth: AuthentificationService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: providersDeTest() });
    service = TestBed.inject(UserConnectedService);
    auth = TestBed.inject(AuthentificationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  function connecter(): void {
    auth.login('jean@example.com', 'Password1!').subscribe();
    http.expectOne('/api/auth/login').flush({
      token: 'jeton',
      tokenType: 'Bearer',
      expiresInSeconds: 3600,
      user: userDeTest()
    });
  }

  it('ne charge rien tant que personne n est connecte', () => {
    service.chargerBibliotheque();
    http.expectNone('/api/me/sauvegardes');
    expect(service.nombreSauvegardes()).toBe(0);
  });

  it('charge sauvegardes et playlists une fois connecte', () => {
    connecter();
    service.chargerBibliotheque();

    http.expectOne('/api/me/sauvegardes').flush([titreDeTest()]);
    http.expectOne('/api/playlists').flush([playlistDeTest()]);
    http.expectOne('/api/me/historique/titres').flush([titreDeTest(), titreDeTest({ id: 'autre' })]);

    expect(service.nombreSauvegardes()).toBe(1);
    expect(service.nombrePlaylists()).toBe(1);
    expect(service.nombreHistorique()).toBe(2);
    expect(service.isTitleInSauvegarde(titreDeTest().id)).toBeTrue();
  });

  it('ajoute puis retire une sauvegarde en tenant le cache a jour', () => {
    const titre = titreDeTest();

    service.addSauvegarde(titre).subscribe();
    http.expectOne(`/api/me/sauvegardes/${titre.id}`).flush(titre);
    expect(service.isTitleInSauvegarde(titre.id)).toBeTrue();

    service.deleteSauvegardeUser(titre.id).subscribe();
    http.expectOne(`/api/me/sauvegardes/${titre.id}`).flush(null);
    expect(service.isTitleInSauvegarde(titre.id)).toBeFalse();
  });

  it('place un titre ecoute en tete de l historique sans doublon', () => {
    const premier = titreDeTest({ id: 'a', name: 'A' });
    const second = titreDeTest({ id: 'b', name: 'B' });

    service.noterEcoute(premier);
    service.noterEcoute(second);
    service.noterEcoute(premier);

    expect(service.historique().map((t) => t.id)).toEqual(['a', 'b']);
    expect(service.nombreHistorique()).toBe(2);
  });

  it('retire un titre de l historique puis vide le reste', () => {
    service.noterEcoute(titreDeTest({ id: 'a' }));
    service.noterEcoute(titreDeTest({ id: 'b' }));

    service.retirerDeHistorique('a').subscribe();
    const retrait = http.expectOne('/api/me/historique/titres/a');
    expect(retrait.request.method).toBe('DELETE');
    retrait.flush(null);
    expect(service.historique().map((t) => t.id)).toEqual(['b']);

    service.viderHistorique().subscribe();
    const vidage = http.expectOne('/api/me/historique');
    expect(vidage.request.method).toBe('DELETE');
    vidage.flush(null);
    expect(service.nombreHistorique()).toBe(0);
  });

  it('vide toutes les sauvegardes', () => {
    service.addSauvegarde(titreDeTest()).subscribe();
    http.expectOne(`/api/me/sauvegardes/${titreDeTest().id}`).flush(titreDeTest());
    expect(service.nombreSauvegardes()).toBe(1);

    service.viderSauvegardes().subscribe();
    const vidage = http.expectOne('/api/me/sauvegardes');
    expect(vidage.request.method).toBe('DELETE');
    vidage.flush(null);

    expect(service.nombreSauvegardes()).toBe(0);
    expect(service.isTitleInSauvegarde(titreDeTest().id)).toBeFalse();
  });

  it('detecte un nom de playlist deja utilise, sans tenir compte de la casse', () => {
    service.addPlaylist('Route').subscribe();
    http.expectOne('/api/playlists').flush(playlistDeTest());

    expect(service.isPlaylistNameExist('route')).toBeTrue();
    expect(service.isPlaylistNameExist('  ROUTE ')).toBeTrue();
    expect(service.isPlaylistNameExist('Sport')).toBeFalse();
  });

  it('remplace la playlist modifiee apres l ajout d un titre', () => {
    const playlist = playlistDeTest();
    service.addPlaylist('Route').subscribe();
    http.expectOne('/api/playlists').flush(playlist);

    const titre = titreDeTest();
    service.addTitleToPlaylist(playlist.id, titre).subscribe();
    http.expectOne(`/api/playlists/${playlist.id}/titres/${titre.id}`)
      .flush({ ...playlist, titres: [titre] });

    expect(service.playlists()[0].titres.length).toBe(1);
  });
});
