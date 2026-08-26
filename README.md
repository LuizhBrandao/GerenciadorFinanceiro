# 💰 Gerenciador Financeiro Pessoal

[![Java](https://img.shields.io/badge/Java-21-orange.svg?style=flat&logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg?style=flat&logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED.svg?style=flat&logo=docker)](https://www.docker.com/)
[![JavaScript](https://img.shields.io/badge/JavaScript-ES6+-F7DF1E.svg?style=flat&logo=javascript)](https://developer.mozilla.org/)
[![TailwindCSS](https://img.shields.io/badge/TailwindCSS-3.x-38B2AC.svg?style=flat&logo=tailwind-css)](https://tailwindcss.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Aplicação Full-Stack completa para controle financeiro pessoal, desenvolvida com **Spring Boot 3 (Java 21)**, banco de dados **PostgreSQL**, conteinerização com **Docker**, e uma interface frontend moderna e responsiva em **JavaScript (Single Page Application)** com **Tailwind CSS** e gráficos interativos **Chart.js**.

---

## 📑 Sumário

- [Visão Geral & Arquitetura](#-visão-geral--arquitetura)
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

## 🏛️ Visão Geral & Arquitetura

O sistema adota o padrão de arquitetura em camadas (**Layered Architecture**) no backend e uma **SPA modular** no frontend:

```
┌─────────────────────────────────────────────────────────────┐
│                 Frontend SPA (HTML5 / JS / CSS)             │
│   (Dashboard, Gráficos Chart.js, Gestão de Contas, Filtros) │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP / JSON (JWT Bearer)
┌──────────────────────────────▼──────────────────────────────┐
│                  Spring Boot REST API Layer                 │
│  - Controllers & DTOs                                       │
│  - Spring Security + JWT Filter (Stateless)                 │
│  - Services & Business Rules (POO / SOLID / Transactions)   │
│  - Repositories (Spring Data JPA + Specifications)          │
└──────────────────────────────┬──────────────────────────────┘
                               │ JDBC
┌──────────────────────────────▼──────────────────────────────┐
│                    PostgreSQL 15 (Docker)                   │
└─────────────────────────────────────────────────────────────┘
```

---

## ✨ Funcionalidades Principais

### 🔐 1. Autenticação & Segurança
- Cadastro e login de usuários com senhas criptografadas via **BCrypt**.
- Emissão e validação de tokens **JWT (JSON Web Token)** stateless.
- Proteção de rotas da API com Spring Security e injeção automática do usuário autenticado (`@AuthenticationPrincipal`).
- Configuração de **CORS** para permitir desenvolvimento desacoplado.

### 📊 2. Dashboard Financeiro
- Indicadores em tempo real:
  - **Saldo Consolidado Total** (somatório de contas ativas).
  - **Total de Receitas do Mês**.
  - **Total de Despesas do Mês**.
  - **Balanço Líquido** com indicador visual de saldo positivo ou negativo.
- Gráficos interativos (**Chart.js**):
  - *Distribuição de Despesas por Categoria* (Gráfico de Rosca / Doughnut).
  - *Evolução Mensal Receitas vs Despesas* dos últimos 6 meses (Gráfico de Barras).
- Tabela dinâmica com as transações mais recentes.

### 💳 3. Gestão de Contas & Transferências
- Cadastro, edição e exclusão de contas bancárias (Conta Corrente, Poupança, Investimento, Carteira Física/Dinheiro).
- **Transferência entre contas**: Débito e crédito automáticos entre contas com validação de saldo e regras de negócio transacionais.
- **Extrato Detalhado**: Histórico de movimentações por conta (`/contas/{id}/extrato`).

### 💸 4. Transações Financeiras
- Lançamento de **Receitas** e **Despesas** com atualização automática do saldo da conta.
- Suporte a **Lançamento Parcelado**: Divisão automática de compras em N parcelas mensais (`/transacoes/parcelado`).
- **Filtros Avançados**:
  - Busca por intervalo de datas (inicial e final).
  - Filtro por Categoria e por Conta Bancária.
  - Filtro por Faixa de Valor (mínimo e máximo).
- Edição e exclusão de transações com estorno automático de saldos.

### 🏷️ 5. Categorias Customizadas
- Cadastro e listagem de categorias personalizadas (ex: *Alimentação, Moradia, Transporte, Lazer, Salário, Investimentos*).
- Seleção de ícones visuais (FontAwesome).

---

## 🛠️ Tecnologias Utilizadas

### Backend
- **Java 21**
- **Spring Boot 3.3.4**
- **Spring Data JPA / Hibernate**
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
├── src/
│   ├── main/
│   │   ├── java/br/com/gerenciadorfinanceiro/
│   │   │   ├── config/                    # Configurações (Security, CORS, JWT, OpenAPI)
│   │   │   ├── controller/                # Controllers REST e DTOs
│   │   │   ├── exception/                 # Tratamento global de exceções
│   │   │   ├── model/                     # Entidades JPA e Enums
│   │   │   ├── repository/                # Interfaces Spring Data JPA e Specifications
│   │   │   └── service/                   # Regras de negócio e transações
│   │   └── resources/
│   │       ├── application.yml            # Configurações do Spring Boot
│   │       └── static/                    # Frontend SPA
│   │           ├── index.html             # Interface principal
│   │           ├── css/style.css          # Estilos customizados
│   │           └── js/                    # Módulos JavaScript (api, auth, dashboard, contas, etc.)
│   └── test/
│       └── resources/application-test.yml # Configuração de banco em memória para testes
```

---

## ⚙️ Pré-requisitos

Antes de iniciar, certifique-se de ter instalado em sua máquina:
- [JDK 21+](https://www.oracle.com/java/technologies/downloads/#java21)
- [Apache Maven 3.8+](https://maven.apache.org/) (ou use o Maven Wrapper)
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
- **Via VS Code Live Server (Opcional)**: Caso prefira rodar o frontend desacoplado para hot-reload, abra a pasta `src/main/resources/static/` com a extensão *Live Server* (`http://127.0.0.1:5500/`).

---

## 📖 Documentação da API (Swagger / OpenAPI)

Com a aplicação em execução, acesse a documentação interativa do Swagger UI:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

---

## 📡 Endpoints da API REST

| Módulo | Método | Endpoint | Descrição | Autenticação |
| :--- | :--- | :--- | :--- | :--- |
| **Auth** | `POST` | `/auth/register` | Cria uma nova conta de usuário | Pública |
| **Auth** | `POST` | `/auth/login` | Realiza login e retorna o Token JWT | Pública |
| **Contas** | `GET` | `/contas` | Lista todas as contas do usuário logado | Bearer Token |
| **Contas** | `GET` | `/contas/{id}` | Obtém os detalhes de uma conta | Bearer Token |
| **Contas** | `GET` | `/contas/saldo-consolidado` | Retorna o somatório dos saldos | Bearer Token |
| **Contas** | `GET` | `/contas/{id}/extrato` | Retorna o extrato detalhado de uma conta | Bearer Token |
| **Contas** | `POST` | `/contas` | Cadastra uma nova conta bancária | Bearer Token |
| **Contas** | `PUT` | `/contas/{id}` | Atualiza dados de uma conta | Bearer Token |
| **Contas** | `DELETE` | `/contas/{id}` | Exclui uma conta bancária | Bearer Token |
| **Contas** | `POST` | `/contas/transferir` | Realiza transferência de saldo entre contas | Bearer Token |
| **Transações** | `GET` | `/transacoes` | Lista todas as transações | Bearer Token |
| **Transações** | `GET` | `/transacoes/busca` | Filtro avançado (datas, categoria, conta, valores) | Bearer Token |
| **Transações** | `GET` | `/transacoes/{id}` | Busca transação por ID | Bearer Token |
| **Transações** | `POST` | `/transacoes` | Cria uma transação simples | Bearer Token |
| **Transações** | `POST` | `/transacoes/parcelado` | Cria transação parcelada em N meses | Bearer Token |
| **Transações** | `PUT` | `/transacoes/{id}` | Atualiza uma transação existente | Bearer Token |
| **Transações** | `DELETE` | `/transacoes/{id}` | Exclui transação e reajusta o saldo | Bearer Token |
| **Categorias** | `GET` | `/categorias` | Lista categorias cadastradas | Bearer Token |
| **Categorias** | `POST` | `/categorias` | Cria uma nova categoria | Bearer Token |
| **Categorias** | `DELETE` | `/categorias/{id}` | Exclui uma categoria | Bearer Token |

---

## 🧪 Testes

Para executar a suíte de testes automatizados com o banco H2 em memória:
```bash
mvn test
```

---

## 👤 Autor

Desenvolvido por **[Luiz Brandão](https://github.com/LuizhBrandao)**.
