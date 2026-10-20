# 5. Contratos de API REST e Tratamento de Exceções

## 5.1 Endpoints REST da Aplicação

Todos os endpoints operam sob o padrão RESTful com serialização JSON. As rotas privadas exigem cabeçalho HTTP:
`Authorization: Bearer <token_jwt>`

### 1. Autenticação e Usuários (`/auth`)
* `POST /auth/register`: Cria novo usuário com suas preferências padrão e inicializa o catálogo de categorias essenciais. Retorna HTTP 200 OK.
* `POST /auth/login`: Autentica credenciais e retorna o token JWT (`LoginResponseDto`). Retorna HTTP 200 OK.

### 2. Contas Bancárias (`/contas`)
* `GET /contas`: Lista as contas do usuário com saldo atual.
* `GET /contas/{id}`: Retorna os detalhes de uma conta específica.
* `POST /contas`: Cadastra uma nova conta.
* `PUT /contas/{id}`: Atualiza os dados cadastrais da conta.
* `DELETE /contas/{id}`: Desativa/exclui a conta.
* `POST /contas/transferir`: Realiza transferência entre contas via JSON body (`origemId`, `destinoId`, `valor`).
* `GET /contas/{id}/extrato`: Retorna o extrato detalhado de transações da conta, ordenado decrescentemente por data.
* `GET /contas/resumo-saldos`: Retorna o resumo consolidado de saldos em conta corrente, poupança e saldo geral.

### 3. Transações Financeiras (`/transacoes`)
* `GET /transacoes`: Lista todas as transações do usuário.
* `GET /transacoes/busca`: Consulta avançada com filtros dinâmicos de período, categoria, conta e faixa de valores.
* `GET /transacoes/{id}`: Detalha uma transação por ID.
* `POST /transacoes`: Cria uma nova transação simples com processamento de tags.
* `POST /transacoes/parcelado`: Cria compras parceladas em N vezes com rateio sem perda de centavos.
* `PUT /transacoes/{id}`: Retifica dados de uma transação com ajuste automático no saldo da conta.
* `PATCH /transacoes/{id}/efetivar`: Transiciona o status de uma transação pendente para `PAGA` e debita da conta.
* `DELETE /transacoes/{id}`: Remove a transação e reverte o impacto de saldo caso estivesse paga.

### 4. Transações Recorrentes (`/recorrencias`)
* `GET /recorrencias`: Lista todas as regras de recorrência cadastradas.
* `GET /recorrencias/ativas`: Lista apenas as recorrências ativas.
* `GET /recorrencias/{id}`: Detalha uma regra de recorrência.
* `POST /recorrencias`: Cria nova regra de recorrência e projeta ocorrências automáticas no calendário.
* `PUT /recorrencias/{id}`: Atualiza os parâmetros do plano e sincroniza as transações vinculadas por `recorrencia_id`.
* `PATCH /recorrencias/{id}/status`: Alterna o status ativo/inativo da recorrência.
* `DELETE /recorrencias/{id}`: Exclui a regra de recorrência.
* `POST /recorrencias/{id}/lancar`: Força o lançamento imediato de uma instância avulsa da recorrência.

### 5. Categorias (`/categorias`)
* `GET /categorias`: Lista o catálogo de categorias cadastradas para o usuário (operação estritamente idempotente de leitura).
* `GET /categorias/{id}`: Busca uma categoria por ID.
* `POST /categorias`: Cria nova categoria personalizada.
* `PUT /categorias/{id}`: Atualiza nome, tipo, ícone e descrição da categoria.
* `DELETE /categorias/{id}`: Exclui a categoria.

### 6. Orçamentos Mensais (`/orcamentos`)
* `GET /orcamentos`: Lista todos os limites orçamentários do usuário com apuração de consumo em tempo real.
* `GET /orcamentos/competencia?ano={ano}&mes={mes}`: Lista os limites e gastos de uma competência específica.
* `GET /orcamentos/{id}`: Detalha um orçamento específico.
* `POST /orcamentos`: Define teto mensal para uma categoria de despesa (`categoriaId`, `ano`, `mes`, `valorLimite`, `observacao`).
* `PUT /orcamentos/{id}`: Atualiza o teto ou observação do orçamento.
* `DELETE /orcamentos/{id}`: Exclui a meta orçamentária.

### 7. Dashboard (`/dashboard`)
* `GET /dashboard`: Retorna indicadores sintetizados através de consultas agregadas:
  * Saldos consolidados (contas correntes, poupanças e total);
  * Métricas do mês atual (receitas pagas, despesas pagas, balanço e valores pendentes);
  * Top 5 transações mais recentes;
  * Despesas agrupadas por categoria com totais e percentuais;
  * Evolução semântica dos últimos 6 meses.

---

## 5.2 Mapeamento de Exceções e Códigos HTTP

O `GlobalExceptionHandler` padroniza os erros da aplicação no formato `StandardError`:

```json
{
  "timestamp": "2026-10-20T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Violação de integridade de dados: registro duplicado ou com dependências vinculadas.",
  "path": "/contas/1"
}
```

| Exceção | Código HTTP | Cenário de Ocorrência |
| :--- | :---: | :--- |
| `EntidadeNaoEncontradaException` | **404 Not Found** | Recurso não existe ou pertence a outro usuário. |
| `RegraNegocioException` | **400 Bad Request** | Violação de regra de negócio (ex: orçamento para categoria de receita, e-mail já existente). |
| `SaldoInsuficienteException` | **400 Bad Request** | Tentativa de débito/saque maior que o saldo em conta corrente. |
| `OrcamentoUltrapassadoException` | **400 Bad Request** | Alerta de estouro de teto orçamentário. |
| `MethodArgumentNotValidException` | **422 Unprocessable Entity** | Falha de validação dos campos obrigatórios da requisição (@NotNull, @Positive, etc.). |
| `DataIntegrityViolationException` | **409 Conflict** | Violação de chave única ou restrição de integridade referencial no banco de dados. |
| `ObjectOptimisticLockingFailureException` | **409 Conflict** | Conflito de concorrência detectado via `@Version` na entidade `Conta`. |
| `AccessDeniedException` | **403 Forbidden** | Usuário não tem permissão para acessar o recurso. |
| `AuthenticationException` | **401 Unauthorized** | Credenciais inválidas ou token JWT expirado/inválido. |
| `Exception` (Genérica) | **500 Internal Server Error** | Erros inesperados não tratados. |
