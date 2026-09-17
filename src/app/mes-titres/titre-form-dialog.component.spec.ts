import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { albumDeTest, providersDeTest, titreDeTest } from '../_core/testing';
import { NOUVEL_ALBUM, TitreFormDialogComponent } from './titre-form-dialog.component';

describe('TitreFormDialogComponent', () => {
  let fixture: ComponentFixture<TitreFormDialogComponent>;
  let composant: TitreFormDialogComponent;
  let http: HttpTestingController;
  let fermeture: jasmine.Spy;

  function creer(data: object): void {
    fermeture = jasmine.createSpy('close');
    TestBed.configureTestingModule({
      imports: [TitreFormDialogComponent],
      providers: [
        ...providersDeTest(),
        { provide: MatDialogRef, useValue: { close: fermeture } },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    });
    fixture = TestBed.createComponent(TitreFormDialogComponent);
    composant = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  it('cree l album, puis le titre, puis depose le fichier', () => {
    creer({ albums: [] });
    composant.form.patchValue({ name: 'Nouveau', annee: 2026, albumId: NOUVEL_ALBUM, nouvelAlbumNom: 'Premier album' });
    composant.fichier.set(new File(['son'], 'piste.mp3', { type: 'audio/mpeg' }));

    composant.enregistrer();

    http.expectOne('/api/albums').flush(albumDeTest({ id: 'album-1' }));
    const creation = http.expectOne('/api/titres');
    expect(creation.request.body.albumId).toBe('album-1');
    creation.flush(titreDeTest({ id: 't-1', mediaUrl: null }));
    const depot = http.expectOne('/api/titres/t-1/media');
    expect(depot.request.body instanceof FormData).toBeTrue();
    depot.flush(titreDeTest({ id: 't-1' }));

    expect(fermeture).toHaveBeenCalledWith(true);
  });

  it('modifie un titre existant sans fichier en conservant son type', () => {
    creer({ titre: titreDeTest({ id: 't-1', mediaType: 'video' }), albums: [] });
    composant.form.patchValue({ name: 'Renomme' });

    composant.enregistrer();

    const modification = http.expectOne('/api/titres/t-1');
    expect(modification.request.method).toBe('PUT');
    expect(modification.request.body.mediaType).toBe('video');
    modification.flush(titreDeTest({ id: 't-1' }));
    http.expectNone('/api/titres/t-1/media');
    expect(fermeture).toHaveBeenCalledWith(true);
  });

  it('refuse un format de fichier non pris en charge', () => {
    creer({ albums: [] });
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [new File(['x'], 'virus.exe')] });

    composant.choisirFichier({ target: input } as unknown as Event);

    expect(composant.fichier()).toBeNull();
    expect(composant.erreurFichier()).toContain('.exe');
  });

  it('bloque l envoi tant que le titre est vide', () => {
    creer({ albums: [] });
    composant.form.patchValue({ name: '' });

    composant.enregistrer();

    http.expectNone('/api/titres');
    expect(fermeture).not.toHaveBeenCalled();
  });
});
