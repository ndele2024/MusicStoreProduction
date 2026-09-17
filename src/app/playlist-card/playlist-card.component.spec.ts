import { ComponentFixture, TestBed } from '@angular/core/testing';

import { albumDeTest, playlistDeTest, providersDeTest } from '../_core/testing';
import { PlaylistCardComponent } from './playlist-card.component';

describe('PlaylistCardComponent', () => {
  let fixture: ComponentFixture<PlaylistCardComponent>;
  let composant: PlaylistCardComponent;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [PlaylistCardComponent],
      providers: providersDeTest()
    }).compileComponents();

    fixture = TestBed.createComponent(PlaylistCardComponent);
    composant = fixture.componentInstance;
  });

  afterEach(() => localStorage.clear());

  it('affiche le nom et l annee pour un album', () => {
    fixture.componentRef.setInput('param', 'album');
    fixture.componentRef.setInput('album', albumDeTest());
    fixture.detectChanges();

    expect(composant.titre()).toBe('Divide');
    expect(composant.sousTitre()).toBe(2017);
    expect(composant.nombreTitres()).toBe(1);
  });

  it('affiche le nom sans annee pour une playlist', () => {
    fixture.componentRef.setInput('param', 'playlist');
    fixture.componentRef.setInput('playlist', playlistDeTest());
    fixture.detectChanges();

    expect(composant.titre()).toBe('Route');
    expect(composant.sousTitre()).toBeNull();
    expect(composant.nombreTitres()).toBe(0);
  });
});
