# Validação, testes e riscos

## Primeiro recorte real conferido — 03/11/2026

Executada carga pontual do relatório Binance 2024 fornecido por Jaime, sem criar importador de produto. Preparação decimal independente, validação dos 20 eventos, backup integral com recuperação comprovada e limpeza local específica autorizada antecederam a carga pela API. Tipos, quantidades, principal, taxas, referências, UTC, posições, custos e custódia conferidos; repetição com mesmas chaves antes/depois do reinício preservou IDs e revisão. Interface consultada após reinício; outro perfil permaneceu vazio.

As taxas das compras OCBS foram separadas do débito total conforme esclarecimento de Jaime, sem desconto duplo; avaliações em BRL do arquivo não substituíram os movimentos efetivos. CSV, valores, identificadores, backup e evidências ficam somente em `.local/binance2024/`. Nenhum dado real foi convertido em fixture ou publicado. Esta execução não altera o comportamento da aplicação e não exigiu repetir a suíte automatizada de produto.

Resultado limitado a depósitos/compras deste recorte e às premissas confirmadas; não comprova saldo de encerramento por evidência externa independente, vendas, taxas em BNB, transferências ou conformidade fiscal. Avaliação de utilidade, outro participante e sessões posteriores seguem pendentes. [Retomada](06-retomada.md#carga-real-e-conferência-local-concluídas--03112026).

## Estratégia

Três evidências diferentes são necessárias: cálculo correto para casos conhecidos, aplicação funcionando de ponta a ponta e benefício percebido pelos participantes. Uma não substitui as outras.

Usar dados fictícios nos testes automatizados e manter dados reais fora do Git. Casos pequenos devem ter valores esperados calculados e revisados independentemente da função testada. O motor JavaScript pode ajudar na comparação, mas não será o único oráculo.

## Camadas de teste propostas

| Camada | Ferramentas propostas | Evidência |
| --- | --- | --- |
| Domínio | JUnit e BigDecimal, sem contexto Spring | Custos, resultados, taxas, arredondamento e invariantes. |
| API | MockMvc | Contratos e validação das futuras APIs de negócio; não há endpoints de acesso no backend. |
| Persistência/transação | MySQL em Testcontainers, Flyway e MockMvc quando necessário | SQL real, constraints, rollback, revisões e migrations. H2 não substitui esta evidência. |
| Interface | Vitest e React Testing Library | Formulários, erros, carregamento, filtros e limpeza após logout. |
| Fluxo completo | Playwright, como proposta para poucos fluxos críticos | Login → operação → resultado → conferência → exportação; troca de usuário. |
| Volume e recuperação | Histórico sintético e banco descartável | Até 5.000 eventos, estabilidade dos totais e restauração. |

## Matriz inicial de cenários

### MockMvc e banco respondem perguntas diferentes

MockMvc exercita o processamento Spring MVC sem iniciar um servidor HTTP real. Ele não obriga a substituir os serviços por mocks: um teste pode usar MockMvc com contexto completo e persistência real em MySQL descartável. Já um teste focado no controller pode simular o caso de uso para verificar somente entrada, resposta e segurança.

Proposta para manter a suíte simples: testes unitários sem banco para o motor; testes focados de API quando houver contrato relevante; testes de integração com MockMvc, Spring Boot e MySQL/Testcontainers para persistência, migrations, isolamento e rollback. Não criar o mesmo teste nas três camadas sem uma pergunta distinta.

Decisão confirmada por Jaime em 01/11/2026: não incluir H2 nesta etapa, pois domínio não precisa de banco e persistência será validada em MySQL/Testcontainers. Não criar dependência, perfil ou schema H2. Reavaliar somente diante de uma necessidade concreta. Containers de teste não usam as credenciais nem as tabelas do MySQL pessoal.

Referências: [MockMvc](https://docs.spring.io/spring-framework/reference/testing/mockmvc.html) e [integração Spring Boot/Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html).

### Cenários

Cada linha poderá gerar mais de um teste. Completar esta matriz antes de implementar a regra correspondente; adicionar regressão quando surgir um defeito relevante.

| ID | Cenário | Resultado esperado |
| --- | --- | --- |
| T01 | Saldo inicial conhecido, ausente, zero explícito e BRL | Conhecido aceito; ausente recusado; zero confirmado e sinalizado; BRL com custo igual à quantidade. |
| T02 | Compras sucessivas e venda parcial/total | Média móvel correta, baixa proporcional e consumo do custo residual no encerramento. |
| T03 | Compra e venda usando stablecoin | Separar resultado do ativo e da moeda de liquidação; compra pode ter resultado cambial. |
| T04 | Taxa em BRL, ativo recebido/vendido e terceiro ativo | Saldo e custo coerentes; sem desconto duplo; terceiro ativo precisa de saldo e cotação histórica. |
| T05 | Conversão | Um evento com pernas coerentes; preservar a composição de resultado. |
| T06 | Transferência própria e taxa de rede | Principal preserva custo; custódia muda; taxa não aparece duas vezes no resultado. |
| T07 | Aporte, retirada e imposto externo/da carteira | Capital não vira lucro; imposto externo não debita carteira; moeda pode ter resultado cambial próprio. |
| T08 | Saída sem saldo, inclusive taxa e conta errada | Rejeitar tudo sem gravação parcial ou saldo negativo. |
| T09 | Mesmo ativo em contas e usuários diferentes | Custos individuais preservados; consolidação futura soma resultados, sem reutilizar média do grupo nas operações. Perfil A não lista/altera registros do perfil B pelo contrato local; isso não comprova autorização real. |
| T10 | Mesmo instante e eventos fora de ordem de cadastro | Ordem determinística; filtros processam histórico anterior antes da apresentação. |
| T11 | Correção/cancelamento retroativo válido e inválido | Recalcular ou recusar atomicamente; preservar versão anterior, motivo e autoria. |
| T12 | Reenvio da requisição e revisão antiga | Não duplicar fatos; rejeitar conflito de edição com orientação para atualizar. |
| T13 | Comparação de saldo e correção posterior | Diferença atualizada na data informada, sem ajuste automático. |
| T14 | Pendências e mesma referência em vários fills | Sinalizar para revisão; nunca excluir automaticamente. |
| T15 | Ano anterior sem histórico, posição encerrada e ano corrente | Ausência não vira zero certificado; zero sustentado pelo histórico é distinto; ano atual é prévia. |
| T16 | Decimais, divisão periódica e limites de escala | Valores reproduzíveis conforme política versionada; excesso rejeitado; transporte sem Number financeiro. |
| T17 | Login pelo serviço mockado, erro de credenciais, saída e recarga | Jaime/Eliane com senha vazia; credenciais incorretas recusadas; saída/recarga limpam o perfil; nenhuma chamada HTTP. |
| T18 | Contexto por requisição e separação por perfil | Bearer carrega ID/nome/role; ausência/estrutura inválida retorna 401; header/corpo não substituem `sub`. Alternância e requisições simultâneas não misturam registros. Mock não comprova autenticidade; integração externa permanece futura. |
| T19 | CSV/HTML e entradas com caracteres especiais | Totais consistentes; texto não executa script nem fórmula de planilha. |
| T20 | JSON inválido/incompatível e recuperação válida | Falha sem alteração parcial; recuperação mantém isolamento, fatos, auditoria e resultados recalculados. |
| T21 | Migrations em MySQL vazio e banco já migrado | Estrutura criada corretamente; segundo início não altera indevidamente o banco. |
| T22 | Históricos sintéticos crescentes até 5.000 eventos | Paginação correta, totais independentes da página e resultados reproduzíveis. |
| T23 | Prévia no backend, edição, respostas fora de ordem e confirmação após mudança do livro | Mesmo motor na prévia/confirmação; nenhuma gravação na prévia; edição substitui o evento; resposta antiga não sobrescreve dados atuais; revisão desatualizada exige atualização. |
| T24 | Carteiras com uma/várias instituições, edição, nome duplicado e seleção vazia/repetida/inválida | Nome normalizado até 160 caracteres; seleção obrigatória; 400/409 legíveis; falha preserva formulário e não deixa gravação parcial. |
| T25 | Duas carteiras selecionam Binance | IDs de vínculos distintos apontam à mesma instituição; não significam históricos diferentes. Usuários continuam isolados pelo contexto. |
| T26 | Editar instituições, arquivar/reativar e inativar instituição | Edição mantém/cria/remove somente vínculos da carteira; arquivar os preserva. Instituição inativa existente pode ser mantida/removida, nova seleção é recusada. |
| T27 | Concorrência do mesmo nome e rollback | Uma carteira por nome/usuário; respostas 201/409 sem conta órfã ou gravação parcial. |
| T28 | V7 → V8, cadastros antigos não vazios e reaplicação | Recusar dados antigos antes de alterar tabelas; com cadastros vazios preservar ativos, criar constraints/comentários e catálogo; segunda aplicação não muda dados. Testes históricos até V7 preservados. |

T24–T28 usam JPA/MockMvc/MySQL e testes React. [Resultados executados](06-retomada.md). Cenários financeiros abaixo são critérios futuros, ainda sem implementação.

| ID | Cenário futuro do livro | Resultado esperado |
| --- | --- | --- |
| T29 | Mesmo lançamento Binance apresentado em duas carteiras do usuário | Uma operação persistida, com mesmo ID/dados nas duas visões; alteração refletida em ambas. |
| T30 | Visão combinada de carteiras com Binance repetida | Cada operação/saldo/resultado considerado uma vez, usando a união das instituições do usuário. |
| T31 | Remover vínculo, arquivar ou reassociar Binance | Histórico/custos preservados; reassociação mostra os registros existentes sem reimportar. |
| T32 | Jaime e Eliane na mesma instituição | Operações filtradas pelo usuário e instituição; nenhum compartilhamento de histórico entre usuários. |
| T33 | Saldo inicial com seis ativos e data comum por usuário | Regras e resultados fictícios SI01–SI13 do [contrato](12-saldo-inicial.md), incluindo custo conhecido, zero explícito, BRL e precisão. |
| T34 | Repetição de envio ou primeira abertura concorrente | SI14/SI15/SI19/SI20: nenhum fato duplicado, data comum consistente e rollback integral. |
| T35 | Corrigir/cancelar abertura e consultar histórico | SI16–SI18: valores anteriores preservados, motivo/autoria e posição baseada somente nos confirmados. |
| T36 | Reiniciar e consultar várias carteiras/páginas | SI02/SI04/SI21/SI22: histórico único, usuários separados e totais independentes da paginação. |

Saldo inicial implementado e verificado em 02/11/2026: 15 testes integrados de API/transação/concorrência, 3 de migration V9, 2 unitários de cálculo, 5 testes React do fluxo e 2 de decimais. As suítes completas passaram: 44 backend e 20 frontend, além de lint/build/pacote integrado. T29–T36 têm cobertura nesta fatia de abertura; isso não comprova tipos de operação futuros.

Casos cobertos incluem catálogo/contexto, isolamento, união de carteiras sem duplicação, custo médio global, BRL/USD/USDT, precisão/calendário, JSON numérico recusado, zero explícito, repetição após cancelamento/inativação, redefinição de data sem confirmados, correção/cancelamento, rollback e três disputas concorrentes. Banco vazio, atualização V8→V9, reexecução sem mudanças, catálogo incompatível, constraints e comentários também foram exercitados.

No navegador local foram executados cadastro, consulta por duas carteiras, correção, cancelamento e consulta das versões após reiniciar o JAR. As duas versões temporárias foram removidas por identificação exata; dados adicionados pelo usuário durante a conferência e cadastros anteriores foram preservados. Esses ensaios não representam validação fiscal, piloto de utilidade, teste de volume ou CI desta alteração.

Não prometer cobertura de todos os casos possíveis. A matriz cobre as regras conhecidas e deve evoluir com o histórico real.

### Exemplo financeiro de referência para T02

Fixture fictícia em BRL, com um usuário e uma conta; usar strings decimais na implementação:

1. Aporte de R$ 10.000.
2. Compra de 0,01 BTC ao preço unitário de R$ 400.000, com taxa de R$ 10 em BRL. Custo incorporado: R$ 4.010; caixa: R$ 5.990.
3. Venda de 0,004 BTC ao preço unitário de R$ 450.000, com taxa de R$ 5 em BRL. Receita bruta: R$ 1.800; líquida: R$ 1.795; custo baixado: R$ 1.604; resultado da venda: R$ 191.
4. Posição final: 0,006 BTC com custo de R$ 2.406 e caixa de R$ 7.785. Taxas informativas: R$ 15, já consideradas; não descontar novamente do resultado de R$ 191.

Esse caso tem resultados exatos e verifica a ligação entre fatos, custos, caixa e resultado. Não cobre divisões periódicas, outras moedas de taxa ou política tributária; usar casos adicionais para essas regras.

## Ensaio de volume

Gerar eventos válidos, com múltiplas contas/usuários, taxas e correções. Conferir um fechamento conhecido também na massa grande; não medir velocidade sobre eventos financeiramente inválidos.

Medir listagem filtrada, confirmação com recálculo, visão geral e exportação anual, registrando equipamento e versão do banco. Metas provisórias a calibrar: listagem em até 1 segundo e confirmação com recálculo em até 5 segundos no computador do piloto. Ultrapassagem exige investigação e registro antes do piloto, não uma mudança automática para arquitetura distribuída.

## Riscos e resposta planejada

| Risco | Resposta |
| --- | --- |
| Digitação de histórico grande inviabiliza o uso | Validar recorte pequeno e medir esforço; importação CSV é uma próxima decisão, não requisito escondido. |
| Aparência correta esconde cálculo errado | Fixtures independentes, rastreabilidade e revisão contábil quando disponível; não certificar apuração tributária. |
| Modelo de 20 tabelas aumenta escopo | Revisar para as capacidades atuais; não construir importadores ou telas só porque existem tabelas propostas. |
| Perda de dados no computador | Backup técnico, exportação e recuperação testada em banco separado; cópia protegida fora do disco principal. |
| Troca de usuário revela dados | Saída limpa o perfil em memória. Não tratar o mock como proteção; definir contrato externo e separação dos dados antes das APIs de negócio. |
| Pausas longas dificultam retomada | Tarefas pequenas e notas de retomada com próximo passo explícito. |
| Só os fundadores gostarem da solução | Registrar limite da evidência; validar usuários externos e pagamento apenas em etapa posterior. |

## Registro de uma sessão real

```text
Participante e data:
Recorte e contas cobertas (sem dados sensíveis no Git):
Tarefa realizada:
Tempo aproximado e comparação com controle atual:
Resultado conferido / divergência e explicação:
Ajuda necessária:
Utilidade percebida e dificuldade principal:
Próxima ação:
```

## Checklist de saída do piloto local

- [ ] Jaime e Eliane completaram o fluxo principal com suas carteiras, sem misturar os cadastros entre perfis.
- [ ] Houve manutenção do histórico em sessões posteriores, não apenas demonstração inicial.
- [ ] Resultados e saldos do recorte foram conferidos, com divergências explicadas.
- [ ] CSVs/relatório foram avaliados quanto à utilidade; revisão por contador registrada se ocorrer, sem presumir que já aconteceu.
- [ ] Backup e recuperação foram demonstrados sem usar o banco real como ambiente de teste destrutivo.
- [ ] Esforço de atualização e dependência de ajuda foram registrados.
- [ ] Próximo investimento escolhido a partir dessas evidências.

## Evidência da refatoração arquitetural — 02/11/2026

`check` aprovado com 51 testes: 13 unitários e 38 integrados, sem falhas/ignorados. Preserva os casos financeiros e de migrations e acrescenta dois testes arquiteturais, quatro testes do tratamento global/logging e um cenário integrado de limite do nome após normalização.

As verificações arquiteturais inspecionam imports e tipos por reflexão: domínio sem Spring/camadas externas, aplicação sem infraestrutura, controllers dependentes de interfaces de usecase, contratos sem entidades e tipos de produção sem aninhamento. DTOs genéricos e records são inspecionados recursivamente. Isso não substitui revisão de código nem analisa todo acesso possível no bytecode.

Erros exercitados: 400/401/404/409/500/503, mídia 415 e método 405 com Allow preservado, integridade conhecida e inesperada, além de uma emissão de log sem a mensagem sensível da exceção. Frontend lint/20 testes/typecheck/build e pacote integrado passaram. Consulta no navegador e snapshots privados idênticos confirmaram a execução local preservando os dados. Sem nova CI nesta tarefa.

## Lombok e prefixo I

`check` aprovado com 53 testes: 15 unitários e 38 integrados MySQL/Testcontainers. O padrão de interfaces e a ausência de setters em entidades são verificados por reflexão; a coleção de vínculos é testada como snapshot imutável que não acompanha alterações posteriores nem permite mutações externas. Casos existentes continuam cobrindo persistência, IDs, auditoria, UTC, concorrência e contratos HTTP.

Revisão independente confirmou a cópia de @Qualifier para o construtor Lombok e a propagação das mensagens das exceções no bytecode. Pacote integrado e testes unitários finais passaram; inicialização local e consultas HTTP foram conferidas sem modificar dados. Não houve nova execução do frontend ou CI nesta tarefa de backend.

## Auditoria modular

Compilação/check aprovados com **64 testes**: 26 unitários e 38 integrados, sem falhas/ignorados. Sete testes ArchUnit analisam bytecode para contratos públicos/matriz, ciclos, camadas, domínio com exceção JPA limitada, encapsulamento de contratos e tratamento transversal. Três testes de contratos cobrem identidade/união/ausência de carteira alheia, metadados inativos em lote e elegibilidade de ativo sem ocultar histórico. Testes existentes de APIs, decimais, rollback, concorrência, idempotência e migrations foram preservados.

Baseline registrou falha de compilação nas exceções e, após reparo mínimo, três falhas do handler. A auditoria corrigiu essas regressões. Pacote integrado e consultas HTTP dos dois perfis passaram; snapshots privados confirmam preservação integral das seis tabelas. Sem migrations novas, frontend, CI ou publicação Git nesta tarefa. [Diagnóstico e limites](13-auditoria-modular.md).

## Handler mínimo revisado por Jaime

Total validado: 66 testes (28 unitários, 38 integrados), sem falhas/ignorados. Foram preservados os conflitos/concorrência reais e rollback, agora com tradução UNIQUE na origem. Testes HTTP verificam mensagem de negócio/constraint na resposta e no log, ResponseEntity<ProblemDetail> e ausência de catch técnico genérico. Erros MVC não personalizados ficam com Spring. ArchUnit proíbe persistência no pacote do handler. Pacote integrado e respostas locais 400/401 conferidos, sem gravação de cadastro ou mudanças de schema.

## Resultado da verificação — 03/11/2026

- Compilação de produção e testes concluída.
- 32 testes unitários/arquiteturais executados: 30 aprovados, 2 falhas preexistentes em TratamentoGlobalExceptionTests; nenhum ignorado.
- 38 testes de integração com MySQL descartável aprovados, incluindo concorrência, atomicidade, migrações e isolamento por usuário.
- Três novos testes de DTOs/endpoints aprovados: limites após normalização, IDs/coleções/cancelamento e rejeição antes do usecase em criação, substituição e cancelamento.
- Novo teste de sincronização dos vínculos aprovado, preservando objetos mantidos e unicidade.
- Check global permanece vermelho pelas duas divergências do handler descritas nas pendências. Não declarar esta suíte integralmente aprovada.
- Revisão independente da refatoração principal não identificou regressões concretas. Complemento Bean Validation revisado localmente; revisor adicional indisponível por capacidade.
- Nenhuma alteração em migrations, schema ou dados locais; sem reinício da aplicação, publicação ou commit. Frontend não foi alterado nem revalidado nesta tarefa.

Detalhes em [Revisão de implementação](14-clean-code-backend.md).

## Contrato do handler confirmado — 03/11/2026

Jaime confirmou o comportamento vigente da classe: respostas 401 sem WWW-Authenticate e log descritivo único em ERROR com status HTTP e mensagem da origem. Os testes foram ajustados para verificar esse contrato; a implementação do handler não foi alterada.

`gradlew check --offline --no-daemon --console=plain` aprovado: 70 testes, sendo 32 unitários/arquiteturais e 38 integrados MySQL, sem falhas, erros ou testes ignorados. As duas divergências registradas na execução anterior estão resolvidas. Sem alteração em schema, migrations ou dados, sem publicação e sem reinício da aplicação.

## Verificação concluída — enums, 03/11/2026

Compilação e `gradlew check --offline --no-daemon --console=plain` aprovados: 74 testes, 35 unitários/arquiteturais e 39 integrados MySQL, sem falhas, erros ou ignorados. Revisão independente sem regressões identificadas. Cobertura adicional de enums inclui JSON, filtros e leitura histórica dos três estados; colunas continuam VARCHAR. Nenhuma migration, alteração em dados locais, commit, publicação ou reinício da aplicação.

## Conferência frontend/backend para publicação — 03/11/2026

Contrato HTTP dos enums compatível com o frontend: mesmos campos e valores textuais. Ativo.tipo no TypeScript foi restringido a FIAT | CRIPTO; Saldo.situacao já usava CONFIRMADA | SUBSTITUIDA | CANCELADA. Filtro TODAS preservado, sem alterar payloads, URLs ou representação textual de IDs e decimais.

Frontend aprovado em lint, 20 testes e build/typecheck. Protótipo aprovado em 1 teste. Backend check aprovado com 74 testes (35 unitários/arquiteturais, 39 MySQL), pacote bootJar com frontend reconstruído. Aplicação integrada iniciada em 8080, página e consultas de carteiras, instituições, ativos, saldos e posições com HTTP 200 para os dois perfis. Filtro de histórico conferido no navegador, sem alterar cadastros ou operações locais.

Branch de publicação: codex/saldo-inicial-arquitetura. Escopo reúne saldo inicial e refatorações pendentes desde o merge do PR #13. Credenciais, backups, caches e builds permanecem ignorados. PR deve ficar aberto para revisão, sem merge automático.

## Entrega de negociações concluída — 03/11/2026

Implementados aporte em BRL, compra e venda com BRL/USDT/USDC como cotação, taxas nas moedas do par, prévia/confirmacão pelo motor Java, revisão/idempotência, histórico por versões, correção/cancelamento e posição atual global com custódia. Saldo inicial continua acessível e participa do mesmo recálculo ao ser alterado. [Contrato e regras](16-negociacoes.md).

Validação: backend check aprovado com **104 testes** (55 unitários/arquiteturais e 49 integrações MySQL), incluindo 5.000 eventos sintéticos e V9→V10 com 1.105 versões históricas. Frontend aprovado em lint, **44 testes**, typecheck/build; protótipo preservado com **1 teste** aprovado. Pacote integrado reconstruído com o bundle final.

V10 aplicada no MySQL local após backup privado. Comparação de snapshots das seis tabelas confirmou preservação integral dos campos e fatos anteriores; comentários dos campos conferidos, sem ausências. Reinicialização validou dez migrations sem reaplicação. Aplicação integrada disponível em http://127.0.0.1:8080/.

No navegador: acesso, listagem unificada, posição atual, abertura do formulário, campos específicos de aporte e prévia calculada sem gravação. Consultas novas e legadas retornaram 200 para os dois perfis. Nenhuma operação fictícia foi gravada no banco pessoal. Escritas/correções/cancelamentos e concorrência foram exercitados em MySQL descartável nos testes.

Revisões corrigidas: limite temporário da CTE na migração; autoria no DTO; proteção contra saída no primeiro POST pendente; rejeição de consulta com revisões divergentes; moeda negativa HALF_EVEN e duas casas com valor exato acessível. Nenhum achado material aberto dessas revisões.

Branch local: codex/negociacoes-livro. Alterações do backlog anteriores preservadas; sem commit, push ou novo PR nesta tarefa. BNB permanece confirmado para o futuro. Próximo passo de produto: validar um recorte real do ciclo e escolher os complementos do backlog com base nessa conferência.
