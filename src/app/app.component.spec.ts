import { ComponentFixture, TestBed } from '@angular/core/testing';

import { providersDeTest } from './_core/testing';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  let fixture: ComponentFixture<AppComponent>;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: providersDeTest()
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
  });

  it('se cree et se rend sans erreur', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });
});
