import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { Router } from '@angular/router';

import { providersDeTest } from '../_core/testing';
import { ToolbarComponent } from './toolbar.component';

describe('ToolbarComponent', () => {
  let fixture: ComponentFixture<ToolbarComponent>;
  let router: Router;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [ToolbarComponent],
      providers: providersDeTest()
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(ToolbarComponent);
    fixture.detectChanges();
  });

  it('propose la connexion a un visiteur', () => {
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Se connecter');
  });

  it('lance la recherche apres la frappe, sans requete a chaque touche', fakeAsync(() => {
    const navigation = spyOn(router, 'navigate').and.resolveTo(true);
    const champ = (fixture.nativeElement as HTMLElement).querySelector('input[type=search]') as HTMLInputElement;

    for (const valeur of ['p', 'po', 'pop']) {
      champ.value = valeur;
      champ.dispatchEvent(new Event('input'));
      tick(100);
    }
    expect(navigation).not.toHaveBeenCalled();

    tick(300);
    expect(navigation).toHaveBeenCalledOnceWith(['/recherche'], { queryParams: { q: 'pop' }, replaceUrl: false });
  }));

  it('affiche le bouton de menu uniquement sur mobile', () => {
    const boutonMenu = () => (fixture.nativeElement as HTMLElement).querySelector('button[aria-label="Ouvrir le menu"]');
    expect(boutonMenu()).toBeNull();

    fixture.componentRef.setInput('estMobile', true);
    fixture.detectChanges();
    expect(boutonMenu()).not.toBeNull();
  });
});
