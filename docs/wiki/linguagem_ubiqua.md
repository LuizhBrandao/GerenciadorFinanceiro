# 2. Dicionário da Linguagem Ubíqua e Mapa de Contextos Delimitados

## 2.1 Dicionário da Linguagem Ubíqua

Os termos abaixo constituem o vocabulário oficial e obrigatório em todo o código-fonte, classes, métodos, DTOs, tabelas de banco e endpoints do sistema.

| Termo Ubíquo | Significado no Domínio Financeiro | Sinônimos Proibidos (Anti-Padrões) |
| :--- | :--- | :--- |
| **Conta** | Recipiente patrimonial onde os recursos do usuário estão custodiados (Corrente, Poupança ou Investimento). Possui saldo e controle de concorrência. | "Carteira", "Bolsa", "Banco", "Caixinha" |
| **Transação** | Registro imutável de movimentação financeira que altera ou programa a alteração do saldo de uma conta em uma data específica. | "Movimento", "Lançamento Avulso", "Item" |
| **Transferência Patrimonial** | Movimentação de recursos entre duas contas do mesmo usuário, composta por saída da origem e entrada no destino sem gerar despesa ou receita operacional. | "TED", "PIX genérico", "Gasto entre contas" |
| **Categoria** | Agrupador temático e taxonômico exclusivo para transações e orçamentos, tipado estritamente como RECEITA ou DESPESA. | "Tag de pasta", "Tipo de gasto", "Setor" |
| **Recorrência** | Regra de plano temporal que gera transações de forma automatizada e previsível com frequência periódica (diária, semanal, quinzenal, mensal ou anual). | "Assinatura externa", "Mensalidade genérica", "Boleto" |
| **Orçamento** | Teto financeiro delimitado para uma categoria de despesa em uma competência mensal específica (mês e ano). | "Previsão", "Cota", "Meta de economia" |
| **Competência Mensal** | Identificador de período financeiro expresso pelo par `(mês, ano)`, utilizado para apuração orçamentária e métricas. | "Data de fechamento", "Mês/Ano solto" |
| **Valor Monetário** | Quantia financeira expressa em moeda corrente (`BRL`), com 2 casas decimais e precisão exata, imune a erros de ponto flutuante. | `Double`, `Float`, "Preço" |
| **Tag** | Rótulo livre e transversal para marcação contextual e busca cruzada de transações (ex: "Férias", "Reembolsável"). | "Etiqueta solta", "Observação" |

---

## 2.2 Mapa de Contextos Delimitados (*Context Map*)

A solução é decomposta em quatro Contextos Delimitados primários:

```mermaid
flowchart LR
    subgraph CTX_AUTH["Contexto de Identidade e Autenticação"]
        direction TB
        USUARIO["Usuário"]
        PREF["Preferências"]
    end

    subgraph CTX_CONTAS["Contexto de Gestão Patrimonial e Contas"]
        direction TB
        CONTA["Conta Bancária"]
        SALDO["Saldo Consolidado"]
    end

    subgraph CTX_TRANS["Contexto de Transações e Recorrências"]
        direction TB
        TRANS["Transação"]
        REC["Transação Recorrente"]
        TAG["Tag"]
    end

    subgraph CTX_ORC["Contexto de Orçamentos e Planejamento"]
        direction TB
        ORC["Orçamento Mensal"]
        CAT["Categoria"]
    end

    CTX_AUTH -->|Upstream / Fornecedor (Token JWT)| CTX_CONTAS
    CTX_AUTH -->|Upstream / Fornecedor (Token JWT)| CTX_TRANS
    CTX_AUTH -->|Upstream / Fornecedor (Token JWT)| CTX_ORC

    CTX_CONTAS <-->|Parceria / Consistência Transacional| CTX_TRANS
    CTX_ORC -.->|Consulta de Despesas Pagas (Read Side)| CTX_TRANS
```

### Relacionamentos Estratégicos entre Contextos:
1. **Identidade -> Todos os Contextos (Upstream / Downstream com Customer-Supplier):**
   O contexto de autenticação emite o Token JWT que carrega a identidade do usuário (`UsuarioPrincipal`). Todos os serviços downstream utilizam o identificador imutável do usuário (`usuarioId`) para particionamento de dados (Multi-Tenancy por usuário).
2. **Contas <-> Transações (Parceria e Consistência Forte):**
   A liquidação de uma transação atualiza atomicamente o saldo da conta associada (`conta.debitar()` ou `conta.creditar()`). Transferências utilizam o serviço de domínio `TransferenciaService` para orquestrar as duas contas de forma coordenada.
3. **Orçamentos e Categorias -> Transações (Read-Side Desacoplado):**
   O agregador de orçamento consulta o total gasto diretamente através de projeções de agregação SQL/JPQL (`somarDespesasPagasPorCategoriaECompetencia`), mantendo o modelo de escrita do orçamento imune a acoplamentos desnecessários com a entidade `Transacao`.
