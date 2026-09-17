import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBar } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { Observable, of, switchMap } from 'rxjs';

import { messageErreur } from '../_core/auth.interceptor';
import { Album, MediaKind, Titre } from '../_model/model';
import { AlbumService } from '../_services/album.service';
import { TitreService } from '../_services/titre.service';

/** Mêmes extensions et même plafond que la validation du backend. */
const EXTENSIONS_AUDIO = ['mp3', 'wav', 'ogg', 'm4a', 'flac', 'aac'];
const EXTENSIONS_VIDEO = ['mp4', 'webm', 'ogv', 'mov', 'mkv'];
const TAILLE_MAX_OCTETS = 200 * 1024 * 1024;

/** Valeur spéciale du sélecteur d'album : créer l'album en même temps que le titre. */
export const NOUVEL_ALBUM = '__nouvel_album__';

export interface TitreFormData {
  titre?: Titre;
  albums: Album[];
}

@Component({
  selector: 'app-titre-form-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIcon,
    MatProgressBar
  ],
  templateUrl: './titre-form-dialog.component.html',
  styleUrl: './titre-form-dialog.component.css'
})
export class TitreFormDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<TitreFormDialogComponent, boolean>);
  private readonly titreService = inject(TitreService);
  private readonly albumService = inject(AlbumService);
  private readonly fb = inject(FormBuilder);
  readonly data = inject<TitreFormData>(MAT_DIALOG_DATA);

  readonly nouvelAlbum = NOUVEL_ALBUM;
  readonly edition = !!this.data.titre;
  readonly accept = [...EXTENSIONS_AUDIO, ...EXTENSIONS_VIDEO].map((e) => '.' + e).join(',');

  readonly albums = signal<Album[]>(this.data.albums);
  readonly fichier = signal<File | null>(null);
  readonly erreurFichier = signal('');
  readonly erreur = signal('');
  readonly enCours = signal(false);

  /** Identifiant du titre une fois créé : si le dépôt du fichier échoue, un nouvel essai ne le recrée pas. */
  private titreId: string | null = this.data.titre?.id ?? null;
  /** Vrai dès qu'une écriture a abouti : la liste appelante devra se rafraîchir. */
  private aEnregistre = false;

  readonly form = this.fb.nonNullable.group({
    name: [this.data.titre?.name ?? '', [Validators.required, Validators.maxLength(160)]],
    description: [this.data.titre?.description ?? '', [Validators.maxLength(1000)]],
    genre: [this.data.titre?.genre ?? '', [Validators.maxLength(60)]],
    annee: [this.data.titre?.annee ?? new Date().getFullYear(), [Validators.required, Validators.min(1900), Validators.max(2100)]],
    albumId: [this.data.titre?.albumId ?? ''],
    nouvelAlbumNom: [''],
    nouvelAlbumAnnee: [new Date().getFullYear()]
  });

  private readonly albumChoisi = toSignal(this.form.controls.albumId.valueChanges, {
    initialValue: this.form.controls.albumId.value
  });
  readonly creeAlbum = computed(() => this.albumChoisi() === NOUVEL_ALBUM);

  readonly fichierActuel = this.data.titre?.mediaUrl ? (this.data.titre.nomFichier ?? 'fichier déposé') : null;

  choisirFichier(event: Event): void {
    const input = event.target as HTMLInputElement;
    const fichier = input.files?.[0] ?? null;
    input.value = '';
    this.erreurFichier.set('');
    if (!fichier) {
      return;
    }
    const extension = fichier.name.split('.').pop()?.toLowerCase() ?? '';
    if (![...EXTENSIONS_AUDIO, ...EXTENSIONS_VIDEO].includes(extension)) {
      this.erreurFichier.set(`Format non pris en charge (.${extension})`);
      return;
    }
    if (fichier.size > TAILLE_MAX_OCTETS) {
      this.erreurFichier.set('Fichier trop volumineux : 200 Mo au maximum');
      return;
    }
    this.fichier.set(fichier);
  }

  retirerFichier(): void {
    this.fichier.set(null);
  }

  tailleLisible(octets: number): string {
    return octets >= 1024 * 1024 ? `${(octets / 1024 / 1024).toFixed(1)} Mo` : `${Math.ceil(octets / 1024)} Ko`;
  }

  enregistrer(): void {
    const valeurs = this.form.getRawValue();
    if (this.creeAlbum()) {
      this.form.controls.nouvelAlbumNom.setValidators([Validators.required, Validators.maxLength(160)]);
      this.form.controls.nouvelAlbumAnnee.setValidators([Validators.required, Validators.min(1900), Validators.max(2100)]);
    } else {
      this.form.controls.nouvelAlbumNom.clearValidators();
      this.form.controls.nouvelAlbumAnnee.clearValidators();
    }
    this.form.controls.nouvelAlbumNom.updateValueAndValidity();
    this.form.controls.nouvelAlbumAnnee.updateValueAndValidity();
    this.form.markAllAsTouched();
    if (this.form.invalid || this.enCours()) {
      return;
    }

    this.enCours.set(true);
    this.erreur.set('');

    const album$: Observable<string | null> = this.creeAlbum()
      ? this.albumService.creer(valeurs.nouvelAlbumNom.trim(), valeurs.nouvelAlbumAnnee).pipe(
          switchMap((album) => {
            // L'album existe désormais : un nouvel essai doit le réutiliser, pas le recréer.
            this.aEnregistre = true;
            this.albums.update((liste) => [album, ...liste]);
            this.form.controls.albumId.setValue(album.id);
            return of(album.id);
          })
        )
      : of(valeurs.albumId || null);

    const fichier = this.fichier();
    album$
      .pipe(
        switchMap((albumId) => {
          const payload = {
            name: valeurs.name.trim(),
            description: valeurs.description.trim(),
            genre: valeurs.genre.trim(),
            annee: valeurs.annee,
            mediaType: this.typeMedia(fichier),
            albumId
          };
          return this.titreId ? this.titreService.modifier(this.titreId, payload) : this.titreService.creer(payload);
        }),
        switchMap((titre) => {
          this.titreId = titre.id;
          this.aEnregistre = true;
          return fichier ? this.titreService.deposerMedia(titre.id, fichier) : of(titre);
        })
      )
      .subscribe({
        next: () => {
          this.enCours.set(false);
          this.dialogRef.close(true);
        },
        error: (erreur) => {
          this.enCours.set(false);
          const prefixe = this.titreId && fichier && this.aEnregistre ? 'Titre enregistré, mais le fichier n\'a pas pu être déposé : ' : '';
          this.erreur.set(prefixe + messageErreur(erreur, 'Enregistrement impossible'));
        }
      });
  }

  annuler(): void {
    // Un titre ou un album enregistré avant un échec existe bien : la liste doit se rafraîchir.
    this.dialogRef.close(this.aEnregistre);
  }

  private typeMedia(fichier: File | null): MediaKind {
    if (fichier) {
      const extension = fichier.name.split('.').pop()?.toLowerCase() ?? '';
      return EXTENSIONS_VIDEO.includes(extension) ? 'video' : 'audio';
    }
    return this.data.titre?.mediaType ?? 'audio';
  }
}
