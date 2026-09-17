import { ComponentFixture, TestBed } from '@angular/core/testing';

import { providersDeTest } from '../_core/testing';
import { ConnexionComponent } from './connexion.component';

describe('ConnexionComponent', () => {
  let fixture: ComponentFixture<ConnexionComponent>;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [ConnexionComponent],
      providers: providersDeTest()
    }).compileComponents();

    fixture = TestBed.createComponent(ConnexionComponent);
    fixture.detectChanges();
  });

  it('se cree et se rend sans erreur', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });
});
