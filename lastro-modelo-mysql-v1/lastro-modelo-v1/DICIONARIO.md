# Dicionário de dados — Lastro v1

20 tabelas. Chaves estrangeiras usam RESTRICT: o histórico não é apagado em cascata.

## usuarios

Identidade de acesso; autenticação delegada a provedor OIDC, sem senha em texto ou documento fiscal.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador interno, sem significado de negócio. |
| `nome` | `VARCHAR(160) NOT NULL` | Nome de exibição. |
| `email` | `VARCHAR(254) NOT NULL` | Endereço de acesso. |
| `auth_emissor` | `VARCHAR(180) COLLATE utf8mb4_bin NOT NULL` | Issuer OIDC configurado no backend. |
| `auth_sujeito` | `VARCHAR(180) COLLATE utf8mb4_bin NOT NULL` | Subject estável do provedor. |
| `ativo` | `BOOLEAN NOT NULL DEFAULT TRUE` | Habilitação do acesso. |
| `criado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | Instante UTC de criação. |

Chaves: UNIQUE(email); UNIQUE(auth_emissor,auth_sujeito)

## espacos

Unidade de isolamento de dados; pode conter titulares de uma família.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador interno, sem significado de negócio. |
| `nome` | `VARCHAR(120) NOT NULL` | Nome do espaço. |
| `criado_por` | `BIGINT UNSIGNED NOT NULL` | Usuário fundador. |
| `revisao_livro` | `BIGINT UNSIGNED NOT NULL DEFAULT 0` | Incrementada na mesma transação de confirmação, cancelamento ou substituição de operações. |
| `criado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | Instante UTC de criação. |

Chaves: PK(id).

## membros_espaco

Quem pode acessar o espaço; permissões aplicadas pela API.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `espaco_id` | `BIGINT UNSIGNED NOT NULL` | Espaço ao qual o registro pertence. |
| `usuario_id` | `BIGINT UNSIGNED NOT NULL` | Usuário autorizado. |
| `papel` | `ENUM('ADMIN','EDITOR','LEITOR') NOT NULL` | Perfil de acesso. |
| `ativo` | `BOOLEAN NOT NULL DEFAULT TRUE` | Revogação sem apagar autoria. |
| `criado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | Instante UTC de criação. |

Chaves: PRIMARY(espaco_id,usuario_id)

## titulares

Pessoas cujos custos são calculados separadamente, mesmo dentro do mesmo espaço.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador interno, sem significado de negócio. |
| `espaco_id` | `BIGINT UNSIGNED NOT NULL` | Espaço ao qual o registro pertence. |
| `nome` | `VARCHAR(160) NOT NULL` | Nome do titular. |
| `tipo` | `ENUM('PF','PJ') NOT NULL DEFAULT 'PF'` | Identificação cadastral, não regime de apuração. |
| `pais_residencia` | `CHAR(2) NULL` | País ISO-3166, quando informado. |
| `documento_cifrado` | `VARBINARY(512) NULL` | CPF/CNPJ cifrado pela aplicação; chave fora do banco. |
| `documento_hash` | `BINARY(32) NULL` | HMAC de documento normalizado para identificação, não SHA simples de CPF. |
| `ativo` | `BOOLEAN NOT NULL DEFAULT TRUE` | Titular arquivado permanece no histórico. |
| `criado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | Instante UTC de criação. |

Chaves: UNIQUE(espaco_id,id); UNIQUE(espaco_id,documento_hash)

## instituicoes

