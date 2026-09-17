import { ComponentFixture, TestBed } from '@angular/core/testing';

import { providersDeTest } from '../_core/testing';
import { Page404Component } from './page404.component';

describe('Page404Component', () => {
  let fixture: ComponentFixture<Page404Component>;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Page404Component],
      providers: providersDeTest()
    }).compileComponents();

    fixture = TestBed.createComponent(Page404Component);
    fixture.detectChanges();
  });

  it('se cree et se rend sans erreur', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });
});
