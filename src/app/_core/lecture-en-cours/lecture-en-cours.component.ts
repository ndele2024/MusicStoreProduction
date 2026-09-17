import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Petit égaliseur animé affiché à la place du bouton lecture d'un titre qui joue. */
@Component({
  selector: 'app-lecture-en-cours',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="egaliseur" role="img" aria-label="Lecture en cours">
      <span></span><span></span><span></span><span></span>
    </span>
  `,
  styles: `
    :host {
      display: inline-flex;
      color: var(--ms-accent);
    }
    .egaliseur {
      display: inline-flex;
      align-items: flex-end;
      justify-content: center;
      gap: 2px;
      width: 24px;
      height: 24px;
      padding: 3px 0;
    }
    .egaliseur span {
      width: 4px;
      height: 100%;
      border-radius: 2px;
      background: currentColor;
      transform-origin: bottom;
      animation: rebond 900ms ease-in-out infinite;
    }
    .egaliseur span:nth-child(2) { animation-delay: -300ms; }
    .egaliseur span:nth-child(3) { animation-delay: -600ms; }
    .egaliseur span:nth-child(4) { animation-delay: -150ms; }
    @keyframes rebond {
      0%, 100% { transform: scaleY(0.25); }
      50% { transform: scaleY(1); }
    }
    @media (prefers-reduced-motion: reduce) {
      .egaliseur span { animation: none; transform: scaleY(0.7); }
    }
  `
})
export class LectureEnCoursComponent {}
