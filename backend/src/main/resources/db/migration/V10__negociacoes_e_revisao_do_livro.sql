ALTER TABLE pontos_partida
    ADD COLUMN revisao BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Revisão do histórico financeiro do usuário.';

ALTER TABLE operacoes
    DROP CHECK ck_operacao_tipo,
    DROP CHECK ck_abertura_custo,
    DROP CHECK ck_abertura_zero,
    DROP INDEX uk_abertura_confirmada,
    MODIFY COLUMN custo_total_brl DECIMAL(38,8) NULL COMMENT 'Custo histórico do saldo inicial em reais.',
    MODIFY COLUMN confirmada TINYINT GENERATED ALWAYS AS (IF(tipo = 'SALDO_INICIAL' AND situacao = 'CONFIRMADA', 1, NULL)) STORED COMMENT 'Marcador da abertura confirmada.',
    MODIFY COLUMN tipo VARCHAR(20) NOT NULL COMMENT 'Tipo: SALDO_INICIAL, APORTE, COMPRA ou VENDA.',
    ADD COLUMN moeda_cotacao_id BIGINT UNSIGNED NULL COMMENT 'Moeda usada para cotar a negociação.',
    ADD COLUMN total_bruto DECIMAL(38,18) NULL COMMENT 'Valor bruto executado na moeda de cotação.',
    ADD COLUMN cotacao_brl DECIMAL(38,18) NULL COMMENT 'Valor histórico em reais de uma unidade da moeda de cotação.',
    ADD COLUMN taxa_ativo_id BIGINT UNSIGNED NULL COMMENT 'Ativo em que a taxa foi cobrada.',
    ADD COLUMN taxa_quantidade DECIMAL(38,18) NOT NULL DEFAULT 0 COMMENT 'Quantidade cobrada como taxa.',
    ADD COLUMN referencia VARCHAR(160) NULL COMMENT 'Referência da operação no documento de origem.',
    ADD COLUMN ordem_livro BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Ordem estável de desempate do lançamento.',
    ADD COLUMN versao_politica INT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Versão da política de cálculo.',
    ADD CONSTRAINT uk_abertura_confirmada UNIQUE (perfil_ref, instituicao_id, ativo_id, confirmada),
    ADD CONSTRAINT fk_operacao_cotacao FOREIGN KEY (moeda_cotacao_id) REFERENCES ativos(id),
    ADD CONSTRAINT fk_operacao_taxa FOREIGN KEY (taxa_ativo_id) REFERENCES ativos(id),
    ADD CONSTRAINT ck_operacao_tipo CHECK (tipo IN ('SALDO_INICIAL','APORTE','COMPRA','VENDA')),
    ADD CONSTRAINT ck_abertura_custo CHECK ((tipo = 'SALDO_INICIAL' AND custo_total_brl IS NOT NULL AND custo_total_brl >= 0) OR (tipo <> 'SALDO_INICIAL' AND custo_total_brl IS NULL)),
    ADD CONSTRAINT ck_abertura_zero CHECK (tipo <> 'SALDO_INICIAL' OR custo_total_brl > 0 OR COALESCE(CHAR_LENGTH(TRIM(justificativa_custo_zero)),0) > 0),
    ADD CONSTRAINT ck_negociacao_valores CHECK ((tipo IN ('COMPRA','VENDA') AND moeda_cotacao_id IS NOT NULL AND moeda_cotacao_id <> ativo_id AND total_bruto IS NOT NULL AND total_bruto > 0 AND cotacao_brl IS NOT NULL AND cotacao_brl > 0) OR (tipo IN ('SALDO_INICIAL','APORTE') AND moeda_cotacao_id IS NULL AND total_bruto IS NULL AND cotacao_brl IS NULL)),
    ADD CONSTRAINT ck_negociacao_taxa CHECK ((taxa_quantidade = 0 AND taxa_ativo_id IS NULL) OR (tipo IN ('COMPRA','VENDA') AND taxa_quantidade > 0 AND taxa_ativo_id IS NOT NULL AND taxa_ativo_id IN (ativo_id,moeda_cotacao_id)));

SET @lastro_v10_limite_cte_anterior = @@SESSION.cte_max_recursion_depth;
SET @lastro_v10_limite_cte = CAST((SELECT GREATEST(COUNT(*) + 1, @lastro_v10_limite_cte_anterior) FROM operacoes) AS UNSIGNED);
SET SESSION cte_max_recursion_depth = @lastro_v10_limite_cte;

WITH RECURSIVE raizes AS (
    SELECT id, id AS raiz FROM operacoes WHERE substitui_id IS NULL
    UNION ALL
    SELECT o.id, r.raiz FROM operacoes o JOIN raizes r ON o.substitui_id = r.id
)
UPDATE operacoes o JOIN raizes r ON r.id = o.id SET o.ordem_livro = r.raiz;

SET SESSION cte_max_recursion_depth = @lastro_v10_limite_cte_anterior;

ALTER TABLE operacoes
    MODIFY COLUMN ordem_livro BIGINT UNSIGNED NOT NULL COMMENT 'Ordem estável de desempate do lançamento.',
    ADD CONSTRAINT ck_operacao_ordem CHECK (ordem_livro > 0),
    ADD CONSTRAINT ck_operacao_politica CHECK (versao_politica > 0);
