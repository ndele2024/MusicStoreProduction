import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIcon } from '@angular/material/icon';

/** Logo MusicSpace dessiné en CSS : net à toute taille et lisible en thème sombre. */
@Component({
  selector: 'app-marque',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIcon],
  template: `
    <span class="marque" [class.grande]="grande()">
      <span class="symbole"><mat-icon aria-hidden="true">headphones</mat-icon></span>
      @if (!compacte()) {
        <span class="nom">Music<span>Space</span></span>
      }
    </span>
  `,
  styles: `
    .marque {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      color: var(--ms-texte);
      text-decoration: none;
    }
    .symbole {
      display: grid;
      place-items: center;
      width: 36px;
      height: 36px;
      border-radius: 11px;
      background: linear-gradient(135deg, var(--mat-sys-primary), var(--mat-sys-tertiary));
      box-shadow: 0 4px 14px color-mix(in srgb, var(--mat-sys-primary) 35%, transparent);
    }
    .symbole mat-icon {
      color: var(--mat-sys-on-primary);
      font-size: 22px;
      width: 22px;
      height: 22px;
    }
    .nom {
      font-size: 1.2rem;
      font-weight: 700;
      letter-spacing: -0.03em;
    }
    .nom span {
      color: var(--mat-sys-primary);
    }
    .grande .symbole {
      width: 56px;
      height: 56px;
      border-radius: 17px;
    }
    .grande .symbole mat-icon {
      font-size: 34px;
      width: 34px;
      height: 34px;
    }
    .grande .nom {
      font-size: 1.8rem;
    }
  `
})
export class MarqueComponent {
  readonly compacte = input(false);
  readonly grande = input(false);
}