Cadastro de instituições; uma carteira própria pode não ter instituição.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador interno, sem significado de negócio. |
| `codigo` | `VARCHAR(80) COLLATE utf8mb4_bin NOT NULL` | Código interno estável, não o nome comercial. |
| `nome` | `VARCHAR(160) NOT NULL` | Nome comercial. |
| `razao_social` | `VARCHAR(200) NULL` | Entidade legal, se conhecida. |
| `cnpj` | `CHAR(14) NULL` | CNPJ somente dígitos, opcional. |
| `pais_sede` | `CHAR(2) NULL` | País cadastral; não determina sozinho o tratamento fiscal. |
| `ativo` | `BOOLEAN NOT NULL DEFAULT TRUE` | Disponível para novas contas. |
| `criado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | Instante UTC de criação. |

Chaves: UNIQUE(codigo)

## ativos

Identidade do ativo/moeda; ticker não é chave primária e não é globalmente único.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador interno, sem significado de negócio. |
| `codigo` | `VARCHAR(80) COLLATE utf8mb4_bin NOT NULL` | Identificador canônico, ex.: fiat:BRL, crypto:BTC, crypto:USDC. |
| `ticker` | `VARCHAR(32) NOT NULL` | Símbolo de apresentação, pode repetir em ativos distintos. |
| `nome` | `VARCHAR(160) NOT NULL` | Nome do ativo. |
| `tipo` | `ENUM('FIAT','CRIPTO') NOT NULL` | Tipo de cadastro, não classificação tributária. |
| `casas_decimais` | `TINYINT UNSIGNED NOT NULL DEFAULT 8` | Precisão suportada pelo MVP, até 18 casas. |
| `ativo` | `BOOLEAN NOT NULL DEFAULT TRUE` | Ativo disponível para novos lançamentos. |
| `criado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | Instante UTC de criação. |

Chaves: UNIQUE(codigo); INDEX(ticker)

## contas

Custódia por titular: corretora, carteira ou conta bancária.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador interno, sem significado de negócio. |
| `espaco_id` | `BIGINT UNSIGNED NOT NULL` | Espaço ao qual o registro pertence. |
| `titular_id` | `BIGINT UNSIGNED NOT NULL` | Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso. |
| `instituicao_id` | `BIGINT UNSIGNED NULL` | Instituição cadastrada; nula para carteira própria. |
| `nome` | `VARCHAR(120) NOT NULL` | Apelido da conta, não identificação externa sensível. |
| `tipo` | `ENUM('CORRETORA','CARTEIRA','BANCO') NOT NULL` | Local de custódia. |
| `identificador_externo` | `VARCHAR(180) NULL` | Referência externa opcional; não guardar segredo de API. |
| `ativo` | `BOOLEAN NOT NULL DEFAULT TRUE` | Conta arquivada mantém histórico. |
| `criado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | Instante UTC de criação. |

Chaves: UNIQUE(espaco_id,titular_id,id); UNIQUE(espaco_id,titular_id,nome)

## arquivos

Metadados de documentos e CSVs. Conteúdo privado fora do banco.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador interno, sem significado de negócio. |
| `espaco_id` | `BIGINT UNSIGNED NOT NULL` | Espaço ao qual o registro pertence. |
| `enviado_por` | `BIGINT UNSIGNED NOT NULL` | Membro que enviou o arquivo. |
| `nome_original` | `VARCHAR(255) NOT NULL` | Nome do arquivo. |
| `storage_key` | `VARCHAR(500) COLLATE utf8mb4_bin NOT NULL` | Chave privada de armazenamento, sem URL assinada ou credenciais. |
| `sha256` | `BINARY(32) NOT NULL` | Hash dos bytes; usar UNHEX no SQL, bytes na API. |
| `mime_type` | `VARCHAR(100) NOT NULL` | Tipo do arquivo. |
| `tamanho_bytes` | `BIGINT UNSIGNED NOT NULL` | Tamanho em bytes. |
| `criado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | Instante UTC de criação. |

Chaves: UNIQUE(espaco_id,id); UNIQUE(espaco_id,sha256)

## importacoes

