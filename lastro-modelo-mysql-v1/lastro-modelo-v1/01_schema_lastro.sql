-- Lastro: modelo relacional inicial v1.0 | 2026-11-01
-- Alvo: MySQL 8.0.16+ / 8.4, InnoDB. Importar no Workbench como CREATE Script.
-- Banco novo; este arquivo nao migra nem sobrescreve tabelas existentes.
-- Todas as datas DATETIME sao UTC; data_local e a data de negocio.
CREATE SCHEMA IF NOT EXISTS lastro_mvp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lastro_mvp;
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- Identidade de acesso; autenticação delegada a provedor OIDC, sem senha em texto ou documento fiscal.
CREATE TABLE `usuarios` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno, sem significado de negócio.',
  `nome` VARCHAR(160) NOT NULL COMMENT 'Nome de exibição.',
  `email` VARCHAR(254) NOT NULL COMMENT 'Endereço de acesso.',
  `auth_emissor` VARCHAR(180) COLLATE utf8mb4_bin NOT NULL COMMENT 'Issuer OIDC configurado no backend.',
  `auth_sujeito` VARCHAR(180) COLLATE utf8mb4_bin NOT NULL COMMENT 'Subject estável do provedor.',
  `ativo` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Habilitação do acesso.',
  `criado_em` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de criação.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_usuarios_1` (`email`),
  UNIQUE KEY `uk_usuarios_2` (`auth_emissor`, `auth_sujeito`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Identidade de acesso; autenticação delegada a provedor OIDC, sem senha em texto ou documento fiscal.';

-- Unidade de isolamento de dados; pode conter titulares de uma família.
CREATE TABLE `espacos` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno, sem significado de negócio.',
  `nome` VARCHAR(120) NOT NULL COMMENT 'Nome do espaço.',
  `criado_por` BIGINT UNSIGNED NOT NULL COMMENT 'Usuário fundador.',
  `revisao_livro` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Incrementada na mesma transação de confirmação, cancelamento ou substituição de operações.',
  `criado_em` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de criação.',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_espacos_1` FOREIGN KEY (`criado_por`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Unidade de isolamento de dados; pode conter titulares de uma família.';

-- Quem pode acessar o espaço; permissões aplicadas pela API.
CREATE TABLE `membros_espaco` (
  `espaco_id` BIGINT UNSIGNED NOT NULL COMMENT 'Espaço ao qual o registro pertence.',
  `usuario_id` BIGINT UNSIGNED NOT NULL COMMENT 'Usuário autorizado.',
  `papel` ENUM('ADMIN','EDITOR','LEITOR') NOT NULL COMMENT 'Perfil de acesso.',
  `ativo` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Revogação sem apagar autoria.',
  `criado_em` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de criação.',
  PRIMARY KEY (`espaco_id`, `usuario_id`),
  CONSTRAINT `fk_membros_espaco_1` FOREIGN KEY (`espaco_id`) REFERENCES `espacos` (`id`),
  CONSTRAINT `fk_membros_espaco_2` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Quem pode acessar o espaço; permissões aplicadas pela API.';

-- Pessoas cujos custos são calculados separadamente, mesmo dentro do mesmo espaço.
CREATE TABLE `titulares` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno, sem significado de negócio.',
  `espaco_id` BIGINT UNSIGNED NOT NULL COMMENT 'Espaço ao qual o registro pertence.',
  `nome` VARCHAR(160) NOT NULL COMMENT 'Nome do titular.',
  `tipo` ENUM('PF','PJ') NOT NULL DEFAULT 'PF' COMMENT 'Identificação cadastral, não regime de apuração.',
  `pais_residencia` CHAR(2) NULL COMMENT 'País ISO-3166, quando informado.',
  `documento_cifrado` VARBINARY(512) NULL COMMENT 'CPF/CNPJ cifrado pela aplicação; chave fora do banco.',
  `documento_hash` BINARY(32) NULL COMMENT 'HMAC de documento normalizado para identificação, não SHA simples de CPF.',
  `ativo` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Titular arquivado permanece no histórico.',
  `criado_em` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de criação.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_titulares_1` (`espaco_id`, `id`),
  UNIQUE KEY `uk_titulares_2` (`espaco_id`, `documento_hash`),
  CONSTRAINT `fk_titulares_1` FOREIGN KEY (`espaco_id`) REFERENCES `espacos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Pessoas cujos custos são calculados separadamente, mesmo dentro do mesmo espaço.';

-- Cadastro de instituições; uma carteira própria pode não ter instituição.
CREATE TABLE `instituicoes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno, sem significado de negócio.',
  `codigo` VARCHAR(80) COLLATE utf8mb4_bin NOT NULL COMMENT 'Código interno estável, não o nome comercial.',
  `nome` VARCHAR(160) NOT NULL COMMENT 'Nome comercial.',
  `razao_social` VARCHAR(200) NULL COMMENT 'Entidade legal, se conhecida.',
  `cnpj` CHAR(14) NULL COMMENT 'CNPJ somente dígitos, opcional.',
  `pais_sede` CHAR(2) NULL COMMENT 'País cadastral; não determina sozinho o tratamento fiscal.',
  `ativo` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Disponível para novas contas.',
  `criado_em` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de criação.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_instituicoes_1` (`codigo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Cadastro de instituições; uma carteira própria pode não ter instituição.';

-- Identidade do ativo/moeda; ticker não é chave primária e não é globalmente único.
CREATE TABLE `ativos` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno, sem significado de negócio.',
  `codigo` VARCHAR(80) COLLATE utf8mb4_bin NOT NULL COMMENT 'Identificador canônico, ex.: fiat:BRL, crypto:BTC, crypto:USDC.',
  `ticker` VARCHAR(32) NOT NULL COMMENT 'Símbolo de apresentação, pode repetir em ativos distintos.',
  `nome` VARCHAR(160) NOT NULL COMMENT 'Nome do ativo.',
  `tipo` ENUM('FIAT','CRIPTO') NOT NULL COMMENT 'Tipo de cadastro, não classificação tributária.',
  `casas_decimais` TINYINT UNSIGNED NOT NULL DEFAULT 8 COMMENT 'Precisão suportada pelo MVP, até 18 casas.',
  `ativo` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Ativo disponível para novos lançamentos.',
  `criado_em` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de criação.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ativos_1` (`codigo`),
  KEY `ix_ativos_2` (`ticker`),
  CONSTRAINT `ck_ativos_1` CHECK (casas_decimais <= 18)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Identidade do ativo/moeda; ticker não é chave primária e não é globalmente único.';

-- Custódia por titular: corretora, carteira ou conta bancária.
CREATE TABLE `contas` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno, sem significado de negócio.',
  `espaco_id` BIGINT UNSIGNED NOT NULL COMMENT 'Espaço ao qual o registro pertence.',
  `titular_id` BIGINT UNSIGNED NOT NULL COMMENT 'Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso.',
  `instituicao_id` BIGINT UNSIGNED NULL COMMENT 'Instituição cadastrada; nula para carteira própria.',
  `nome` VARCHAR(120) NOT NULL COMMENT 'Apelido da conta, não identificação externa sensível.',
  `tipo` ENUM('CORRETORA','CARTEIRA','BANCO') NOT NULL COMMENT 'Local de custódia.',
  `identificador_externo` VARCHAR(180) NULL COMMENT 'Referência externa opcional; não guardar segredo de API.',
  `ativo` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Conta arquivada mantém histórico.',
  `criado_em` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de criação.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contas_1` (`espaco_id`, `titular_id`, `id`),
  UNIQUE KEY `uk_contas_2` (`espaco_id`, `titular_id`, `nome`),
  CONSTRAINT `fk_contas_1` FOREIGN KEY (`espaco_id`, `titular_id`) REFERENCES `titulares` (`espaco_id`, `id`),
  CONSTRAINT `fk_contas_2` FOREIGN KEY (`instituicao_id`) REFERENCES `instituicoes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Custódia por titular: corretora, carteira ou conta bancária.';

-- Metadados de documentos e CSVs. Conteúdo privado fora do banco.
CREATE TABLE `arquivos` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno, sem significado de negócio.',
  `espaco_id` BIGINT UNSIGNED NOT NULL COMMENT 'Espaço ao qual o registro pertence.',
  `enviado_por` BIGINT UNSIGNED NOT NULL COMMENT 'Membro que enviou o arquivo.',
  `nome_original` VARCHAR(255) NOT NULL COMMENT 'Nome do arquivo.',
  `storage_key` VARCHAR(500) COLLATE utf8mb4_bin NOT NULL COMMENT 'Chave privada de armazenamento, sem URL assinada ou credenciais.',
  `sha256` BINARY(32) NOT NULL COMMENT 'Hash dos bytes; usar UNHEX no SQL, bytes na API.',
  `mime_type` VARCHAR(100) NOT NULL COMMENT 'Tipo do arquivo.',
  `tamanho_bytes` BIGINT UNSIGNED NOT NULL COMMENT 'Tamanho em bytes.',
  `criado_em` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de criação.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_arquivos_1` (`espaco_id`, `id`),
  UNIQUE KEY `uk_arquivos_2` (`espaco_id`, `sha256`),
  CONSTRAINT `fk_arquivos_1` FOREIGN KEY (`espaco_id`, `enviado_por`) REFERENCES `membros_espaco` (`espaco_id`, `usuario_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Metadados de documentos e CSVs. Conteúdo privado fora do banco.';

-- Lote de importação com contexto fixo e versão do mapeador. Preparação para etapa futura.
CREATE TABLE `importacoes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno, sem significado de negócio.',
  `espaco_id` BIGINT UNSIGNED NOT NULL COMMENT 'Espaço ao qual o registro pertence.',
  `titular_id` BIGINT UNSIGNED NOT NULL COMMENT 'Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso.',
  `conta_id` BIGINT UNSIGNED NOT NULL COMMENT 'Conta de origem do arquivo.',
  `arquivo_id` BIGINT UNSIGNED NOT NULL COMMENT 'CSV original preservado.',
  `formato` VARCHAR(80) NOT NULL COMMENT 'Identificador do formato, ex.: corretora-operacoes-v1.',
  `versao_mapeador` VARCHAR(50) NOT NULL COMMENT 'Versão que normalizou os dados.',
  `status` ENUM('PENDENTE','EM_REVISAO','APLICADO','FALHOU','CANCELADO') NOT NULL DEFAULT 'PENDENTE' COMMENT 'Estado do fluxo de prévia/confirmação.',
  `iniciado_por` BIGINT UNSIGNED NOT NULL COMMENT 'Membro responsável.',
  `aplicado_em` DATETIME(6) NULL COMMENT 'Instante UTC de aplicação.',
  `criado_em` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de criação.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_importacoes_1` (`espaco_id`, `titular_id`, `id`),
  CONSTRAINT `fk_importacoes_1` FOREIGN KEY (`espaco_id`, `titular_id`, `conta_id`) REFERENCES `contas` (`espaco_id`, `titular_id`, `id`),
  CONSTRAINT `fk_importacoes_2` FOREIGN KEY (`espaco_id`, `arquivo_id`) REFERENCES `arquivos` (`espaco_id`, `id`),
  CONSTRAINT `fk_importacoes_3` FOREIGN KEY (`espaco_id`, `iniciado_por`) REFERENCES `membros_espaco` (`espaco_id`, `usuario_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lote de importação com contexto fixo e versão do mapeador. Preparação para etapa futura.';

-- Linha original e proposta normalizada; não altera o livro antes da confirmação.
CREATE TABLE `linhas_importacao` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno, sem significado de negócio.',
  `espaco_id` BIGINT UNSIGNED NOT NULL COMMENT 'Espaço ao qual o registro pertence.',
  `titular_id` BIGINT UNSIGNED NOT NULL COMMENT 'Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso.',
  `importacao_id` BIGINT UNSIGNED NOT NULL COMMENT 'Lote de origem.',
  `numero_linha` INT UNSIGNED NOT NULL COMMENT 'Número no arquivo original.',
  `dados_originais` JSON NOT NULL COMMENT 'Valores textuais conforme arquivo, sem inferir precisão com ponto flutuante.',
  `dados_normalizados` JSON NULL COMMENT 'Prévia do evento normalizado.',
  `fingerprint` BINARY(32) NULL COMMENT 'Candidato a duplicação; não basta para excluir automaticamente.',
  `status` ENUM('PENDENTE','NORMALIZADA','IGNORADA','ERRO','APLICADA') NOT NULL DEFAULT 'PENDENTE' COMMENT 'Estado da revisão.',
  `mensagem` VARCHAR(500) NULL COMMENT 'Pendência ou decisão da revisão.',
  `criado_em` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de criação.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_linhas_importacao_1` (`espaco_id`, `titular_id`, `id`),
  UNIQUE KEY `uk_linhas_importacao_2` (`importacao_id`, `numero_linha`),
  KEY `ix_linhas_importacao_3` (`espaco_id`, `fingerprint`),
  CONSTRAINT `fk_linhas_importacao_1` FOREIGN KEY (`espaco_id`, `titular_id`, `importacao_id`) REFERENCES `importacoes` (`espaco_id`, `titular_id`, `id`),
  CONSTRAINT `ck_linhas_importacao_1` CHECK (numero_linha > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Linha original e proposta normalizada; não altera o livro antes da confirmação.';

-- Evento de negócio. Uma edição cria substituta e preserva a versão anterior.
CREATE TABLE `operacoes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno, sem significado de negócio.',
  `espaco_id` BIGINT UNSIGNED NOT NULL COMMENT 'Espaço ao qual o registro pertence.',
  `titular_id` BIGINT UNSIGNED NOT NULL COMMENT 'Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso.',
  `conta_principal_id` BIGINT UNSIGNED NULL COMMENT 'Conta principal da operação; movimentos identificam todas as contas envolvidas.',
  `tipo` ENUM('SALDO_INICIAL','APORTE','COMPRA','VENDA','CONVERSAO','RETIRADA','TRANSFERENCIA','IMPOSTO') NOT NULL COMMENT 'Eventos suportados pelo MVP à vista.',
  `status` ENUM('RASCUNHO','CONFIRMADA','CANCELADA','SUBSTITUIDA') NOT NULL DEFAULT 'RASCUNHO' COMMENT 'Somente confirmadas entram em novos cálculos.',
  `ocorreu_em_utc` DATETIME(6) NOT NULL COMMENT 'Instante real em UTC.',
  `data_local` DATE NOT NULL COMMENT 'Data da operação no fuso informado, para relatórios.',
  `fuso_origem` VARCHAR(64) NOT NULL DEFAULT 'America/Sao_Paulo' COMMENT 'Fuso IANA usado para determinar data_local.',
  `ordem_livro` BIGINT UNSIGNED NOT NULL COMMENT 'Desempate determinístico de eventos no mesmo instante; preservado em substituições.',
  `ativo_preco_id` BIGINT UNSIGNED NULL COMMENT 'Ativo cujo preço unitário foi negociado.',
  `moeda_preco_id` BIGINT UNSIGNED NULL COMMENT 'Moeda em que o preço foi expresso.',
  `preco_unitario` DECIMAL(38,18) NULL COMMENT 'Unidades da moeda_preco por unidade do ativo_preco.',
  `pais_instituicao` CHAR(2) NULL COMMENT 'País cadastral informado à época; nulo significa desconhecido.',
  `referencia_externa` VARCHAR(180) NULL COMMENT 'Ordem/negócio/comprovante; não é única porque uma ordem pode ter vários fills.',
  `origem` ENUM('MANUAL','CSV') NOT NULL DEFAULT 'MANUAL' COMMENT 'Origem do lançamento.',
  `linha_importacao_id` BIGINT UNSIGNED NULL COMMENT 'Linha original para rastreabilidade.',
  `idempotencia` BINARY(32) NULL COMMENT 'Chave estável de requisição/item importado para impedir reaplicação exata.',
  `substitui_operacao_id` BIGINT UNSIGNED NULL COMMENT 'Versão anterior da mesma operação.',
  `observacao` VARCHAR(1000) NULL COMMENT 'Contexto e origem de custo.',
  `criado_por` BIGINT UNSIGNED NOT NULL COMMENT 'Membro autor.',
  `criado_em` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de criação.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_operacoes_1` (`espaco_id`, `titular_id`, `id`),
  UNIQUE KEY `uk_operacoes_2` (`espaco_id`, `idempotencia`),
  UNIQUE KEY `uk_operacoes_3` (`espaco_id`, `titular_id`, `substitui_operacao_id`),
  KEY `ix_operacoes_4` (`espaco_id`, `titular_id`, `status`, `ocorreu_em_utc`, `ordem_livro`),
  KEY `ix_operacoes_5` (`espaco_id`, `titular_id`, `data_local`),
  KEY `ix_operacoes_6` (`espaco_id`, `referencia_externa`),
  CONSTRAINT `fk_operacoes_1` FOREIGN KEY (`espaco_id`, `titular_id`) REFERENCES `titulares` (`espaco_id`, `id`),
  CONSTRAINT `fk_operacoes_2` FOREIGN KEY (`espaco_id`, `titular_id`, `conta_principal_id`) REFERENCES `contas` (`espaco_id`, `titular_id`, `id`),
  CONSTRAINT `fk_operacoes_3` FOREIGN KEY (`ativo_preco_id`) REFERENCES `ativos` (`id`),
  CONSTRAINT `fk_operacoes_4` FOREIGN KEY (`moeda_preco_id`) REFERENCES `ativos` (`id`),
  CONSTRAINT `fk_operacoes_5` FOREIGN KEY (`espaco_id`, `titular_id`, `linha_importacao_id`) REFERENCES `linhas_importacao` (`espaco_id`, `titular_id`, `id`),
  CONSTRAINT `fk_operacoes_6` FOREIGN KEY (`espaco_id`, `titular_id`, `substitui_operacao_id`) REFERENCES `operacoes` (`espaco_id`, `titular_id`, `id`),
  CONSTRAINT `fk_operacoes_7` FOREIGN KEY (`espaco_id`, `criado_por`) REFERENCES `membros_espaco` (`espaco_id`, `usuario_id`),
  CONSTRAINT `ck_operacoes_1` CHECK (((tipo IN ('COMPRA','VENDA','CONVERSAO') AND ativo_preco_id IS NOT NULL AND moeda_preco_id IS NOT NULL AND ativo_preco_id <> moeda_preco_id AND preco_unitario IS NOT NULL AND preco_unitario > 0) OR (tipo NOT IN ('COMPRA','VENDA','CONVERSAO') AND ativo_preco_id IS NULL AND moeda_preco_id IS NULL AND preco_unitario IS NULL))),
  CONSTRAINT `ck_operacoes_2` CHECK (origem <> 'CSV' OR linha_importacao_id IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Evento de negócio. Uma edição cria substituta e preserva a versão anterior.';

-- Pernas explícitas de quantidade e conta; inclui taxas sem embuti-las uma segunda vez no saldo.
CREATE TABLE `movimentos` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno, sem significado de negócio.',
  `espaco_id` BIGINT UNSIGNED NOT NULL COMMENT 'Espaço ao qual o registro pertence.',
  `titular_id` BIGINT UNSIGNED NOT NULL COMMENT 'Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso.',
  `operacao_id` BIGINT UNSIGNED NOT NULL COMMENT 'Evento ao qual pertence.',
  `conta_id` BIGINT UNSIGNED NOT NULL COMMENT 'Conta movimentada, necessariamente do mesmo titular.',
  `ativo_id` BIGINT UNSIGNED NOT NULL COMMENT 'Ativo/moeda movimentado.',
  `papel` ENUM('ATIVO','LIQUIDACAO','TAXA','CAIXA','SALDO_INICIAL','TRANSFERENCIA') NOT NULL COMMENT 'Função da perna no evento.',
  `direcao` ENUM('ENTRADA','SAIDA') NOT NULL COMMENT 'Sentido da quantidade, que é sempre positiva.',
  `quantidade` DECIMAL(38,18) NOT NULL COMMENT 'Quantidade original do ativo.',
  `cotacao_brl` DECIMAL(38,18) NULL COMMENT 'Reais por unidade na data. Não é cotação atual.',
  `valor_referencia_brl` DECIMAL(38,8) NULL COMMENT 'Valor histórico bruto da perna conforme os dados disponíveis.',
  `fonte_cotacao` VARCHAR(200) NULL COMMENT 'Fonte/documento da conversão em reais.',
  `custo_inicial_brl` DECIMAL(38,8) NULL COMMENT 'Custo total informado em saldo inicial; NULL desconhecido e zero explicitamente conhecido.',
  `observacao` VARCHAR(500) NULL COMMENT 'Origem do custo ou detalhe da taxa.',
  `criado_em` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de criação.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_movimentos_1` (`espaco_id`, `titular_id`, `operacao_id`, `id`),
  KEY `ix_movimentos_2` (`espaco_id`, `titular_id`, `conta_id`, `ativo_id`),
  KEY `ix_movimentos_3` (`operacao_id`, `papel`),
  CONSTRAINT `fk_movimentos_1` FOREIGN KEY (`espaco_id`, `titular_id`, `operacao_id`) REFERENCES `operacoes` (`espaco_id`, `titular_id`, `id`),
  CONSTRAINT `fk_movimentos_2` FOREIGN KEY (`espaco_id`, `titular_id`, `conta_id`) REFERENCES `contas` (`espaco_id`, `titular_id`, `id`),
  CONSTRAINT `fk_movimentos_3` FOREIGN KEY (`ativo_id`) REFERENCES `ativos` (`id`),
  CONSTRAINT `ck_movimentos_1` CHECK (quantidade > 0),
  CONSTRAINT `ck_movimentos_2` CHECK (cotacao_brl IS NULL OR cotacao_brl > 0),
  CONSTRAINT `ck_movimentos_3` CHECK (valor_referencia_brl IS NULL OR valor_referencia_brl >= 0),
  CONSTRAINT `ck_movimentos_4` CHECK (custo_inicial_brl IS NULL OR custo_inicial_brl >= 0),
  CONSTRAINT `ck_movimentos_5` CHECK (papel = 'SALDO_INICIAL' OR custo_inicial_brl IS NULL),
  CONSTRAINT `ck_movimentos_6` CHECK (papel <> 'SALDO_INICIAL' OR direcao = 'ENTRADA'),
  CONSTRAINT `ck_movimentos_7` CHECK (papel <> 'TAXA' OR direcao = 'SAIDA')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Pernas explícitas de quantidade e conta; inclui taxas sem embuti-las uma segunda vez no saldo.';

-- Pagamento já realizado e informado, sem cálculo de obrigação tributária.
CREATE TABLE `pagamentos_imposto` (
  `espaco_id` BIGINT UNSIGNED NOT NULL COMMENT 'Espaço ao qual o registro pertence.',
  `titular_id` BIGINT UNSIGNED NOT NULL COMMENT 'Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso.',
  `operacao_id` BIGINT UNSIGNED NOT NULL COMMENT 'Operação do tipo IMPOSTO, validada pelo serviço.',
  `moeda_id` BIGINT UNSIGNED NOT NULL COMMENT 'Moeda efetivamente paga.',
  `quantidade` DECIMAL(38,18) NOT NULL COMMENT 'Quantidade paga.',
  `cotacao_brl` DECIMAL(38,18) NOT NULL COMMENT 'Reais por unidade na data do pagamento.',
  `valor_pago_brl` DECIMAL(38,8) NOT NULL COMMENT 'Valor total registrado em reais.',
  `competencia` DATE NULL COMMENT 'Primeiro dia do mês de referência, pode diferir do pagamento.',
  `origem_pagamento` ENUM('EXTERNO','CARTEIRA') NOT NULL COMMENT 'Externo não movimenta os saldos do Lastro.',
  `codigo_receita` VARCHAR(20) NULL COMMENT 'Código informado do documento, sem inferência.',
  `referencia_documento` VARCHAR(180) NULL COMMENT 'Número do comprovante.',
  `arquivo_id` BIGINT UNSIGNED NULL COMMENT 'Comprovante privado opcional.',
  PRIMARY KEY (`operacao_id`),
  CONSTRAINT `fk_pagamentos_imposto_1` FOREIGN KEY (`espaco_id`, `titular_id`, `operacao_id`) REFERENCES `operacoes` (`espaco_id`, `titular_id`, `id`),
  CONSTRAINT `fk_pagamentos_imposto_2` FOREIGN KEY (`moeda_id`) REFERENCES `ativos` (`id`),
  CONSTRAINT `fk_pagamentos_imposto_3` FOREIGN KEY (`espaco_id`, `arquivo_id`) REFERENCES `arquivos` (`espaco_id`, `id`),
  CONSTRAINT `ck_pagamentos_imposto_1` CHECK (quantidade > 0),
  CONSTRAINT `ck_pagamentos_imposto_2` CHECK (cotacao_brl > 0),
  CONSTRAINT `ck_pagamentos_imposto_3` CHECK (valor_pago_brl > 0),
  CONSTRAINT `ck_pagamentos_imposto_4` CHECK (competencia IS NULL OR DAYOFMONTH(competencia) = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Pagamento já realizado e informado, sem cálculo de obrigação tributária.';

-- Snapshot versionado e reproduzível do processamento gerencial, não motor fiscal.
CREATE TABLE `execucoes_calculo` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno, sem significado de negócio.',
  `espaco_id` BIGINT UNSIGNED NOT NULL COMMENT 'Espaço ao qual o registro pertence.',
  `revisao_livro` BIGINT UNSIGNED NOT NULL COMMENT 'Revisão processada, deve coincidir com a atual para relatório vigente.',
  `data_limite` DATE NOT NULL COMMENT 'Processa TODO o histórico até esta data local; não apenas o mês do relatório.',
  `versao_motor` VARCHAR(80) NOT NULL COMMENT 'Versão da política de custo médio, taxas, câmbio e arredondamento.',
  `status` ENUM('PROCESSANDO','CONCLUIDO','FALHOU','INCOMPLETO') NOT NULL DEFAULT 'PROCESSANDO' COMMENT 'INCOMPLETO quando faltam custos/dados que impedem concluir os resultados.',
  `iniciado_em` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de início.',
  `concluido_em` DATETIME(6) NULL COMMENT 'Instante UTC de término.',
  `erro` VARCHAR(1000) NULL COMMENT 'Motivo de falha ou incompletude.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_execucoes_calculo_1` (`espaco_id`, `id`),
  KEY `ix_execucoes_calculo_2` (`espaco_id`, `revisao_livro`, `data_limite`, `status`),
  CONSTRAINT `fk_execucoes_calculo_1` FOREIGN KEY (`espaco_id`) REFERENCES `espacos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Snapshot versionado e reproduzível do processamento gerencial, não motor fiscal.';

-- Composição por evento em uma execução; NÃO altera os fatos originais.
CREATE TABLE `resultados_operacoes` (
  `espaco_id` BIGINT UNSIGNED NOT NULL COMMENT 'Espaço ao qual o registro pertence.',
  `titular_id` BIGINT UNSIGNED NOT NULL COMMENT 'Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso.',
  `calculo_id` BIGINT UNSIGNED NOT NULL COMMENT 'Execução de origem.',
  `operacao_id` BIGINT UNSIGNED NOT NULL COMMENT 'Versão exata da operação processada.',
  `receita_liquida_venda_brl` DECIMAL(38,8) NULL COMMENT 'Receita de venda após taxas alocadas, quando aplicável.',
  `custo_baixado_brl` DECIMAL(38,8) NULL COMMENT 'Custo baixado do ativo principal; NULL quando não aplicável ou desconhecido.',
  `custo_incorporado_brl` DECIMAL(38,8) NULL COMMENT 'Custo incorporado no ativo principal, quando aplicável; outras pernas ficam em resultados_movimentos.',
  `resultado_venda_brl` DECIMAL(38,8) NULL COMMENT 'Resultado de venda; não confundir com câmbio da moeda usada.',
  `resultado_cambio_brl` DECIMAL(38,8) NULL COMMENT 'Resultado de conversões e uso de moedas.',
  `resultado_rede_brl` DECIMAL(38,8) NULL COMMENT 'Resultado de despesas de rede e outras taxas reconhecidas.',
  `resultado_total_brl` DECIMAL(38,8) NULL COMMENT 'Soma das parcelas conhecidas; NULL se insuficientes.',
  `situacao` ENUM('CALCULADO','PENDENTE') NOT NULL COMMENT 'Resultado pendente nunca deve aparecer como zero confirmado.',
  `pendencia` VARCHAR(500) NULL COMMENT 'Explicação da ausência de resultado.',
  PRIMARY KEY (`calculo_id`, `operacao_id`),
  UNIQUE KEY `uk_resultados_operacoes_2` (`espaco_id`, `titular_id`, `calculo_id`, `operacao_id`),
  CONSTRAINT `fk_resultados_operacoes_1` FOREIGN KEY (`espaco_id`, `calculo_id`) REFERENCES `execucoes_calculo` (`espaco_id`, `id`),
  CONSTRAINT `fk_resultados_operacoes_2` FOREIGN KEY (`espaco_id`, `titular_id`, `operacao_id`) REFERENCES `operacoes` (`espaco_id`, `titular_id`, `id`),
  CONSTRAINT `ck_resultados_operacoes_1` CHECK (situacao <> 'CALCULADO' OR (resultado_venda_brl IS NOT NULL AND resultado_cambio_brl IS NOT NULL AND resultado_rede_brl IS NOT NULL AND resultado_total_brl IS NOT NULL AND resultado_total_brl = resultado_venda_brl + resultado_cambio_brl + resultado_rede_brl))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Composição por evento em uma execução; NÃO altera os fatos originais.';

-- Rastreabilidade do custo e resultado por perna, inclusive taxas em outro token.
CREATE TABLE `resultados_movimentos` (
  `espaco_id` BIGINT UNSIGNED NOT NULL COMMENT 'Espaço ao qual o registro pertence.',
  `titular_id` BIGINT UNSIGNED NOT NULL COMMENT 'Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso.',
  `calculo_id` BIGINT UNSIGNED NOT NULL COMMENT 'Execução de cálculo.',
  `operacao_id` BIGINT UNSIGNED NOT NULL COMMENT 'Operação correspondente ao movimento e ao resultado pai.',
  `movimento_id` BIGINT UNSIGNED NOT NULL COMMENT 'Movimento original.',
  `custo_baixado_brl` DECIMAL(38,8) NULL COMMENT 'Custo retirado do estoque gerencial.',
  `custo_incorporado_brl` DECIMAL(38,8) NULL COMMENT 'Custo adicionado ao estoque.',
  `resultado_venda_brl` DECIMAL(38,8) NULL COMMENT 'Parcela de venda.',
  `resultado_cambio_brl` DECIMAL(38,8) NULL COMMENT 'Parcela de câmbio/conversão.',
  `resultado_rede_brl` DECIMAL(38,8) NULL COMMENT 'Parcela de rede/taxa.',
  `situacao` ENUM('CALCULADO','PENDENTE') NOT NULL COMMENT 'Disponibilidade dos cálculos da perna.',
  PRIMARY KEY (`calculo_id`, `movimento_id`),
  CONSTRAINT `fk_resultados_movimentos_1` FOREIGN KEY (`espaco_id`, `titular_id`, `calculo_id`, `operacao_id`) REFERENCES `resultados_operacoes` (`espaco_id`, `titular_id`, `calculo_id`, `operacao_id`),
  CONSTRAINT `fk_resultados_movimentos_2` FOREIGN KEY (`espaco_id`, `titular_id`, `operacao_id`, `movimento_id`) REFERENCES `movimentos` (`espaco_id`, `titular_id`, `operacao_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Rastreabilidade do custo e resultado por perna, inclusive taxas em outro token.';

-- Snapshot por titular/ativo: preço médio não é calculado separadamente por corretora.
CREATE TABLE `posicoes_calculadas` (
  `espaco_id` BIGINT UNSIGNED NOT NULL COMMENT 'Espaço ao qual o registro pertence.',
  `titular_id` BIGINT UNSIGNED NOT NULL COMMENT 'Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso.',
  `calculo_id` BIGINT UNSIGNED NOT NULL COMMENT 'Snapshot até a data limite.',
  `ativo_id` BIGINT UNSIGNED NOT NULL COMMENT 'Ativo do estoque.',
  `quantidade` DECIMAL(38,18) NOT NULL COMMENT 'Quantidade remanescente, zero permitido para ativo encerrado.',
  `custo_total_brl` DECIMAL(38,8) NULL COMMENT 'Custo a aquisição; NULL quando histórico não permite concluir.',
  `preco_medio_brl` DECIMAL(38,18) NULL COMMENT 'Custo unitário gerencial em reais, sem preço de mercado.',
  `situacao_custo` ENUM('CONHECIDO','PENDENTE') NOT NULL COMMENT 'Qualidade do custo, não certificação contábil.',
  PRIMARY KEY (`calculo_id`, `titular_id`, `ativo_id`),
  CONSTRAINT `fk_posicoes_calculadas_1` FOREIGN KEY (`espaco_id`, `calculo_id`) REFERENCES `execucoes_calculo` (`espaco_id`, `id`),
  CONSTRAINT `fk_posicoes_calculadas_2` FOREIGN KEY (`espaco_id`, `titular_id`) REFERENCES `titulares` (`espaco_id`, `id`),
  CONSTRAINT `fk_posicoes_calculadas_3` FOREIGN KEY (`ativo_id`) REFERENCES `ativos` (`id`),
  CONSTRAINT `ck_posicoes_calculadas_1` CHECK (quantidade >= 0),
  CONSTRAINT `ck_posicoes_calculadas_2` CHECK (custo_total_brl IS NULL OR custo_total_brl >= 0),
  CONSTRAINT `ck_posicoes_calculadas_3` CHECK (preco_medio_brl IS NULL OR preco_medio_brl >= 0),
  CONSTRAINT `ck_posicoes_calculadas_4` CHECK (situacao_custo <> 'CONHECIDO' OR (custo_total_brl IS NOT NULL AND preco_medio_brl IS NOT NULL)),
  CONSTRAINT `ck_posicoes_calculadas_5` CHECK (quantidade <> 0 OR (custo_total_brl = 0 AND preco_medio_brl = 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Snapshot por titular/ativo: preço médio não é calculado separadamente por corretora.';

-- Quantidade por conta para conciliação; separada do custo médio por titular.
CREATE TABLE `saldos_contas` (
  `espaco_id` BIGINT UNSIGNED NOT NULL COMMENT 'Espaço ao qual o registro pertence.',
  `titular_id` BIGINT UNSIGNED NOT NULL COMMENT 'Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso.',
  `calculo_id` BIGINT UNSIGNED NOT NULL COMMENT 'Execução de origem.',
  `conta_id` BIGINT UNSIGNED NOT NULL COMMENT 'Custódia.',
  `ativo_id` BIGINT UNSIGNED NOT NULL COMMENT 'Ativo.',
  `quantidade` DECIMAL(38,18) NOT NULL COMMENT 'Saldo total, incluindo quantidades bloqueadas em ordens.',
  PRIMARY KEY (`calculo_id`, `conta_id`, `ativo_id`),
  CONSTRAINT `fk_saldos_contas_1` FOREIGN KEY (`espaco_id`, `calculo_id`) REFERENCES `execucoes_calculo` (`espaco_id`, `id`),
  CONSTRAINT `fk_saldos_contas_2` FOREIGN KEY (`espaco_id`, `titular_id`, `conta_id`) REFERENCES `contas` (`espaco_id`, `titular_id`, `id`),
  CONSTRAINT `fk_saldos_contas_3` FOREIGN KEY (`ativo_id`) REFERENCES `ativos` (`id`),
  CONSTRAINT `ck_saldos_contas_1` CHECK (quantidade >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Quantidade por conta para conciliação; separada do custo médio por titular.';

-- Saldo digitado do extrato. Diferença é consultada contra um cálculo vigente, não congelada como selo.
CREATE TABLE `conciliacoes_saldo` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno, sem significado de negócio.',
  `espaco_id` BIGINT UNSIGNED NOT NULL COMMENT 'Espaço ao qual o registro pertence.',
  `titular_id` BIGINT UNSIGNED NOT NULL COMMENT 'Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso.',
  `conta_id` BIGINT UNSIGNED NOT NULL COMMENT 'Conta do extrato.',
  `ativo_id` BIGINT UNSIGNED NOT NULL COMMENT 'Ativo do extrato.',
  `data_saldo` DATE NOT NULL COMMENT 'Fim do dia no fuso da operação/espaço definido no serviço.',
  `quantidade_extrato` DECIMAL(38,18) NOT NULL COMMENT 'Quantidade total do extrato, incluindo bloqueios.',
  `referencia_documento` VARCHAR(200) NOT NULL COMMENT 'Comprovante que permite refazer a conferência.',
  `arquivo_id` BIGINT UNSIGNED NULL COMMENT 'Extrato privado opcional.',
  `informado_por` BIGINT UNSIGNED NOT NULL COMMENT 'Membro responsável.',
  `criado_em` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de criação.',
  `atualizado_em` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Atualização cadastral; guardar versão anterior na auditoria.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conciliacoes_saldo_1` (`espaco_id`, `conta_id`, `ativo_id`, `data_saldo`),
  CONSTRAINT `fk_conciliacoes_saldo_1` FOREIGN KEY (`espaco_id`, `titular_id`, `conta_id`) REFERENCES `contas` (`espaco_id`, `titular_id`, `id`),
  CONSTRAINT `fk_conciliacoes_saldo_2` FOREIGN KEY (`ativo_id`) REFERENCES `ativos` (`id`),
  CONSTRAINT `fk_conciliacoes_saldo_3` FOREIGN KEY (`espaco_id`, `arquivo_id`) REFERENCES `arquivos` (`espaco_id`, `id`),
  CONSTRAINT `fk_conciliacoes_saldo_4` FOREIGN KEY (`espaco_id`, `informado_por`) REFERENCES `membros_espaco` (`espaco_id`, `usuario_id`),
  CONSTRAINT `ck_conciliacoes_saldo_1` CHECK (quantidade_extrato >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Saldo digitado do extrato. Diferença é consultada contra um cálculo vigente, não congelada como selo.';

-- Trilha append-only por serviço; não colocar senhas, tokens ou documentos abertos nos JSONs.
CREATE TABLE `auditoria` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno, sem significado de negócio.',
  `espaco_id` BIGINT UNSIGNED NOT NULL COMMENT 'Espaço ao qual o registro pertence.',
  `usuario_id` BIGINT UNSIGNED NULL COMMENT 'Autor; NULL para trabalho de sistema.',
  `entidade` VARCHAR(64) NOT NULL COMMENT 'Tipo de entidade, ex.: operacoes, conciliacoes_saldo.',
  `registro_id` BIGINT UNSIGNED NOT NULL COMMENT 'Identificador lógico; integridade polimórfica aplicada pelo serviço.',
  `acao` ENUM('CRIAR','CONFIRMAR','CANCELAR','SUBSTITUIR','ATUALIZAR','IMPORTAR') NOT NULL COMMENT 'Ação auditada.',
  `dados_antes` JSON NULL COMMENT 'Estado anterior sem informações sigilosas desnecessárias.',
  `dados_depois` JSON NULL COMMENT 'Estado seguinte.',
  `correlacao_id` VARCHAR(80) NULL COMMENT 'Identificador de requisição/job.',
  `criado_em` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de criação.',
  PRIMARY KEY (`id`),
  KEY `ix_auditoria_1` (`espaco_id`, `entidade`, `registro_id`, `criado_em`),
  CONSTRAINT `fk_auditoria_1` FOREIGN KEY (`espaco_id`) REFERENCES `espacos` (`id`),
  CONSTRAINT `fk_auditoria_2` FOREIGN KEY (`espaco_id`, `usuario_id`) REFERENCES `membros_espaco` (`espaco_id`, `usuario_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Trilha append-only por serviço; não colocar senhas, tokens ou documentos abertos nos JSONs.';
