import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';

import { providersDeTest } from '../_core/testing';
import { DialogAddPlaylistComponent } from './dialog-add-playlist.component';

describe('DialogAddPlaylistComponent', () => {
  let fixture: ComponentFixture<DialogAddPlaylistComponent>;
  let composant: DialogAddPlaylistComponent;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [DialogAddPlaylistComponent],
      providers: [...providersDeTest(), { provide: MatDialogRef, useValue: { close: () => undefined } }]
    }).compileComponents();

    fixture = TestBed.createComponent(DialogAddPlaylistComponent);
    composant = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => localStorage.clear());

  it('refuse la validation tant que le nom est vide', () => {
    expect(composant.peutValider()).toBeFalse();

    composant.playlistName.set('  ');
    expect(composant.peutValider()).toBeFalse();

    composant.playlistName.set('Route');
    expect(composant.peutValider()).toBeTrue();
  });
});