Lote de importação com contexto fixo e versão do mapeador. Preparação para etapa futura.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador interno, sem significado de negócio. |
| `espaco_id` | `BIGINT UNSIGNED NOT NULL` | Espaço ao qual o registro pertence. |
| `titular_id` | `BIGINT UNSIGNED NOT NULL` | Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso. |
| `conta_id` | `BIGINT UNSIGNED NOT NULL` | Conta de origem do arquivo. |
| `arquivo_id` | `BIGINT UNSIGNED NOT NULL` | CSV original preservado. |
| `formato` | `VARCHAR(80) NOT NULL` | Identificador do formato, ex.: corretora-operacoes-v1. |
| `versao_mapeador` | `VARCHAR(50) NOT NULL` | Versão que normalizou os dados. |
| `status` | `ENUM('PENDENTE','EM_REVISAO','APLICADO','FALHOU','CANCELADO') NOT NULL DEFAULT 'PENDENTE'` | Estado do fluxo de prévia/confirmação. |
| `iniciado_por` | `BIGINT UNSIGNED NOT NULL` | Membro responsável. |
| `aplicado_em` | `DATETIME(6) NULL` | Instante UTC de aplicação. |
| `criado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | Instante UTC de criação. |

Chaves: UNIQUE(espaco_id,titular_id,id)

## linhas_importacao

Linha original e proposta normalizada; não altera o livro antes da confirmação.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador interno, sem significado de negócio. |
| `espaco_id` | `BIGINT UNSIGNED NOT NULL` | Espaço ao qual o registro pertence. |
| `titular_id` | `BIGINT UNSIGNED NOT NULL` | Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso. |
| `importacao_id` | `BIGINT UNSIGNED NOT NULL` | Lote de origem. |
| `numero_linha` | `INT UNSIGNED NOT NULL` | Número no arquivo original. |
| `dados_originais` | `JSON NOT NULL` | Valores textuais conforme arquivo, sem inferir precisão com ponto flutuante. |
| `dados_normalizados` | `JSON NULL` | Prévia do evento normalizado. |
| `fingerprint` | `BINARY(32) NULL` | Candidato a duplicação; não basta para excluir automaticamente. |
| `status` | `ENUM('PENDENTE','NORMALIZADA','IGNORADA','ERRO','APLICADA') NOT NULL DEFAULT 'PENDENTE'` | Estado da revisão. |
| `mensagem` | `VARCHAR(500) NULL` | Pendência ou decisão da revisão. |
| `criado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | Instante UTC de criação. |

Chaves: UNIQUE(espaco_id,titular_id,id); UNIQUE(importacao_id,numero_linha); INDEX(espaco_id,fingerprint)

## operacoes

Evento de negócio. Uma edição cria substituta e preserva a versão anterior.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador interno, sem significado de negócio. |
| `espaco_id` | `BIGINT UNSIGNED NOT NULL` | Espaço ao qual o registro pertence. |
| `titular_id` | `BIGINT UNSIGNED NOT NULL` | Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso. |
| `conta_principal_id` | `BIGINT UNSIGNED NULL` | Conta principal da operação; movimentos identificam todas as contas envolvidas. |
| `tipo` | `ENUM('SALDO_INICIAL','APORTE','COMPRA','VENDA','CONVERSAO','RETIRADA','TRANSFERENCIA','IMPOSTO') NOT NULL` | Eventos suportados pelo MVP à vista. |
| `status` | `ENUM('RASCUNHO','CONFIRMADA','CANCELADA','SUBSTITUIDA') NOT NULL DEFAULT 'RASCUNHO'` | Somente confirmadas entram em novos cálculos. |
| `ocorreu_em_utc` | `DATETIME(6) NOT NULL` | Instante real em UTC. |
| `data_local` | `DATE NOT NULL` | Data da operação no fuso informado, para relatórios. |
| `fuso_origem` | `VARCHAR(64) NOT NULL DEFAULT 'America/Sao_Paulo'` | Fuso IANA usado para determinar data_local. |
| `ordem_livro` | `BIGINT UNSIGNED NOT NULL` | Desempate determinístico de eventos no mesmo instante; preservado em substituições. |
| `ativo_preco_id` | `BIGINT UNSIGNED NULL` | Ativo cujo preço unitário foi negociado. |
| `moeda_preco_id` | `BIGINT UNSIGNED NULL` | Moeda em que o preço foi expresso. |
| `preco_unitario` | `DECIMAL(38,18) NULL` | Unidades da moeda_preco por unidade do ativo_preco. |
| `pais_instituicao` | `CHAR(2) NULL` | País cadastral informado à época; nulo significa desconhecido. |
| `referencia_externa` | `VARCHAR(180) NULL` | Ordem/negócio/comprovante; não é única porque uma ordem pode ter vários fills. |
| `origem` | `ENUM('MANUAL','CSV') NOT NULL DEFAULT 'MANUAL'` | Origem do lançamento. |
| `linha_importacao_id` | `BIGINT UNSIGNED NULL` | Linha original para rastreabilidade. |
| `idempotencia` | `BINARY(32) NULL` | Chave estável de requisição/item importado para impedir reaplicação exata. |
| `substitui_operacao_id` | `BIGINT UNSIGNED NULL` | Versão anterior da mesma operação. |
| `observacao` | `VARCHAR(1000) NULL` | Contexto e origem de custo. |
| `criado_por` | `BIGINT UNSIGNED NOT NULL` | Membro autor. |
| `criado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | Instante UTC de criação. |

Chaves: UNIQUE(espaco_id,titular_id,id); UNIQUE(espaco_id,idempotencia); UNIQUE(espaco_id,titular_id,substitui_operacao_id); INDEX(espaco_id,titular_id,status,ocorreu_em_utc,ordem_livro); INDEX(espaco_id,titular_id,data_local); INDEX(espaco_id,referencia_externa)

## movimentos

Pernas explícitas de quantidade e conta; inclui taxas sem embuti-las uma segunda vez no saldo.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador interno, sem significado de negócio. |
| `espaco_id` | `BIGINT UNSIGNED NOT NULL` | Espaço ao qual o registro pertence. |
| `titular_id` | `BIGINT UNSIGNED NOT NULL` | Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso. |
| `operacao_id` | `BIGINT UNSIGNED NOT NULL` | Evento ao qual pertence. |
| `conta_id` | `BIGINT UNSIGNED NOT NULL` | Conta movimentada, necessariamente do mesmo titular. |
| `ativo_id` | `BIGINT UNSIGNED NOT NULL` | Ativo/moeda movimentado. |
| `papel` | `ENUM('ATIVO','LIQUIDACAO','TAXA','CAIXA','SALDO_INICIAL','TRANSFERENCIA') NOT NULL` | Função da perna no evento. |
| `direcao` | `ENUM('ENTRADA','SAIDA') NOT NULL` | Sentido da quantidade, que é sempre positiva. |
| `quantidade` | `DECIMAL(38,18) NOT NULL` | Quantidade original do ativo. |
| `cotacao_brl` | `DECIMAL(38,18) NULL` | Reais por unidade na data. Não é cotação atual. |
| `valor_referencia_brl` | `DECIMAL(38,8) NULL` | Valor histórico bruto da perna conforme os dados disponíveis. |
| `fonte_cotacao` | `VARCHAR(200) NULL` | Fonte/documento da conversão em reais. |
| `custo_inicial_brl` | `DECIMAL(38,8) NULL` | Custo total informado em saldo inicial; NULL desconhecido e zero explicitamente conhecido. |
| `observacao` | `VARCHAR(500) NULL` | Origem do custo ou detalhe da taxa. |
| `criado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | Instante UTC de criação. |

