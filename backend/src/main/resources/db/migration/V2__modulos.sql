-- Módulos: agenda, vacinas, compras e finanças. Tudo pertence a uma Casa.

create table compromisso (
    id               uuid primary key,
    casa_id          uuid not null references casa (id),
    titulo           varchar(160) not null,
    descricao        varchar(1000),
    inicio           timestamp not null,      -- data e hora locais da família
    lembrete_minutos integer,                 -- minutos antes do início; nulo = sem lembrete
    dependente_id    uuid references dependente (id) on delete set null,
    criado_em        timestamp with time zone not null
);
create index idx_compromisso_casa_inicio on compromisso (casa_id, inicio);

create table registro_vacina (
    id            uuid primary key,
    casa_id       uuid not null references casa (id),
    dependente_id uuid not null references dependente (id) on delete cascade,
    codigo_dose   varchar(40) not null,       -- código da dose no calendário (CalendarioVacinal)
    aplicada_em   date not null,
    unique (dependente_id, codigo_dose)
);

create table item_compra (
    id         uuid primary key,
    casa_id    uuid not null references casa (id),
    nome       varchar(160) not null,
    quantidade varchar(60),
    comprado   boolean not null,
    criado_em  timestamp with time zone not null
);
create index idx_item_compra_casa on item_compra (casa_id);

create table lancamento (
    id              uuid primary key,
    casa_id         uuid not null references casa (id),
    tipo            varchar(10) not null,     -- RECEITA ou DESPESA
    valor_centavos  bigint not null,
    categoria       varchar(60) not null,
    descricao       varchar(255),
    data_lancamento date not null,
    criado_em       timestamp with time zone not null
);
create index idx_lancamento_casa_data on lancamento (casa_id, data_lancamento);
