import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { providersDeTest, titreDeTest } from '../_core/testing';
import { AudiolecteurService } from '../_services/audiolecteur.service';
import { MiniLecteurComponent } from './mini-lecteur.component';

describe('MiniLecteurComponent', () => {
  let fixture: ComponentFixture<MiniLecteurComponent>;
  let lecteur: AudiolecteurService;
  let http: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [MiniLecteurComponent],
      providers: providersDeTest()
    }).compileComponents();

    spyOn(HTMLMediaElement.prototype, 'play').and.returnValue(Promise.resolve());
    spyOn(HTMLMediaElement.prototype, 'load').and.stub();
    lecteur = TestBed.inject(AudiolecteurService);
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MiniLecteurComponent);
    fixture.detectChanges();
  });

  const texte = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

  it('reste invisible tant qu aucun titre n est selectionne', () => {
    expect((fixture.nativeElement as HTMLElement).querySelector('.mini')).toBeNull();
  });

  it('affiche le titre et l artiste de la piste en cours', () => {
    const titre = titreDeTest({ id: 'a', name: 'Aube', artisteNom: 'Nova' });
    lecteur.lire(titre);
    http.expectOne('/api/titres/a/lectures').flush(titre);
    fixture.detectChanges();

    expect(texte()).toContain('Aube');
    expect(texte()).toContain('Nova');
  });

  it('ouvre la page lecteur sans relancer la piste', () => {
    const titre = titreDeTest({ id: 'a' });
    lecteur.lire(titre);
    http.expectOne('/api/titres/a/lectures').flush(titre);
    fixture.detectChanges();
    const navigation = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const selection = spyOn(lecteur, 'selectTrack');

    fixture.componentInstance.ouvrirLecteur();

    expect(navigation).toHaveBeenCalledWith(['/lecteur']);
    expect(selection).not.toHaveBeenCalled();
  });

  it('renvoie vers la page lecteur pour une video plutot que de la lire en fond', () => {
    const clip = titreDeTest({ id: 'v', mediaType: 'video' });
    lecteur.lire(clip);
    http.expectOne('/api/titres/v/lectures').flush(clip);
    fixture.detectChanges();
    const navigation = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

    fixture.componentInstance.lecturePause();

    expect(navigation).toHaveBeenCalledWith(['/lecteur']);
  });

  it('se ferme et disparait', () => {
    const titre = titreDeTest({ id: 'a' });
    lecteur.lire(titre);
    http.expectOne('/api/titres/a/lectures').flush(titre);
    fixture.detectChanges();

    fixture.componentInstance.fermer();
    fixture.detectChanges();

    expect(lecteur.currentTrack()).toBeNull();
    expect((fixture.nativeElement as HTMLElement).querySelector('.mini')).toBeNull();
  });
});