Chaves: UNIQUE(espaco_id,titular_id,operacao_id,id); INDEX(espaco_id,titular_id,conta_id,ativo_id); INDEX(operacao_id,papel)

## pagamentos_imposto

Pagamento já realizado e informado, sem cálculo de obrigação tributária.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `espaco_id` | `BIGINT UNSIGNED NOT NULL` | Espaço ao qual o registro pertence. |
| `titular_id` | `BIGINT UNSIGNED NOT NULL` | Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso. |
| `operacao_id` | `BIGINT UNSIGNED NOT NULL` | Operação do tipo IMPOSTO, validada pelo serviço. |
| `moeda_id` | `BIGINT UNSIGNED NOT NULL` | Moeda efetivamente paga. |
| `quantidade` | `DECIMAL(38,18) NOT NULL` | Quantidade paga. |
| `cotacao_brl` | `DECIMAL(38,18) NOT NULL` | Reais por unidade na data do pagamento. |
| `valor_pago_brl` | `DECIMAL(38,8) NOT NULL` | Valor total registrado em reais. |
| `competencia` | `DATE NULL` | Primeiro dia do mês de referência, pode diferir do pagamento. |
| `origem_pagamento` | `ENUM('EXTERNO','CARTEIRA') NOT NULL` | Externo não movimenta os saldos do Lastro. |
| `codigo_receita` | `VARCHAR(20) NULL` | Código informado do documento, sem inferência. |
| `referencia_documento` | `VARCHAR(180) NULL` | Número do comprovante. |
| `arquivo_id` | `BIGINT UNSIGNED NULL` | Comprovante privado opcional. |

Chaves: PRIMARY(operacao_id)

## execucoes_calculo

