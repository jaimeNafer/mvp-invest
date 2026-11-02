ALTER TABLE titulares
    COMMENT = 'Pessoas a quem pertencem os investimentos. Cada titular possui suas contas e histórico financeiro independente; perfil_ref identifica o perfil responsável pelo cadastro, sem criar usuário ou grupo financeiro.',
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador interno e estável do titular, gerado pelo banco. É referenciado pelas contas; não representa CPF, identificador do perfil de acesso ou instituição.',
    MODIFY COLUMN perfil_ref VARCHAR(120) COLLATE utf8mb4_bin NOT NULL
        COMMENT 'Referência externa do perfil responsável pelo titular, obtida do sub do Bearer via UsuarioAtual. Distingue maiúsculas de minúsculas. Não é CPF, senha ou chave estrangeira de uma tabela de usuários; o token atual é mockado.',
    MODIFY COLUMN nome VARCHAR(160) NOT NULL
        COMMENT 'Nome de exibição da pessoa titular dos investimentos. A API exige texto não vazio e remove espaços nas extremidades. Nomes iguais são permitidos; este campo não comprova identidade fiscal.',
    MODIFY COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE
        COMMENT 'Situação cadastral do titular: 1 para ativo e 0 para arquivado. Arquivar preserva o registro, suas contas e os vínculos existentes, permitindo reativação; não significa ausência de investimentos.',
    MODIFY COLUMN criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT 'Instante de criação do cadastro, com precisão de microssegundos. Gerado pelo banco quando omitido e tratado como UTC pela aplicação. Não é atualizado automaticamente ao editar ou arquivar o titular.';

ALTER TABLE instituicoes
    COMMENT = 'Catálogo de instituições de custódia, como corretoras e bancos. Uma instituição pode ser referenciada por contas de diferentes titulares; não representa o dono dos investimentos.',
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador interno e estável da instituição, gerado pelo banco e referenciado por contas.instituicao_id. Não é código bancário, CNPJ ou identificador de conta.',
    MODIFY COLUMN codigo VARCHAR(80) COLLATE utf8mb4_bin NOT NULL
        COMMENT 'Código interno estável e único da instituição para referência cadastral. Distingue maiúsculas de minúsculas; não deve ser confundido com nome comercial, código bancário ou CNPJ.',
    MODIFY COLUMN nome VARCHAR(160) NOT NULL
        COMMENT 'Nome de exibição da instituição, como o nome comercial de uma corretora ou banco. A identificação única do cadastro é feita pelo id e pelo código, não por este texto.',
    MODIFY COLUMN pais_sede CHAR(2) NULL
        COMMENT 'País da sede da instituição, quando conhecido, convencionado como código ISO 3166-1 alpha-2, por exemplo BR. NULL indica não informado. O banco não valida a lista ISO; o campo não determina sozinho tratamento tributário.',
    MODIFY COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE
        COMMENT 'Indicador de disponibilidade cadastral da instituição: 1 para ativa e 0 para inativa. A inativação preserva o cadastro e as referências de contas existentes; não exclui registros vinculados.';

ALTER TABLE ativos
    COMMENT = 'Catálogo de moedas e criptoativos identificados de forma independente das contas e titulares. Define identidade e precisão do ativo; não armazena quantidade em carteira, cotação ou resultado financeiro.',
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador interno e estável da moeda ou criptoativo, gerado pelo banco. Representa o ativo no catálogo, não uma posição, conta, operação ou identificador de perfil.',
    MODIFY COLUMN codigo VARCHAR(80) COLLATE utf8mb4_bin NOT NULL
        COMMENT 'Identificador canônico e único do ativo, distinguindo maiúsculas de minúsculas, por exemplo fiat:BRL ou crypto:BTC. Diferencia ativos que podem compartilhar o mesmo ticker de exibição.',
    MODIFY COLUMN ticker VARCHAR(32) NOT NULL
        COMMENT 'Símbolo curto usado para apresentar a moeda ou criptoativo, como BRL ou BTC. Não é uma chave única: ativos distintos podem usar o mesmo símbolo; para identificá-los, utilizar id ou código.',
    MODIFY COLUMN nome VARCHAR(160) NOT NULL
        COMMENT 'Nome legível da moeda ou criptoativo no catálogo, como Real brasileiro ou Bitcoin. Destina-se à exibição e não substitui o código canônico na identificação do ativo.',
    MODIFY COLUMN tipo VARCHAR(6) NOT NULL
        COMMENT 'Categoria cadastral permitida pelo banco: FIAT para moeda fiduciária ou CRIPTO para criptoativo. Não define automaticamente classificação fiscal, regime de tributação ou regras de cálculo de imposto.',
    MODIFY COLUMN casas_decimais TINYINT UNSIGNED NOT NULL
        COMMENT 'Número de casas decimais de quantidade previsto para o ativo, entre 0 e 18, exigido no cadastro e limitado por CHECK. É um metadado de precisão; não arredonda valores automaticamente nem define casas de exibição em reais.',
    MODIFY COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE
        COMMENT 'Indicador de disponibilidade cadastral do ativo financeiro: 1 para disponível e 0 para inativo. Não representa saldo, quantidade possuída ou posição aberta; a inativação não remove o cadastro.';

ALTER TABLE contas
    COMMENT = 'Locais de custódia dos investimentos, cada um pertencente a um único titular e opcionalmente associado a uma instituição. Contas não pertencem a espaços ou grupos gerenciais.',
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador interno e estável da conta de custódia, gerado pelo banco. Não é número bancário, endereço de carteira, credencial de corretora ou identificador externo da conta.',
    MODIFY COLUMN titular_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Titular proprietário dos investimentos desta conta. Chave estrangeira obrigatória para titulares.id; cada conta tem exatamente um titular. Não corresponde necessariamente ao perfil que utiliza o aplicativo.',
    MODIFY COLUMN instituicao_id BIGINT UNSIGNED NULL
        COMMENT 'Instituição de custódia associada à conta, quando informada. Chave estrangeira opcional para instituicoes.id; NULL permite conta sem instituição cadastrada, como carteira própria. Não identifica o titular.',
    MODIFY COLUMN nome VARCHAR(120) NOT NULL
        COMMENT 'Nome de exibição ou apelido da conta, único dentro do mesmo titular, inclusive entre contas arquivadas. Titulares diferentes podem usar o mesmo nome. Não precisa conter número bancário ou outra identificação sensível.',
    MODIFY COLUMN tipo VARCHAR(10) NOT NULL
        COMMENT 'Tipo de local de custódia aceito pelo banco: CORRETORA, CARTEIRA ou BANCO. Classifica a conta, sem determinar titularidade, país da instituição ou tratamento tributário dos investimentos.',
    MODIFY COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE
        COMMENT 'Situação cadastral da conta: 1 para ativa e 0 para arquivada. O arquivamento preserva o registro e os vínculos; não significa saldo zero, venda dos investimentos ou encerramento na instituição.',
    MODIFY COLUMN criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT 'Instante de criação do cadastro da conta, com precisão de microssegundos, gerado pelo banco quando omitido e tratado como UTC pela aplicação. Não é a data de abertura da conta na instituição nem se atualiza em edições.';
