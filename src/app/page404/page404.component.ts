import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-page404',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonModule, MatIcon],
  template: `
    <div class="page">
      <div class="introuvable">
        <div class="code" aria-hidden="true">
          <span>4</span><mat-icon>album</mat-icon><span>4</span>
        </div>
        <h1 class="titre-page">Cette page a changé de piste</h1>
        <p class="sous-titre-page">L'adresse demandée n'existe pas ou n'est plus disponible.</p>
        <a mat-flat-button routerLink="/home">
          <mat-icon>home</mat-icon>
          Retour à l'accueil
        </a>
      </div>
    </div>
  `,
  styles: `
    .introuvable {
      display: grid;
      justify-items: center;
      gap: 12px;
      padding: clamp(40px, 10vh, 96px) 16px;
      text-align: center;
    }
    .code {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: clamp(5rem, 18vw, 9rem);
      font-weight: 700;
      line-height: 1;
      letter-spacing: -0.05em;
      background: linear-gradient(135deg, var(--mat-sys-primary), var(--mat-sys-tertiary));
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
    }
    .code mat-icon {
      font-size: 0.85em;
      width: 1em;
      height: 1em;
      color: var(--mat-sys-primary);
      animation: tourne 3s linear infinite;
    }
    @keyframes tourne {
      to {
        transform: rotate(360deg);
      }
    }
    a {
      margin-top: 12px;
    }
  `
})
export class Page404Component {}
