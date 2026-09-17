import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { MatIcon } from '@angular/material/icon';

/**
 * Note sur cinq étoiles.
 *
 * En lecture seule, elle affiche la moyenne à la demi-étoile près. En mode interactif,
 * le survol prévisualise la note et un clic l'envoie ; l'étoile choisie par l'utilisateur
 * reste soulignée.
 */
@Component({
  selector: 'app-etoiles',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIcon],
  template: `
    <span
      class="etoiles"
      [class.interactif]="interactif()"
      [attr.role]="interactif() ? 'radiogroup' : 'img'"
      [attr.aria-label]="libelle()"
      (mouseleave)="survol.set(0)"
    >
      @for (etoile of etoiles(); track etoile.valeur) {
        @if (interactif()) {
          <button
            type="button"
            class="etoile"
            role="radio"
            [attr.aria-checked]="maNote() === etoile.valeur"
            [attr.aria-label]="'Noter ' + etoile.valeur + ' sur 5'"
            [class.choisie]="maNote() === etoile.valeur"
            (mouseenter)="survol.set(etoile.valeur)"
            (focus)="survol.set(etoile.valeur)"
            (blur)="survol.set(0)"
            (click)="choisir($event, etoile.valeur)"
          >
            <mat-icon>{{ etoile.icone }}</mat-icon>
          </button>
        } @else {
          <mat-icon class="etoile">{{ etoile.icone }}</mat-icon>
        }
      }
    </span>
  `,
  styles: `
    :host {
      display: inline-flex;
      vertical-align: middle;
    }
    .etoiles {
      display: inline-flex;
      align-items: center;
      line-height: 1;
    }
    .etoile {
      display: inline-grid;
      place-items: center;
      padding: 0;
      margin: 0;
      border: 0;
      background: none;
      color: var(--ms-etoile);
    }
    mat-icon,
    .etoile.mat-icon {
      font-size: var(--taille-etoile, 16px);
      width: var(--taille-etoile, 16px);
      height: var(--taille-etoile, 16px);
    }
    .interactif .etoile {
      cursor: pointer;
      border-radius: 4px;
      transition: transform var(--ms-transition);
    }
    .interactif .etoile:hover {
      transform: scale(1.2);
    }
    .etoile.choisie {
      box-shadow: inset 0 -2px 0 var(--ms-etoile);
    }
  `
})
export class EtoilesComponent {
  readonly note = input(0);
  readonly maNote = input<number | null | undefined>(null);
  readonly interactif = input(false);
  readonly noter = output<number>();

  readonly survol = signal(0);

  readonly etoiles = computed(() => {
    const valeur = this.survol() || this.note();
    return Array.from({ length: 5 }, (_, i) => {
      const rang = i + 1;
      const icone = valeur >= rang - 0.25 ? 'star' : valeur >= rang - 0.75 ? 'star_half' : 'star_border';
      return { valeur: rang, icone };
    });
  });

  readonly libelle = computed(() =>
    this.note() > 0 ? `Note moyenne ${this.note().toFixed(1)} sur 5` : 'Pas encore noté'
  );

  choisir(event: Event, valeur: number): void {
    // La carte parente est cliquable : la note ne doit pas ouvrir autre chose.
    event.stopPropagation();
    this.noter.emit(valeur);
  }
}
