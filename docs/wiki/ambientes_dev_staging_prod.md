# Guia de Ambientes: Dev, Staging e Prod

Este documento estabelece a arquitetura, governança e operação dos três ambientes do sistema **Gerenciador Financeiro**: **Dev** (Desenvolvimento), **Staging** (Homologação / Pré-produção) e **Prod** (Produção).

---

## 1. Visão Geral e Matriz Comparativa

| Critério / Recurso | Dev (Desenvolvimento) | Staging (Homologação) | Prod (Produção) |
| :--- | :--- | :--- | :--- |
| **Spring Profile** | `dev` | `staging` | `prod` |
| **Branch Git** | `develop` | `staging` | `main` |
| **Porta da Aplicação** | `8080` | `8081` | `80` (ou `8080` reverso) |
| **Porta do MySQL** | `3306` (host) | `3307` (host) | Interna na rede Docker |
| **Nome do Banco** | `financeiro_dev_db` | `financeiro_staging_db` | `financeiro_prod_db` |
| **Hibernate DDL** | `update` | `update` (ou `validate`) | `validate` (Estrito) |
| **Show SQL / Logs** | `true` (DEBUG / TRACE) | `false` (INFO) | `false` (WARN / INFO) |
| **Swagger / OpenAPI** | Ativado (`/swagger-ui.html`) | Ativado (`/swagger-ui.html`) | **Desativado** (Segurança) |
| **Carga de Dados Demo** | **Ativado** (`DataInitializer`) | **Desativado** | **Desativado** |
| **Actuator Probes** | `health`, `info`, `metrics` | `health`, `info`, `metrics` | `health`, `info` (sem detalhes) |
| **HikariCP Pool** | Padrão local | Max: 10, Min-Idle: 3 | Max: 20, Min-Idle: 5, Leak: 2s |
| **Docker Compose** | `docker-compose.yml` | `docker-compose.staging.yml` | `docker-compose.prod.yml` |
| **Arquivo de Env** | `.env` (`.env.dev.example`) | `.env.staging` (`.env.staging.example`) | `.env.prod` (`.env.prod.example`) |

---

## 2. Detalhamento dos Ambientes

### 2.1. Ambiente de Desenvolvimento (Dev)
* **Objetivo:** Produtividade, agilidade para novas features e depuração local.
* **Características:**
  - Auto-geração e atualização de tabelas (`ddl-auto: update`).
  - `DataInitializer` ativo para popular usuário de testes (`teste@financeiro.com` / `123456`), contas e transações de exemplo.
  - Swagger UI e logs detalhados de queries SQL para fácil diagnóstico.
  - Pode rodar apenas o banco MySQL via Docker (`docker compose up -d`) e a aplicação via IDE (IntelliJ/VS Code), ou a stack completa (`docker compose --profile full up -d`).

### 2.2. Ambiente de Homologação / Staging (Pré-Produção)
* **Objetivo:** Validação de QA, testes de carga, homologação de regras de negócio com usuários-chave e validação de contratos com front-end antes da liberação.
* **Características:**
  - Isolamento de rede (`staging_network`) e porta dedicada (`8081`).
  - Sem injeção de dados falsos em runtime (`app.seed-demo-data: false`), garantindo que apenas dados de teste controlados existam.
  - Swagger UI mantido ativo para auditoria de contratos e testes manuais da equipe de QA.
  - Probes de saúde monitoradas via `/actuator/health`.

### 2.3. Ambiente de Produção (Prod)
* **Objetivo:** Máxima segurança, alta disponibilidade, performance e conformidade com dados financeiros reais.
* **Características:**
  - **Segurança Reforçada:** Documentação Swagger e OpenAPI completamente desligadas para evitar varredura de endpoints.
  - **Validação Estrita de Schema:** `ddl-auto: validate` assegura que alterações de estrutura ocorram somente via scripts de migração planejados.
  - **Hikari Connection Pool de Alta Capacidade:** Ajustado para suporte a concorrência e detecção de leaks de conexão.
  - **Limites de Recursos Docker:** Limitação de CPU e memória (`cpus: 1.5`, `memory: 1024M`) e rotação de logs em disco (`max-size: 10m`, `max-file: 3`).
  - **Obrigatoriedade de Segredos Fortes:** Nenhuma credencial padrão é aceita; todas as variáveis devem vir injetadas por `.env.prod`.

---

## 3. Comandos de Operação

### Utilizando o Script de Gerenciamento (`scripts/manage-env.ps1` ou `scripts/manage-env.sh`)

#### No Windows (PowerShell):
```powershell
# 1. Desenvolvimento (Apenas MySQL para rodar a aplicação na IDE):
.\scripts\manage-env.ps1 -Environment dev -Action up

# 1.1. Desenvolvimento (Stack completa em Docker):
.\scripts\manage-env.ps1 -Environment dev -Action up -FullDev

# 2. Homologação / Staging:
.\scripts\manage-env.ps1 -Environment staging -Action up

# 3. Produção:
.\scripts\manage-env.ps1 -Environment prod -Action up

# Ver status dos contêineres:
.\scripts\manage-env.ps1 -Environment staging -Action status

# Ver logs em tempo real:
.\scripts\manage-env.ps1 -Environment prod -Action logs

# Parar ambiente:
.\scripts\manage-env.ps1 -Environment dev -Action down
```

#### No Linux / macOS (Bash):
```bash
chmod +x ./scripts/manage-env.sh

# Subir Staging
./scripts/manage-env.sh staging up

# Subir Produção
./scripts/manage-env.sh prod up
```

---

## 4. Governança de Branches Git

Seguindo o fluxo de entrega contínua e as regras de **DoR (Definition of Ready)** e **DoD (Definition of Done)**:

1. **`develop`**: Código ativo em desenvolvimento. O pipeline de CI executa testes unitários a cada push/PR.
2. **`staging`**: Branch de release candidata. O merge para `staging` aciona a esteira de validação integrada e deploy no ambiente de homologação.
3. **`main`**: Código aprovado em staging e pronto para produção. Deploy restrito e tagueamento de versão (`v1.x.x`).
