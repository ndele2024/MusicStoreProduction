import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  RouterStateSnapshot,
  UrlTree
} from '@angular/router';

import { providersDeTest, userDeTest } from '../_core/testing';
import { AuthentificationService } from '../_services/authentification.service';
import { artisteGuard, authGuard } from './auth.guard';

describe('authGuard', () => {
  let auth: AuthentificationService;
  let http: HttpTestingController;

  const executer = (guard: CanActivateFn, url = '/profil') =>
    TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot)
    );

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: providersDeTest() });
    auth = TestBed.inject(AuthentificationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  function connecter(role: 'auditeur' | 'artiste'): void {
    auth.login('jean@example.com', 'Password1!').subscribe();
    http.expectOne('/api/auth/login').flush({
      token: 'jeton',
      tokenType: 'Bearer',
      expiresInSeconds: 3600,
      user: userDeTest({ role })
    });
  }

  it('redirige vers la connexion en conservant la destination', () => {
    const resultat = executer(authGuard, '/profil');

    expect(resultat instanceof UrlTree).toBeTrue();
    expect((resultat as UrlTree).toString()).toContain('redirect=%2Fprofil');
  });

  it('laisse passer un utilisateur connecte', () => {
    connecter('auditeur');
    expect(executer(authGuard)).toBeTrue();
  });

  it('reserve les routes artiste aux comptes artiste', () => {
    connecter('auditeur');
    expect(executer(artisteGuard) instanceof UrlTree).toBeTrue();
  });

  it('laisse passer un artiste', () => {
    connecter('artiste');
    expect(executer(artisteGuard)).toBeTrue();
  });
});
