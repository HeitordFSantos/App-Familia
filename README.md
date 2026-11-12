# App Família

Secretária digital para a administração de uma casa: agenda e compromissos, agenda vacinal dos filhos, lista de compras por foto, finanças e bloqueio de redes sociais no horário do devocional.

## Estrutura do repositório

```
App-Familia/
├── backend/   API em Java 21 + Spring Boot + PostgreSQL
└── android/   App Android em Java (XML + MVVM)
```

## Estado atual

| Módulo | Situação | O que falta |
|---|---|---|
| Núcleo: Casa, membros, filhos, convite | Pronto | Envio do convite por e-mail |
| Login por e-mail e senha | Pronto | |
| Login com Google | Código pronto | Criar as credenciais no Google Cloud Console |
| Agenda e compromissos | Pronto | Editar compromisso (hoje é remover e criar de novo) |
| Agenda vacinal | Pronto, com leitura da caderneta por foto | Revisar o calendário a cada atualização do Ministério da Saúde; configurar o modelo com visão |
| Lista de compras | Lista compartilhada e sugestões por foto prontas | Configurar o modelo com visão |
| Devocional diário | Código pronto: plano da Casa, passagem, reflexão, referências | Configurar o modelo de linguagem, a pesquisa e a licença da tradução |
| Finanças | Lançamento manual e resumo mensal prontos | Ligação com banco via Open Finance (precisa de um agregador) |
| Horário do devocional | Bloqueio no Android pronto | Validar em aparelho, principalmente Xiaomi/HyperOS |

Tudo o que está marcado como pronto passa nos testes automáticos do backend e compila no app.
O comportamento do app em um aparelho real ainda precisa ser validado.

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
| `LLM_URL` | Endereço do modelo de linguagem, no formato da API de chat da OpenAI | vazio (devocional e fotos desligados) |
| `LLM_CHAVE` | Chave do serviço do modelo, se houver | vazio |
| `LLM_MODELO`, `LLM_MODELO_VISAO` | Nome do modelo de texto e do modelo que lê imagens | `llama3.2`, `llama3.2-vision` |
| `PESQUISA_URL` | Endereço de uma instância do SearXNG com saída JSON habilitada | vazio (devocional sem referências externas) |
| `BIBLIA_API_CHAVE`, `BIBLIA_API_VERSAO` | Chave da API.Bible e id da tradução licenciada | vazio (devocional só com a referência) |

### Modelo de linguagem (devocional e fotos)

