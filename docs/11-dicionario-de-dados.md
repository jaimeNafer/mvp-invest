# Dicionário de dados — schema vigente

Schema V1–V12, com a evolução financeira de V10 descrita ao final. Oito tabelas de negócio documentadas com comentários breves. Carteiras selecionam instituições pelos vínculos em `contas`; operações são fatos próprios do usuário/instituição.

Saldo inicial implementado por V9, com catálogo de seis ativos, ponto de partida e fatos versionados. [Contrato e operação](12-saldo-inicial.md).

Tipos, nulabilidade e defaults estão na coluna Definição. Os campos `tipo` usam VARCHAR com CHECK para restringir as opções. `flyway_schema_history` é infraestrutura gerenciada pelo Flyway.

V11 amplia somente os dados do catálogo com oito criptoativos; tipos, campos, constraints e comentários permanecem inalterados. [Códigos, nomes, precisão e compatibilidade](17-catalogo-de-ativos.md).

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

## V10 — negociações e revisão do histórico

| Tabela/campo | Tipo | Finalidade |
|---|---|---|
| pontos_partida.revisao | BIGINT UNSIGNED | Revisão financeira por usuário, incrementada em alterações efetivas. |
| operacoes.tipo | VARCHAR(20) | SALDO_INICIAL, APORTE, COMPRA ou VENDA; enum TipoOperacao. |
| operacoes.custo_total_brl | DECIMAL(38,8), nulo fora do saldo inicial | Custo histórico informado na abertura. Resultados das negociações são calculados. |
| operacoes.moeda_cotacao_id | BIGINT UNSIGNED, opcional | Ativo utilizado para cotar a negociação. |
| operacoes.total_bruto | DECIMAL(38,18), opcional | Total bruto executado na moeda de cotação, sem taxa. |
| operacoes.cotacao_brl | DECIMAL(38,18), opcional | Valor histórico de uma unidade da moeda de cotação em reais. |
| operacoes.taxa_ativo_id | BIGINT UNSIGNED, opcional | Uma das duas moedas negociadas quando há taxa. |
| operacoes.taxa_quantidade | DECIMAL(38,18) | Quantidade da taxa; zero quando ausente. |
| operacoes.referencia | VARCHAR(160), opcional | Referência do documento, sem unicidade obrigatória. |
| operacoes.ordem_livro | BIGINT UNSIGNED | Desempate estável; preservado entre versões da mesma raiz. |
| operacoes.versao_politica | INT UNSIGNED | Versão da política de cálculo, inicialmente 1. |
| operacoes.confirmada | TINYINT gerado | Marcador de unicidade somente do SALDO_INICIAL confirmado. |

Moedas de cotação e taxa têm FKs para ativos; fatos continuam pertencendo ao usuário/instituição, sem FK para carteira ou vínculo. Compra/venda exigem moeda distinta e total/cotação positivos. Aporte e abertura não recebem esses campos. Taxa zero não recebe moeda; taxa positiva usa ativo ou moeda de cotação. Instantes, quantidades, ciclo de vida e cadeia de substituição existentes são preservados. [Contrato das negociações](16-negociacoes.md).

## importacoes — V12

Recibos e arquivos confirmados de importação por usuário.

| Campo | Definição | Finalidade |
| --- | --- | --- |
| id | BIGINT UNSIGNED, PK, AUTO_INCREMENT | Identificador do recibo. |
| perfil_ref | VARCHAR(120), utf8mb4_bin, NOT NULL | Referência do usuário proprietário. |
| chave_requisicao | CHAR(36), ascii_bin, NOT NULL | Chave de repetição segura da confirmação. |
| hash | CHAR(64), ascii_bin, NOT NULL | SHA-256 do arquivo original. |
| decisoes | VARCHAR(64), NOT NULL | Confirmações de aportes próprios, taxas incluídas e posição anterior zerada. |
| nome_arquivo | VARCHAR(255), NOT NULL | Nome do arquivo importado. |
| versao_leitor | VARCHAR(40), NOT NULL | Versão do formato interpretado. |
| arquivo | MEDIUMBLOB, NOT NULL | Conteúdo original do arquivo confirmado. |
| criado_em | DATETIME(6), NOT NULL | Instante da confirmação em UTC. |
| novos | INT, NOT NULL | Quantidade de operações registradas. |
| reconhecidos | INT, NOT NULL | Quantidade de operações anteriormente registradas. |

Unicidades por `(perfil_ref,chave_requisicao)` e `(perfil_ref,hash)`. Índice de histórico por perfil, instante e ID. Todas as colunas não nulas e sem default.

## importacoes_eventos — V12

Vínculos entre eventos de arquivos confirmados e operações financeiras.

| Campo | Definição | Finalidade |
| --- | --- | --- |
| id | BIGINT UNSIGNED, PK, AUTO_INCREMENT | Identificador do vínculo. |
| importacao_id | BIGINT UNSIGNED, NOT NULL | Recibo que contém o evento. |
| linha | INT, NOT NULL | Número do registro no arquivo de origem, incluindo cabeçalho. |
| referencia | VARCHAR(160), utf8mb4_bin, NOT NULL | Referência do evento na instituição. |
| operacao_id | BIGINT UNSIGNED, NOT NULL | Operação financeira registrada ou reconhecida. |

UNIQUE `(importacao_id,referencia)`. FKs para `importacoes.id` e `operacoes.id`, sem exclusão em cascata. O vínculo usa a operação original mesmo depois de correção/cancelamento. [Regras e API](19-importacao-binance.md).


