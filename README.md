# Gerenciador Financeiro Pessoal

[![Java](https://img.shields.io/badge/Java-21-orange.svg?style=flat&logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-blue.svg?style=flat&logo=mysql)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED.svg?style=flat&logo=docker)](https://www.docker.com/)
[![JavaScript](https://img.shields.io/badge/JavaScript-ES6+-F7DF1E.svg?style=flat&logo=javascript)](https://developer.mozilla.org/)
[![TailwindCSS](https://img.shields.io/badge/TailwindCSS-3.x-38B2AC.svg?style=flat&logo=tailwind-css)](https://tailwindcss.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Aplicacao Full-Stack completa para controle financeiro pessoal e gestao de pagamentos resilientes, desenvolvida com Spring Boot 3 (Java 21), banco de dados MySQL 8, conteinerizacao com Docker, arquitetura orientada a dominio (Domain-Driven Design - DDD), politicas estritas de idempotencia ponta a ponta e uma interface frontend moderna e responsiva em JavaScript (Single Page Application) com Tailwind CSS e graficos interativos Chart.js.

---

## Sumario

- [Visao Geral e Arquitetura](#visao-geral-e-arquitetura)
- [Wiki de Engenharia DDD](#wiki-de-engenharia-ddd)
- [Arquitetura de Idempotencia e Pagamentos](#arquitetura-de-idempotencia-e-pagamentos)
- [Funcionalidades Principais](#funcionalidades-principais)
- [Tecnologias Utilizadas](#tecnologias-utilizadas)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Pre-requisitos](#pre-requisitos)
- [Como Executar a Aplicacao](#como-executar-a-aplicacao)
- [Documentacao da API (Swagger / OpenAPI)](#documentacao-da-api-swagger--openapi)
- [Endpoints da API REST](#endpoints-da-api-rest)
- [Testes Automatizados](#testes-automatizados)
- [Autor](#autor)

---

## Visao Geral e Arquitetura

O sistema adota o padrao de arquitetura em 4 camadas (Domain-Driven Design) no backend e uma SPA modular no frontend:

```
+-------------------------------------------------------------+
|                 Frontend SPA (HTML5 / JS / CSS)             |
|   (Dashboard, Graficos Chart.js, Gestao de Contas, Filtros) |
+------------------------------+------------------------------+
                               | HTTP / JSON (JWT Bearer)
+------------------------------v------------------------------+
|                  Presentation Layer (Controllers & DTOs)    |
|  - Spring Security + JWT Filter (Stateless)                 |
|  - IdempotencyFilter (Header Idempotency-Key)               |
|  - WebhookController (HMAC-SHA256 Raw Body Verifier)        |
+-------------------------------------------------------------+
|                  Application Layer (Service Implementations)|
|  - Orquestracao de Casos de Uso e Transacoes                |
|  - IdempotencyService & WebhookService                      |
|  - WebhookAsyncWorker (Execucao assincrona desacoplada)     |
+-------------------------------------------------------------+
|                  Domain Layer (O Coracao do Software)       |
|  - Agregados Ricos (Conta, Transacao, Orcamento, Categoria) |
|  - Entidades de Idempotencia (EventoWebhook, RegistroIdem.) |
|  - Value Objects Imutaveis (ValorMonetario, Competencia)    |
+-------------------------------------------------------------+
|                  Infrastructure Layer                       |
|  - Repositories (Spring Data JPA + Specifications)          |
|  - GatewayPaymentClient (Integracao Outbound com UUID v4)   |
|  - Consultas Agregadas CQRS no Banco de Dados               |
+------------------------------+------------------------------+
                               | JDBC
+------------------------------v------------------------------+
|                    MySQL 8.4 (Docker)                   |
+-------------------------------------------------------------+
```

---

## Wiki de Engenharia DDD

A documentacao detalhada de requisitos, modelagem de dominio e governanca tecnica esta centralizada na [Wiki do Projeto (docs/wiki/)](docs/wiki/README.md):

* [1. Visao Geral, Diagnostico de Riscos e Anatomia da Solucao](docs/wiki/visao_geral_e_dominio.md): Matriz dos 4 Grandes Riscos de Marty Cagan, Personas, AS IS vs. TO BE e Requisitos.
* [2. Dicionario da Linguagem Ubiqua e Contextos Delimitados](docs/wiki/linguagem_ubiqua.md): Vocabulario oficial e Context Map entre Identidade, Contas, Transacoes e Orcamentos.
* [3. Arquitetura e Design Tatico DDD](docs/wiki/arquitetura_e_design_tatico.md): 4 camadas, Value Objects, Comandos de Agregados, Optimistic Locking (@Version) e desativacao do OSIV (open-in-view: false).
* [4. Domain Storytelling e Event Storming](docs/wiki/event_storming_e_fluxos.md): Narrativas de negocios, eventos de dominio, politicas e mitigacao de hotspots.
* [5. Contratos de API REST e Tratamento de Excecoes](docs/wiki/api_e_contratos.md): Especificacao dos endpoints, DTOs e codigos de erro (HTTP 400, 404, 409, 422).

---

## Arquitetura de Idempotencia e Pagamentos

O sistema implementa politicas robustas de idempotencia ponta a ponta para eliminar efeitos duplicados em cobrancas, webhooks e operacoes financeiras:

### 1. Webhooks Inbound Resilientes (/webhooks/stripe)
- Verificacao de Assinatura no Corpo Bruto: Validacao de assinatura HMAC-SHA256 executada byte a byte em tempo constante com `MessageDigest.isEqual`, antes de qualquer desserializacao JSON, prevenindo ataques de timing e adulteracoes.
- Padrao Insert-First Atomico: Ao receber um evento de webhook, a aplicacao tenta inserir o `event_id` na tabela `eventos_webhook` em transacao isolada (`REQUIRES_NEW`). Se o identificador ja existir, a excecao de integridade unica e interceptada e a API devolve HTTP 200 de imediato, sem reexecucao.
- Independencia da Ordem de Eventos: O processamento nao confia no payload do webhook para o estado final; busca o objeto atualizado de forma autoritativa na API do gateway.
- Desacoplamento Assincrono: Responde HTTP 200 prontamente e delega o processamento de negocio para um pool de threads dedicado (`webhookTaskExecutor`).

### 2. Idempotencia nos Endpoints Inbound do App (Header Idempotency-Key)
- Interceptador `IdempotencyFilter`: Requisicoes mutantes com o header `Idempotency-Key` sao avaliadas e cacheadas na tabela `registros_idempotencia`.
- Replay de Resposta Identica: Chamadas repetidas com a mesma chave e mesmo corpo recebem a resposta original gravada (mesmo HTTP status e payload JSON) acompanhada do header `Idempotent-Replayed: true`.
- Concorrencia Protegida: Requisicoes simultaneas com a mesma chave recebem HTTP 409 Conflict enquanto o primeiro processamento estiver ativo.
- Validacao de Payload: Se a mesma chave for reenviada com corpo divergente (hash SHA-256 diferente), a requisicao e rejeitada com HTTP 422 Unprocessable Entity.
- Bloqueio de Dados Pessoais (PII): Chaves de idempotencia que contenham e-mail ou CPF sao sumariamente rejeitadas com HTTP 400 Bad Request.

### 3. Chamadas Outbound ao Gateway de Pagamentos
- Clientes de integracao (`GatewayPaymentClient`, `StripeGatewayPaymentClient`) geram chaves de idempotencia padrao UUID v4 atreladas a operacao de negocio e as reutilizam em caso de retentativas (retries), garantindo protecao contra cobrancas duplicadas.

---

## Funcionalidades Principais

### 1. Autenticacao e Seguranca Desacoplada
- Cadastro e login de usuarios gerenciados pela camada de aplicacao (`AuthService`) com senhas criptografadas via BCrypt.
- Emissao e validacao de tokens JWT (JSON Web Token) stateless.
- Protecao de rotas da API com Spring Security e injecao automatica do usuario autenticado (`@AuthenticationPrincipal`).
- Configuracao de CORS para desenvolvimento desacoplado com liberacao dos headers de idempotencia.

### 2. Dashboard Financeiro Otimizado (CQRS Leve)
- Consultas agregadas diretamente no banco de dados relacional:
  - Saldo Consolidado Total (contas correntes, poupancas e investimentos).
  - Total de Receitas do Mes e Total de Despesas do Mes (desconsiderando transferencias patrimoniais).
  - Balanco Liquido com indicador visual de saldo positivo ou negativo.
- Graficos interativos (Chart.js):
  - Distribuicao de Despesas por Categoria com percentuais exatos.
  - Evolucao Mensal Receitas vs Despesas dos ultimos 6 meses.
- Tabela dinamica com as 5 transacoes mais recentes.

### 3. Gestao de Contas e Transferencias Patrimoniais
- Cadastro, edicao e exclusao de contas bancarias com Concorrencia Otimista (`@Version`).
- Transferencia entre contas: Debito na origem e credito no destino em uma unica transacao atomica, registrando lancamentos espelhados no extrato sem duplicar despesas ou receitas no Dashboard.
- Extrato Detalhado: Historico de movimentacoes ordenado decrescentemente por data (`/contas/{id}/extrato`).

### 4. Transacoes Financeiras, Tags e Parcelamento Exato
- Lancamento de Receitas e Despesas com atualizacao automatica do saldo da conta.
- Suporte a Tags transversais persistidas em cada transacao.
- Rateio Exato de Parcelamento: Divisao de compras a prazo sem perda de centavos via `ValorMonetario`, alocando o residuo na primeira parcela (`/transacoes/parcelado`).
- Filtros Avancados: Intervalo de datas, categoria, conta bancaria e faixa de valores.
- Edicao e exclusao com estorno automatico de saldos.

### 5. Transacoes Recorrentes Deterministicas
- Identidade forte via chave estrangeira `recorrencia_id` no banco de dados.
- Projecao de despesas e receitas periodicas com liquidacao automatica para datas anteriores ao dia atual.
- Sincronizacao automatica e sem colisao com transacoes manuais de mesmo nome.

### 6. Orcamentos Mensais por Categoria
- Definicao de limites orcamentarios por categoria de despesa e competencia mensal (`mes/ano`).
- Apuracao do consumo em tempo real com indicador de limite ultrapassado.

---

## Tecnologias Utilizadas

### Backend
- Java 21
- Spring Boot 3.3.4
- Spring Data JPA / Hibernate (open-in-view: false)
- Spring Security
- Java JWT (Auth0)
- MySQL 8.4 (mysql-connector-j)
- H2 Database (para execucao de testes unitarios e de integracao)
- SpringDoc OpenAPI 2.6.0 (Swagger UI)
- Lombok
- Maven

### Frontend
- JavaScript Moderno (ES6+)
- HTML5 Semantico
- Tailwind CSS (via CDN)
- Chart.js (graficos financeiros interativos)
- FontAwesome 6 (icones)
- CSS3 Customizado (animacoes, modais e notificacoes toast)

### DevOps e Ferramentas
- Docker e Docker Compose
- Git e GitHub

---

## Estrutura do Projeto

```
Gerenciador Financeiro/
|-- docker-compose.yml                     # Orquestracao do MySQL 8
|-- pom.xml                                # Gerenciamento de dependencias Maven
|-- docs/                                  # Central de Documentacao
|   `-- wiki/                              # Wiki Viva de Engenharia DDD
|       |-- README.md                      # Indice da Wiki
|       |-- visao_geral_e_dominio.md       # Visao do Produto, Riscos e Requisitos
|       |-- linguagem_ubiqua.md            # Dicionario Ubiquo e Mapa de Contextos
|       |-- arquitetura_e_design_tatico.md # 4 Camadas, Agregados, VOs e Concorrencia
|       |-- event_storming_e_fluxos.md     # Domain Storytelling e Event Storming
|       `-- api_e_contratos.md             # Contratos REST e Tratamento de Erros
|-- src/
|   |-- main/
|   |   |-- java/br/com/gerenciadorfinanceiro/
|   |   |   |-- config/                    # Configuracoes (Security, CORS, JWT, OpenAPI, Async)
|   |   |   |   `-- idempotency/           # Filtro, Request Wrapper e DTOs de Idempotencia
|   |   |   |-- controller/                # Camada Presentation (REST Controllers e DTOs)
|   |   |   |-- exception/                 # StandardError e GlobalExceptionHandler
|   |   |   |-- infrastructure/            # Camada Infrastructure (Gateway Outbound, Security)
|   |   |   |-- model/                     # Camada Domain (Agregados, Entidades e Enums)
|   |   |   |   |-- enums/                 # Enums de Dominio e Status de Processamento
|   |   |   |   `-- vo/                    # Value Objects (ValorMonetario, Competencia, Dia)
|   |   |   |-- repository/                # Camada Infrastructure (Spring Data JPA e Queries CQRS)
|   |   |   `-- service/                   # Camada Application (Servicos e Workers Assincronos)
|   |   |       `-- impl/                  # Implementacoes de Servicos de Aplicacao
|   |   `-- resources/
|   |       |-- application.yml            # Configuracoes do Spring Boot (OSIV desativado)
|   |       |-- data.sql                   # Carga inicial alinhada com Preferencias e Versao
|   |       |-- migrations/                # Scripts Flyway (V1, V2, V3 com tabelas de idempotencia)
|   |       `-- static/                    # Frontend SPA
|   `-- test/
|       |-- java/br/com/gerenciadorfinanceiro/ # Suite com 64 Testes Automatizados
|       `-- resources/application-test.yml # Configuracao de banco H2 em memoria para testes
```

---

## Pre-requisitos

Antes de iniciar, certifique-se de ter instalado em sua maquina:
- [JDK 21+](https://www.oracle.com/java/technologies/downloads/#java21)
- [Apache Maven 3.8+](https://maven.apache.org/)
- [Docker](https://www.docker.com/) e [Docker Compose](https://docs.docker.com/compose/)

---

## Ambientes de Execução (Dev, Staging, Prod)

A aplicação conta com arquitetura multi-ambiente isolada com perfis do Spring Boot, conteinerização Docker Compose e governança de branches Git:

| Ambiente | Perfil Spring | Branch Git | Finalidade | Swagger UI | Seed Demo |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Dev** | `dev` | `develop` | Desenvolvimento local, testes e depuração | Ativado (`:8080`) | Ativado (`teste@financeiro.com`) |
| **Staging** | `staging` | `staging` | Homologação, validação de QA e pré-produção | Ativado (`:8081`) | Desativado |
| **Prod** | `prod` | `main` | Produção em nuvem / Oracle Cloud | **Desativado** | Desativado |

Para mais detalhes arquiteturais, consulte a [Wiki de Ambientes](docs/wiki/ambientes_dev_staging_prod.md).

---

## Como Executar a Aplicacao

### 1. Clonar o repositorio
```bash
git clone https://github.com/LuizhBrandao/GerenciadorFinanceiro.git
cd GerenciadorFinanceiro
```

### 2. Executando nos Ambientes

#### A) Ambiente de Desenvolvimento (Dev)
* **Opção 1: Banco no Docker + Aplicação na IDE (Recomendado para Dev)**
  ```bash
  # Inicia apenas o MySQL 8.4 na porta 3306 (financeiro_dev_db)
  docker compose up -d

  # Executa a aplicação com perfil dev
  mvn spring-boot:run -Dspring-boot.run.profiles=dev
  ```
* **Opção 2: Stack Completa no Docker via Script:**
  ```powershell
  # No Windows PowerShell:
  .\scripts\manage-env.ps1 -Environment dev -Action up -FullDev

  # No Linux/macOS:
  ./scripts/manage-env.sh dev up --full
  ```
* **Acessar Dev:**
  - Aplicação / Frontend: [http://localhost:8080](http://localhost:8080)
  - Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
  - Usuário padrão gerado: `teste@financeiro.com` / Senha: `123456`

#### B) Ambiente de Homologação / Staging (Porta 8081)
```powershell
# No Windows PowerShell:
.\scripts\manage-env.ps1 -Environment staging -Action up

# Ou via Docker Compose diretamente:
docker compose -f docker-compose.staging.yml --env-file .env.staging up -d --build
```
* **Acessar Staging:**
  - Aplicação / Frontend: [http://localhost:8081](http://localhost:8081)
  - Swagger UI: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
  - Healthcheck: [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health)

#### C) Ambiente de Produção (Prod)
```powershell
# No Windows PowerShell:
.\scripts\manage-env.ps1 -Environment prod -Action up

# Ou via Docker Compose diretamente:
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```
* **Acessar Prod:**
  - Aplicação: [http://localhost:80](http://localhost:80)
  - Healthcheck: [http://localhost:80/actuator/health](http://localhost:80/actuator/health)
  - *(Swagger UI e endpoints de depuração são desativados em produção por segurança)*

---

## Documentacao da API (Swagger / OpenAPI)

Com a aplicacao em execucao, acesse a documentacao interativa do Swagger UI:
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

---

## Endpoints da API REST

| Modulo | Metodo | Endpoint | Descricao | Autenticacao |
| :--- | :--- | :--- | :--- | :--- |
| **Auth** | `POST` | `/auth/register` | Cria uma nova conta de usuario | Publica |
| **Auth** | `POST` | `/auth/login` | Realiza login e retorna o Token JWT | Publica |
| **Webhooks** | `POST` | `/webhooks/stripe` | Recebe eventos do gateway Stripe com validacao HMAC | Publica (Assinatura Stripe) |
| **Contas** | `GET` | `/contas` | Lista todas as contas do usuario logado | Bearer Token |
| **Contas** | `GET` | `/contas/{id}` | Obtem os detalhes de uma conta | Bearer Token |
| **Contas** | `GET` | `/contas/resumo-saldos` | Retorna o resumo consolidado de saldos | Bearer Token |
| **Contas** | `GET` | `/contas/{id}/extrato` | Retorna o extrato detalhado de uma conta | Bearer Token |
| **Contas** | `POST` | `/contas` | Cadastra uma nova conta bancaria (suporta Idempotency-Key) | Bearer Token |
| **Contas** | `PUT` | `/contas/{id}` | Atualiza dados cadastrais de uma conta | Bearer Token |
| **Contas** | `DELETE` | `/contas/{id}` | Exclui uma conta bancaria | Bearer Token |
| **Contas** | `POST` | `/contas/transferir` | Realiza transferencia patrimonial entre contas (suporta Idempotency-Key) | Bearer Token |
| **Transacoes** | `GET` | `/transacoes` | Lista todas as transacoes | Bearer Token |
| **Transacoes** | `GET` | `/transacoes/busca` | Filtro avancado (datas, categoria, conta, valores) | Bearer Token |
| **Transacoes** | `GET` | `/transacoes/{id}` | Busca transacao por ID | Bearer Token |
| **Transacoes** | `POST` | `/transacoes` | Cria uma transacao simples (suporta Idempotency-Key) | Bearer Token |
| **Transacoes** | `POST` | `/transacoes/parcelado` | Cria compra parcelada sem perda de centavos (suporta Idempotency-Key) | Bearer Token |
| **Transacoes** | `PUT` | `/transacoes/{id}` | Retifica uma transacao existente | Bearer Token |
| **Transacoes** | `PATCH` | `/transacoes/{id}/efetivar` | Liquida uma transacao pendente | Bearer Token |
| **Transacoes** | `DELETE` | `/transacoes/{id}` | Exclui transacao e estorna o saldo | Bearer Token |
| **Recorrencias** | `GET` | `/recorrencias` | Lista todas as recorrencias cadastradas | Bearer Token |
| **Recorrencias** | `GET` | `/recorrencias/ativas` | Lista apenas as recorrencias ativas | Bearer Token |
| **Recorrencias** | `POST` | `/recorrencias` | Cria nova regra e projeta ocorrencias (suporta Idempotency-Key) | Bearer Token |
| **Recorrencias** | `PUT` | `/recorrencias/{id}` | Atualiza plano e sincroniza filhas | Bearer Token |
| **Recorrencias** | `PATCH` | `/recorrencias/{id}/status` | Ativa ou desativa a recorrencia | Bearer Token |
| **Recorrencias** | `DELETE` | `/recorrencias/{id}` | Exclui a regra de recorrencia | Bearer Token |
| **Categorias** | `GET` | `/categorias` | Lista catalogo de categorias cadastradas | Bearer Token |
| **Categorias** | `POST` | `/categorias` | Cria uma nova categoria | Bearer Token |
| **Categorias** | `PUT` | `/categorias/{id}` | Atualiza uma categoria | Bearer Token |
| **Categorias** | `DELETE` | `/categorias/{id}` | Exclui uma categoria | Bearer Token |
| **Orcamentos** | `GET` | `/orcamentos` | Lista metas e consumo em tempo real | Bearer Token |
| **Orcamentos** | `GET` | `/orcamentos/competencia` | Consulta orcamento de uma competencia especifica | Bearer Token |
| **Orcamentos** | `POST` | `/orcamentos` | Define teto mensal para categoria de despesa | Bearer Token |
| **Orcamentos** | `PUT` | `/orcamentos/{id}` | Atualiza o teto do orcamento | Bearer Token |
| **Orcamentos** | `DELETE` | `/orcamentos/{id}` | Exclui uma meta orcamentaria | Bearer Token |
| **Dashboard** | `GET` | `/dashboard` | Retorna metricas agregadas via CQRS | Bearer Token |

---

## Testes Automatizados

A aplicacao conta com uma suite completa de **64 testes automatizados** cobrindo Value Objects, Agregados, Regras de Negocio, Casos de Uso, Idempotencia Sequencial, Idempotencia Concorrente Multithread e Seguranca:

```bash
mvn test
```

Saida esperada:
```
[INFO] Tests run: 64, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## Autor

Desenvolvido por [Luiz Brandao](https://github.com/LuizhBrandao).
