import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';

import { albumDeTest, providersDeTest, titreDeTest } from '../_core/testing';
import { MainContentComponent } from './main-content.component';

describe('MainContentComponent', () => {
  let fixture: ComponentFixture<MainContentComponent>;
  let http: HttpTestingController;

  function creer(section: string, q = ''): void {
    fixture = TestBed.createComponent(MainContentComponent);
    fixture.componentRef.setInput('section', section);
    fixture.componentRef.setInput('q', q);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [MainContentComponent],
      providers: providersDeTest()
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => localStorage.clear());

  it('charge nouveautes, titres populaires et albums sur l accueil', () => {
    creer('accueil');

    const requetes = http.match((r) => r.url === '/api/titres');
    expect(requetes.length).toBe(2);
    requetes[0].flush({ content: [titreDeTest()], page: 0, size: 12, totalElements: 1, totalPages: 1, last: true });
    requetes[1].flush({ content: [], page: 0, size: 12, totalElements: 0, totalPages: 0, last: true });
    http.expectOne('/api/albums').flush([albumDeTest()]);
    fixture.detectChanges();

    expect(fixture.componentInstance.nouveautes().length).toBe(1);
    expect(fixture.componentInstance.albums().length).toBe(1);
    expect(fixture.componentInstance.chargement()).toBeFalse();
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('app-music-card').length).toBe(1);
  });

  it('affiche des cartes fantomes pendant le chargement', () => {
    creer('accueil');
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('.squelette').length).toBeGreaterThan(0);
  });

  it('interroge le serveur avec le texte recherche', fakeAsync(() => {
    creer('recherche', 'pop');
    http.match((r) => r.url === '/api/titres' && !r.params.has('q'));
    http.match('/api/albums');

    tick(100);
    const recherche = http.expectOne((r) => r.url === '/api/titres' && r.params.get('q') === 'pop');
    recherche.flush({ content: [titreDeTest()], page: 0, size: 60, totalElements: 1, totalPages: 1, last: true });
    fixture.detectChanges();

    expect(fixture.componentInstance.resultatsTitres().length).toBe(1);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('« pop »');
  }));

  it('affiche un etat vide sur les sauvegardes sans titre', () => {
    creer('sauvegardes');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aucune sauvegarde');
  });
});
