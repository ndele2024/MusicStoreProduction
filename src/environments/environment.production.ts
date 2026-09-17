/** En production, nginx relaie /api vers le conteneur backend (voir nginx.conf). */
export const environment = {
  production: true,
  apiUrl: '/api'
};
