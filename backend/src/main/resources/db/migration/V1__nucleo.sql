-- Núcleo: toda informação do app pertence a uma Casa.

create table casa (
    id        uuid primary key,
    nome      varchar(120) not null,
    criado_em timestamp with time zone not null
);

create table membro (
    id         uuid primary key,
    casa_id    uuid not null references casa (id),
    nome       varchar(120) not null,
    email      varchar(255) not null unique,
    senha_hash varchar(100),          -- nulo para quem entra só com Google
    google_sub varchar(64) unique,    -- identificador da conta Google, se vinculada
    papel      varchar(20) not null,  -- ADMIN ou MEMBRO
    criado_em  timestamp with time zone not null
);
create index idx_membro_casa on membro (casa_id);

create table dependente (
    id              uuid primary key,
    casa_id         uuid not null references casa (id),
    nome            varchar(120) not null,
    data_nascimento date not null,
    criado_em       timestamp with time zone not null
);
create index idx_dependente_casa on dependente (casa_id);

create table convite (
    id        uuid primary key,
    casa_id   uuid not null references casa (id),
    email     varchar(255) not null,
    token     varchar(64) not null unique,
    expira_em timestamp with time zone not null,
    usado_em  timestamp with time zone
);
