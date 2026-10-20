# 💰 Gerenciador Financeiro Pessoal

[![Java](https://img.shields.io/badge/Java-21-orange.svg?style=flat&logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg?style=flat&logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED.svg?style=flat&logo=docker)](https://www.docker.com/)
[![JavaScript](https://img.shields.io/badge/JavaScript-ES6+-F7DF1E.svg?style=flat&logo=javascript)](https://developer.mozilla.org/)
[![TailwindCSS](https://img.shields.io/badge/TailwindCSS-3.x-38B2AC.svg?style=flat&logo=tailwind-css)](https://tailwindcss.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Aplicação Full-Stack completa para controle financeiro pessoal, desenvolvida com **Spring Boot 3 (Java 21)**, banco de dados **PostgreSQL**, conteinerização com **Docker**, arquitetura orientada a domínio (**Domain-Driven Design - DDD**), e uma interface frontend moderna e responsiva em **JavaScript (Single Page Application)** com **Tailwind CSS** e gráficos interativos **Chart.js**.

---

## 📑 Sumário

- [Visão Geral & Arquitetura](#-visão-geral--arquitetura)
- [Wiki de Engenharia DDD](#-wiki-de-engenharia-ddd)
- [Funcionalidades Principais](#-funcionalidades-principais)
- [Tecnologias Utilizadas](#-tecnologias-utilizadas)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Pré-requisitos](#-pré-requisitos)
- [Como Executar a Aplicação](#-como-executar-a-aplicação)
- [Documentação da API (Swagger / OpenAPI)](#-documentação-da-api-swagger--openapi)
- [Endpoints da API REST](#-endpoints-da-api-rest)
- [Testes](#-testes)
- [Autor](#-autor)

---

## 🏛 Visão Geral & Arquitetura

O sistema adota o padrão de arquitetura em 4 camadas (**Domain-Driven Design**) no backend e uma **SPA modular** no frontend:

```
┌─────────────────────────────────────────────────────────────┐
│                 Frontend SPA (HTML5 / JS / CSS)             │
│   (Dashboard, Gráficos Chart.js, Gestão de Contas, Filtros) │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP / JSON (JWT Bearer)
┌──────────────────────────────▼──────────────────────────────┐
│                  Presentation Layer (Controllers & DTOs)    │
│  - Spring Security + JWT Filter (Stateless)                 │
├─────────────────────────────────────────────────────────────┤
│                  Application Layer (Service Implementations)│
│  - Orquestração de Casos de Uso e Transações (@Transactional)│
├─────────────────────────────────────────────────────────────┤
│                  Domain Layer (O Coração do Software)       │
│  - Agregados Ricos (Conta, Transacao, Orcamento, Categoria) │
│  - Value Objects Imutáveis (ValorMonetario, Competencia)    │
├─────────────────────────────────────────────────────────────┤
│                  Infrastructure Layer                       │
│  - Repositories (Spring Data JPA + Specifications)          │
│  - Consultas Agregadas CQRS no Banco de Dados               │
└──────────────────────────────┬──────────────────────────────┘
                               │ JDBC
┌──────────────────────────────▼──────────────────────────────┐
│                    PostgreSQL 15 (Docker)                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 📖 Wiki de Engenharia DDD

A documentação detalhada de requisitos, modelagem de domínio e governança técnica está centralizada na [**Wiki do Projeto (docs/wiki/)**](docs/wiki/README.md):

* 🧭 [**1. Visão Geral, Diagnóstico de Riscos e Anatomia da Solução**](docs/wiki/visao_geral_e_dominio.md): Matriz dos 4 Grandes Riscos de Marty Cagan, Personas, AS IS vs. TO BE e Requisitos.
* 🗣 [**2. Dicionário da Linguagem Ubíqua e Contextos Delimitados**](docs/wiki/linguagem_ubiqua.md): Vocabulário oficial e Context Map entre Identidade, Contas, Transações e Orçamentos.
* 🧩 [**3. Arquitetura e Design Tático DDD**](docs/wiki/arquitetura_e_design_tatico.md): 4 camadas, Value Objects, Comandos de Agregados, Optimistic Locking (`@Version`) e desativação do OSIV (`open-in-view: false`).
* ⚡ [**4. Domain Storytelling e Event Storming**](docs/wiki/event_storming_e_fluxos.md): Narrativas de negócios, eventos de domínio, políticas e mitigação de hotspots.
* 🔌 [**5. Contratos de API REST e Tratamento de Exceções**](docs/wiki/api_e_contratos.md): Especificação dos endpoints, DTOs e códigos de erro (HTTP 400, 404, 409, 422).

---

## ✨ Funcionalidades Principais

### 1. Autenticação & Segurança Desacoplada
- Cadastro e login de usuários gerenciados pela camada de aplicação (`AuthService`) com senhas criptografadas via **BCrypt**.
- Emissão e validação de tokens **JWT (JSON Web Token)** stateless.
- Proteção de rotas da API com Spring Security e injeção automática do usuário autenticado (`@AuthenticationPrincipal`).
- Configuração de **CORS** para desenvolvimento desacoplado.

### 2. Dashboard Financeiro Otimizado (CQRS Leve)
- Consultas agregadas diretamente no banco de dados relacional:
  - **Saldo Consolidado Total** (contas correntes, poupanças e investimentos).
  - **Total de Receitas do Mês** e **Total de Despesas do Mês** (desconsiderando transferências patrimoniais).
  - **Balanço Líquido** com indicador visual de saldo positivo ou negativo.
- Gráficos interativos (**Chart.js**):
  - *Distribuição de Despesas por Categoria* com percentuais exatos.
  - *Evolução Mensal Receitas vs Despesas* dos últimos 6 meses.
- Tabela dinâmica com as 5 transações mais recentes.

### 3. Gestão de Contas & Transferências Patrimoniais
- Cadastro, edição e exclusão de contas bancárias com **Concorrência Otimista** (`@Version`).
- **Transferência entre contas**: Débito na origem e crédito no destino em uma única transação atômica, registrando lançamentos espelhados no extrato sem duplicar despesas/receitas no Dashboard.
- **Extrato Detalhado**: Histórico de movimentações ordenado decrescentemente por data (`/contas/{id}/extrato`).

### 4. Transações Financeiras, Tags e Parcelamento Exato
- Lançamento de **Receitas** e **Despesas** com atualização automática do saldo da conta.
- Suporte a **Tags** transversais persistidas em cada transação.
- **Rateio Exato de Parcelamento**: Divisão de compras a prazo sem perda de centavos via `ValorMonetario`, alocando o resíduo na 1ª parcela (`/transacoes/parcelado`).
- **Filtros Avançados**: Intervalo de datas, categoria, conta bancária e faixa de valores.
- Edição e exclusão com estorno automático de saldos.

### 5. Transações Recorrentes Determinísticas
- Identidade forte via chave estrangeira `recorrencia_id` no banco de dados.
- Projeção de despesas/receitas periódicas com liquidação automática para datas anteriores ao dia atual.
- Sincronização automática e sem colisão com transações manuais de mesmo nome.

### 6. Orçamentos Mensais por Categoria
- Definição de limites orçamentários por categoria de despesa e competência mensal (`mês/ano`).
- Apuração do consumo em tempo real com indicador de limite ultrapassado.

---

## 🛠 Tecnologias Utilizadas

### Backend
- **Java 21**
- **Spring Boot 3.3.4**
- **Spring Data JPA / Hibernate** (`open-in-view: false`)
- **Spring Security**
- **Java JWT (Auth0)**
- **PostgreSQL 15** (Driver JDBC)
- **H2 Database** (para execução de testes unitários e de integração)
- **SpringDoc OpenAPI 2.6.0** (Swagger UI)
- **Lombok**
- **Maven**

### Frontend
- **JavaScript Moderno (ES6+)**
- **HTML5 Semântico**
- **Tailwind CSS** (via CDN)
- **Chart.js** (gráficos financeiros interativos)
- **FontAwesome 6** (ícones)
- **CSS3 Customizado** (animações, modais e notificações toast)

### DevOps & Ferramentas
- **Docker & Docker Compose**
- **Git & GitHub**

---

## 📂 Estrutura do Projeto

```
Gerenciador Financeiro/
├── docker-compose.yml                     # Orquestração do PostgreSQL
├── pom.xml                                # Gerenciamento de dependências Maven
├── docs/                                  # Central de Documentação
│   └── wiki/                              # Wiki Viva de Engenharia DDD
│       ├── README.md                      # Índice da Wiki
│       ├── visao_geral_e_dominio.md       # Visão do Produto, Riscos e Requisitos
│       ├── linguagem_ubiqua.md            # Dicionário Ubíquo e Mapa de Contextos
│       ├── arquitetura_e_design_tatico.md # 4 Camadas, Agregados, VOs e Concorrência
│       ├── event_storming_e_fluxos.md     # Domain Storytelling e Event Storming
│       └── api_e_contratos.md             # Contratos REST e Tratamento de Erros
├── src/
│   ├── main/
│   │   ├── java/br/com/gerenciadorfinanceiro/
│   │   │   ├── config/                    # Configurações (Security, CORS, JWT, OpenAPI)
│   │   │   ├── controller/                # Camada Presentation (REST Controllers e DTOs)
│   │   │   ├── exception/                 # StandardError e GlobalExceptionHandler (HTTP 409, etc.)
│   │   │   ├── model/                     # Camada Domain (Agregados, Entidades e Enums)
│   │   │   │   └── vo/                    # Value Objects (ValorMonetario, Competencia, Dia)
│   │   │   ├── repository/                # Camada Infrastructure (Spring Data JPA e Queries CQRS)
│   │   │   └── service/                   # Camada Application (Interfaces de Serviço e Regras)
│   │   │       └── impl/                  # Implementações de Serviços de Aplicação
│   │   └── resources/
│   │       ├── application.yml            # Configurações do Spring Boot (OSIV desativado)
│   │       ├── data.sql                   # Carga inicial alinhada com Preferencias e Versão
│   │       └── static/                    # Frontend SPA
│   └── test/
│       ├── java/br/com/gerenciadorfinanceiro/ # Suíte com 30 Testes Unitários Automatizados
│       └── resources/application-test.yml # Configuração de banco H2 em memória para testes
```

---

## ⚙ Pré-requisitos

Antes de iniciar, certifique-se de ter instalado em sua máquina:
- [JDK 21+](https://www.oracle.com/java/technologies/downloads/#java21)
- [Apache Maven 3.8+](https://maven.apache.org/)
- [Docker](https://www.docker.com/) e [Docker Compose](https://docs.docker.com/compose/)

---

## 🚀 Como Executar a Aplicação

### 1. Clonar o repositório
```bash
git clone https://github.com/LuizhBrandao/GerenciadorFinanceiro.git
cd GerenciadorFinanceiro
```

### 2. Iniciar o Banco de Dados (PostgreSQL via Docker)
Suba o container do banco de dados na porta `5433`:
```bash
docker-compose up -d
```

### 3. Executar o Backend
Execute a aplicação via Maven:
```bash
mvn spring-boot:run
```
O servidor iniciará em `http://localhost:8080`.

### 4. Acessar o Frontend
- **Pelo próprio Spring Boot**: Abra seu navegador em:
  ```
  http://localhost:8080/
  ```

---

## 📚 Documentação da API (Swagger / OpenAPI)

Com a aplicação em execução, acesse a documentação interativa do Swagger UI:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

---

## 🔗 Endpoints da API REST

| Módulo | Método | Endpoint | Descrição | Autenticação |
| :--- | :--- | :--- | :--- | :--- |
| **Auth** | `POST` | `/auth/register` | Cria uma nova conta de usuário | Pública |
| **Auth** | `POST` | `/auth/login` | Realiza login e retorna o Token JWT | Pública |
| **Contas** | `GET` | `/contas` | Lista todas as contas do usuário logado | Bearer Token |
| **Contas** | `GET` | `/contas/{id}` | Obtém os detalhes de uma conta | Bearer Token |
| **Contas** | `GET` | `/contas/resumo-saldos` | Retorna o resumo consolidado de saldos | Bearer Token |
| **Contas** | `GET` | `/contas/{id}/extrato` | Retorna o extrato detalhado de uma conta | Bearer Token |
| **Contas** | `POST` | `/contas` | Cadastra uma nova conta bancária | Bearer Token |
| **Contas** | `PUT` | `/contas/{id}` | Atualiza dados cadastrais de uma conta | Bearer Token |
| **Contas** | `DELETE` | `/contas/{id}` | Exclui uma conta bancária | Bearer Token |
| **Contas** | `POST` | `/contas/transferir` | Realiza transferência patrimonial entre contas | Bearer Token |
| **Transações** | `GET` | `/transacoes` | Lista todas as transações | Bearer Token |
| **Transações** | `GET` | `/transacoes/busca` | Filtro avançado (datas, categoria, conta, valores) | Bearer Token |
| **Transações** | `GET` | `/transacoes/{id}` | Busca transação por ID | Bearer Token |
| **Transações** | `POST` | `/transacoes` | Cria uma transação simples (com tags) | Bearer Token |
| **Transações** | `POST` | `/transacoes/parcelado` | Cria compra parcelada sem perda de centavos | Bearer Token |
| **Transações** | `PUT` | `/transacoes/{id}` | Retifica uma transação existente | Bearer Token |
| **Transações** | `PATCH` | `/transacoes/{id}/efetivar` | Liquida uma transação pendente | Bearer Token |
| **Transações** | `DELETE` | `/transacoes/{id}` | Exclui transação e estorna o saldo | Bearer Token |
| **Recorrências** | `GET` | `/recorrencias` | Lista todas as recorrências cadastradas | Bearer Token |
| **Recorrências** | `GET` | `/recorrencias/ativas` | Lista apenas as recorrências ativas | Bearer Token |
| **Recorrências** | `POST` | `/recorrencias` | Cria nova regra e projeta ocorrências | Bearer Token |
| **Recorrências** | `PUT` | `/recorrencias/{id}` | Atualiza plano e sincroniza filhas | Bearer Token |
| **Recorrências** | `PATCH` | `/recorrencias/{id}/status` | Ativa ou desativa a recorrência | Bearer Token |
| **Recorrências** | `DELETE` | `/recorrencias/{id}` | Exclui a regra de recorrência | Bearer Token |
| **Categorias** | `GET` | `/categorias` | Lista catálogo de categorias cadastradas | Bearer Token |
| **Categorias** | `POST` | `/categorias` | Cria uma nova categoria | Bearer Token |
| **Categorias** | `PUT` | `/categorias/{id}` | Atualiza uma categoria | Bearer Token |
| **Categorias** | `DELETE` | `/categorias/{id}` | Exclui uma categoria | Bearer Token |
| **Orçamentos** | `GET` | `/orcamentos` | Lista metas e consumo em tempo real | Bearer Token |
| **Orçamentos** | `GET` | `/orcamentos/competencia` | Consulta orçamento de uma competência específica | Bearer Token |
| **Orçamentos** | `POST` | `/orcamentos` | Define teto mensal para categoria de despesa | Bearer Token |
| **Orçamentos** | `PUT` | `/orcamentos/{id}` | Atualiza o teto do orçamento | Bearer Token |
| **Orçamentos** | `DELETE` | `/orcamentos/{id}` | Exclui uma meta orçamentária | Bearer Token |
| **Dashboard** | `GET` | `/dashboard` | Retorna métricas agregadas via CQRS | Bearer Token |

---

## 🧪 Testes Automatizados

A aplicação conta com uma suíte de **30 testes unitários automatizados** cobrindo Value Objects, Agregados, Regras de Negócio e Casos de Uso:

```bash
mvn test
```

Saída esperada:
```
[INFO] Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 👨‍💻 Autor

Desenvolvido por **[Luiz Brandão](https://github.com/LuizhBrandao)**.
