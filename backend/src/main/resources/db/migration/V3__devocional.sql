-- Devocional diário: o plano escolhido pela Casa e o devocional gerado para cada dia.

create table plano_devocional (
    id               uuid primary key,
    casa_id          uuid not null unique references casa (id),
    tipo             varchar(10) not null,    -- LIVRO (leitura em sequência) ou TEMA
    valor            varchar(120) not null,   -- código do livro (ex.: PSA) ou o tema
    proximo_capitulo integer not null
);

create table devocional_diario (
    id            uuid primary key,
    casa_id       uuid not null references casa (id),
    data_dia      date not null,
    referencia    varchar(80) not null,
    texto_biblico varchar(40000),             -- nulo quando não há fonte bíblica licenciada configurada
    direitos      varchar(1000),              -- aviso de direitos autorais da tradução
    titulo        varchar(200) not null,
    reflexao      varchar(8000) not null,
    aplicacao     varchar(2000),
    oracao        varchar(2000),
    fontes_json   varchar(4000),              -- páginas consultadas: [{"titulo": "...", "url": "..."}]
    criado_em     timestamp with time zone not null,
    unique (casa_id, data_dia)
);
