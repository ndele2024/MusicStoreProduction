# MusicStore

Plateforme de publication et d'écoute de titres audio et vidéo : les artistes publient
albums et titres, les auditeurs connectés les lisent, les notent de 1 à 5 étoiles et
organisent leurs sauvegardes et playlists. Vues et notes sont comptabilisées côté serveur.

- **Frontend** : Angular 19 et Material 3 (signaux, routes chargées à la demande, thème clair et sombre)
- **Backend** : Spring Boot 3.4, Java 21, Spring Security avec jetons JWT
- **Base de données** : PostgreSQL 16, schéma géré par Flyway
- **Conteneurs** : Docker Compose

## Démarrage rapide

### 1. Base de données et API

```bash
docker compose up -d --build db api
```

L'API écoute sur `http://localhost:8080`, la documentation interactive est sur
`http://localhost:8080/swagger-ui.html`.

Pour ne lancer que PostgreSQL et exécuter l'API depuis un IDE :

```bash
docker compose up -d db
```

```bash
mvn -f backend/pom.xml spring-boot:run
```

### 2. Frontend

```bash
npm install
```

```bash
npm start
```

L'application est servie sur `http://localhost:4200`. Le serveur de développement relaie
`/api` vers `http://localhost:8080` grâce à `proxy.conf.json` : aucune configuration CORS
n'est nécessaire en développement.

### 3. Toute la pile en conteneurs

```bash
docker compose --profile full up -d --build
```

Le frontend est alors servi par nginx sur `http://localhost:8081`, qui relaie lui-même
`/api` vers le conteneur de l'API.

## Comptes de démonstration

Au premier démarrage sur une base vide, un jeu de données est inséré. Tous les comptes
partagent le mot de passe `Password1!`.

| Email | Rôle |
| --- | --- |
| `admin@musicstore.local` | admin |
| `ed@example.com` | artiste |
| `weeknd@example.com` | artiste |
| `jean@example.com` | auditeur |
| `alice@example.com` | auditeur |

Le titre « Shape of You » est livré avec un fichier audio réel : il est lisible
immédiatement.

Un titre n'apparaît dans le catalogue public, la recherche et les albums qu'une fois son
fichier déposé. Son artiste le voit dès sa création via `/api/titres/artiste/{id}`. Un album
sans aucun titre lisible n'est pas listé.

### Espace artiste

Un compte artiste accède à « Gérer vos titres » depuis le menu. La page liste ses titres,
y compris ceux encore sans fichier, et permet d'en ajouter, de les modifier, d'en remplacer
le fichier et de les supprimer. Un album peut être créé directement depuis le formulaire.

### Catalogue d'exemple

```bash
python scripts/seed-demo-media.py
```

Le script compose et synthétise localement 7 morceaux audio et 1 clip vidéo originaux, puis
les publie via l'API sous deux artistes fictifs, `nova@musicstore.local` et
`ondes@musicstore.local`, avec le mot de passe commun. Quelques écoutes et notes sont
ajoutées par les auditeurs de démonstration. Le script est rejouable sans doublon.
Il requiert `numpy`, et `imageio-ffmpeg` avec `opencv-python` pour les MP3 et la vidéo.

Pour désactiver l'insertion : `MUSICSTORE_SEED_ENABLED=false`.

## Configuration

Copiez `.env.example` en `.env` avant un déploiement. Les variables importantes :

| Variable | Rôle |
| --- | --- |
| `MUSICSTORE_JWT_SECRET` | Clé de signature des jetons, 32 octets minimum. **À changer en production.** |
| `MUSICSTORE_JWT_EXPIRATION` | Durée de validité d'un jeton, en secondes (12 h par défaut) |
| `MUSICSTORE_CORS_ORIGINS` | Origines autorisées, motifs acceptés (`http://localhost:[*]` par défaut) |
| `MUSICSTORE_STORAGE_LOCATION` | Répertoire de stockage des fichiers média |
| `MUSICSTORE_SEED_ENABLED` | Insertion du jeu de démonstration sur une base vide |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Connexion PostgreSQL |

## API

Toutes les routes sont préfixées par `/api`.

