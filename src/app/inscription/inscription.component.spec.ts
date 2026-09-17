import { ComponentFixture, TestBed } from '@angular/core/testing';

import { providersDeTest } from '../_core/testing';
import { InscriptionComponent } from './inscription.component';

describe('InscriptionComponent', () => {
  let fixture: ComponentFixture<InscriptionComponent>;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [InscriptionComponent],
      providers: providersDeTest()
    }).compileComponents();

    fixture = TestBed.createComponent(InscriptionComponent);
    fixture.detectChanges();
  });

  it('se cree et se rend sans erreur', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });
});
