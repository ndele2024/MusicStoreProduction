import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { providersDeTest, titreDeTest, userDeTest } from '../_core/testing';
import { AuthentificationService } from './authentification.service';
import { TitreService } from './titre.service';

describe('TitreService', () => {
  let service: TitreService;
  let auth: AuthentificationService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: providersDeTest() });
    service = TestBed.inject(TitreService);
    auth = TestBed.inject(AuthentificationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('construit la requete de recherche avec les parametres attendus', () => {
    service.getTitres({ q: 'shape', sort: 'populaire', size: 5 }).subscribe();

    const requete = http.expectOne((r) => r.url === '/api/titres');
    expect(requete.request.params.get('q')).toBe('shape');
    expect(requete.request.params.get('sort')).toBe('populaire');
    expect(requete.request.params.get('size')).toBe('5');
    expect(requete.request.params.get('page')).toBe('0');
    requete.flush({ content: [], page: 0, size: 5, totalElements: 0, totalPages: 0, last: true });
  });

  it('omet le filtre texte quand la recherche est vide', () => {
    service.getTitres().subscribe();

    const requete = http.expectOne((r) => r.url === '/api/titres');
    expect(requete.request.params.has('q')).toBeFalse();
    requete.flush({ content: [], page: 0, size: 24, totalElements: 0, totalPages: 0, last: true });
  });

  it('envoie la note sur la route dediee', () => {
    service.noter('11111111-1111-1111-1111-111111111111', 4).subscribe();

    const requete = http.expectOne('/api/titres/11111111-1111-1111-1111-111111111111/note');
    expect(requete.request.method).toBe('PUT');
    expect(requete.request.body).toEqual({ valeur: 4 });
    requete.flush(titreDeTest());
  });

  it('ajoute le jeton a l URL de lecture, les balises media ne portant pas d en-tete', () => {
    auth.login('jean@example.com', 'Password1!').subscribe();
    http.expectOne('/api/auth/login').flush({
      token: 'jeton',
      tokenType: 'Bearer',
      expiresInSeconds: 3600,
      user: userDeTest()
    });

    expect(service.mediaUrl(titreDeTest())).toContain('?token=jeton');
  });

  it('ne propose pas d URL pour un titre sans fichier', () => {
    expect(service.mediaUrl(titreDeTest({ mediaUrl: null }))).toBeNull();
  });
});
