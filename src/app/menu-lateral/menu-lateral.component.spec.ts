import { ComponentFixture, TestBed } from '@angular/core/testing';

import { providersDeTest } from '../_core/testing';
import { MenuLateralComponent } from './menu-lateral.component';

describe('MenuLateralComponent', () => {
  let fixture: ComponentFixture<MenuLateralComponent>;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [MenuLateralComponent],
      providers: providersDeTest()
    }).compileComponents();

    fixture = TestBed.createComponent(MenuLateralComponent);
    fixture.detectChanges();
  });

  it('se cree et se rend sans erreur', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });
});
