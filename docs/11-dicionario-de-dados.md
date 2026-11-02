# Dicionário de dados — schema vigente

Schema V1–V8. Cinco tabelas de negócio e 23 campos, todos documentados com comentários breves. V8 substitui titulares por carteiras e contas do usuário; instituições e ativos são preservados.

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

Contas de custódia do usuário.

| Campo | Definição | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador da conta. |
| `perfil_ref` | `VARCHAR(120) COLLATE utf8mb4_bin NOT NULL` | Referência do usuário proprietário da conta. |
| `instituicao_id` | `BIGINT UNSIGNED NOT NULL` | Instituição de custódia da conta. |
| `criado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | Data e hora de criação do cadastro em UTC. |

Chaves: PK `id`; UNIQUE `uk_conta_usuario_instituicao(perfil_ref, instituicao_id)`; FK para `instituicoes.id`. Uma mesma conta pode compor várias carteiras.

## carteira_contas

Contas que compõem cada carteira.

| Campo | Definição | Finalidade |
| --- | --- | --- |
| `carteira_id` | `BIGINT UNSIGNED NOT NULL` | Carteira que reúne a conta. |
| `conta_id` | `BIGINT UNSIGNED NOT NULL` | Conta vinculada à carteira. |

Chaves: PK composta `(carteira_id, conta_id)`; FKs para `carteiras.id` e `contas.id`, sem exclusão em cascata. A service só associa contas do mesmo usuário da carteira.

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
