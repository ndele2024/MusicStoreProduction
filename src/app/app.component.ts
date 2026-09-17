import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, computed, effect, HostListener, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRouteSnapshot, NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';

import { AudiolecteurService } from './_services/audiolecteur.service';
import { AuthentificationService } from './_services/authentification.service';
import { UserConnectedService } from './_services/user-connected.service';
import { ThemeService } from './_ui/theme.service';
import { DonneesRoute } from './app.routes';
import { MenuLateralComponent } from './menu-lateral/menu-lateral.component';
import { MiniLecteurComponent } from './mini-lecteur/mini-lecteur.component';
import { ToolbarComponent } from './toolbar/toolbar.component';

/** En dessous de cette largeur, le menu devient un tiroir ouvert à la demande. */
const REQUETE_MOBILE = '(max-width: 959.98px)';

/**
 * Coquille de l'application : barre du haut, menu, contenu et mini-lecteur.
 * Les pages de connexion et d'inscription s'affichent en plein écran, sans coquille.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ToolbarComponent, MenuLateralComponent, MiniLecteurComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent implements OnInit {
  private readonly auth = inject(AuthentificationService);
  private readonly bibliotheque = inject(UserConnectedService);
  private readonly lecteur = inject(AudiolecteurService);
  private readonly router = inject(Router);
  // Injecté pour appliquer le thème mémorisé dès le démarrage.
  private readonly theme = inject(ThemeService);

  readonly estMobile = toSignal(
    inject(BreakpointObserver).observe(REQUETE_MOBILE).pipe(map((etat) => etat.matches)),
    { initialValue: typeof matchMedia !== 'undefined' && matchMedia(REQUETE_MOBILE).matches }
  );

  readonly menuOuvert = signal(false);

  private readonly route = toSignal(
    this.router.events.pipe(
      filter((evenement): evenement is NavigationEnd => evenement instanceof NavigationEnd),
      map((evenement) => ({ url: evenement.urlAfterRedirects, donnees: donneesFeuille(this.router.routerState.snapshot.root) }))
    ),
    { initialValue: { url: this.router.url, donnees: {} as DonneesRoute } }
  );

  readonly pleinEcran = computed(() => !!this.route().donnees.pleinEcran);
  private readonly surLecteur = computed(() => this.route().url.startsWith('/lecteur'));

  readonly miniLecteurVisible = computed(
    () => !!this.lecteur.currentTrack() && !this.surLecteur() && !this.pleinEcran()
  );

  constructor() {
    // Le tiroir se referme après chaque navigation, et n'a pas de sens sur grand écran.
    effect(() => {
      this.route();
      this.estMobile();
      this.menuOuvert.set(false);
    });
  }

  ngOnInit(): void {
    if (!this.auth.isAuthenticated()) {
      return;
    }
    // Le profil gardé en cache peut avoir changé depuis la dernière visite ;
    // un jeton devenu invalide est détecté ici plutôt qu'au premier clic.
    this.auth.refreshProfile().subscribe({ error: () => undefined });
    this.bibliotheque.chargerBibliotheque();
  }

  @HostListener('document:keydown.escape')
  fermerMenu(): void {
    this.menuOuvert.set(false);
  }
}

function donneesFeuille(racine: ActivatedRouteSnapshot): DonneesRoute {
  let courant = racine;
  while (courant.firstChild) {
    courant = courant.firstChild;
  }
  return courant.data as DonneesRoute;
}