## Conciliação progressiva — V13

| Tabela | Campos | Finalidade |
| --- | --- | --- |
| contas_custodia | id, perfil_ref, instituicao_id, codigo, nome, criado_em | Conta única por proprietário, instituição e código; legado associado a Não especificada. |
| registros_conferencia | id, perfil_ref, arquivo_ref, origem_ref, instituicao_id, original_json, erros_json, proposta_json, aplicada_json, versao, revisado_por, revisado_em, criado_em | Originais imutáveis, proposta versionada e última proposta aplicada separados; origem única por proprietário. |
| ajustes_conciliatorios | id, perfil_ref, lacuna_ref, ancora_ref, conta_custodia_id, instituicao_id, ativo_id, quantidade, custo_total_brl, versao, ativo, vigente, anterior_id, aceito_por, criado_em, encerrado_em | Versões aceitas de lacunas com custo opcional e posição técnica pela âncora. Marcador vigente gerado limita uma versão ativa. |
| comandos_conciliacao | id, perfil_ref, chave, fingerprint, resposta_json, criado_em | Chave idempotente por proprietário e resposta original. |
| arquivos_recebidos | id, perfil_ref, hash, nome_arquivo, versao_leitor, registros, criado_em, conteudo | Arquivos originais recebidos, únicos por proprietário e hash. |

Custos desconhecidos permanecem NULL. Quantidades usam DECIMAL(38,18), custos DECIMAL(38,8) e instantes DATETIME(6) UTC. Não há inferência de conta Spot nem alteração dos fatos legados. A edição da proposta não altera o snapshot aplicado até a confirmação transacional.


### Histórico de propostas — V13

| Campo | Definição | Finalidade |
| --- | --- | --- |
| registro_id | BIGINT UNSIGNED, FK | Registro de conferência alterado. |
| ordem | INT, parte da PK | Ordem da versão anterior. |
| versao | BIGINT, NOT NULL | Versão dos dados preservados. |
| proposta_json | JSON, NOT NULL | Dados anteriores à alteração. |
| alterado_por | VARCHAR(120), utf8mb4_bin | Autor da alteração. |
| alterado_em | DATETIME(6) | Alteração em UTC. |

PK `(registro_id,ordem)`, FK para `registros_conferencia.id`, sem exclusão em cascata. Originais, dados aplicados e versões anteriores têm responsabilidades distintas. A proposta contém natureza `OBSERVADA`, `SALDO_ANTERIOR`, `APORTE`, `RETIRADA`, `SAIDA_CRIPTO`, `COMPRA` ou `VENDA`. Saídas de cripto podem registrar `finalidadeCripto`; dados ausentes continuam nulos. Custos de ajustes usam oito casas e HALF_EVEN antes da persistência. [Semântica e contrato](21-contrato-conciliacao.md).


## Associação de registros — V14

### grupos_associacao

| Campo | Definição | Finalidade |
| --- | --- | --- |
| id | BIGINT UNSIGNED, PK, AUTO_INCREMENT | Identificador interno que compõe o número OP-N. |
| perfil_ref | VARCHAR(120), utf8mb4_bin, NOT NULL | Usuário proprietário. |
| proposta_json | JSON, NOT NULL | Linhas, versões e papéis propostos; declaração de taxa zero, cotação opcional e desfazimento. |
| aplicada_json | JSON, NULL | Snapshot incorporado ao livro; ausência indica proposta ainda não aplicada. |
| versao | BIGINT, NOT NULL, maior que zero | Versão da proposta. |
| alterado_por | VARCHAR(120), utf8mb4_bin, NOT NULL | Autor da última decisão. |
| alterado_em | DATETIME(6), NOT NULL | Última decisão em UTC. |
| criado_em | DATETIME(6), NOT NULL | Criação em UTC. |

Índice por perfil. As linhas do JSON preservam identificador, versão, instituição, conta, ativo, código, ticker, instante exato, quantidade, tipo original, referência e papel. Papéis permitidos: RECEBIMENTO, PAGAMENTO, TAXA e NAO_DEFINIDO. Valores financeiros ausentes são nulos.

### linhas_grupos_associacao

| Campo | Definição | Finalidade |
| --- | --- | --- |
| registro_id | BIGINT UNSIGNED, PK e FK para registros_conferencia | Linha reservada a um grupo. |
| grupo_id | BIGINT UNSIGNED, FK para grupos_associacao | Grupo que reserva a linha. |

A PK impede reservar a mesma linha para dois grupos. A reserva cobre a união da proposta e da versão aplicada até a confirmação; não identifica duas movimentações financeiras. Nenhuma FK exclui originais em cascata.

### historico_grupos_associacao

| Campo | Definição | Finalidade |
| --- | --- | --- |
| grupo_id | BIGINT UNSIGNED, FK, parte da PK | Grupo alterado. |
| ordem | INT, parte da PK | Ordem da versão preservada. |
| versao | BIGINT, NOT NULL | Versão anterior. |
| proposta_json | JSON, NOT NULL | Linhas e decisões da versão anterior. |
| alterado_por | VARCHAR(120), utf8mb4_bin, NOT NULL | Autor da versão preservada. |
| alterado_em | DATETIME(6), NOT NULL | Instante da decisão em UTC. |

A confirmação não elimina propostas anteriores. Desfazer mantém grupo e histórico, liberando reservas somente ao aplicar. [Regras e contrato](22-associacao-registros-binance.md).