Snapshot versionado e reproduzível do processamento gerencial, não motor fiscal.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador interno, sem significado de negócio. |
| `espaco_id` | `BIGINT UNSIGNED NOT NULL` | Espaço ao qual o registro pertence. |
| `revisao_livro` | `BIGINT UNSIGNED NOT NULL` | Revisão processada, deve coincidir com a atual para relatório vigente. |
| `data_limite` | `DATE NOT NULL` | Processa TODO o histórico até esta data local; não apenas o mês do relatório. |
| `versao_motor` | `VARCHAR(80) NOT NULL` | Versão da política de custo médio, taxas, câmbio e arredondamento. |
| `status` | `ENUM('PROCESSANDO','CONCLUIDO','FALHOU','INCOMPLETO') NOT NULL DEFAULT 'PROCESSANDO'` | INCOMPLETO quando faltam custos/dados que impedem concluir os resultados. |
| `iniciado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | Instante UTC de início. |
| `concluido_em` | `DATETIME(6) NULL` | Instante UTC de término. |
| `erro` | `VARCHAR(1000) NULL` | Motivo de falha ou incompletude. |

Chaves: UNIQUE(espaco_id,id); INDEX(espaco_id,revisao_livro,data_limite,status)

## resultados_operacoes

Composição por evento em uma execução; NÃO altera os fatos originais.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `espaco_id` | `BIGINT UNSIGNED NOT NULL` | Espaço ao qual o registro pertence. |
| `titular_id` | `BIGINT UNSIGNED NOT NULL` | Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso. |
| `calculo_id` | `BIGINT UNSIGNED NOT NULL` | Execução de origem. |
| `operacao_id` | `BIGINT UNSIGNED NOT NULL` | Versão exata da operação processada. |
| `receita_liquida_venda_brl` | `DECIMAL(38,8) NULL` | Receita de venda após taxas alocadas, quando aplicável. |
| `custo_baixado_brl` | `DECIMAL(38,8) NULL` | Custo baixado do ativo principal; NULL quando não aplicável ou desconhecido. |
| `custo_incorporado_brl` | `DECIMAL(38,8) NULL` | Custo incorporado no ativo principal, quando aplicável; outras pernas ficam em resultados_movimentos. |
| `resultado_venda_brl` | `DECIMAL(38,8) NULL` | Resultado de venda; não confundir com câmbio da moeda usada. |
| `resultado_cambio_brl` | `DECIMAL(38,8) NULL` | Resultado de conversões e uso de moedas. |
| `resultado_rede_brl` | `DECIMAL(38,8) NULL` | Resultado de despesas de rede e outras taxas reconhecidas. |
| `resultado_total_brl` | `DECIMAL(38,8) NULL` | Soma das parcelas conhecidas; NULL se insuficientes. |
| `situacao` | `ENUM('CALCULADO','PENDENTE') NOT NULL` | Resultado pendente nunca deve aparecer como zero confirmado. |
| `pendencia` | `VARCHAR(500) NULL` | Explicação da ausência de resultado. |

Chaves: PRIMARY(calculo_id,operacao_id); UNIQUE(espaco_id,titular_id,calculo_id,operacao_id)

## resultados_movimentos

Rastreabilidade do custo e resultado por perna, inclusive taxas em outro token.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `espaco_id` | `BIGINT UNSIGNED NOT NULL` | Espaço ao qual o registro pertence. |
| `titular_id` | `BIGINT UNSIGNED NOT NULL` | Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso. |
| `calculo_id` | `BIGINT UNSIGNED NOT NULL` | Execução de cálculo. |
| `operacao_id` | `BIGINT UNSIGNED NOT NULL` | Operação correspondente ao movimento e ao resultado pai. |
| `movimento_id` | `BIGINT UNSIGNED NOT NULL` | Movimento original. |
| `custo_baixado_brl` | `DECIMAL(38,8) NULL` | Custo retirado do estoque gerencial. |
| `custo_incorporado_brl` | `DECIMAL(38,8) NULL` | Custo adicionado ao estoque. |
| `resultado_venda_brl` | `DECIMAL(38,8) NULL` | Parcela de venda. |
| `resultado_cambio_brl` | `DECIMAL(38,8) NULL` | Parcela de câmbio/conversão. |
| `resultado_rede_brl` | `DECIMAL(38,8) NULL` | Parcela de rede/taxa. |
| `situacao` | `ENUM('CALCULADO','PENDENTE') NOT NULL` | Disponibilidade dos cálculos da perna. |

Chaves: PRIMARY(calculo_id,movimento_id)

## posicoes_calculadas

Snapshot por titular/ativo: preço médio não é calculado separadamente por corretora.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `espaco_id` | `BIGINT UNSIGNED NOT NULL` | Espaço ao qual o registro pertence. |
| `titular_id` | `BIGINT UNSIGNED NOT NULL` | Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso. |
| `calculo_id` | `BIGINT UNSIGNED NOT NULL` | Snapshot até a data limite. |
| `ativo_id` | `BIGINT UNSIGNED NOT NULL` | Ativo do estoque. |
| `quantidade` | `DECIMAL(38,18) NOT NULL` | Quantidade remanescente, zero permitido para ativo encerrado. |
| `custo_total_brl` | `DECIMAL(38,8) NULL` | Custo a aquisição; NULL quando histórico não permite concluir. |
| `preco_medio_brl` | `DECIMAL(38,18) NULL` | Custo unitário gerencial em reais, sem preço de mercado. |
| `situacao_custo` | `ENUM('CONHECIDO','PENDENTE') NOT NULL` | Qualidade do custo, não certificação contábil. |

Chaves: PRIMARY(calculo_id,titular_id,ativo_id)

## saldos_contas

Quantidade por conta para conciliação; separada do custo médio por titular.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `espaco_id` | `BIGINT UNSIGNED NOT NULL` | Espaço ao qual o registro pertence. |
| `titular_id` | `BIGINT UNSIGNED NOT NULL` | Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso. |
| `calculo_id` | `BIGINT UNSIGNED NOT NULL` | Execução de origem. |
| `conta_id` | `BIGINT UNSIGNED NOT NULL` | Custódia. |
| `ativo_id` | `BIGINT UNSIGNED NOT NULL` | Ativo. |
| `quantidade` | `DECIMAL(38,18) NOT NULL` | Saldo total, incluindo quantidades bloqueadas em ordens. |

Chaves: PRIMARY(calculo_id,conta_id,ativo_id)

## conciliacoes_saldo

Saldo digitado do extrato. Diferença é consultada contra um cálculo vigente, não congelada como selo.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador interno, sem significado de negócio. |
| `espaco_id` | `BIGINT UNSIGNED NOT NULL` | Espaço ao qual o registro pertence. |
| `titular_id` | `BIGINT UNSIGNED NOT NULL` | Titular beneficiário dos investimentos; não é necessariamente o usuário de acesso. |
| `conta_id` | `BIGINT UNSIGNED NOT NULL` | Conta do extrato. |
| `ativo_id` | `BIGINT UNSIGNED NOT NULL` | Ativo do extrato. |
| `data_saldo` | `DATE NOT NULL` | Fim do dia no fuso da operação/espaço definido no serviço. |
| `quantidade_extrato` | `DECIMAL(38,18) NOT NULL` | Quantidade total do extrato, incluindo bloqueios. |
| `referencia_documento` | `VARCHAR(200) NOT NULL` | Comprovante que permite refazer a conferência. |
| `arquivo_id` | `BIGINT UNSIGNED NULL` | Extrato privado opcional. |
| `informado_por` | `BIGINT UNSIGNED NOT NULL` | Membro responsável. |
| `criado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | Instante UTC de criação. |
| `atualizado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)` | Atualização cadastral; guardar versão anterior na auditoria. |

