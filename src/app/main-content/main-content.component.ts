import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, map, of, startWith, switchMap } from 'rxjs';

import { messageErreur } from '../_core/auth.interceptor';
import { Album, Titre } from '../_model/model';
import { AlbumService } from '../_services/album.service';
import { AudiolecteurService } from '../_services/audiolecteur.service';
import { AuthentificationService } from '../_services/authentification.service';
import { DialoguesService } from '../_services/dialogues.service';
import { NotificationService } from '../_services/notification.service';
import { TitreService } from '../_services/titre.service';
import { UserConnectedService } from '../_services/user-connected.service';
import { Section } from '../app.routes';
import { MusicCardComponent } from '../music-card/music-card.component';
import { PlaylistCardComponent } from '../playlist-card/playlist-card.component';

/** Cartes affichées par rayon sur l'accueil. */
const TAILLE_RAYON = 12;
/** Cartes affichées sur la page Nouveautés. */
const TAILLE_GRILLE = 48;

interface EtatRecherche {
  chargement: boolean;
  titres: Titre[];
}

/**
 * Page de contenu : accueil, nouveautés, recherche et bibliothèque personnelle.
 * La section vient des données de la route, la recherche du paramètre « q ».
 */
@Component({
  selector: 'app-main-content',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIcon, RouterLink, MusicCardComponent, PlaylistCardComponent],
  templateUrl: './main-content.component.html',
  styleUrl: './main-content.component.scss'
})
export class MainContentComponent implements OnInit {
  private readonly titreService = inject(TitreService);
  private readonly albumService = inject(AlbumService);
  private readonly bibliotheque = inject(UserConnectedService);
  private readonly auth = inject(AuthentificationService);
  private readonly lecteur = inject(AudiolecteurService);
  private readonly notification = inject(NotificationService);
  private readonly dialogues = inject(DialoguesService);
  private readonly router = inject(Router);

  readonly section = input<Section>('accueil');
  readonly q = input<string | undefined>('');

  readonly nouveautes = signal<Titre[]>([]);
  readonly populaires = signal<Titre[]>([]);
  readonly albums = signal<Album[]>([]);
  readonly chargement = signal(true);
  readonly videEnCours = signal(false);

  readonly sauvegardes = this.bibliotheque.sauvegardes;
  readonly playlists = this.bibliotheque.playlists;
  readonly historique = this.bibliotheque.historique;
  readonly user = this.auth.user;

  readonly squelettes = Array.from({ length: 6 });

  readonly salutation = computed(() => {
    const heure = new Date().getHours();
    const moment = heure < 5 || heure >= 18 ? 'Bonsoir' : 'Bonjour';
    const prenom = this.user()?.fullName?.split(' ')[0];
    return prenom ? `${moment}, ${prenom}` : `${moment} !`;
  });

  /** Genres repérés dans le catalogue, proposés comme raccourcis de recherche. */
  readonly genres = computed(() => {
    const vus = new Set<string>();
    for (const titre of [...this.populaires(), ...this.nouveautes()]) {
      if (titre.genre) {
        vus.add(titre.genre);
      }
    }
    return [...vus].slice(0, 12);
  });

  readonly requete = computed(() => (this.q() ?? '').trim());

  /** Recherche déléguée au serveur ; switchMap annule la requête précédente. */
  private readonly recherche = toSignal(
    toObservable(this.requete).pipe(
      debounceTime(50),
      distinctUntilChanged(),
      switchMap((q) =>
        q === ''
          ? of<EtatRecherche>({ chargement: false, titres: [] })
          : this.titreService.getTitres({ q, size: 60 }).pipe(
              map((page): EtatRecherche => ({ chargement: false, titres: page.content })),
              catchError(() => of<EtatRecherche>({ chargement: false, titres: [] })),
              startWith<EtatRecherche>({ chargement: true, titres: [] })
            )
      )
    ),
    { initialValue: { chargement: false, titres: [] } as EtatRecherche }
  );

  readonly resultatsTitres = computed(() => this.recherche().titres);
  readonly rechercheEnCours = computed(() => this.recherche().chargement);

  readonly resultatsAlbums = computed<Album[]>(() => {
    const q = this.requete().toLowerCase();
    return q === ''
      ? []
      : this.albums().filter(
          (album) =>
            album.name.toLowerCase().includes(q) ||
            String(album.annee).includes(q) ||
            (album.artisteNom ?? '').toLowerCase().includes(q)
        );
  });

  readonly resultatsPlaylists = computed(() => {
    const q = this.requete().toLowerCase();
    return q === '' ? [] : this.playlists().filter((playlist) => playlist.nom.toLowerCase().includes(q));
  });

  readonly aucunResultat = computed(
    () =>
      !this.rechercheEnCours() &&
      this.resultatsTitres().length === 0 &&
      this.resultatsAlbums().length === 0 &&
      this.resultatsPlaylists().length === 0
  );

  ngOnInit(): void {
    switch (this.section()) {
      case 'accueil':
      case 'recherche':
        this.chargerCatalogue(TAILLE_RAYON);
        break;
      case 'nouveautes':
        this.chargerCatalogue(TAILLE_GRILLE);
        break;
      case 'historique':
        // L'historique change à chaque écoute : il est rechargé à chaque ouverture.
        this.bibliotheque.chargerHistorique();
        this.chargement.set(false);
        break;
      default:
        this.chargement.set(false);
    }
  }

  toutEcouter(titres: Titre[]): void {
    if (!this.auth.isAuthenticated()) {
      void this.router.navigate(['/login']);
      return;
    }
    if (titres.length === 0) {
      return;
    }
    this.lecteur.lire(titres[0], titres);
    void this.router.navigate(['/lecteur']);
  }

  nouvellePlaylist(): void {
    void this.dialogues.nouvellePlaylist();
  }

  rechercherGenre(genre: string): void {
    void this.router.navigate(['/recherche'], { queryParams: { q: genre } });
  }

  viderSauvegardes(): void {
    if (!confirm('Retirer tous les titres de vos sauvegardes ?')) {
      return;
    }
    this.videEnCours.set(true);
    this.bibliotheque.viderSauvegardes().subscribe({
      next: () => {
        this.videEnCours.set(false);
        this.notification.show('Sauvegardes vidées');
      },
      error: (erreur) => {
        this.videEnCours.set(false);
        this.notification.show(messageErreur(erreur, 'Impossible de vider les sauvegardes'));
      }
    });
  }

  viderHistorique(): void {
    if (!confirm('Effacer tout votre historique de lecture ?')) {
      return;
    }
    this.videEnCours.set(true);
    this.bibliotheque.viderHistorique().subscribe({
      next: () => {
        this.videEnCours.set(false);
        this.notification.show('Historique vidé');
      },
      error: (erreur) => {
        this.videEnCours.set(false);
        this.notification.show(messageErreur(erreur, "Impossible de vider l'historique"));
      }
    });
  }

  private chargerCatalogue(taille: number): void {
    this.titreService.getTitres({ sort: 'recent', size: taille }).subscribe({
      next: (page) => {
        this.nouveautes.set(page.content);
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false)
    });
    if (this.section() !== 'nouveautes') {
      this.titreService.getTitres({ sort: 'populaire', size: TAILLE_RAYON }).subscribe({
        next: (page) => this.populaires.set(page.content),
        error: () => this.populaires.set([])
      });
      this.albumService.getAlbums().subscribe({
        next: (albums) => this.albums.set(albums),
        error: () => this.albums.set([])
      });
    }
  }
}
