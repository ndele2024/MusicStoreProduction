import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatRadioButton, MatRadioGroup } from '@angular/material/radio';
import { Router, RouterLink } from '@angular/router';

import { messageErreur } from '../_core/auth.interceptor';
import { Role } from '../_model/model';
import { AuthentificationService } from '../_services/authentification.service';
import { NotificationService } from '../_services/notification.service';
import { UserConnectedService } from '../_services/user-connected.service';
import { CadreAuthComponent } from '../_ui/cadre-auth.component';

@Component({
  selector: 'app-inscription',
  imports: [
    MatFormFieldModule,
    MatButtonModule,
    MatInputModule,
    ReactiveFormsModule,
    MatIcon,
    RouterLink,
    FormsModule,
    MatRadioGroup,
    MatRadioButton,
    CadreAuthComponent
  ],
  templateUrl: './inscription.component.html',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './inscription.component.scss'
})
export class InscriptionComponent {
  private readonly auth = inject(AuthentificationService);
  private readonly bibliotheque = inject(UserConnectedService);
  private readonly notification = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly step = signal(1);
  readonly erreur = signal('');
  readonly enCours = signal(false);
  readonly masquerMotDePasse = signal(true);

  readonly etapes = ['Email', 'Mot de passe', 'Profil', 'Confirmation'];

  readonly inscriptionForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    // Même règle que la validation serveur : 8 caractères, une majuscule, un chiffre ou un caractère spécial.
    password: ['', [Validators.required, Validators.minLength(8), Validators.pattern(/^(?=.*[A-Z])(?=.*[^A-Za-z]).{8,}$/)]],
    name: ['', Validators.required],
    username: ['', [Validators.required, Validators.minLength(3)]],
    birthdate: ['', Validators.required],
    gender: ['', Validators.required],
    role: ['auditeur', Validators.required]
  });

  isStepValid(): boolean {
    switch (this.step()) {
      case 1:
        return this.inscriptionForm.get('email')?.valid ?? false;
      case 2:
        return this.inscriptionForm.get('password')?.valid ?? false;
      case 3:
        return (
          (this.inscriptionForm.get('name')?.valid &&
            this.inscriptionForm.get('username')?.valid &&
            this.inscriptionForm.get('birthdate')?.valid &&
            this.inscriptionForm.get('gender')?.valid) ??
          false
        );
      case 4:
        return this.inscriptionForm.valid;
      default:
        return false;
    }
  }

  nextStep(): void {
    if (this.step() === 1) {
      const email = this.inscriptionForm.get('email')?.value as string;
      if (!email) {
        return;
      }
      this.erreur.set('');
      this.auth.emailExists(email).subscribe({
        next: ({ exists }) => {
          if (exists) {
            this.erreur.set('Cet email est déjà utilisé, veuillez en choisir un autre');
          } else {
            this.step.set(2);
          }
        },
        error: (erreur) => this.erreur.set(messageErreur(erreur, "Vérification de l'email impossible"))
      });
      return;
    }

    if (this.isStepValid() && this.step() < 4) {
      this.erreur.set('');
      this.step.update((valeur) => valeur + 1);
    }
  }

  prevStep(): void {
    if (this.step() > 1) {
      this.erreur.set('');
      this.step.update((valeur) => valeur - 1);
    }
  }

  submitForm(): void {
    if (this.inscriptionForm.invalid || this.enCours()) {
      return;
    }

    const age = this.calculerAge(this.inscriptionForm.value.birthdate);
    if (age === null) {
      this.erreur.set('La date de naissance fournie est invalide');
      return;
    }

    this.enCours.set(true);
    this.erreur.set('');
    this.auth
      .register({
        fullName: this.inscriptionForm.value.name,
        age,
        sex: this.inscriptionForm.value.gender,
        userName: this.inscriptionForm.value.username,
        userEmail: this.inscriptionForm.value.email,
        password: this.inscriptionForm.value.password,
        role: this.inscriptionForm.value.role as Role
      })
      .subscribe({
        next: () => {
          this.enCours.set(false);
          this.bibliotheque.chargerBibliotheque();
          this.notification.show('Bienvenue sur MusicSpace');
          void this.router.navigate(['/home']);
        },
        error: (erreur) => {
          this.enCours.set(false);
          this.erreur.set(messageErreur(erreur, "L'inscription a échoué"));
        }
      });
  }

  /** Règles du mot de passe, vérifiées à chaque frappe pour guider la saisie. */
  reglesMotDePasse(): { libelle: string; ok: boolean }[] {
    const valeur: string = this.inscriptionForm.get('password')?.value ?? '';
    return [
      { libelle: '8 caractères minimum', ok: valeur.length >= 8 },
      { libelle: 'Une majuscule', ok: /[A-Z]/.test(valeur) },
      { libelle: 'Un chiffre ou un caractère spécial', ok: /[^A-Za-z]/.test(valeur) }
    ];
  }

  /** Âge révolu, ou `null` si la date saisie n'est pas exploitable. */
  private calculerAge(dateTexte: string | null | undefined): number | null {
    if (!dateTexte) {
      return null;
    }
    const naissance = new Date(dateTexte);
    if (Number.isNaN(naissance.getTime())) {
      return null;
    }
    const aujourdHui = new Date();
    let age = aujourdHui.getFullYear() - naissance.getFullYear();
    const moisEcoules = aujourdHui.getMonth() - naissance.getMonth();
    if (moisEcoules < 0 || (moisEcoules === 0 && aujourdHui.getDate() < naissance.getDate())) {
      age -= 1;
    }
    return age >= 1 && age <= 130 ? age : null;
  }
}
