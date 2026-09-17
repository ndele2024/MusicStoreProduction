import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { providersDeTest, userDeTest } from '../_core/testing';
import { AuthentificationService } from './authentification.service';

describe('AuthentificationService', () => {
  let service: AuthentificationService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: providersDeTest() });
    service = TestBed.inject(AuthentificationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('demarre deconnecte quand le stockage est vide', () => {
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.user()).toBeNull();
  });

  it('ouvre la session et persiste le jeton apres une connexion reussie', () => {
    const user = userDeTest();
    service.login('jean@example.com', 'Password1!').subscribe();

    const requete = http.expectOne('/api/auth/login');
    expect(requete.request.method).toBe('POST');
    requete.flush({ token: 'jeton', tokenType: 'Bearer', expiresInSeconds: 3600, user });

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.token()).toBe('jeton');
    expect(service.user()?.userEmail).toBe('jean@example.com');
    expect(localStorage.getItem('musicstore.token')).toBe('jeton');
  });

  it('reconnait un artiste et un administrateur', () => {
    service.register({
      fullName: 'Ed Sheeran',
      age: 32,
      sex: 'Homme',
      userName: 'ed',
      userEmail: 'ed@example.com',
      password: 'Password1!',
      role: 'artiste'
    }).subscribe();

    http.expectOne('/api/auth/register').flush({
      token: 'jeton',
      tokenType: 'Bearer',
      expiresInSeconds: 3600,
      user: userDeTest({ role: 'artiste' })
    });

    expect(service.isArtiste()).toBeTrue();
  });

  it('la deconnexion vide la session et le stockage', () => {
    service.login('jean@example.com', 'Password1!').subscribe();
    http.expectOne('/api/auth/login').flush({
      token: 'jeton',
      tokenType: 'Bearer',
      expiresInSeconds: 3600,
      user: userDeTest()
    });

    service.logout();

    expect(service.isAuthenticated()).toBeFalse();
    expect(localStorage.getItem('musicstore.token')).toBeNull();
  });

  it('conserve la langue choisie', () => {
    service.setLanguage('Anglais');
    expect(service.language()).toBe('Anglais');
    expect(localStorage.getItem('musicstore.language')).toBe('Anglais');
  });
});
