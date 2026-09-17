import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';

import { messageErreur } from '../_core/auth.interceptor';
import { AuthentificationService } from '../_services/authentification.service';
import { NotificationService } from '../_services/notification.service';

@Component({
  selector: 'app-profil',
  templateUrl: './profil.component.html',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './profil.component.scss',
  imports: [
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    MatIcon,
    RouterLink
  ]
})
export class ProfilComponent {
  private readonly auth = inject(AuthentificationService);
  private readonly notification = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  readonly user = this.auth.user;
  readonly editMode = signal(false);
  readonly enCours = signal(false);
  readonly erreur = signal('');

  readonly initiales = computed(() =>
    (this.user()?.fullName ?? '?')
      .split(' ')
      .filter((mot) => mot.length > 0)
      .map((mot) => mot[0])
      .join('')
      .slice(0, 2)
      .toUpperCase()
  );

  readonly form: FormGroup = this.fb.group({
    fullName: [{ value: '', disabled: true }, [Validators.required]],
    userEmail: [{ value: '', disabled: true }, [Validators.required, Validators.email]]
  });

  constructor() {
    this.remplirDepuisProfil();
  }

  activerEdition(): void {
    this.editMode.set(true);
    this.erreur.set('');
    this.form.enable();
  }

  enregistrerModifications(): void {
    if (this.form.invalid || this.enCours()) {
      return;
    }
    this.enCours.set(true);
    const courant = this.user();
    this.auth
      .updateProfile({
        fullName: this.form.value.fullName,
        userEmail: this.form.value.userEmail,
        age: courant?.age ?? null,
        sex: courant?.sex ?? null,
        avatar: courant?.avatar ?? '',
        preferences: courant?.preferences ?? []
      })
      .subscribe({
        next: () => {
          this.enCours.set(false);
          this.editMode.set(false);
          this.form.disable();
          this.notification.show('Profil mis à jour');
        },
        error: (erreur) => {
          this.enCours.set(false);
          this.erreur.set(messageErreur(erreur, 'Mise à jour impossible'));
        }
      });
  }

  annulerModifications(): void {
    this.remplirDepuisProfil();
    this.erreur.set('');
    this.editMode.set(false);
    this.form.disable();
  }

  private remplirDepuisProfil(): void {
    const user = this.user();
    this.form.patchValue({
      fullName: user?.fullName ?? '',
      userEmail: user?.userEmail ?? ''
    });
  }
}