Chaves: UNIQUE(espaco_id,conta_id,ativo_id,data_saldo)

## auditoria

Trilha append-only por serviço; não colocar senhas, tokens ou documentos abertos nos JSONs.

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` | Identificador interno, sem significado de negócio. |
| `espaco_id` | `BIGINT UNSIGNED NOT NULL` | Espaço ao qual o registro pertence. |
| `usuario_id` | `BIGINT UNSIGNED NULL` | Autor; NULL para trabalho de sistema. |
| `entidade` | `VARCHAR(64) NOT NULL` | Tipo de entidade, ex.: operacoes, conciliacoes_saldo. |
| `registro_id` | `BIGINT UNSIGNED NOT NULL` | Identificador lógico; integridade polimórfica aplicada pelo serviço. |
| `acao` | `ENUM('CRIAR','CONFIRMAR','CANCELAR','SUBSTITUIR','ATUALIZAR','IMPORTAR') NOT NULL` | Ação auditada. |
| `dados_antes` | `JSON NULL` | Estado anterior sem informações sigilosas desnecessárias. |
| `dados_depois` | `JSON NULL` | Estado seguinte. |
| `correlacao_id` | `VARCHAR(80) NULL` | Identificador de requisição/job. |
| `criado_em` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | Instante UTC de criação. |

Chaves: INDEX(espaco_id,entidade,registro_id,criado_em)
