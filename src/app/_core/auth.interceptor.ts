import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { environment } from '../../environments/environment';
import { ApiError } from '../_model/model';
import { AuthentificationService } from '../_services/authentification.service';
import { NotificationService } from '../_services/notification.service';

/**
 * Ajoute le jeton porteur aux appels de l'API et traduit les erreurs HTTP en messages lisibles.
 *
 * Un 401 met fin à la session : le jeton est expiré ou révoqué, le garder ne ferait
 * qu'enchaîner les échecs sur les écrans suivants.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthentificationService);
  const router = inject(Router);
  const notification = inject(NotificationService);

  const versApi = request.url.startsWith(environment.apiUrl);
  const token = auth.token();

  const requete =
    versApi && token
      ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : request;

  return next(requete).pipe(
    catchError((erreur: HttpErrorResponse) => {
      if (!versApi) {
        return throwError(() => erreur);
      }

      if (erreur.status === 401 && !request.url.includes('/auth/login')) {
        auth.logout();
        notification.show('Votre session a expiré, veuillez vous reconnecter');
        void router.navigate(['/login']);
      } else if (erreur.status === 0) {
        notification.show('Serveur injoignable, vérifiez que le backend est démarré');
      }

      return throwError(() => erreur);
    })
  );
};

/** Extrait le message métier renvoyé par le backend, avec un repli lisible. */
export function messageErreur(erreur: unknown, repli: string): string {
  if (erreur instanceof HttpErrorResponse) {
    if (erreur.status === 0) {
      return 'Serveur injoignable';
    }
    // Le corps n'est exploité que s'il a bien la forme d'une erreur de l'API : une réponse
    // non JSON produit sinon un message de parsing incompréhensible pour l'utilisateur.
    const corps = erreur.error as Partial<ApiError> | null;
    if (corps && typeof corps === 'object' && typeof corps.status === 'number') {
      const champs = corps.fieldErrors ? Object.values(corps.fieldErrors) : [];
      if (champs.length > 0) {
        return champs[0];
      }
      if (typeof corps.message === 'string' && corps.message !== '') {
        return corps.message;
      }
    }
  }
  return repli;
}
