# Dicionário de dados — schema vigente

Schema V1–V9. Seis tabelas de negócio e 47 campos, todos documentados com comentários breves. Carteiras selecionam instituições pelos vínculos em `contas`; operações são fatos próprios do usuário/instituição.

Saldo inicial implementado por V9, com catálogo de seis ativos, ponto de partida e fatos versionados. [Contrato e operação](12-saldo-inicial.md).

Tipos, nulabilidade e defaults estão na coluna Definição. Os campos `tipo` usam VARCHAR com CHECK para restringir as opções. `flyway_schema_history` é infraestrutura gerenciada pelo Flyway.

## carteiras

Carteiras de investimento do usuário.

| Campo | Definição | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador da carteira. |
| `perfil_ref` | `VARCHAR(120) COLLATE utf8mb4_bin NOT NULL` | Referência do usuário proprietário da carteira. |
| `nome` | `VARCHAR(160) NOT NULL` | Nome da carteira, único por usuário. |
| `ativo` | `BOOLEAN NOT NULL DEFAULT TRUE` | Situação da carteira: 1 = ativa; 0 = arquivada. |
| `criado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | Data e hora de criação do cadastro em UTC. |

Chaves: PK `id`; UNIQUE `uk_carteira_nome(perfil_ref, nome)`, inclusive para arquivadas.

## instituicoes

Instituições de custódia dos investimentos.

| Campo | Definição | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador da instituição. |
| `codigo` | `VARCHAR(80) COLLATE utf8mb4_bin NOT NULL` | Código único da instituição. |
| `nome` | `VARCHAR(160) NOT NULL` | Nome da instituição. |
| `pais_sede` | `CHAR(2) NULL` | Código do país da sede no padrão ISO 3166-1 alpha-2. |
| `ativo` | `BOOLEAN NOT NULL DEFAULT TRUE` | Situação da instituição: 1 = ativa; 0 = inativa. |

Chaves: PK `id`; UNIQUE `codigo`.

## ativos

Catálogo de moedas e criptoativos.

| Campo | Definição | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador do ativo. |
| `codigo` | `VARCHAR(80) COLLATE utf8mb4_bin NOT NULL` | Código único do ativo. |
| `ticker` | `VARCHAR(32) NOT NULL` | Símbolo de exibição do ativo. |
| `nome` | `VARCHAR(160) NOT NULL` | Nome do ativo. |
| `tipo` | `VARCHAR(6) NOT NULL` | Tipo do ativo: FIAT ou CRIPTO. |
| `casas_decimais` | `TINYINT UNSIGNED NOT NULL` | Número de casas decimais da quantidade, de 0 a 18. |
| `ativo` | `BOOLEAN NOT NULL DEFAULT TRUE` | Situação do ativo: 1 = ativo; 0 = inativo. |

Chaves: PK `id`; UNIQUE `codigo`; CHECK para `tipo` e `casas_decimais`.

## contas

Associações entre carteiras e instituições. O ID identifica o vínculo; duas associações à mesma instituição não representam históricos financeiros diferentes. Operações pertencem ao usuário/instituição e não usam este vínculo como proprietário. Os comentários de schema abaixo refletem a V8 aplicada, preservada neste esclarecimento.

| Campo | Definição | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador da conta. |
| `carteira_id` | `BIGINT UNSIGNED NOT NULL` | Carteira proprietária da conta. |
| `instituicao_id` | `BIGINT UNSIGNED NOT NULL` | Instituição de custódia da conta. |
| `criado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | Data e hora de criação do cadastro em UTC. |

Chaves: PK `id`; UNIQUE `uk_conta_carteira_instituicao(carteira_id, instituicao_id)`; FKs para `carteiras.id` e `instituicoes.id`, sem exclusão em cascata no banco. Não há `contas.perfil_ref` ou tabela `carteira_contas`; o usuário proprietário é obtido pela carteira.


## pontos_partida

| Campo | Definição | Finalidade |
| --- | --- | --- |
| `perfil_ref` | `VARCHAR(120) COLLATE utf8mb4_bin NOT NULL PRIMARY KEY` | Referência do usuário proprietário do histórico. |
| `data_referencia` | `DATE NOT NULL` | Data de início do acompanhamento financeiro. |
| `criado_em` | `DATETIME(6) NOT NULL` | Data e hora de criação em UTC. |
| `criado_por` | `VARCHAR(120) COLLATE utf8mb4_bin NOT NULL` | Referência do autor da criação. |
| `alterado_em` | `DATETIME(6) NOT NULL` | Data e hora da última alteração em UTC. |
| `alterado_por` | `VARCHAR(120) COLLATE utf8mb4_bin NOT NULL` | Referência do autor da última alteração. |

