import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EtoilesComponent } from './etoiles.component';
import { DureePipe, VuesPipe } from './format.pipes';
import { teinteDepuis } from './pochette.component';
import { ThemeService } from './theme.service';

describe('Briques visuelles', () => {
  describe('VuesPipe', () => {
    const pipe = new VuesPipe();

    it('abrege les grands nombres et accorde le mot', () => {
      expect(pipe.transform(0)).toBe('0 vue');
      expect(pipe.transform(1)).toBe('1 vue');
      expect(pipe.transform(42)).toBe('42 vues');
      expect(pipe.transform(1_500_000).replace(/\s/g, ' ')).toBe('1,5 M vues');
    });
  });

  describe('DureePipe', () => {
    const pipe = new DureePipe();

    it('formate les secondes en minutes', () => {
      expect(pipe.transform(0)).toBe('0:00');
      expect(pipe.transform(65)).toBe('1:05');
      expect(pipe.transform(Number.NaN)).toBe('0:00');
      expect(pipe.transform(null)).toBe('0:00');
    });
  });

  describe('teinteDepuis', () => {
    it('donne toujours la meme teinte pour la meme graine', () => {
      expect(teinteDepuis('titre-1')).toBe(teinteDepuis('titre-1'));
      expect(teinteDepuis('titre-1')).toBeGreaterThanOrEqual(0);
      expect(teinteDepuis('titre-1')).toBeLessThan(360);
    });
  });

  describe('EtoilesComponent', () => {
    @Component({
      standalone: true,
      imports: [EtoilesComponent],
      template: `<app-etoiles [note]="note()" [interactif]="interactif()" (noter)="recu = $event" />`
    })
    class HoteComponent {
      readonly note = signal(3.5);
      readonly interactif = signal(false);
      recu = 0;
    }

    let fixture: ComponentFixture<HoteComponent>;
    const icones = () =>
      [...(fixture.nativeElement as HTMLElement).querySelectorAll('mat-icon')].map((i) => i.textContent?.trim());

    beforeEach(() => {
      fixture = TestBed.createComponent(HoteComponent);
      fixture.detectChanges();
    });

    it('affiche la moyenne a la demi etoile pres', () => {
      expect(icones()).toEqual(['star', 'star', 'star', 'star_half', 'star_border']);
    });

    it('emet la note choisie en mode interactif', () => {
      fixture.componentInstance.interactif.set(true);
      fixture.detectChanges();
      const boutons = (fixture.nativeElement as HTMLElement).querySelectorAll('button');
      (boutons[3] as HTMLButtonElement).click();
      expect(fixture.componentInstance.recu).toBe(4);
    });
  });

  describe('ThemeService', () => {
    afterEach(() => {
      localStorage.removeItem('musicstore.theme');
      document.documentElement.style.colorScheme = '';
    });

    it('force le mode sombre puis revient au systeme', () => {
      const theme = TestBed.inject(ThemeService);

      theme.choisir('sombre');
      TestBed.flushEffects();
      expect(document.documentElement.style.colorScheme).toBe('dark');
      expect(localStorage.getItem('musicstore.theme')).toBe('sombre');

      theme.choisir('systeme');
      TestBed.flushEffects();
      expect(document.documentElement.style.colorScheme).toBe('light dark');
    });
  });
});
