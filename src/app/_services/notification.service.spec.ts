import { TestBed } from '@angular/core/testing';

import { providersDeTest } from '../_core/testing';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: providersDeTest() });
    service = TestBed.inject(NotificationService);
  });

  it('est instancie', () => {
    expect(service).toBeTruthy();
  });
});
