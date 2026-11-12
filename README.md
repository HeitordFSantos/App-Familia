# App Família

Secretária digital para a administração de uma casa: agenda e compromissos, agenda vacinal dos filhos, lista de compras por foto, finanças e bloqueio de redes sociais no horário do devocional.

## Estrutura do repositório

```
App-Familia/
├── backend/   API em Java 21 + Spring Boot + PostgreSQL
└── android/   App Android em Java (a criar)
```

## Estado atual

| Etapa | Situação |
|---|---|
| Núcleo: Casa, Membro, Dependente, Convite | Pronto |
| Login por e-mail e senha | Pronto |
| Login com Google (lado do servidor) | Pronto; falta configurar o client-id |
| App Android | A fazer |
| Agenda e compromissos | A fazer |
| Agenda vacinal | A fazer |
| Bloqueio para o devocional | A fazer |
| Lista de compras por foto | A fazer |
| Finanças | A fazer |

## Backend

### Como rodar

Pré-requisitos: Java 21, Maven e Docker.

```bash
cd backend
docker compose up -d        # sobe o PostgreSQL
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`. As tabelas são criadas pelo Flyway na primeira execução.

Para rodar os testes (usam banco em memória, não precisam do Docker):

```bash
mvn test
```

### Configuração

| Variável | Para que serve | Padrão |
|---|---|---|
| `DB_URL`, `DB_USUARIO`, `DB_SENHA` | Conexão com o PostgreSQL | banco local do docker-compose |
| `JWT_SEGREDO` | Assina os tokens de sessão (32+ caracteres) | valor de desenvolvimento; **trocar em produção** |
| `GOOGLE_CLIENT_ID` | ID do cliente Web do Google Cloud Console | vazio (login Google desligado) |

### Endpoints

| Método | Caminho | Acesso | O que faz |
|---|---|---|---|
| POST | `/api/auth/cadastro` | público | Cria o membro. Com `nomeCasa` cria uma Casa nova; com `conviteToken` entra em uma existente |
| POST | `/api/auth/login` | público | Login com e-mail e senha |
| POST | `/api/auth/google` | público | Login com o ID token do Google |
| GET | `/api/casa` | logado | Dados da Casa e seus membros |
| GET | `/api/dependentes` | logado | Lista os filhos da Casa |
| POST | `/api/dependentes` | logado | Cadastra um filho |
| DELETE | `/api/dependentes/{id}` | logado | Remove um filho |
| POST | `/api/convites` | ADMIN | Gera convite para outro adulto |

Os endpoints de login devolvem um `token`. Nas demais chamadas, envie o cabeçalho `Authorization: Bearer <token>`.

### Organização do código

```
com.appfamilia
├── core/
│   ├── auth/         cadastro, login, token, verificação do Google
│   ├── casa/         a Casa, unidade que agrupa tudo
│   ├── membro/       adultos com login
│   ├── dependente/   filhos, sem login
│   └── convite/      entrada de um segundo adulto na Casa
└── shared/
    ├── seguranca/    configuração do Spring Security e usuário logado
    └── erro/         formato único de erro
```

Regras que valem para todo módulo novo:

- Toda tabela tem `casa_id` e toda consulta filtra por ele.
- O `casa_id` vem sempre do token (`UsuarioAtual`), nunca do corpo da requisição.
- Um módulo não depende de outro; todos dependem só de `core` e `shared`.
- Serviços externos (Google, banco, IA) ficam atrás de uma interface.
