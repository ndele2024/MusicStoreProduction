import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthentificationService } from '../_services/authentification.service';

/**
 * Protège les routes réservées aux comptes connectés.
 *
 * La route demandée est passée en paramètre de redirection pour y revenir après connexion.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthentificationService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree(['/login'], { queryParams: { redirect: state.url } });
};

/** Réserve une route aux artistes et aux administrateurs. */
export const artisteGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthentificationService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login'], { queryParams: { redirect: state.url } });
  }
  return auth.isArtiste() ? true : router.createUrlTree(['/home']);
};
