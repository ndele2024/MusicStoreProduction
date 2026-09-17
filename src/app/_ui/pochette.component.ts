import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { MatIcon } from '@angular/material/icon';

/**
 * Pochette générée à partir d'une graine (identifiant ou nom).
 *
 * Aucun titre n'a d'image : un dégradé stable par titre suffit à distinguer les cartes,
 * sans télécharger la moindre image et en restant lisible dans les deux thèmes.
 */
@Component({
  selector: 'app-pochette',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIcon],
  template: `
    <div class="pochette" [style.background]="fond()" [class.arrondie]="arrondie()">
      <mat-icon aria-hidden="true">{{ icone() }}</mat-icon>
    </div>
  `,
  styles: `
    :host {
      display: block;
      aspect-ratio: 1;
      container-type: inline-size;
    }
    .pochette {
      position: relative;
      width: 100%;
      height: 100%;
      display: grid;
      place-items: center;
      border-radius: var(--ms-rayon-m);
      overflow: hidden;
      isolation: isolate;
    }
    .pochette::after {
      content: '';
      position: absolute;
      inset: 0;
      background:
        radial-gradient(circle at 25% 20%, rgb(255 255 255 / 28%), transparent 45%),
        radial-gradient(circle at 80% 90%, rgb(0 0 0 / 22%), transparent 50%);
      z-index: -1;
    }
    .pochette.arrondie {
      border-radius: 50%;
    }
    mat-icon {
      color: rgb(255 255 255 / 92%);
      font-size: clamp(18px, 34cqi, 72px);
      width: auto;
      height: auto;
      filter: drop-shadow(0 2px 6px rgb(0 0 0 / 25%));
    }
  `
})
export class PochetteComponent {
  readonly graine = input.required<string>();
  readonly icone = input<string>('music_note');
  readonly arrondie = input(false);

  readonly fond = computed(() => {
    const teinte = teinteDepuis(this.graine());
    return `linear-gradient(135deg, hsl(${teinte} 72% 58%), hsl(${(teinte + 48) % 360} 68% 38%))`;
  });
}

/** Hachage simple et déterministe : la même graine donne toujours la même couleur. */
export function teinteDepuis(graine: string): number {
  let hash = 0;
  for (let i = 0; i < graine.length; i++) {
    hash = (hash * 31 + graine.charCodeAt(i)) | 0;
  }
  return Math.abs(hash) % 360;
}
