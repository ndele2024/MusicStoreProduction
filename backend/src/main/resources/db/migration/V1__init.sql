-- Schema initial de MusicStore.
-- Volontairement ecrit en SQL portable : la meme migration tourne sur PostgreSQL
-- et sur H2 en mode PostgreSQL, ce qui permet aux tests de valider le mapping JPA.

create table users (
    id          uuid         not null,
    full_name   varchar(120) not null,
    age         integer,
    sex         varchar(30),
    user_name   varchar(60)  not null,
    user_email  varchar(180) not null,
    password    varchar(100) not null,
    role        varchar(20)  not null,
    avatar      varchar(255),
    created_at  timestamp(6) with time zone not null,
    constraint pk_users primary key (id),
    constraint uk_users_user_name unique (user_name),
    constraint uk_users_user_email unique (user_email)
);

create table user_preferences (
    user_id    uuid not null,
    preference varchar(80),
    constraint fk_user_preferences_user foreign key (user_id) references users (id) on delete cascade
);

create index idx_user_preferences_user on user_preferences (user_id);

create table albums (
    id         uuid         not null,
    name       varchar(160) not null,
    annee      integer      not null,
    artist_id  uuid,
    created_at timestamp(6) with time zone not null,
    constraint pk_albums primary key (id),
    constraint fk_albums_artist foreign key (artist_id) references users (id) on delete set null
);

create index idx_albums_artist on albums (artist_id);

create table titres (
    id           uuid         not null,
    name         varchar(160) not null,
    description  varchar(1000),
    genre        varchar(60),
    annee        integer      not null,
    nom_fichier  varchar(255),
    nom_image    varchar(255),
    media_type   varchar(10)  not null,
    storage_key  varchar(255),
    content_type varchar(100),
    vues         bigint       not null default 0,
    somme_notes  bigint       not null default 0,
    nombre_notes integer      not null default 0,
    artist_id    uuid,
    album_id     uuid,
    created_at   timestamp(6) with time zone not null,
    constraint pk_titres primary key (id),
    constraint fk_titres_artist foreign key (artist_id) references users (id) on delete set null,
    constraint fk_titres_album foreign key (album_id) references albums (id) on delete set null
);

create index idx_titres_artist on titres (artist_id);
create index idx_titres_album on titres (album_id);
-- Les listes « nouveautes » et « populaires » de la page d'accueil trient sur ces colonnes.
create index idx_titres_annee on titres (annee desc);
create index idx_titres_vues on titres (vues desc);

create table ratings (
    id         uuid    not null,
    user_id    uuid    not null,
    titre_id   uuid    not null,
    valeur     integer not null,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    constraint pk_ratings primary key (id),
    constraint uk_rating_user_titre unique (user_id, titre_id),
    constraint ck_ratings_valeur check (valeur between 1 and 5),
    constraint fk_ratings_user foreign key (user_id) references users (id) on delete cascade,
    constraint fk_ratings_titre foreign key (titre_id) references titres (id) on delete cascade
);

create index idx_ratings_titre on ratings (titre_id);

create table playlists (
    id         uuid         not null,
    nom        varchar(120) not null,
    owner_id   uuid         not null,
    created_at timestamp(6) with time zone not null,
    constraint pk_playlists primary key (id),
    constraint uk_playlist_owner_nom unique (owner_id, nom),
    constraint fk_playlists_owner foreign key (owner_id) references users (id) on delete cascade
);

create table playlist_titres (
    playlist_id uuid    not null,
    titre_id    uuid    not null,
    ordre       integer not null,
    constraint pk_playlist_titres primary key (playlist_id, ordre),
    constraint fk_playlist_titres_playlist foreign key (playlist_id) references playlists (id) on delete cascade,
    constraint fk_playlist_titres_titre foreign key (titre_id) references titres (id) on delete cascade
);

create index idx_playlist_titres_titre on playlist_titres (titre_id);

create table favorites (
    id         uuid not null,
    user_id    uuid not null,
    titre_id   uuid not null,
    created_at timestamp(6) with time zone not null,
    constraint pk_favorites primary key (id),
    constraint uk_favorite_user_titre unique (user_id, titre_id),
    constraint fk_favorites_user foreign key (user_id) references users (id) on delete cascade,
    constraint fk_favorites_titre foreign key (titre_id) references titres (id) on delete cascade
);

create index idx_favorites_user on favorites (user_id);

create table play_history (
    id        uuid not null,
    user_id   uuid not null,
    titre_id  uuid not null,
    played_at timestamp(6) with time zone not null,
    constraint pk_play_history primary key (id),
    constraint fk_play_history_user foreign key (user_id) references users (id) on delete cascade,
    constraint fk_play_history_titre foreign key (titre_id) references titres (id) on delete cascade
);

create index idx_play_history_user_played on play_history (user_id, played_at desc);
