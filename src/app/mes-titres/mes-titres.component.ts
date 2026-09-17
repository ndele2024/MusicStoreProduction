import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { MatProgressBar } from '@angular/material/progress-bar';
import { MatTooltip } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import { messageErreur } from '../_core/auth.interceptor';
import { LectureEnCoursComponent } from '../_core/lecture-en-cours/lecture-en-cours.component';
import { Album, Titre } from '../_model/model';
import { AlbumService } from '../_services/album.service';
import { AudiolecteurService } from '../_services/audiolecteur.service';
import { AuthentificationService } from '../_services/authentification.service';
import { NotificationService } from '../_services/notification.service';
import { TitreService } from '../_services/titre.service';
import { UserConnectedService } from '../_services/user-connected.service';
import { VuesPipe } from '../_ui/format.pipes';
import { PochetteComponent } from '../_ui/pochette.component';
import { TitreFormData, TitreFormDialogComponent } from './titre-form-dialog.component';

/** Espace artiste : liste, ajout, modification et suppression de ses propres titres. */
@Component({
  selector: 'app-mes-titres',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIcon, MatProgressBar, MatTooltip, LectureEnCoursComponent, PochetteComponent, VuesPipe],
  templateUrl: './mes-titres.component.html',
  styleUrl: './mes-titres.component.scss'
})
export class MesTitresComponent implements OnInit {
  private readonly auth = inject(AuthentificationService);
  private readonly titreService = inject(TitreService);
  private readonly albumService = inject(AlbumService);
  private readonly bibliotheque = inject(UserConnectedService);
  private readonly lecteur = inject(AudiolecteurService);
  private readonly notification = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);

  readonly titres = signal<Titre[]>([]);
  readonly albums = signal<Album[]>([]);
  readonly chargement = signal(true);
  readonly erreur = signal('');
  readonly suppressionEnCours = signal<string | null>(null);

  readonly nombrePublies = computed(() => this.titres().filter((t) => !!t.mediaUrl).length);
  readonly nombreEnAttente = computed(() => this.titres().length - this.nombrePublies());
  readonly vuesTotales = computed(() => this.titres().reduce((total, t) => total + t.vues, 0));

  /** Titres en attente de fichier en tête : ce sont eux qui demandent une action. */
  readonly titresTries = computed(() =>
    [...this.titres()].sort((a, b) => {
      const attenteA = a.mediaUrl ? 1 : 0;
      const attenteB = b.mediaUrl ? 1 : 0;
      return attenteA - attenteB || b.annee - a.annee || a.name.localeCompare(b.name);
    })
  );

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    const artisteId = this.auth.user()?.id;
    if (!artisteId) {
      return;
    }
    this.chargement.set(true);
    this.erreur.set('');
    forkJoin({
      titres: this.titreService.getTitresArtiste(artisteId),
      albums: this.albumService.getAlbumsArtiste(artisteId)
    }).subscribe({
      next: ({ titres, albums }) => {
        this.titres.set(titres);
        this.albums.set(albums);
        this.chargement.set(false);
      },
      error: (erreur) => {
        this.chargement.set(false);
        this.erreur.set(messageErreur(erreur, 'Impossible de charger vos titres'));
      }
    });
  }

  ajouter(): void {
    this.ouvrirFormulaire({ albums: this.albums() });
  }

  modifier(titre: Titre): void {
    this.ouvrirFormulaire({ titre, albums: this.albums() });
  }

  supprimer(titre: Titre): void {
    if (!confirm(`Supprimer définitivement « ${titre.name} » ? Il disparaîtra aussi des playlists et sauvegardes des auditeurs.`)) {
      return;
    }
    this.suppressionEnCours.set(titre.id);
    this.titreService.supprimer(titre.id).subscribe({
      next: () => {
        this.suppressionEnCours.set(null);
        this.titres.update((liste) => liste.filter((t) => t.id !== titre.id));
        this.lecteur.arreterSi(titre.id);
        this.apresModification();
        this.notification.show(`« ${titre.name} » a été supprimé`);
      },
      error: (erreur) => {
        this.suppressionEnCours.set(null);
        this.notification.show(messageErreur(erreur, 'Suppression impossible'));
      }
    });
  }

  ecouter(titre: Titre): void {
    this.lecteur.lire(titre);
    void this.router.navigate(['/lecteur']);
  }

  enLecture(titre: Titre): boolean {
    return this.lecteur.enLecture(titre.id);
  }

  noteAffichee(titre: Titre): string {
    return titre.nombreNotes === 0 ? '–' : `${titre.noteMoyenne.toFixed(1)} (${titre.nombreNotes})`;
  }

  private ouvrirFormulaire(data: TitreFormData): void {
    this.dialog
      .open<TitreFormDialogComponent, TitreFormData, boolean>(TitreFormDialogComponent, {
        data,
        width: '600px',
        maxWidth: '95vw',
        maxHeight: '92vh',
        disableClose: true,
        panelClass: 'dialogue-plein-ecran-mobile'
      })
      .afterClosed()
      .subscribe((modifie) => {
        if (modifie) {
          this.notification.show(data.titre ? 'Titre mis à jour' : 'Titre ajouté');
          this.charger();
          this.apresModification();
        }
      });
  }

  /** Les listes mises en cache ailleurs dans l'application reflètent l'ancien catalogue. */
  private apresModification(): void {
    this.albumService.viderCache();
    this.bibliotheque.chargerBibliotheque();
  }
}
