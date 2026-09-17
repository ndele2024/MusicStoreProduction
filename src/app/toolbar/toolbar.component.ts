import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  input,
  output,
  signal,
  untracked,
  viewChild
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltip } from '@angular/material/tooltip';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, filter, map, Subject } from 'rxjs';

import { AudiolecteurService } from '../_services/audiolecteur.service';
import { AuthentificationService } from '../_services/authentification.service';
import { UserConnectedService } from '../_services/user-connected.service';
import { MarqueComponent } from '../_ui/marque.component';
import { ModeTheme, ThemeService } from '../_ui/theme.service';

const COULEURS_AVATAR = ['#7c4dff', '#e91e63', '#00897b', '#f4511e', '#3949ab', '#8e24aa'];

@Component({
  selector: 'app-toolbar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule, MatMenuModule, MatDividerModule, MatTooltip, RouterLink, MarqueComponent],
  templateUrl: './toolbar.component.html',
  styleUrl: './toolbar.component.scss'
})
export class ToolbarComponent {
  private readonly auth = inject(AuthentificationService);
  private readonly bibliotheque = inject(UserConnectedService);
  private readonly lecteur = inject(AudiolecteurService);
  private readonly router = inject(Router);
  readonly theme = inject(ThemeService);

  readonly estMobile = input(false);
  readonly basculerMenu = output<void>();

  readonly user = this.auth.user;
  readonly isConnected = this.auth.isAuthenticated;
  readonly isArtiste = this.auth.isArtiste;
  readonly language = this.auth.language;

  /** Sur mobile, la recherche s'ouvre à la place du logo. */
  readonly rechercheOuverte = signal(false);
  readonly texte = signal('');

  private readonly champ = viewChild<ElementRef<HTMLInputElement>>('champRecherche');
  private readonly saisie = new Subject<string>();

  readonly avatarName = computed(() => initiales(this.user()?.fullName));
  readonly avatarColor = computed(() => {
    const nom = this.user()?.fullName ?? '';
    const somme = [...nom].reduce((total, caractere) => total + caractere.charCodeAt(0), 0);
    return COULEURS_AVATAR[somme % COULEURS_AVATAR.length];
  });

  readonly themes: { mode: ModeTheme; libelle: string; icone: string }[] = [
    { mode: 'systeme', libelle: 'Comme le système', icone: 'brightness_auto' },
    { mode: 'clair', libelle: 'Clair', icone: 'light_mode' },
    { mode: 'sombre', libelle: 'Sombre', icone: 'dark_mode' }
  ];

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects)
    ),
    { initialValue: this.router.url }
  );

  constructor() {
    // Le champ reflète la recherche de l'adresse, y compris après un rechargement ou un retour arrière.
    effect(() => {
      const url = this.url();
      untracked(() => {
        if (url.startsWith('/recherche')) {
          const q = this.router.parseUrl(url).queryParamMap.get('q') ?? '';
          // Pendant la frappe, c'est le champ qui fait foi : l'adresse a toujours un temps de retard.
          const enSaisie = document.activeElement === this.champ()?.nativeElement;
          if (!enSaisie && q.trim() !== this.texte().trim()) {
            this.texte.set(q);
          }
        } else {
          this.texte.set('');
          this.rechercheOuverte.set(false);
        }
      });
    });

    this.saisie
      .pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed(inject(DestroyRef)))
      .subscribe((valeur) => this.rechercher(valeur));
  }

  onSaisie(event: Event): void {
    const valeur = (event.target as HTMLInputElement).value;
    this.texte.set(valeur);
    this.saisie.next(valeur);
  }

  valider(event: Event): void {
    event.preventDefault();
    this.rechercher(this.texte());
  }

  effacer(): void {
    this.texte.set('');
    this.saisie.next('');
    this.champ()?.nativeElement.focus();
  }

  ouvrirRecherche(): void {
    this.rechercheOuverte.set(true);
    setTimeout(() => this.champ()?.nativeElement.focus());
  }

  fermerRecherche(): void {
    this.rechercheOuverte.set(false);
    if (this.router.url.startsWith('/recherche')) {
      void this.router.navigate(['/home']);
    }
  }

  changeLanguage(lang: string): void {
    this.auth.setLanguage(lang);
  }

  deconnexion(): void {
    this.lecteur.fermer();
    this.auth.logout();
    this.bibliotheque.vider();
    void this.router.navigate(['/login']);
  }

  private rechercher(valeur: string): void {
    const q = valeur.trim();
    const surRecherche = this.router.url.startsWith('/recherche');
    if (q === '') {
      if (surRecherche) {
        void this.router.navigate(['/home']);
      }
      return;
    }
    // Chaque frappe remplace l'entrée d'historique : le bouton retour quitte la recherche d'un coup.
    void this.router.navigate(['/recherche'], { queryParams: { q }, replaceUrl: surRecherche });
  }
}

function initiales(nom: string | undefined | null): string {
  if (!nom) {
    return '?';
  }
  return nom
    .split(' ')
    .filter((mot) => mot.length > 0)
    .map((mot) => mot[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();
}
