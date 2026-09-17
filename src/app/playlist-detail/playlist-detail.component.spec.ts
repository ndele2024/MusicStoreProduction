import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { providersDeTest } from '../_core/testing';
import { PlaylistDetailComponent } from './playlist-detail.component';

describe('PlaylistDetailComponent', () => {
  let fixture: ComponentFixture<PlaylistDetailComponent>;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [PlaylistDetailComponent],
      providers: [
        ...providersDeTest(),
        { provide: MatDialogRef, useValue: { close: () => undefined } },
        { provide: MAT_DIALOG_DATA, useValue: { param: 'playlist', playlistId: 'inexistante' } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PlaylistDetailComponent);
    fixture.detectChanges();
  });

  afterEach(() => localStorage.clear());

  it('affiche une selection vide quand la playlist est introuvable', () => {
    expect(fixture.componentInstance.titres()).toEqual([]);
    expect(fixture.componentInstance.chargement()).toBeFalse();
  });

  it('affiche un etat vide explicite', () => {
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aucun titre');
  });
});
