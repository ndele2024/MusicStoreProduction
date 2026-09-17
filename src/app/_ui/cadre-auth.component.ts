import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { MarqueComponent } from './marque.component';

/** Mise en page commune aux écrans de connexion et d'inscription. */
@Component({
  selector: 'app-cadre-auth',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MarqueComponent, MatIcon, MatButtonModule, RouterLink],
  template: `
    <div class="cadre">
      <aside class="vitrine">
        <app-marque [grande]="true" />
        <p class="accroche">La musique de vos artistes, où que vous soyez.</p>
        <ul class="atouts">
          <li><mat-icon>play_circle</mat-icon>Écoutez titres audio et clips vidéo</li>
          <li><mat-icon>star</mat-icon>Notez vos morceaux préférés</li>
          <li><mat-icon>queue_music</mat-icon>Composez vos playlists</li>
          <li><mat-icon>mic</mat-icon>Publiez vos propres titres en tant qu'artiste</li>
        </ul>
        <div class="onde" aria-hidden="true">
          @for (barre of barres; track $index) {
            <span [style.animation-delay]="barre"></span>
          }
        </div>
      </aside>

      <main class="zone">
        <div class="zone-haut">
          <a mat-button routerLink="/home">
            <mat-icon>arrow_back</mat-icon>
            Accueil
          </a>
          <app-marque class="marque-mobile" />
        </div>
        <div class="formulaire">
          <ng-content />
        </div>
      </main>
    </div>
  `,
  styles: `
    :host {
      display: block;
      min-height: 100dvh;
      background: var(--ms-fond);
    }
    .cadre {
      min-height: 100dvh;
      display: grid;
      grid-template-columns: minmax(360px, 1fr) minmax(0, 1.1fr);
    }
    .vitrine {
      position: relative;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      justify-content: center;
      gap: 24px;
      padding: clamp(32px, 6vw, 72px);
      background:
        radial-gradient(120% 90% at 0% 0%, color-mix(in srgb, var(--mat-sys-primary) 45%, transparent), transparent 60%),
        radial-gradient(90% 80% at 100% 100%, color-mix(in srgb, var(--mat-sys-tertiary) 40%, transparent), transparent 60%),
        var(--mat-sys-surface-container);
    }
    .accroche {
      font-size: clamp(1.6rem, 2.6vw, 2.4rem);
      line-height: 1.15;
      font-weight: 700;
      letter-spacing: -0.03em;
      max-width: 420px;
    }
    .atouts {
      display: grid;
      gap: 14px;
      margin: 0;
      padding: 0;
      list-style: none;
      color: var(--ms-texte-doux);
    }
    .atouts li {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .atouts mat-icon {
      color: var(--ms-accent);
    }
    .onde {
      position: absolute;
      inset: auto 0 0 0;
      height: 120px;
      display: flex;
      align-items: flex-end;
      gap: 6px;
      padding: 0 24px;
      opacity: 0.35;
      pointer-events: none;
    }
    .onde span {
      flex: 1;
      height: 100%;
      border-radius: 6px 6px 0 0;
      background: linear-gradient(to top, var(--mat-sys-primary), var(--mat-sys-tertiary));
      transform-origin: bottom;
      animation: onde 1.8s ease-in-out infinite;
    }
    @keyframes onde {
      0%, 100% { transform: scaleY(0.2); }
      50% { transform: scaleY(0.9); }
    }
    .zone {
      display: flex;
      flex-direction: column;
      padding: 16px clamp(16px, 4vw, 48px) 32px;
    }
    .zone-haut {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .marque-mobile {
      display: none;
    }
    .formulaire {
      width: 100%;
      max-width: 440px;
      margin: auto;
      padding: 24px 0;
    }
    @media (max-width: 899.98px) {
      .cadre {
        grid-template-columns: minmax(0, 1fr);
      }
      .vitrine {
        display: none;
      }
      .marque-mobile {
        display: inline-flex;
      }
    }
  `
})
export class CadreAuthComponent {
  readonly barres = Array.from({ length: 18 }, (_, i) => `${-((i * 37) % 18) / 10}s`);
}
