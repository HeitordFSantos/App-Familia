# App Família

Secretária digital para a administração de uma casa: agenda e compromissos, agenda vacinal dos filhos, lista de compras por foto, finanças e bloqueio de redes sociais no horário do devocional.

## Estrutura do repositório

```
App-Familia/
├── backend/   API em Java 21 + Spring Boot + PostgreSQL
└── android/   App Android em Java (XML + MVVM)
```

## Estado atual

| Etapa | Situação |
|---|---|
| Núcleo: Casa, Membro, Dependente, Convite | Pronto |
| Login por e-mail e senha | Pronto |
| Login com Google (lado do servidor) | Pronto; falta configurar o client-id |
| App Android: login, cadastro, início, filhos, convite | Pronto |
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

## App Android

Java, telas em XML, Retrofit para a API. A cada envio para o GitHub, o fluxo **Android** (aba Actions) gera o APK de depuração como artefato `app-familia-debug`. O APK é sempre assinado com a mesma chave, então uma versão nova instala por cima da anterior.

Para gerar na sua máquina:

```bash
cd android
./gradlew assembleDebug      # no Windows: gradlew.bat assembleDebug
```

O APK sai em `android/app/build/outputs/apk/debug/app-debug.apk`.

### Ligando o app ao backend

O app precisa alcançar o backend pela rede. Enquanto o backend roda no seu computador:

1. Celular e computador na mesma rede Wi-Fi.
2. Descubra o IP do computador (`ipconfig` no Windows), por exemplo `192.168.0.10`.
3. Na tela de login do app, toque em **Servidor** e informe `192.168.0.10:8080`.
4. Se não conectar, libere a porta 8080 no firewall do Windows.

### Login com Google

O botão só aparece quando `googleClientId` está preenchido em `android/gradle.properties`. O mesmo valor vai na variável `GOOGLE_CLIENT_ID` do backend. É preciso criar no Google Cloud Console um cliente OAuth do tipo **Web** (esse é o ID usado) e um do tipo **Android** com o pacote `com.appfamilia` e o SHA-1 da chave `android/app/debug.keystore`.

### Organização do código

```
com.appfamilia
├── App.java            objetos únicos: sessão e acesso à API
├── core/
│   ├── Sessao.java     token, usuário e endereço do servidor
│   └── rede/           Retrofit, modelos e tratamento de erro
└── feature/
    ├── auth/           login, cadastro, Google
    ├── inicio/         tela inicial e convite
    └── dependentes/    filhos
```