Chave primária: referência do perfil. Coordena a data comum e o bloqueio transacional do histórico, sem tabela de autenticação.

## operacoes

| Campo | Definição | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY` | Identificador da versão do lançamento. |
| `perfil_ref` | `VARCHAR(120) COLLATE utf8mb4_bin NOT NULL` | Referência do usuário proprietário do lançamento. |
| `instituicao_id` | `BIGINT UNSIGNED NOT NULL` | Instituição de custódia do lançamento. |
| `ativo_id` | `BIGINT UNSIGNED NOT NULL` | Ativo movimentado. |
| `tipo` | `VARCHAR(20) NOT NULL` | Tipo de lançamento: SALDO_INICIAL. |
| `data_referencia` | `DATE NOT NULL` | Data de negócio do lançamento. |
| `ocorrida_em` | `DATETIME(6) NOT NULL` | Instante do lançamento em UTC. |
| `quantidade` | `DECIMAL(38,18) NOT NULL` | Quantidade do ativo. |
| `custo_total_brl` | `DECIMAL(38,8) NOT NULL` | Custo histórico total em reais. |
| `situacao` | `VARCHAR(12) NOT NULL` | Situação: CONFIRMADA, SUBSTITUIDA ou CANCELADA. |
| `confirmada` | `TINYINT GENERATED ALWAYS AS (IF(situacao = 'CONFIRMADA', 1, NULL)) STORED` | Marcador de unicidade da abertura confirmada. |
| `substitui_id` | `BIGINT UNSIGNED NULL` | Versão anterior substituída pelo lançamento. |
| `justificativa_custo_zero` | `VARCHAR(300) NULL` | Justificativa do custo histórico zero. |
| `motivo_alteracao` | `VARCHAR(300) NULL` | Motivo da correção ou cancelamento. |
| `chave_requisicao` | `CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL` | Chave de repetição segura da solicitação. |
| `fingerprint` | `CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL` | Resumo dos dados normalizados da solicitação. |
| `criado_em` | `DATETIME(6) NOT NULL` | Data e hora de criação em UTC. |
| `criado_por` | `VARCHAR(120) COLLATE utf8mb4_bin NOT NULL` | Referência do autor da criação. |
| `alterado_em` | `DATETIME(6) NOT NULL` | Data e hora da última alteração em UTC. |
| `alterado_por` | `VARCHAR(120) COLLATE utf8mb4_bin NOT NULL` | Referência do autor da última alteração. |

Chaves: PK id; UNIQUE perfil/chave de requisição; UNIQUE versão substituída; UNIQUE perfil/instituição/ativo/marcador de confirmação. Marcador 1 para confirmada e NULL para históricas. FKs para ponto de partida, instituição, ativo e versão anterior, sem exclusão em cascata. Não há FK para carteiras ou vínculos. Quantidade positiva, custo não negativo e justificativa obrigatória para custo zero, protegidos por CHECK.

## Consultar no MySQL Workbench

```sql
SHOW FULL COLUMNS FROM lastro_local.carteiras;

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'lastro_local'
  AND TABLE_NAME <> 'flyway_schema_history'
ORDER BY TABLE_NAME, ORDINAL_POSITION;
```

Documentação complementar: [modelagem vigente](10-modelagem-de-carteiras.md), [cadastro de carteiras](09-cadastro-de-carteiras.md) e [contexto de acesso](07-acesso-local.md). Catálogo inicial: Binance (`binance`) e BitGet (`bitget`), ativas e sem país informado.

O [dicionário do modelo v1](../lastro-modelo-mysql-v1/lastro-modelo-v1/DICIONARIO.md) é histórico. As migrations aplicadas permanecem imutáveis; alterações dos comentários são feitas por novas migrations.

## Correspondência com enums Java — 03/11/2026

| Coluna SQL preservada | Tipo Java | Valores |
|---|---|---|
| ativos.tipo VARCHAR(6) | ativos.domain.model.TipoAtivo | FIAT, CRIPTO |
| operacoes.tipo VARCHAR(20) | operacoes.domain.model.TipoOperacao | SALDO_INICIAL |
| operacoes.situacao VARCHAR(12) | operacoes.domain.model.SituacaoOperacao | CONFIRMADA, SUBSTITUIDA, CANCELADA |

Persistência textual pelo nome, sem ORDINAL ou alteração de schema. TODAS existe somente no filtro HTTP. [Análise e compatibilidade](15-enums-de-dominio.md).