O backend conversa com qualquer servidor que aceite a API de chat no formato da OpenAI. Para rodar
Llama no próprio computador com o [Ollama](https://ollama.com):

```bash
ollama pull llama3.2
ollama pull llama3.2-vision
# depois, ao subir o backend:
LLM_URL=http://localhost:11434/v1 mvn spring-boot:run
```

Três peças são independentes e cada uma tem um comportamento quando falta:

| Peça | Para que serve | Sem ela |
|---|---|---|
| Modelo de linguagem | Escreve a reflexão e lê as fotos | Devocional e fotos respondem que não estão configurados |
| Pesquisa (SearXNG) | Traz páginas de referência sobre a passagem do dia | Devocional sai sem a seção "Para ler mais" |
| Texto bíblico (API.Bible) | Entrega o texto exato da tradução licenciada | O app mostra só a referência e pede para ler na Bíblia |

Regras do desenho:

- O texto bíblico nunca é escrito pelo modelo. O modelo escolhe a referência e a fonte licenciada entrega o texto.
- Das páginas pesquisadas, o app mostra só o título e o link; o modelo é instruído a não copiar frases delas.
- As fotos são repassadas ao modelo e descartadas. A leitura só gera sugestões; o usuário confirma o que é gravado.
- A API.Bible libera traduções com direitos autorais conforme o plano da conta e a licença de cada tradução.
  Antes de publicar o app, confirme que a tradução escolhida pode ser usada no seu caso.

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
| GET | `/api/compromissos?de=AAAA-MM-DD` | logado | Compromissos a partir de uma data (padrão: hoje) |
| POST | `/api/compromissos` | logado | Cria compromisso, com lembrete e filho opcionais |
| DELETE | `/api/compromissos/{id}` | logado | Remove um compromisso |
| GET | `/api/dependentes/{id}/vacinas` | logado | Agenda vacinal do filho, com a situação de cada dose |
| PUT | `/api/dependentes/{id}/vacinas/{codigo}` | logado | Marca uma dose como aplicada |
| DELETE | `/api/dependentes/{id}/vacinas/{codigo}` | logado | Desmarca uma dose |
| GET | `/api/compras` | logado | Lista de compras |
| POST | `/api/compras` | logado | Adiciona item |
| PUT | `/api/compras/{id}/comprado` | logado | Marca ou desmarca como comprado |
| DELETE | `/api/compras/comprados` | logado | Remove todos os itens já comprados |
| DELETE | `/api/compras/{id}` | logado | Remove um item |
| POST | `/api/dependentes/{id}/vacinas/foto` | logado | Lê uma foto da caderneta e sugere doses a marcar |
| POST | `/api/compras/foto` | logado | Sugere itens a partir de uma foto do armário ou da geladeira |
| GET | `/api/devocional/hoje` | logado | Devocional do dia (gerado na primeira chamada do dia) |
| GET | `/api/devocional/plano` | logado | Plano de leitura da Casa |
| PUT | `/api/devocional/plano` | ADMIN | Troca o plano: livro em sequência ou tema |
| GET | `/api/devocional/livros` | logado | Livros disponíveis para o plano |
| GET | `/api/lancamentos?mes=AAAA-MM` | logado | Lançamentos e totais do mês |
| POST | `/api/lancamentos` | logado | Cria receita ou despesa (valor em centavos) |
| DELETE | `/api/lancamentos/{id}` | logado | Remove um lançamento |

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
├── agenda/           compromissos
├── vacinas/          calendário vacinal e doses aplicadas
├── compras/          lista de compras
├── financas/         receitas e despesas
├── devocional/       plano da Casa, devocional do dia e fonte do texto bíblico
└── shared/
    ├── ia/           cliente do modelo de linguagem, pesquisa e foto
    ├── seguranca/    configuração do Spring Security e usuário logado
    └── erro/         formato único de erro
```

### Agenda vacinal

As doses ficam em `vacinas/CalendarioVacinal.java`, com base no Calendário Nacional de Vacinação da
criança (Ministério da Saúde, versão de 30/01/2026) mais o 2º reforço da VIP aos 4 anos. É uma
referência para lembrar a família; quem define o que aplicar é a unidade de saúde. Ao atualizar o
calendário, mantenha os códigos das doses existentes, porque os registros de aplicação apontam para eles.

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
│   ├── ui/             base das telas de lista
│   └── Alarmes, Formatos, BootReceiver
└── feature/
    ├── auth/           login, cadastro, Google
    ├── inicio/         tela inicial e convite
    ├── dependentes/    filhos
    ├── vacinas/        agenda vacinal de cada filho
    ├── agenda/         compromissos e lembretes por notificação
    ├── compras/        lista de compras
    ├── financas/       receitas e despesas do mês
    └── devocional/     devocional do dia, plano e bloqueio de aplicativos em horário definido
```

### Horário do devocional

Funciona só no aparelho, sem passar pelo backend. No horário configurado, um serviço confere qual
aplicativo está aberto e cobre os bloqueados com um aviso. Precisa de duas permissões, pedidas na
própria tela: **acesso de uso** e **sobrepor a outros apps**. Em aparelhos Xiaomi, ative também o
início automático do app e deixe a economia de bateria sem restrições, senão o sistema encerra o serviço.

### Lembretes da agenda

São notificações locais agendadas no aparelho quando a agenda é aberta. Um compromisso criado por
outra pessoa da Casa só gera lembrete no seu aparelho depois que você abrir a agenda.
