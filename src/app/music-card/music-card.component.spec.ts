import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { providersDeTest, titreDeTest } from '../_core/testing';
import { MusicCardComponent } from './music-card.component';

describe('MusicCardComponent', () => {
  let fixture: ComponentFixture<MusicCardComponent>;
  let composant: MusicCardComponent;
  let http: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [MusicCardComponent],
      providers: providersDeTest()
    }).compileComponents();

    fixture = TestBed.createComponent(MusicCardComponent);
    composant = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('titre', titreDeTest());
    fixture.componentRef.setInput('param', 'autre');
    fixture.detectChanges();
  });

  afterEach(() => localStorage.clear());

  it('affiche le nom du titre et son nombre de vues', () => {
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texte).toContain('Shape of You');
    expect(texte).toContain('42 vues');
    expect(texte).toContain('Ed Sheeran');
  });

  it('affiche cinq etoiles interactives', () => {
    const etoiles = (fixture.nativeElement as HTMLElement).querySelectorAll('app-etoiles button.etoile');
    expect(etoiles.length).toBe(5);
  });

  it('propose de retirer le titre de l historique dans ce contexte', () => {
    fixture.componentRef.setInput('param', 'historique');
    fixture.detectChanges();
    const libelles = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')].map((b) => b.getAttribute('aria-label'));
    expect(libelles).toContain("Retirer de l'historique");
  });

  it('n envoie pas de note quand personne n est connecte', () => {
    composant.noter(5);
    http.expectNone((requete) => requete.url.includes('/note'));
  });
});
