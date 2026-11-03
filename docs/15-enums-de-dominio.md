# Classificações tipadas com enums — 03/11/2026

## Análise e decisões

| Campo ou valor | Decisão | Motivo |
|---|---|---|
| Ativo.tipo e AtivoResponse.tipo | TipoAtivo: FIAT, CRIPTO | Classificação fechada, controlada pelo sistema e restringida no banco |
| Operacao.tipo | TipoOperacao: SALDO_INICIAL, APORTE, COMPRA, VENDA | Tipos implementados; V10 amplia a constraint para a entrega de negociações, sem antecipar operações futuras |
| Operacao.situacao, SaldoInicialResponse.situacao, filtros e repositories | SituacaoOperacao: CONFIRMADA, SUBSTITUIDA, CANCELADA | Estados fechados utilizados nas transições, consultas e unicidade |
| Parâmetro HTTP situacao | String na fronteira de entrada, convertida uma vez no mapper | Preserva nomes aceitos, mensagem personalizada de erro e opção TODAS |
| TODAS | Opção de consulta, não estado da operação | Convertida em ausência de restrição (null) no FiltroOperacoes; jamais persistida |
| role de acesso | String | Claim fornecida por serviço externo; backend disponibiliza o texto, sem catálogo próprio de permissões ou autorização por role |
| codigo/ticker de ativo | String | Catálogo extensível; os seis ativos do MVP não constituem uma enumeração permanente |
| codigo/nome de instituição, nome de carteira | String | Catálogos e textos abertos |
| perfilRef, autores, IDs REST, chave de requisição, fingerprint | String | Identificadores/referências externos e representações de transporte, não classificações |
| quantidades, custos e datas em DTOs | String/JsonNode preservados | Representação exata do contrato financeiro e parsing estrito, não conjunto fechado |
| justificativas, motivos e mensagens | String | Texto livre |
| ativo de carteira/ativo/instituição | boolean preservado | Já tipado; não exige enum |
| antigos tipos de conta | Não aplicável | Campo removido no modelo vigente; SQL histórico não deve ser reescrito |
| Algoritmo de hash e estrutura do JWT mockado | Literais de protocolo preservados | Detalhes técnicos, não novos conceitos de domínio |

Foram inspecionados entidades, records/DTOs, contratos, usecases, mappers, adapters, controllers, repositories, consultas, migrations, testes e literais. Não existem outros campos vigentes de categoria/modalidade/movimentação que justifiquem enum nesta entrega.

## Organização e implementação

- `ativos.domain.model.TipoAtivo` pertence ao catálogo de ativos.
- `operacoes.domain.model.TipoOperacao` e `SituacaoOperacao` pertencem às operações.
- Enums simples, sem atributos, construtores ou interfaces extras. Nenhum pacote global enums.
- Entidades usam @Enumerated(EnumType.STRING) e @JdbcTypeCode(SqlTypes.VARCHAR), com os comprimentos existentes. A indicação VARCHAR impede que o mapeamento Hibernate escolha ENUM nativo do MySQL.
- DTOs de saída usam o enum do próprio contexto; serialização continua produzindo os nomes textuais anteriores. Contratos públicos entre contextos não foram alterados.
- Repositories e Criteria recebem SituacaoOperacao; comparação de estado usa enum. O tipo fixo da consulta Criteria usa TipoOperacao.SALDO_INICIAL.
- SaldoInicialMapper.situacaoDoFiltro concentra a conversão da entrada. Nomes são exatos e sensíveis a maiúsculas; inválidos continuam retornando 400 com “Verifique os filtros e a paginação.” Não há valueOf espalhado nem captura técnica adicional no handler.

## Compatibilidade com o banco

Consulta somente leitura ao MySQL local antes de editar confirmou: ativos.tipo contém FIAT/CRIPTO; operações existentes contêm SALDO_INICIAL/CONFIRMADA. Constraints das migrations autorizam também SUBSTITUIDA/CANCELADA. Tipos físicos confirmados: ativos.tipo VARCHAR(6), operacoes.tipo VARCHAR(20), operacoes.situacao VARCHAR(12).

Nenhuma migration necessária ou criada. Dados, índices, checks, coluna gerada confirmada, tamanhos, FKs e schema permanecem iguais. Não usar ORDINAL nem converter colunas em ENUM nativo. Dados de teste históricos são inseridos/atualizados como texto apenas no MySQL descartável e lidos pelo mapeamento tipado.

## Testes e riscos

O teste de tipagem falhou antes da alteração e passou após a conversão. Testes adicionais cobrem JSON dos enums/DTO, rejeição de valores desconhecidos, compatibilidade do filtro TODAS, mensagens de erro e leitura via API dos três estados gravados como texto. Suíte de integração confere que as colunas continuam VARCHAR, além dos testes existentes de concorrência, rollback, idempotência e isolamento.

Renomear uma constante persistida passa a ser alteração de contrato de dados: exige estratégia explícita, nunca simples refatoração de nome. Novos tipos de operação dependem da implementação do negócio e evolução das constraints, não apenas de acrescentar constantes. Nenhuma mudança incompatível foi identificada ou aplicada; acesso real e autorização continuam fora desta entrega.

## Verificação concluída — enums, 03/11/2026

Compilação e `gradlew check --offline --no-daemon --console=plain` aprovados: 74 testes, 35 unitários/arquiteturais e 39 integrados MySQL, sem falhas, erros ou ignorados. Revisão independente sem regressões identificadas. Cobertura adicional de enums inclui JSON, filtros e leitura histórica dos três estados; colunas continuam VARCHAR. Nenhuma migration, alteração em dados locais, commit, publicação ou reinício da aplicação.

## Conferência frontend/backend para publicação — 03/11/2026

Contrato HTTP dos enums compatível com o frontend: mesmos campos e valores textuais. Ativo.tipo no TypeScript foi restringido a FIAT | CRIPTO; Saldo.situacao já usava CONFIRMADA | SUBSTITUIDA | CANCELADA. Filtro TODAS preservado, sem alterar payloads, URLs ou representação textual de IDs e decimais.

Frontend aprovado em lint, 20 testes e build/typecheck. Protótipo aprovado em 1 teste. Backend check aprovado com 74 testes (35 unitários/arquiteturais, 39 MySQL), pacote bootJar com frontend reconstruído. Aplicação integrada iniciada em 8080, página e consultas de carteiras, instituições, ativos, saldos e posições com HTTP 200 para os dois perfis. Filtro de histórico conferido no navegador, sem alterar cadastros ou operações locais.

Branch de publicação: codex/saldo-inicial-arquitetura. Escopo reúne saldo inicial e refatorações pendentes desde o merge do PR #13. Credenciais, backups, caches e builds permanecem ignorados. PR deve ficar aberto para revisão, sem merge automático.