| Méthode | Route | Accès |
| --- | --- | --- |
| `POST` | `/auth/register`, `/auth/login` | public |
| `GET` | `/auth/email-disponible?email=` | public |
| `GET` `PUT` | `/auth/me` | connecté |
| `GET` | `/titres`, `/titres/{id}`, `/titres/artiste/{id}` | public |
| `POST` `PUT` `DELETE` | `/titres`, `/titres/{id}` | artiste propriétaire ou admin |
| `POST` | `/titres/{id}/media` | artiste propriétaire (multipart) |
| `POST` | `/titres/{id}/lectures` | connecté |
| `PUT` | `/titres/{id}/note` | connecté |
| `GET` | `/albums`, `/albums/{id}`, `/albums/{id}/titres`, `/albums/artiste/{id}` | public, brouillons visibles par leur artiste |
| `POST` `PUT` `DELETE` | `/albums`, `/albums/{id}` | artiste propriétaire ou admin |
| `GET` `POST` `PUT` `DELETE` | `/playlists...` | propriétaire uniquement |
| `GET` `POST` `DELETE` | `/me/sauvegardes...`, `DELETE /me/sauvegardes` pour tout vider | connecté |
| `GET` `DELETE` | `/me/historique`, `/me/historique/titres/{id}` : lecture, retrait d'un titre, vidage complet | connecté, les vues restent comptées |
| `GET` | `/media/{titreId}` | connecté, requêtes `Range` gérées |

La liste des titres est paginée : `?q=`, `?page=`, `?size=`, `?sort=recent|populaire|note|nom`.

### Sécurité

- Mots de passe hachés en BCrypt, jamais renvoyés par l'API.
- Jetons JWT signés en HS256, session sans état côté serveur.
- Autorisations par rôle sur la publication, par propriétaire sur les playlists et les titres.
- Les fichiers déposés sont renommés et vérifiés par extension ; les chemins sortant du
  répertoire de stockage sont rejetés.
- Le streaming accepte le jeton en paramètre de requête, car les balises `audio` et `video`
  ne peuvent pas poser d'en-tête `Authorization`. Cette tolérance est limitée à cette route.

## Tests

Backend, 82 tests unitaires et d'intégration :

```bash
mvn -f backend/pom.xml test
```

Les tests d'intégration démarrent le contexte Spring complet sur H2 en mode PostgreSQL et
rejouent les migrations Flyway de production, avec `ddl-auto: validate` : une divergence
entre le schéma SQL et le mapping JPA fait échouer la suite. Aucun Docker n'est requis.

Frontend, 69 tests :

```bash
npm run test:ci
```

## Interface

- **Responsive.** Au-dessus de 960 px, le menu est une colonne fixe. En dessous, il devient un
  tiroir ouvert par le bouton de menu, et la recherche se déplie à la place du logo.
- **Mini-lecteur.** Dès qu'un titre est sélectionné, un lecteur compact reste en bas de toutes
  les pages : lecture et pause, titres précédent et suivant, arrêt, volume, barre d'avancement
  déplaçable. Il se masque sur la page lecteur, qui affiche la même piste en grand, et se ferme
  avec sa croix.
- **Thème.** Clair, sombre ou calqué sur le système, au choix dans le menu des préférences.
- **Adresses par section.** `/home`, `/nouveautes`, `/recherche?q=…`, `/historique`,
  `/sauvegardes`, `/playlists`, `/lecteur`, `/mes-titres`, `/profil`. Le bouton retour et le
  rechargement de page fonctionnent partout.
- **Pochettes générées.** Chaque titre, album ou playlist reçoit un dégradé stable calculé à
  partir de son identifiant : aucune image n'est téléchargée pour afficher le catalogue.

## Structure

```
backend/                  API Spring Boot
  src/main/java/com/musicstore/
    config/               sécurité, CORS, OpenAPI, jeu de démonstration
    domain/               entités JPA
    dto/                  contrats d'entrée et de sortie
    repository/           accès aux données
    security/             JWT, filtre d'authentification, gestion des refus
    service/              règles métier
    web/                  contrôleurs REST et gestion des erreurs
  src/main/resources/db/migration/   migrations Flyway
src/app/                  application Angular
  _core/                  intercepteur HTTP, animation de lecture et utilitaires de test
  _ui/                    briques visuelles : pochette, étoiles, marque, thème, formats
  _guard/                 gardes de routes
  _model/                 types partagés avec l'API
  _services/              appels HTTP et état applicatif
docker-compose.yml        PostgreSQL, API, et frontend nginx (profil « full »)
```

## Notes d'environnement

**Port 5432 déjà pris.** Un PostgreSQL installé sur la machine occupe ce port et gagne
sur le conteneur pour les connexions à `localhost`, ce qui produit une erreur
`password authentication failed`. Publier le conteneur sur un autre port règle le conflit :

```bash
POSTGRES_PORT=55432 docker compose up -d db
```

L'API doit alors pointer vers ce port : `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:55432/musicstore`.
Entre conteneurs, le service `api` joint `db` sur le réseau interne : ce réglage ne le concerne pas.

**Échec Maven `PKIX path building failed`.** Un proxy TLS intercepte les téléchargements.
Sur Windows, faire confiance au magasin de certificats du système suffit :

```bash
MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT" mvn -f backend/pom.xml test
```

**Port 4200 déjà pris.** `ng serve --port 4300` fonctionne sans autre réglage : le proxy de
développement vise toujours `http://localhost:8080` et la configuration CORS accepte
n'importe quel port de `localhost`.
