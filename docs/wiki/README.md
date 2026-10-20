# Wiki de Engenharia de Software e Arquitetura DDD

Bem-vindo à documentação viva e consolidada do **Gerenciador Financeiro**.

Esta Wiki reúne as decisões de Engenharia Upstream (Levantamento de Requisitos, Discovery, Event Storming, Domain Storytelling e Mitigação de Riscos) e Engenharia Downstream (Design Tático DDD, 4 Camadas, Encapsulamento de Agregados, Value Objects, Concorrência Otimista e Otimização CQRS).

---

##  Índice da Documentação

1. [**Visão Geral, Diagnóstico de Riscos e Anatomia da Solução**](visao_geral_e_dominio.md)
   * Visão do Produto e Persona
   * Matriz dos 4 Grandes Riscos de Marty Cagan
   * Comparativo da Jornada: Cenário Atual (*AS IS*) vs. Cenário Alvo (*TO BE*)
   * Requisitos Funcionais (RF) e Não-Funcionais (RNF)

2. [**Dicionário da Linguagem Ubíqua e Contextos Delimitados**](linguagem_ubiqua.md)
   * Termos Ubíquos Universais e Proibições Terminológicas
   * Mapa de Contextos Delimitados (*Context Map*)
   * Padrões Estratégicos de Integração e Camadas Anticorrupção (*ACL*)

3. [**Arquitetura e Design Tático DDD**](arquitetura_e_design_tatico.md)
   * Organização do Código nas 4 Camadas Rigorosas
   * Blocos de Construção: Agregados, Entidades e Value Objects
   * Comandos de Negócio vs. Proibição de Anemic Domain Model
   * Concorrência Otimista (*Optimistic Locking*) e Desativação do OSIV

4. [**Domain Storytelling e Event Storming**](event_storming_e_fluxos.md)
   * Narrativa Sequencial de Domínio (Atores, Atividades e Objetos de Trabalho)
   * Big Picture do Event Storming: Eventos Pivotais, Comandos, Políticas e Read Models
   * Hotspots Mapeados e Resolvidos no Domínio

5. [**Contratos de API REST e Tratamento de Exceções**](api_e_contratos.md)
   * Especificação dos Endpoints REST por Contexto
   * DTOs e Isolamento dos Modelos de Entrada/Saída
   * Mapeamento de Códigos HTTP e Resiliência (HTTP 400, 404, 409, 422)
   * Governança de Portões de Qualidade (DoR e DoD)

---

> [!NOTE]
> Esta documentação é mantida em sincronia com o código-fonte da aplicação e reflete os padrões exigidos pelas diretrizes de arquitetura DDD e governança upstream.
