import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { providersDeTest, titreDeTest } from '../_core/testing';
import { AudiolecteurService } from './audiolecteur.service';
import { UserConnectedService } from './user-connected.service';

describe('AudiolecteurService', () => {
  let service: AudiolecteurService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: providersDeTest() });
    service = TestBed.inject(AudiolecteurService);
    http = TestBed.inject(HttpTestingController);
    // Aucun vrai flux ne doit partir pendant les tests.
    spyOn(HTMLMediaElement.prototype, 'play').and.returnValue(Promise.resolve());
    spyOn(HTMLMediaElement.prototype, 'load').and.stub();
  });

  afterEach(() => localStorage.clear());

  it('demarre un nouveau titre avec sa selection et comptabilise l ecoute', () => {
    const titre = titreDeTest({ id: 'a' });
    const autre = titreDeTest({ id: 'b' });

    service.lire(titre, [titre, autre]);

    expect(service.currentTrack()?.id).toBe('a');
    expect(service.playlist().length).toBe(2);
    http.expectOne('/api/titres/a/lectures').flush(titre);
  });

  it('ne relance pas le titre deja selectionne', () => {
    const titre = titreDeTest({ id: 'a' });
    service.lire(titre, [titre]);
    http.expectOne('/api/titres/a/lectures').flush(titre);

    const selection = spyOn(service, 'selectTrack').and.callThrough();
    service.lire(titre);

    expect(selection).not.toHaveBeenCalled();
    expect(service.playlist().length).toBe(1);
    http.expectNone('/api/titres/a/lectures');
  });

  it('ajoute le titre ecoute a l historique local', () => {
    const titre = titreDeTest({ id: 'a' });
    service.lire(titre);
    http.expectOne('/api/titres/a/lectures').flush(titre);

    expect(TestBed.inject(UserConnectedService).historique()[0].id).toBe('a');
  });

  it('confie la video a la page lecteur et suit son etat', () => {
    const clip = titreDeTest({ id: 'v', mediaType: 'video' });
    service.lire(clip);
    http.expectOne('/api/titres/v/lectures').flush(clip);

    expect(service.estVideo()).toBeTrue();
    expect(service.enLecture('v')).toBeFalse();

    service.signalerVideo(true, 12);
    expect(service.enLecture('v')).toBeTrue();
    expect(service.positionVideo).toBe(12);
  });

  it('le bouton stop coupe la lecture et revient au debut en gardant la piste', () => {
    const clip = titreDeTest({ id: 'v', mediaType: 'video' });
    service.lire(clip);
    http.expectOne('/api/titres/v/lectures').flush(clip);
    service.signalerVideo(true, 42);

    service.arreter();

    expect(service.isPlaying()).toBeFalse();
    expect(service.enLecture('v')).toBeFalse();
    expect(service.positionVideo).toBe(0);
    expect(service.currentTime()).toBe(0);
    expect(service.currentTrack()?.id).toBe('v');
  });

  it('arrete la lecture d un titre supprime', () => {
    const clip = titreDeTest({ id: 'v', mediaType: 'video' });
    service.lire(clip);
    http.expectOne('/api/titres/v/lectures').flush(clip);
    service.signalerVideo(true);

    service.arreterSi('v');

    expect(service.currentTrack()).toBeNull();
    expect(service.isPlaying()).toBeFalse();
  });
});
