import { ComponentFixture, TestBed } from '@angular/core/testing';

import { providersDeTest } from '../_core/testing';
import { LecteurAudioComponent } from './lecteur-audio.component';

describe('LecteurAudioComponent', () => {
  let fixture: ComponentFixture<LecteurAudioComponent>;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [LecteurAudioComponent],
      providers: providersDeTest()
    }).compileComponents();

    fixture = TestBed.createComponent(LecteurAudioComponent);
    fixture.detectChanges();
  });

  it('se cree et se rend sans erreur', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });
});
