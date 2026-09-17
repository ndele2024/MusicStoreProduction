import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { albumDeTest, providersDeTest, titreDeTest, userDeTest } from '../_core/testing';
import { AuthentificationService } from '../_services/authentification.service';
import { MesTitresComponent } from './mes-titres.component';

describe('MesTitresComponent', () => {
  let fixture: ComponentFixture<MesTitresComponent>;
  let http: HttpTestingController;
  const artiste = userDeTest({ id: 'artiste-1', role: 'artiste' });

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [MesTitresComponent],
      providers: providersDeTest()
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthentificationService).login('ed@example.com', 'Password1!').subscribe();
    http.expectOne('/api/auth/login').flush({ token: 'jeton', tokenType: 'Bearer', expiresInSeconds: 3600, user: artiste });

    fixture = TestBed.createComponent(MesTitresComponent);
    fixture.detectChanges();
  });

  afterEach(() => localStorage.clear());

  it('charge les titres et albums de l artiste, brouillons compris', () => {
    http.expectOne('/api/titres/artiste/artiste-1').flush([
      titreDeTest({ id: 'publie', name: 'Publie' }),
      titreDeTest({ id: 'attente', name: 'En attente', mediaUrl: null })
    ]);
    http.expectOne('/api/albums/artiste/artiste-1').flush([albumDeTest()]);
    fixture.detectChanges();

    const composant = fixture.componentInstance;
    expect(composant.nombrePublies()).toBe(1);
    expect(composant.nombreEnAttente()).toBe(1);
    // Les titres qui attendent un fichier passent en tete de liste.
    expect(composant.titresTries()[0].id).toBe('attente');

    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texte).toContain('En attente');
    expect(texte).toContain('Publie');
  });

  it('retire le titre de la liste apres suppression', () => {
    http.expectOne('/api/titres/artiste/artiste-1').flush([titreDeTest({ id: 'a' })]);
    http.expectOne('/api/albums/artiste/artiste-1').flush([]);
    spyOn(window, 'confirm').and.returnValue(true);

    fixture.componentInstance.supprimer(fixture.componentInstance.titres()[0]);
    const requete = http.expectOne('/api/titres/a');
    expect(requete.request.method).toBe('DELETE');
    requete.flush(null);

    expect(fixture.componentInstance.titres().length).toBe(0);
  });
});
