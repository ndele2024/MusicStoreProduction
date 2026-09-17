import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { messageErreur } from '../_core/auth.interceptor';
import { AuthentificationService } from '../_services/authentification.service';
import { UserConnectedService } from '../_services/user-connected.service';
import { CadreAuthComponent } from '../_ui/cadre-auth.component';

@Component({
  selector: 'app-connexion',
  imports: [MatFormFieldModule, MatButtonModule, MatInputModule, ReactiveFormsModule, MatIcon, RouterLink, CadreAuthComponent],
  templateUrl: './connexion.component.html',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './connexion.component.scss'
})
export class ConnexionComponent {
  private readonly authenticationService = inject(AuthentificationService);
  private readonly bibliotheque = inject(UserConnectedService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly erorMessage = signal('');
  readonly enCours = signal(false);
  readonly hide = signal(true);

  readonly emailFormControl = new FormControl('', [Validators.required, Validators.email]);
  readonly passwordFormControl = new FormControl('', [Validators.required]);

  clickEvent(event: MouseEvent): void {
    this.hide.set(!this.hide());
    event.stopPropagation();
  }

  authenticateUser(): void {
    this.emailFormControl.markAsTouched();
    this.passwordFormControl.markAsTouched();
    if (this.emailFormControl.invalid || this.passwordFormControl.invalid || this.enCours()) {
      return;
    }

    this.enCours.set(true);
    this.erorMessage.set('');
    this.authenticationService
      .login(this.emailFormControl.value ?? '', this.passwordFormControl.value ?? '')
      .subscribe({
        next: () => {
          this.enCours.set(false);
          this.bibliotheque.chargerBibliotheque();
          // Retour à la page qui avait déclenché la redirection, l'accueil par défaut.
          const redirect = this.route.snapshot.queryParamMap.get('redirect') ?? '/home';
          void this.router.navigateByUrl(redirect);
        },
        error: (erreur) => {
          this.enCours.set(false);
          this.erorMessage.set(messageErreur(erreur, 'Email ou mot de passe invalide'));
        }
      });
  }
}
