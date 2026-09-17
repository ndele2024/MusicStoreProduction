import { DOCUMENT } from '@angular/common';
import { computed, effect, inject, Injectable, signal } from '@angular/core';

export type ModeTheme = 'systeme' | 'clair' | 'sombre';

const CLE = 'musicstore.theme';

/**
 * Choix du thème clair, sombre ou calqué sur le système.
 *
 * Le thème Material utilise light-dark() : il suffit de forcer la propriété color-scheme
 * de la racine pour basculer toutes les couleurs, sans recharger de feuille de style.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly _mode = signal<ModeTheme>(lireMode());

  readonly mode = this._mode.asReadonly();
  readonly icone = computed(() => ({ systeme: 'brightness_auto', clair: 'light_mode', sombre: 'dark_mode' })[this._mode()]);

  constructor() {
    effect(() => {
      const mode = this._mode();
      this.document.documentElement.style.colorScheme =
        mode === 'clair' ? 'light' : mode === 'sombre' ? 'dark' : 'light dark';
    });
  }

  choisir(mode: ModeTheme): void {
    this._mode.set(mode);
    try {
      localStorage.setItem(CLE, mode);
    } catch {
      // Stockage indisponible : le choix vaut pour la session.
    }
  }
}

function lireMode(): ModeTheme {
  try {
    const valeur = localStorage.getItem(CLE);
    return valeur === 'clair' || valeur === 'sombre' ? valeur : 'systeme';
  } catch {
    return 'systeme';
  }
}
