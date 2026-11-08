## Verificação antes da publicação — 07/11/2026

O frontend passou em lint, 123 testes e build. O backend passou em 197 testes unitários e arquiteturais; na suíte completa de integração, 155 dos 162 testes passaram. Os sete cenários com falha, que permanecem para investigação e correção, são:

- `AssociacaoIntegrationTests.associacaoDeTaxaAnteriorEncerraAjusteSemRepetirQuantidades`
- `AssociacaoIntegrationTests.editarLinhaNaoMudaGrupoAplicadoAntesDeReavaliar`
- `AssociacaoIntegrationTests.manualAteDoisSegundosConfirmaTaxaZeroSemDuplicarEDesfaz`
- `ConciliacaoCasosIntegrationTests.rejeitarAjustePreservaRegistroAplicadoComDeficitExplicito`
- `ConciliacaoCasosIntegrationTests.revisarNaoResolveLacunaEEdicaoNaoHerdaRevisaoAnterior`
- `ConciliacaoIntegrationTests.csvParcialAntesDoPrimeiroAceitePreservaAsDuasQuantidades`
- `SaldosIniciaisIntegrationTests.catalogoSomenteLeituraEContextoObrigatorio`

As falhas não foram corrigidas nem classificadas como inofensivas nesta publicação. As fixtures foram desvinculadas de identificadores e quantidades de dados reais antes do versionamento; arquivos privados continuam ignorados.

## Conversões entre ativos — 07/11/2026

`Binance Convert` e `Convert` são associados quando as contrapartidas são inequívocas; os originais e as propostas versionadas permanecem preservados. Quantidades podem ser aplicadas sem cotação; custo e resultado dependentes permanecem indisponíveis. Valores consistentes do relatório enriquecido sem taxa não zero ou uma cotação informada permitem cálculo financeiro. Taxas e execuções parciais usam o contrato em [Associação Binance](22-associacao-registros-binance.md#conversões-entre-ativos--07112026).

Verificados 197 testes unitários de backend, 12 cenários integrados focados, lint, build e 123 testes de frontend. A suíte integral de integração não foi repetida; falhas anteriores continuam registradas abaixo. O pacote integrado foi atualizado em `http://127.0.0.1:8080/`; página, bundle e consulta autenticada do histórico responderam HTTP 200. Nenhuma migration foi adicionada nesta alteração.

## Movimentações internas Simple Earn — 07/11/2026

Aplicações e resgates Locked/Flexible com descrição e sinal reconhecidos são propostos como movimentações internas sem efeito patrimonial, inclusive quando apenas uma perna está presente. Rendimento segue como crédito. A reclassificação de efeito previamente aplicado exige confirmação global; o histórico anterior permanece até então. Nomes desconhecidos e sinais contraditórios não recebem essa classificação. [Contrato](21-contrato-conciliacao.md#aplicações-e-resgates-simple-earn--07112026).

Verificados os testes unitários do backend, sete cenários integrados focados (incluindo compra/custo, quatro tipos, isolados, reimportação, histórico aplicado, rendimento e compatibilidade com Convert), 122 testes frontend, lint e build. A suíte integral de integração não foi repetida; falhas anteriores seguem registradas abaixo. O tratamento existente de `Binance Convert` nas linhas de movimentações não foi ampliado nesta entrega. Aplicação integrada atualizada em `http://127.0.0.1:8080/`: página, bundle e API autenticada do histórico responderam HTTP 200. Não houve migration nesta alteração.

## Aplicação local atualizada — 07/11/2026

O pacote integrado mais recente foi reconstruído e iniciado em `http://127.0.0.1:8080/`, com MySQL persistente. Foi criado backup privado antes da migration V15; o Flyway confirmou a versão 15. Página, bundle atualizado e consulta autenticada do histórico responderam HTTP 200. A regra de entregar o aplicativo em 8080 foi registrada em [AGENTS.md](../AGENTS.md). A situação das verificações de integração está descrita na seção de créditos automáticos abaixo.

## Compatibilidade de associações anteriores

Associações persistidas antes da inclusão de `taxaZeroDeclarada` voltam a ser lidas sem erro. A ausência do campo é interpretada como ausência de declaração, preservando o conteúdo armazenado. Testes focados de mapeamento, projeção, agrupamento e arquitetura passaram. Após reiniciar a instância local, as consultas de visão geral, histórico e contas responderam para os dois perfis, sem alteração dos registros.

## Interface de importação

A parte superior de Importações apresenta seleção de arquivos, nomes escolhidos e resumo de conferência com contagens e progresso. A barra considera os registros do histórico informados pela API; o resumo compacto da Visão Geral permanece disponível. O recebimento e a confirmação global mantêm seus fluxos existentes. Lint, build e 120 testes do frontend passaram; a tela foi aberta no navegador com dados existentes, sem importar arquivos.

## Saídas de criptomoedas

Retiradas de cripto identificadas na Binance são apresentadas como saídas, com finalidade informada separadamente: desconhecida, carteira própria, terceiros ou outra. A confirmação global continua necessária para aplicar o débito. A natureza da saída não comprova a contrapartida; a conferência não atribui situação fiscal. A falta de saldo anterior é sinalizada separadamente da identificação da movimentação.

Quando o arquivo informa que a taxa está incluída, mas não discrimina seu valor, o detalhamento permanece pendente. A quantidade retirada é debitada uma única vez; informar a parcela de taxa não acrescenta um segundo débito. O custo e o resultado que dependem de informações ausentes não são apresentados como completos. Os registros originais permanecem disponíveis para consulta e correção.

Passaram os testes focados de classificação, motor e arquitetura, dois cenários integrados em MySQL descartável, lint, build e 16 testes focados do frontend. O pacote atualizado responde em http://127.0.0.1:8081/ com o banco de revisão preservado. A suíte integrada completa e a conferência visual desta alteração permanecem pendentes.

## Depósitos e retiradas em BRL

Depósitos e retiradas em BRL são classificados automaticamente quando o sinal é compatível e não há indícios de transferência. Os registros já recebidos e não editados também são avaliados. Movimentos de cripto e casos com contrapartida possível entre contas seguem para conferência. A retirada debita a custódia sem gerar resultado realizado; os efeitos financeiros são aplicados pela confirmação global.

Os 183 testes unitários do backend passaram antes do ajuste final da indicação de confirmação; depois desse ajuste, passaram os testes focados de domínio e arquitetura. Cinco testes integrados em MySQL descartável cobriram aporte, retirada, ambiguidade entre contas, correção manual, reclassificação de registro anterior e preservação dos resultados financeiros. Lint, build e 15 testes focados do frontend passaram. A versão foi iniciada em http://127.0.0.1:8081/ com o banco de revisão. Um XLSX sintético na instância confirmou aporte e retirada coerentes aguardando confirmação global. A suíte integrada completa de outras funcionalidades segue pendente.

## Coerência e confirmação global

A classificação usa a consistência dos dados e os efeitos calculados pelo motor. A confirmação global pendente é apresentada separadamente e não cria uma pendência para cada linha. O teste integrado cobre compra SOL/BRL com recursos suficientes, custo conhecido e origens preservadas antes da confirmação.

## Taxas importadas e apresentação compacta — 07/11/2026

Por decisão de Jaime, taxa não informada na importação é considerada zero. Taxas informadas continuam sendo processadas, inclusive pendências para taxas em terceiro ativo. O card agrupado mostra um resumo compacto e mantém registros originais recolhidos. Removida a confirmação individual de taxa zero da associação. Testes focados de frontend, domínio, arquitetura e integração foram executados; a suíte integrada completa continua pendente.

## Vínculo automático e associação no card — 07/11/2026

O Histórico agrupa correspondências inequívocas antes da confirmação global. Referências fortes diferentes podem formar operações separadas no mesmo instante. Escolhas manuais ficam no card, com rádio quando há vários destinos e campos adicionais recolhidos.

Instância isolada em http://127.0.0.1:8081/ atualizada e confirmada por HTTP. Um XLSX sintético com NEAR/BRL e SOL/BRL no mesmo segundo apresentou duas operações automáticas, cada uma com duas linhas originais, antes da confirmação. Passaram os testes focados de frontend, de arquitetura e agrupamento e dois cenários integrados em MySQL. A suíte integrada completa e a inspeção visual no navegador seguem pendentes.

## Revisão de Importações e Histórico — 07/11/2026

A timeline reúne uma operação por número de associação antes de paginar. Os detalhes mantêm as linhas de origem e suas ações. CSV enriquecido distingue o identificador repetido da linha física e evita projetar novamente a contrapartida já representada em uma linha própria. Nas compras OCBS em BRL, a taxa incluída no total enviado é separada do principal.

Pacote atualizado em http://127.0.0.1:8081/ com schema isolado em Docker. HTTP 200 com o bundle atualizado. Ensaio de API com CSV sintético confirmou uma compra consolidada, quatro linhas de origem, débito em BRL sem repetição e reimportação reconhecida. Lint, build, 22 testes focados de frontend e quatro cenários integrados de CSV em MySQL passaram. A primeira execução integrada com Docker encontrou oito falhas antes dos ajustes. A execução completa e a conferência visual no navegador precisam ser repetidas. A instância pessoal em 8080 conserva o pacote anterior.

## Consolidação automática Binance — implementação em 07/11/2026

Evolução em andamento na branch `codex/conciliacao-progressiva`: reconhecer automaticamente combinações únicas de linhas da Binance, somar execuções parciais dentro de janela temporal configurável e usar o mesmo fluxo para CSV/XLSX. A tela de Importações reúne recebimento e timeline; ambiguidades apresentam candidatas em rádio sem seleção inicial. Grupos e efeitos só entram no livro após confirmação global.

Validação executada nesta retomada: frontend lint, 115 testes e build aprovados; suíte unitária/arquitetural completa do backend aprovada com 178 testes, incluindo agrupamento e leitores CSV/XLSX. O código dos testes integrados também compila. A configuração da janela integra o contexto de operações e é injetada por interface. A execução integrada MySQL não ocorreu porque Testcontainers não encontrou daemon Docker acessível; a conferência visual segue pendente. Nenhum acesso, limpeza ou escrita no MySQL pessoal foi feito.

Nenhum commit ou publicação foi feito nesta retomada. [Contrato e critérios](22-associacao-registros-binance.md).

## Associação de registros Binance — concluída em 07/11/2026

O Histórico oferece associação contextual, prévia e edição de grupos pelo número interno. O reconhecimento automático exige instante exato e interpretação única. A janela manual é de até dois segundos entre os extremos. A confirmação global aplica as mudanças do grupo e dos ajustes em uma transação, preservando originais e versões. Taxas em terceiro ativo mantêm o débito conhecido e a valoração pendente. [Contrato](22-associacao-registros-binance.md).

O pacote integrado passou com 171 testes unitários e arquiteturais e 131 integrados em MySQL, sem falhas ou ignorados. A cobertura do escopo verificado foi de 98,12% das linhas e 89,26% das ramificações. Lint, 115 testes e build do frontend aprovados. O ensaio em 8081 usou dados sintéticos e conferiu o agrupamento SOL/BRL, o encerramento do ajuste após confirmação e o desfazimento com reprocessamento. A preservação do ajuste enquanto a proposta aguarda confirmação também tem teste de regressão.

O Lastro pessoal está disponível em http://127.0.0.1:8080/. O Flyway confirmou a V14, registrada em 06/11/2026. Nesta retomada foi feito um backup privado do schema vigente antes do reinício. A comparação das 17 tabelas fora do histórico do Flyway confirmou checksums iguais. Nenhuma limpeza ou importação real ocorreu nesta retomada.

## Aplicação pessoal iniciada — 06/11/2026

A pedido de Jaime, pacote integrado iniciado em http://127.0.0.1:8080/, usando MySQL local e configuração privada. Backup completo gerado antes da aplicação da V13; Flyway aplicou a migration e confirmou o schema atualizado no reinício. Processo antigo do Lastro que bloqueava a porta sem responder foi substituído. Frontend integrado respondeu HTTP 200. Nenhuma limpeza ou importação foi executada. A porta 8081 continua sendo o ambiente isolado com dados sintéticos; não confundir com o banco pessoal. Esta nota atualiza o estado operacional das notas abaixo, que preservam o contexto da verificação anterior.

## Conciliação progressiva — implementação de 06/11/2026

Primeiro ciclo implementado e verificado na branch `codex/conciliacao-progressiva`: receber CSV/XLSX, preservar originais e linhas inválidas, consultar timeline, aprovar ajustes BRL/cripto e encerrá-los ou reduzi-los ao informar a origem real. [Contrato HTTP e limites](21-contrato-conciliacao.md). Não houve commit, push ou alteração do banco pessoal nesta execução.

Recepção e aplicação são separadas. Propostas editáveis e último snapshot aplicado impedem somar a entrada real ao ajuste anterior. Alterações ilegíveis ou impedidas preservam os efeitos anteriores; a prévia usa a mesma seleção da confirmação. Ajustes têm âncora técnica, aceite e versões, sem data econômica inventada. Revisão manual permanece independente do estado automático.

V13 acrescenta contas de custódia, registros, histórico de propostas, ajustes, comandos e arquivos recebidos. V1–V12 permanecem intactas. Legados conservam a custódia Não especificada. Receber arquivo anterior não muda a data contratual de uma abertura confirmada.

A Visão geral e posições usam o mesmo livro para históricos completos e progressivos. Ausência de custo ou cobertura gera informação indisponível e subtotais conhecidos; não uma carteira vazia ou resultado zero. Históricos completos mantêm os números anteriores.

Verificação final: 273 testes backend (152 unitários/arquiteturais e 121 integrados MySQL), sem falhas, erros ou testes ignorados; `check` e pacote integrado aprovados. Cobertura do recorte verificado: 97,72% de linhas e 88,61% de branches, preservando mínimos de 90%/85%. Frontend: 104 testes, lint, TypeScript e build aprovados. Revisão independente das cinco correções finais não encontrou regressões diretas; não foi uma nova auditoria geral. CI não executada nesta entrega.

Ensaio visual em `http://127.0.0.1:8081/`, com MySQL descartável e arquivos sintéticos privados: recebimento de linha válida/inválida, aceite de ajuste BTC e encerramento após nova entrada anterior confirmados. Pacote final reiniciado e conferido no navegador: três registros preservados, nenhum ajuste ativo após encerramento, totais incompletos indisponíveis e parcelas conhecidas separadas, sem apresentar carteira vazia ou zero fictício. O banco pessoal de `8080` não recebeu V13 nem dados de teste. Arquivos/evidências em `.local/conciliacao/`.

Limites: sobreposições ambíguas e referências repetidas ficam preservadas e impedidas; não se eliminam ocorrências por coincidência. Associação inicial somente para duas pernas explícitas, opostas e reciprocamente únicas de transferência interna. Reconstrução geral de negociações multilinhas, taxas em BNB, Earn e apuração fiscal continuam fora desta fatia.

As notas seguintes registram o planejamento e as entregas anteriores.

## Revisão da próxima entrega — 06/11/2026

Consolidado o [plano de conciliação progressiva e timeline](20-conciliacao-progressiva.md), incorporando a revisão enviada por Jaime. Direção preservada: receber histórico incompleto, aproveitar quantidades disponíveis, sugerir ajustes BRL/cripto com aceite em lote e corrigir posteriormente, sem inventar custos.

Ajustes incorporados: Coerente separado de revisão manual; ajuste representa lacuna com âncora técnica, sem data fictícia; custódia por usuário/conta/ativo e custo global por usuário/ativo; disponibilidade de quantidade/custo/resultado separada; impacto antes do percentual; reprocessamento idempotente e primeiro ciclo completo antes de ampliar associações/edição.

Adequações ao modelo: contas atual continua vínculo carteira/instituição; conta de custódia será conceito próprio, sem recriar titulares. Legado sem conta não será presumido Spot. A associação inicial será restrita à transferência interna observável e confirmada, sem motor genérico. Relatórios fiscais ficam futuros, com obrigação de separar fatos/ajustes quando existirem.

Somente documentação alterada nesta revisão. Não implementados timeline, custódias, ajustes ou motor parcial. Ainda é necessário fechar contratos HTTP e modelagem física antes de codificar; não há migration V13 aplicada/criada. Testes da aplicação não foram reexecutados por esta mudança documental.

A feature CSV anterior foi publicada na branch codex/importacao-binance, commit 5eef86c, sem PR ou merge. Esta revisão documental permanece sem publicação.

## Importação Binance — retomada de 06/11/2026

Implementada a primeira fatia de importação do CSV enriquecido: depósitos BRL e compras OCBS/CONVERT em BRL, com prévia, confirmação atômica, recibo, arquivo original e proveniência. Interface em **Operações → Importar arquivo**. [Contrato](19-importacao-binance.md).

Branch de entrega `codex/importacao-binance`. Publicação autorizada por Jaime em 06/11/2026, sem merge em `main` ou abertura de PR nesta etapa. Não representa suporte aos XLSX de movimentações ou ao histórico completo de outros tipos financeiros.

Última verificação automatizada concluída em 05/11: **200 testes backend** (105 unitários/arquiteturais + 95 integrados MySQL), sem falhas ou skips; `check` e pacote integrado aprovados. Cobertura do recorte de motor/parser/mappers: **98,61% de linhas e 88,97% de branches**, acima dos mínimos de 90%/85%. Frontend: **91 testes**, lint, typecheck e build aprovados. Cenários incluem concorrência, rollback de operações/recibo, isolamento, legado, sobreposição, cancelamento e 5.000 eventos.

Backup privado restaurado em MySQL descartável, com checksums de todas as tabelas conferidos. Na cópia, a prévia do CSV original reconheceu os 20 eventos existentes, sem registros novos nem erros. Nenhum fato real foi reinserido. Evidências e arquivos reais em `.local/importacao-implementacao/`, ignorada pelo Git.

Pendente: concluir o ensaio visual de upload → prévia → confirmação → detalhes e reinício e revisar eventuais ajustes. A publicação da branch não encerra esse aceite. O teste visual anterior foi interrompido no seletor de arquivos; não foi declarado concluído.

Aplicação local iniciada em 06/11/2026 em http://127.0.0.1:8080/, com frontend e backend no pacote integrado e MySQL `lastro_local`. Gerado novo backup privado antes da V12; migration aplicada e checksums das tabelas anteriores preservados. Frontend/assets e endpoints de importações, operações e visão geral responderam HTTP 200 para os dois perfis. Não foram confirmadas operações durante esta inicialização. Pacote reconstruído com sucesso; não foi repetida a suíte completa nesta retomada, pois não houve mudança de código de aplicação.

As notas seguintes são históricas.

## Evolução da consolidação financeira — 04/11/2026

Concluída a evolução da Visão geral: resultado realizado líquido acumulado mensal, contribuições por ativo, saldos em moedas e avisos de conferência. Composição a custo e custódia preservadas. Contrato ampliado em [Visão geral](18-visao-geral.md), sem novas tabelas ou migrations.

A série começa em zero no período escolhido, preserva meses vazios e limites parciais e termina no resultado do resumo. Motor único mantém custos anteriores. Vendas atribuem realização ao ativo vendido; compras com USDT/USDC atribuem à moeda gasta. Aportes e abertura não geram lucro. Parcelas e ativos reconciliam com o resumo, sem descontar taxas novamente.

Conferência sob demanda, paginada, com custo zero declarado e referências exatamente repetidas por usuário/instituição. Não diagnostica erro nem deduplica. Avisos anteriores ao início do período continuam relevantes até a data dos saldos; diferença de revisão exige atualizar o painel. Detalhes reutilizam o fluxo existente. Custo desconhecido e entradas incompletas continuam rejeitados; retiradas permanecem em entrega separada.

Verificação: **149 testes backend** (66 unitários/arquiteturais e 83 integrados MySQL), sem falhas ou skips; **78 testes frontend**, lint, typecheck/build e JAR integrado aprovados. Cobertos resultado negativo/positivo/zero, USDT/USDC, taxas, ativo esgotado, abertura/aportes, meses parciais/vazios, fronteira do dia em Fortaleza, cinco mil fatos, avisos/paginação/validação, versões e isolamento. Suíte existente de concorrência e atomicidade preservada.

Revisão independente encontrou bloqueio de carregamento ao voltar de um detalhe pendente; teste reproduziu o problema e a separação dos estados da lista/detalhe o corrigiu. Suíte completa verde após a correção. Ensaio visual em MySQL descartável confirmou sete posições (seis criptoativos e BRL), perdas/ganhos, contribuições por ativo, avisos e abertura de detalhe. Em 1366×768, painéis lado a lado; em 390 px, indicadores em duas colunas e conteúdo empilhado, sem transbordamento horizontal da página. Rótulos do gráfico são HTML de tamanho estável, separados da geometria SVG; alternativa tabular preserva os valores completos. Não foi executado ensaio com leitor de tela.

Aplicação pessoal reiniciada com o pacote integrado em http://127.0.0.1:8080/. Contratos anteriores de operações, posições, carteiras e revisões comparados antes/depois para ambos os perfis e preservados; nova série, resultados por ativo, parcelas e avisos reconciliados por cálculo decimal independente. Nenhuma escrita de negócio no banco pessoal. Somente o ambiente descartável recebeu fatos sintéticos e foi encerrado depois da conferência. Evidências privadas em .local/evolucao-visao/, ignoradas pelo Git.

Documentação, roteiro, backlog e acordos atualizados. O novo aceite permite rolagem natural com os painéis adicionais e substitui a restrição anterior de caber integralmente em 768 px. Cotações manuais/históricas têm somente proposta documentada, sem integração ou preço artificial. Série mensal do custo, valor de mercado e rentabilidade seguem distintos e pendentes. Branch de trabalho codex/visao-geral; alterações desta evolução ainda sem commit/push.

As notas seguintes são históricas.

## Ajuda sobre custo de aquisição — 04/11/2026

Substituído o rótulo “Custo baixado” por “Custo de aquisição utilizado” no componente comum à prévia e aos detalhes. Ícone de informação explica o custo médio anterior e o custo de unidades adicionais eventualmente utilizadas para taxas. Ajuda reutilizável por cursor, foco ou clique; Escape dispensa somente a tooltip; botão não submete o formulário. Sem mudança de cálculos, API ou banco.

Frontend: 71 testes, lint, typecheck/build e pacote integrado aprovados. Testes cobrem rótulo, valor preservado, cursor, descrição acessível, teclado/Escape e clique sem submissão. [Contrato atualizado](16-negociacoes.md#clareza-do-custo-utilizado--04112026). Alterações permanecem na branch codex/visao-geral, sem nova publicação.

## Visão geral compacta e percentuais — 03/11/2026

Implementada a página inicial de consolidação por usuário, com período, indicadores, composição a custo, saldos/custódia e parcelas do resultado. Consulta única com snapshot REPEATABLE_READ; fatos anteriores ao período continuam formando o custo. Sem alterações de schema, migrations ou comandos de gravação. [Contrato](18-visao-geral.md).

Percentuais exibidos com duas casas HALF_EVEN por serviço reutilizável BigInt; API/barras preservam precisão e tooltip mostra o valor completo. Valores positivos abaixo de 0,01% não aparecem como zero. Sem cotação de mercado ou rentabilidade nesta fatia.

Verificações: 138 testes backend aprovados (57 unitários/arquiteturais e 81 integrados MySQL), incluindo snapshot durante escrita concorrente, versões e cinco mil fatos sintéticos. Frontend final: 68 testes, lint, typecheck/build; pacote integrado reconstruído. Protótipo: 1 teste aprovado. Revisão independente resolveu ausência de registros apresentada como custo zero e altura do controle de custódia. Corrigida a precedência do padding específico da página e reutilizados os botões da aplicação.

No navegador: seis ativos com duas custódias cada em MySQL descartável, viewport 1366×768 e documento de 768 px, sem rolagem vertical. A 390 px, indicadores em duas colunas e área principal empilhada, sem transbordamento horizontal da página. Aplicação pessoal reiniciada; consultas anteriores, fatos, posições e revisões dos dois perfis comparados e preservados. Somente GET no banco pessoal; evidências e capturas privadas em .local/visao-geral/.

Disponível em http://127.0.0.1:8080/, branch codex/visao-geral, sem commit/push/novo PR desta entrega. Próximas evoluções priorizadas por Jaime: [série mensal do custo, valor de mercado e rentabilidade](ideias-futuras/evolucao-e-valoracao/README.md), cada uma sujeita a planejamento de fonte/metodologia. Demais lacunas do histórico permanecem no backlog.

As notas seguintes são históricas.

## Catálogo de criptoativos concluído — 03/11/2026

Elegibilidade baseada na classificação CRIPTO do catálogo, sem listas fixas de tickers no backend/React. Abertura admite criptoativos e BRL/USD; cotação permanece BRL/USDT/USDC e taxa somente nas moedas do par. REST mantém campos; snapshot interno AtivoResumo acrescenta boolean cripto, sem enum de outro contexto. [Contrato e ativos](17-catalogo-de-ativos.md).

V11 provisiona SOL, LINK, NEAR, LDO, ARKM, RONIN, PYTH e TIA ausentes, preservando dados compatíveis e recusando colisões antes da inserção. Validação de tipo usa comparação exata, pois a collation pode aceitar grafia incompatível com o enum. V1–V10 imutáveis; novas migrations continuam necessárias para manutenção de catálogo, sem outra whitelist Java/React.

Validação: check e pacote integrado aprovados, 123 testes backend (55 unitários/arquiteturais e 68 integrados MySQL), 46 testes frontend, lint/typecheck/build e 1 teste do protótipo. Testes históricos V9→V10 fixados em V10; oito testes V10→V11 foram também executados isoladamente com alvo V11. Cenários: oito ativos com abertura/compra/venda/posição, cripto sintética adicional, taxas, inativação/idempotência, precisão, saldo, entradas inválidas, catálogo parcial e colisões. Revisão independente: asserção dependente de posição antiga no catálogo corrigida; nenhum outro achado material.

Aplicada V11 no MySQL local após backup privado em .local/ativos-catalogo/. Comparação byte a byte dos fatos/cadastros, metadados, catálogo anterior, histórico Flyway anterior e respostas de ambos os perfis comprovou preservação. Reinício validou onze migrations sem reaplicação. Catálogo local com quatorze ativos; formulário de negociações permite selecionar SOL e abertura mostra os novos ativos. Nenhum fato fictício foi gravado no banco pessoal.

Aplicação integrada disponível em http://127.0.0.1:8080/, branch codex/ativos-catalogo. Publicação em branch e PR para main solicitada por Jaime; sem merge automático. Consultar o PR para o resultado da CI remota. Dados/backup/evidências somente privados. Sem carga real de Eliane, manutenção visual de catálogo, novos pares ou outros tipos de movimento. Próximo passo: decidir a conferência do recorte com comprovantes e posição anterior; os demais complementos permanecem no backlog.

As notas seguintes são históricas.

## Diagnóstico documental do histórico concluído — 03/11/2026

Concluída análise dos arquivos fornecidos para Eliane, aprofundando os movimentos com data local em 2024 e usando 2025–2026 para identificar complementos. Arquivos, hashes, cópias preservadas, linhas com proveniência, comparações de sobreposição/multiplicidade, deltas observados por fonte e relatório ficam somente em `.local/eliane2024/`. Nenhum nome completo, identificador de origem, valor ou extrato foi incluído na documentação versionada.

Todas as linhas de movimento foram classificadas e preservadas. Contagem independente por células de data confirmou o inventário. A seleção do ano usa a data da linha, não o nome da pasta ou período nominal. Sobreposições, arquivo repetido, linhas fora de período e repetições internas foram registradas sem deduplicação financeira automática. Deltas por fonte não são saldos finais e fontes sobrepostas não são somadas como carteira.

Os [complementos de históricos](ideias-futuras/historico-completo-corretoras/README.md) registram oportunidades de ativos adicionais, Earn, distribuições, contas internas, pequenos saldos em BNB, cotações históricas, identificação de origem/destino, proveniência e eventos especiais. Reutilizados os registros existentes para BNB, execuções, transferências, retirada, conversões e importação; nenhuma prioridade ou implementação nova aprovada.

Menor próxima entrega recomendada, ainda não aprovada: ampliar controladamente os ativos necessários ao prefixo de depósitos/compras em BRL e obter os comprovantes de posição anterior e taxas. Esse prefixo é candidato de teste, não representação do ano inteiro. Não assumir taxa zero, posição anterior zero ou usar o tratamento de taxas de Jaime sem evidência específica.

Somente consultas GET foram feitas à aplicação. Histórico, carteiras, posições e revisão dos dois perfis foram comparados antes/depois e permaneceram iguais. Sem SQL, carga, limpeza, reinício, alteração de código, catálogos, APIs ou migrations. Não foi necessário repetir testes automatizados de produto; foram verificados parser, classificação, contagens, multiconjuntos, hashes, links e exclusões do Git. Sem commit, push ou novo PR nesta tarefa.

O histórico não está importado ou financeiramente reconciliado. Custos, pareamento definitivo, posição de encerramento, dados faltantes e novas regras continuam pendentes; as perguntas e exemplos concretos estão no relatório privado. As alterações documentais anteriores da carga de Jaime foram preservadas.

As notas seguintes são históricas.

## Carga real e conferência local concluídas — 03/11/2026

Executada a carga pontual autorizada do relatório Binance 2024 de Jaime: 20 eventos, sendo 9 aportes e 11 compras. Preparação decimal independente anterior à limpeza, posição anterior zerada confirmada e taxas das compras OCBS tratadas como incluídas no débito, conforme confirmação de Jaime. Nenhuma abertura, aporte compensatório ou lucro foi inventado para fechar o histórico.

Banco local suspenso durante backup e limpeza. Backup integral restaurado em schema separado e conferido por conteúdo e metadados antes de remover, em uma transação, operações, pontos de partida, contas e carteiras de ambos os perfis. Ativos, instituições, Flyway, constraints e sequências preservados. Criada carteira Binance para Jaime; Eliane permanece sem dados de negócio.

Carga pela API existente, com prévia, revisão e chaves estáveis. Comparação por evento de tipos, referências, quantidades, principal, taxas e UTC aprovada; posições/custos/custódia conferidos contra cálculo independente. Os 20 comandos foram repetidos antes e após reiniciar, retornando os mesmos IDs sem alteração da revisão ou duplicidade. Interface conferida com os 20 registros e posições persistidas.

CSV, hashes, backup, preparação, progresso, comparações e captura de tela ficam exclusivamente em `.local/binance2024/`, ignorado pelo Git. Arquivos temporários de credenciais removidos. Aplicação disponível em http://127.0.0.1:8080/. Sem alterações em código de produto, APIs, schema ou migrations; sem commit, push ou novo PR nesta execução.

O recorte comprova o mapeamento e o ciclo de depósitos/compras deste arquivo, não o confronto com um extrato de custódia de encerramento, vendas, BNB, transferências ou apuração fiscal. Próximo passo: avaliação assistida por Jaime e comparação com seu controle; demais participantes e sessões posteriores continuam pendentes. A autorização de limpeza foi específica desta execução e não deve ser repetida automaticamente.

As notas seguintes são históricas.

## Publicação da entrega de negociações — 03/11/2026

[PR #15 — aportes e negociações com livro financeiro](https://github.com/jaimeNafer/mvp-invest/pull/15) aberto de `codex/negociacoes-livro` para `main`, sem merge automático. Commit da implementação: `9506083`. Validações locais repetidas antes da publicação: 104 testes do backend, 44 do frontend, 1 do protótipo, lint/build e pacote integrado aprovados. CI remota deve ser consultada no PR; esta nota não declara seu resultado.

## Entrega de negociações concluída — 03/11/2026

Implementados aporte em BRL, compra e venda com BRL/USDT/USDC como cotação, taxas nas moedas do par, prévia/confirmação pelo motor Java, revisão/idempotência, histórico por versões, correção/cancelamento e posição atual global com custódia. Saldo inicial continua acessível e participa do mesmo recálculo ao ser alterado. [Contrato e regras](16-negociacoes.md).

Validação: backend check aprovado com **104 testes** (55 unitários/arquiteturais e 49 integrações MySQL), incluindo 5.000 eventos sintéticos e V9→V10 com 1.105 versões históricas. Frontend aprovado em lint, **44 testes**, typecheck/build; protótipo preservado com **1 teste** aprovado. Pacote integrado reconstruído com o bundle final.

V10 aplicada no MySQL local após backup privado. Comparação de snapshots das seis tabelas confirmou preservação integral dos campos e fatos anteriores; comentários dos campos conferidos, sem ausências. Reinicialização validou dez migrations sem reaplicação. Aplicação integrada disponível em http://127.0.0.1:8080/.

No navegador: acesso, listagem unificada, posição atual, abertura do formulário, campos específicos de aporte e prévia calculada sem gravação. Consultas novas e legadas retornaram 200 para os dois perfis. Nenhuma operação fictícia foi gravada no banco pessoal. Escritas/correções/cancelamentos e concorrência foram exercitados em MySQL descartável nos testes.

Revisões corrigidas: limite temporário da CTE na migração; autoria no DTO; proteção contra saída no primeiro POST pendente; rejeição de consulta com revisões divergentes; moeda negativa HALF_EVEN e duas casas com valor exato acessível. Nenhum achado material aberto dessas revisões.

Branch de publicação: codex/negociacoes-livro. Alterações do backlog anteriores incluídas na entrega; publicação do PR solicitada por Jaime, sem merge automático. BNB permanece confirmado para o futuro. Próximo passo de produto: validar um recorte real do ciclo e escolher os complementos do backlog com base nessa conferência.

As notas abaixo são históricas; o estado acima é o atual.


## Backlog complementar registrado — 03/11/2026

Atualização exclusivamente documental. O [índice de evolução](ideias-futuras/README.md) distingue complementos operacionais e ideias pós-MVP. [Taxas pagas em BNB](ideias-futuras/taxas-em-bnb/README.md) são um requisito futuro confirmado; [ordens com múltiplas execuções](ideias-futuras/ordens-com-multiplas-execucoes/README.md) permanecem a avaliar com extrato real. O simulador de risco continua como oportunidade após o MVP.

O [roteiro](03-entregas.md) mantém a sequência e os registros de transferências próprias/taxas de rede, retirada em BRL, conversões/outros pares, conferência, backup/recuperação e importação CSV adiada. Não foram criadas fichas duplicadas para esses itens. Ensaio de recuperação antes do piloto é recomendação, não prioridade aprovada.

Próxima entrega planejada preservada: aporte em BRL, compra e venda, com taxas somente nas moedas da negociação. Não implementar BNB agora nem adaptar sua taxa para outra moeda. Nenhuma alteração em código, banco ou migrations nesta atualização.

## Conferência frontend/backend para publicação — 03/11/2026

Contrato HTTP dos enums compatível com o frontend: mesmos campos e valores textuais. Ativo.tipo no TypeScript foi restringido a FIAT | CRIPTO; Saldo.situacao já usava CONFIRMADA | SUBSTITUIDA | CANCELADA. Filtro TODAS preservado, sem alterar payloads, URLs ou representação textual de IDs e decimais.

Frontend aprovado em lint, 20 testes e build/typecheck. Protótipo aprovado em 1 teste. Backend check aprovado com 74 testes (35 unitários/arquiteturais, 39 MySQL), pacote bootJar com frontend reconstruído. Aplicação integrada iniciada em 8080, página e consultas de carteiras, instituições, ativos, saldos e posições com HTTP 200 para os dois perfis. Filtro de histórico conferido no navegador, sem alterar cadastros ou operações locais.

Branch de publicação: codex/saldo-inicial-arquitetura. Escopo reúne saldo inicial e refatorações pendentes desde o merge do PR #13. Credenciais, backups, caches e builds permanecem ignorados. PR deve ficar aberto para revisão, sem merge automático.

# Retomada — enums de domínio, 03/11/2026

Implementados TipoAtivo, TipoOperacao e SituacaoOperacao nos respectivos contextos. Entidades e respostas tipadas; repositories/Criteria usam enums. String de filtro é convertida no mapper, preservando TODAS e mensagem de erro. Role, identificadores e catálogos abertos continuam strings. [Inventário e justificativas](15-enums-de-dominio.md).

## Verificação concluída — enums, 03/11/2026

Compilação e `gradlew check --offline --no-daemon --console=plain` aprovados: 74 testes, 35 unitários/arquiteturais e 39 integrados MySQL, sem falhas, erros ou ignorados. Revisão independente sem regressões identificadas. Cobertura adicional de enums inclui JSON, filtros e leitura histórica dos três estados; colunas continuam VARCHAR. Nenhuma migration, alteração em dados locais, commit, publicação ou reinício da aplicação.


## Contrato do handler confirmado — 03/11/2026

Jaime confirmou o comportamento vigente da classe: respostas 401 sem WWW-Authenticate e log descritivo único em ERROR com status HTTP e mensagem da origem. Os testes foram ajustados para verificar esse contrato; a implementação do handler não foi alterada.

`gradlew check --offline --no-daemon --console=plain` aprovado: 70 testes, sendo 32 unitários/arquiteturais e 38 integrados MySQL, sem falhas, erros ou testes ignorados. As duas divergências registradas na execução anterior estão resolvidas. Sem alteração em schema, migrations ou dados, sem publicação e sem reinício da aplicação.

As notas abaixo são históricas; o contrato e a validação acima prevalecem.

# Atualização — 03/11/2026: Clean Code e DTOs

Revisão de implementação e padronização Bean Validation aplicadas. Regras permanentes em AGENTS.md, arquitetura e acordos; relatório com classes, exemplos e decisões em [Clean Code](14-clean-code-backend.md).

38 integrações aprovadas; 30/32 testes unitários/arquiteturais aprovados. Essa execução anterior falhou porque os testes ainda exigiam WWW-Authenticate e outro formato de logging. Em 03/11/2026 Jaime confirmou o comportamento da classe: 401 sem esse header e log descritivo em ERROR. As expectativas foram atualizadas, preservando o handler. Não reintroduzir tratamentos técnicos genéricos. Nenhuma mudança em schema/migrations/dados, publicação ou reinício do serviço.

As contagens verdes nas notas históricas abaixo pertencem às execuções anteriores e não substituem este resultado.

# Retomada — backend organizado por contextos

## Handler simplificado — decisão vigente de Jaime

TratamentoGlobalException agora trata apenas negócio, validação de entrada/parâmetros e acesso necessários ao código atual. Todos os retornos são ResponseEntity<ProblemDetail>; responder recebe String mensagem e HttpStatus, sem headers ou Object. A mensagem vem da exceção/constraint de validação, é registrada uma vez e retornada sem substituição por texto genérico.

Removidos UNICIDADES, duplicidadeConhecida, classificação de integridade/concorrência/indisponibilidade no handler, catch genérico e herança de ResponseEntityExceptionHandler. Demais erros são tratados pelo Spring. WWW-Authenticate foi mantido somente no tratamento 401 do filter existente.

Para preservar os conflitos atuais, os adapters de gravação convertem apenas violações UNIQUE reconhecidas pelo Hibernate em exceções do contexto, com mensagens descritivas definidas no ponto de gravação; não há lista de constraints ou utilitário de classificação. Outras falhas propagam. CarteiraConflitanteException cobre o conflito de nome; saldos usam sua exceção existente. Transações e rollback foram preservados.

Validação: check aprovado; testes unitários finais e pacote integrado aprovados. Total **66 testes**, 28 unitários e 38 integrados, sem falhas/ignorados. Os testes do handler foram ajustados ao escopo explicitamente solicitado, incluindo mesma mensagem em log/response, retorno tipado e ausência de catch genérico. ArchUnit impede dependências de persistência no handler. Revisão independente sem achados materiais. Aplicação reiniciada em 8080; respostas reais 400/401 e mensagens de origem conferidas. Nenhum cadastro gravado nessa verificação; schema/migrations preservados. Sem commit/push/PR.

Este acordo substitui o tratamento técnico e fallback descritos nas evidências históricas abaixo.

## Auditoria modular — estado vigente

[Relatório completo, diagnóstico, estrutura e matriz](13-auditoria-modular.md). Jaime reconfirmou JPA nos modelos. Referências cruzadas de entidades/repositories foram substituídas por application.contract e IDs escalares, preservando schema/FKs. Metadados são obtidos em lote; carteira continua seleção de instituições e conta permanece vínculo interno.

Contratos públicos: IUsuarioAtual em acesso; IConsultaAtivos/AtivoResumo; IConsultaInstituicoes/InstituicaoResumo; IConsultaCarteiras para união imutável de IDs sob a identidade atual. Não consumir usecases REST, repositories ou entidades de outro contexto.

Exceções específicas voltaram a domain.exception. A baseline não compilava por falta de construtores de mensagem; essa regressão foi corrigida com StandardException. O handler global foi corrigido para preservar status/headers e não expor mensagens internas ou dados sensíveis. Regras de negócio e contratos REST existentes foram preservados; respostas de erro inseguras/incoerentes foram corrigidas.

ArchUnit core 1.5.1 em testImplementation, sem novo engine ou runtime. Sete regras verificam contratos, matriz, ciclos, camadas, whitelist do domínio (JPA somente nos modelos) e tratamento transversal. Spring Modulith foi avaliado e não adicionado, por falta de benefício necessário nesta entrega.

**Verificação:** compilação/check com 64 testes aprovados (26 unitários, 38 integrados), sem falhas/ignorados; pacote integrado aprovado; HTTP dos dois perfis conferido; dados das seis tabelas preservados integralmente por comparação de snapshots privados. Flyway validou nove migrations, sem reaplicar alterações. Aplicação integrada em 8080. Revisão independente sem achados materiais. Não houve commit/push/PR/CI nesta tarefa.

Próximo desenvolvimento deve seguir a matriz e passar ArquiteturaModularTests. Os registros abaixo são históricos e podem descrever padrões posteriormente substituídos; prevalece a auditoria modular e os acordos atuais.

## Annotations com nomes simples

Normalizadas 11 annotations em oito arquivos: Service, Transactional, Repository, Qualifier e PersistenceContext agora usam imports e nomes simples, preservando argumentos e comportamento. Nenhuma alteração de banco, contrato HTTP ou regras de negócio.

Adicionado ArquiteturaTests.annotationsUtilizamNomesSimplesComImports, que percorre as árvores sintáticas de todos os fontes Java em backend/src (produção e testes) e informa arquivo/linha de annotations qualificadas. O teste falhou com o código anterior; a suíte completa check passou após a correção: 54 testes (16 unitários e 38 integrados), sem falhas/ignorados. Os unitários foram reexecutados após atualizar a chamada do parser para a API vigente do JDK 27. Usa o parser público do JDK sem dependência adicional ou execução de processors. Padrão documentado em AGENTS e arquitetura.

## Lombok e interfaces com prefixo I

Removidos getters e construtores repetitivos do backend em favor de Lombok. Componentes usam @RequiredArgsConstructor com dependências final; entidades mantêm construtor JPA protegido e getters seletivos; utilitários usam construtor privado gerado. Exceções de simples repasse usam @StandardException. AcessoInvalidoException preserva o construtor com mensagem fixa por ser comportamento próprio.

Criação de modelos está em fábricas criar/vincular/iniciar/registrarSaldoInicial, preservando valores iniciais, autoria e UTC. Carteira.contasVinculadas() mantém snapshot imutável, sem expor coleção JPA mutável. DTOs records foram preservados sem Lombok redundante. Nenhum setter foi introduzido.

As 16 interfaces próprias foram renomeadas com prefixo I, incluindo contratos de domínio, JPA, casos de uso e acesso. Consumidores, testes e referências atuais da documentação foram atualizados. backend/lombok.config preserva @Qualifier no construtor gerado do filter; revisão independente confirmou o bytecode e o repasse das mensagens das exceções.

Validação concluída: backend `check` aprovado com **53 testes** (15 unitários e 38 integrados MySQL/Testcontainers), sem falhas/ignorados. Inclui novo teste arquitetural do prefixo I/ausência de setters e teste de snapshot imutável da coleção de vínculos. Pacote integrado e nova execução dos testes unitários aprovados após padronização final das anotações.

Aplicação reiniciada em 8080; página e consultas de carteiras, instituições, ativos, saldos e posições retornaram 200. Bearer ausente preserva 401 e WWW-Authenticate. Flyway validou nove migrations sem aplicar alterações. Sem gravação de dados durante essa conferência e sem mudanças em schema/migrations ou contratos HTTP; alterações locais sem commit/push/PR.

## Refatoração arquitetural — 02/11/2026

Backend organizado por contexto com domain/application/infrastructure/api. Modelos usam JPA por decisão explícita de Jaime; interfaces de domínio são implementadas por adapters Spring Data. Controllers dependem de interfaces de usecases, não conhecem entidades/repositories/mappers e retornam DTOs preparados na aplicação. Tipos de produção separados em arquivos próprios, sem aninhamento.

TratamentoGlobalException centraliza ProblemDetail e logging sanitizado, distinguindo duplicidades conhecidas de falhas internas. O filter encaminha erros de identificação ao resolver global e preserva erros downstream. Configuração versionada migrada para application.yml, mantendo o import privado mysql.properties e as variáveis existentes. Schema e migrations não foram alterados pela refatoração.

Regras permanentes, responsabilidades, exemplos e limites do isolamento estão em [Arquitetura](02-arquitetura.md), [AGENTS](../AGENTS.md) e [acordos de qualidade](08-acordos-de-qualidade.md). Nova funcionalidade deve seguir esse padrão e seus testes arquiteturais.

A revisão independente comparou com snapshot privado das fontes anteriores e não encontrou defeitos materiais.

Validação concluída:

- Backend `check`: **51 testes aprovados**, 13 unitários e 38 integrados MySQL/Testcontainers, sem falhas ou ignorados. Inclui arquitetura, status/headers MVC, integridade conhecida/desconhecida, logging sem mensagem sensível e nome com espaços nas extremidades no limite de tamanho.
- Frontend: lint, **20 testes**, typecheck e build aprovados. Pacote integrado `bootJar -PincludeFrontend` aprovado.
- Aplicação integrada reiniciada em 8080 com YAML e conexão privada existente. Flyway validou nove migrations e informou schema atualizado, sem migration necessária.
- Navegador: login, carteiras, saldo inicial e posição global consultados com sucesso. Nesta conferência não foram criados/editados/cancelados registros.
- Snapshots privados antes/depois das seis tabelas de negócio têm hashes idênticos; todos os dados preservados. Evidência visual e snapshots em `.local/`, fora do Git.
- Links locais e diff verificados. Nenhum commit, push, PR ou execução de CI nesta tarefa. Código permanece no checkout atual, junto à entrega anterior ainda não publicada.

## Histórico da entrega de saldo inicial

## Entrega anterior — saldo inicial em 02/11/2026

[Saldo inicial](12-saldo-inicial.md) implementado: BRL, USD, BTC, ETH, USDT e USDC; uma data comum por usuário; cadastro, consulta, revisão antes de confirmar, correção por substituição, cancelamento com motivo, histórico e posição inicial global. Custo histórico obrigatório; zero explícito exige confirmação e justificativa; BRL tem custo igual à quantidade. IDs/decimais trafegam como strings, sem ponto flutuante financeiro no React.

V9 acrescenta `pontos_partida` e `operacoes`. O usuário vem exclusivamente de `IUsuarioAtual.id()`; fatos pertencem ao usuário/instituição e sobrevivem às alterações nas carteiras. Filtros combinados usam a união das instituições, sem duplicar operações. Uma abertura confirmada por usuário/instituição/ativo e uma chave de requisição por usuário são protegidas no banco. Correção/cancelamento preservam fatos e autoria; bloqueio transacional do ponto de partida e READ_COMMITTED coordenam escritas. Instantes Java usam `Instant` e persistência UTC.

Frontend separa serviço HTTP de saldo inicial, decimais reutilizáveis, formulário/revisão, filtros, posição e coordenação da página. Formulários preservam conteúdo em caso de erro. Falha sem resultado definitivo mantém comando/chave em memória para consulta e repetição segura, inclusive ao fechar/reabrir a modal. Nenhum dado financeiro novo é salvo na localStorage.

### Verificações executadas

- Backend `check`: **44 testes** aprovados, 7 unitários e 37 integrados MySQL/Testcontainers. Novos: 2 de cálculo, 15 de API/transação/concorrência e 3 de migration. Testes históricos de V7/V8 preservados e versionados em seus alvos.
- Frontend: lint, **20 testes** e build aprovados. Novos: 5 de operações e 2 de decimais. Pacote integrado reconstruído com `bootJar -PincludeFrontend`.
- MySQL local: backup privado antes da V9; migration aplicada e reinicialização sem mudanças. Seis tabelas de negócio e **47/47 campos comentados**. Snapshot privado confirmou carteiras, vínculos e instituições anteriores integralmente preservados. Arquivos em `.local/` permanecem fora do Git.
- Navegador em 8080: criar abertura, consultar por duas carteiras sem duplicação, corrigir, cancelar e consultar as versões após reiniciar. As duas versões temporárias do ensaio foram removidas por conferência exata de identidade/valores/motivos. Novo saldo adicionado pelo usuário durante a validação foi preservado, assim como o ponto de partida necessário a ele.
- Teste SQL também verifica UTC do instante de abertura, impedindo dupla conversão de fuso. Foram exercitadas concorrência de primeiras datas, repetição da mesma chave e correção contra cancelamento; falhas não deixam fatos parciais.

Aviso conhecido: MySQL aponta depreciação de BINARY na guarda de compatibilidade do catálogo V9. A execução concluiu; não reescrever a migration aplicada por estilo/aviso. Migrations aplicadas permanecem imutáveis.

### Próximo passo e limites

Aplicação integrada disponível em `http://127.0.0.1:8080/`: entrar e acessar **Operações**. Atualizar a página exige novo login pelo comportamento já existente do mock. Catálogo e dados de negócio usam MySQL real; autenticação continua mockada por acordo.

Próxima tarefa: especificar compra/venda, custódia, taxas, prévia e arredondamentos de baixas/resíduos usando os mesmos fatos. Não ampliar a redefinição da data inicial para novos tipos sem revisar as regras. Compra/venda, importação, relatórios, motor completo e autenticação externa não foram implementados. Não houve commit, push, novo PR ou CI desta alteração; publicação depende de pedido posterior.

## Histórico anterior à implementação do saldo inicial

As evidências e pendências abaixo descrevem suas respectivas etapas anteriores, não o estado atual da fatia acima.

## Decisão vigente — esclarecimento final de 02/11/2026

Jaime confirmou que Binance em duas carteiras apresenta **os mesmos registros**, persistidos uma única vez. `contas` é a tabela de associação N:N entre carteira e instituição. Seus IDs distintos identificam vínculos, não contas com livros financeiros independentes.

Operações futuras terão identidade própria e referência ao usuário/instituição; não devem pertencer a `contas.id` ou à carteira. O usuário virá exclusivamente de `IUsuarioAtual.id()`. Mesma instituição para Eliane e Jaime mantém históricos separados. Carteiras selecionam instituições para apresentar os registros; selecionar várias carteiras deve usar a união de instituições do usuário e contar cada operação uma única vez.

Editar/remover vínculos ou arquivar carteira não apaga operações, custos ou resultados. Reassociar uma instituição mostra seu histórico existente, sem reimportar ou copiar fatos. Isso substitui a interpretação anterior de históricos independentes por carteira.

Esclarecimento registrado nos acordos, produto, arquitetura, contrato, modelo, dicionário e critérios futuros T29–T32. Nesta atualização foram alterados somente documentos; schema V8, código e cadastros foram preservados. Nenhuma nova execução de testes é declarada: resultados da correção Java 27 estão no histórico abaixo. Operações/motor continuam futuros.

O primeiro lançamento e seu detalhamento decimal estão agora no [contrato proposto](12-saldo-inicial.md). A API atual mantém o nome `contas` no DTO, representando associações; essa estrutura não será proprietária dos fatos do saldo inicial.

## Publicação da revisão — 02/11/2026

Jaime solicitou enviar as alterações ao Git. O PR #12 já foi integrado em `main`; esta revisão foi publicada na branch `codex/java27-vinculos-carteiras`, com [PR #13](https://github.com/jaimeNafer/mvp-invest/pull/13) para revisão, sem merge automático. Reúne a correção Java 27/V8 e o esclarecimento de que carteiras apresentam os mesmos registros do usuário/instituição.

Evidências locais preservadas: `check` com 24 testes backend (5 unitários e 19 MySQL/Testcontainers), 13 testes frontend, lint, build, pacote integrado e HTTP real aprovados na correção anterior. O esclarecimento posterior alterou documentos; não implementou o motor. A CI do PR #12 refere-se à entrega anterior; os checks da nova revisão devem ser conferidos no próprio PR. Credenciais, backups e dados locais permanecem excluídos do Git.

## Histórico — recuperação Java 27 e interpretação anterior

As descrições anteriores de históricos independentes por carteira foram substituídas pela decisão acima. As evidências da execução e recuperação permanecem válidas; IDs diferentes verificados eram IDs de vínculos.

# Retomada — Java 27 e contas exclusivas por carteira

## Estado vigente — revisão posterior de 02/11/2026

Jaime alterou o backend para Java 27/Spring Boot 4.2.0-SNAPSHOT e confirmou mudar a modelagem: **cada conta pertence a uma única carteira**. Autorizou zerar carteiras, contas e vínculos locais após backup para corrigir/reaplicar V8; instituições e ativos devem ser preservados. Esta decisão substitui o compartilhamento de contas da entrega anterior.

Schema: `carteiras`, `contas`, `instituicoes`, `ativos`. Conta tem `carteira_id`, sem `perfil_ref`; unicidade `(carteira_id,instituicao_id)`. Sem tabela `carteira_contas`. A carteira guarda o usuário obtido de `IUsuarioAtual.id()`; todas as consultas/alterações continuam separadas pelo contexto do Bearer.

JPA um-para-muitos com persistência/merge e remoção de órfãs. Edição preserva contas das instituições mantidas, cria as novas e exclui as retiradas apenas daquela carteira. Arquivar preserva contas. Não há operações nesta etapa; antes do livro, definir proteção contra exclusão de contas com histórico. Conta não é compartilhada ou reaproveitada de outra carteira.

## Correções e recuperação

- V8 referenciava `perfil_ref` no índice após remover a coluna. Unicidade corrigida para carteira/instituição; JPA, service, testes e dicionário alinhados. Repository independente de contas removido.
- O script ainda apontava para o JAR antigo após `rootProject.name` mudar para `lastro`. Agora inicia `backend/build/libs/lastro-0.0.1-SNAPSHOT.jar`. CI usa Java 27; README exige `JAVA_HOME` no JDK 27. Projeto utilizado é `backend/`; a cópia não versionada em `lastro/` não foi alterada.
- O histórico Flyway havia sido reduzido a V7, mas o schema já continha carteiras e contas. Alterar somente o histórico não refaz o banco. A tentativa inicial de startup foi bloqueada pela revisão automática por risco aos dados; nenhuma migration foi executada nessa tentativa.
- Após autorização explícita de limpeza, aplicação antiga parada, backup privado renovado em `.local/banco-antes-reset-v8-java27-20261102-024407.sql`. Removidas somente carteiras/contas/vínculos autorizados. Definições V7 originais de titulares/contas restauradas vazias a partir do backup anterior, preparando o schema para reaplicação. Catálogos comparados antes/depois e preservados; não alteramos o histórico Flyway.
- V8 corrigida aplicada pelo Flyway na inicialização Java 27.0.1, com validação das oito migrations. Catálogos existentes preservados e seeds inseridos somente quando ausentes. Quatro tabelas e 21/21 campos comentados; FKs e unicidade por carteira conferidas.

Esta reaplicação é uma exceção local autorizada, não um procedimento automático. V1–V7 permaneceram imutáveis. Em outros bancos já migrados, não apagar registros do Flyway ou executar limpeza; planejar nova migration e preservação dos dados.

## Evidências verificadas

- Java Corretto 27.0.1, Spring Boot 4.2.0-SNAPSHOT e Gradle Wrapper 9.8.2: pacote integrado aprovado e aplicação iniciada em 8080.
- `check` aprovado: **24 testes backend**, 5 unitários e 19 integrados MySQL/Testcontainers (11 carteiras, 3 V8, 4 fundação e 1 histórico até V7). Sem falhas/ignorados. Cobre propriedade exclusiva, IDs mantidos na edição, exclusão somente da conta retirada, isolamento, arquivamento, rollback e concorrência de nome.
- Frontend: lint, **13 testes**, TypeScript/build aprovados; textos e fixture ajustados ao novo modelo. Pacote reconstruído com os assets finais.
- HTTP real: frontend atualizado 200, Bearer ausente 401, duas carteiras na mesma instituição com contas distintas, edição mantendo ID existente e arquivamento preservando contas. Dados temporários de verificação removidos por IDs exatos de perfil exclusivo; nenhum cadastro de usuário foi criado no backend.
- Após conferência: carteiras e contas vazias, duas instituições preservadas. Aplicação disponível em [http://127.0.0.1:8080/](http://127.0.0.1:8080/).

## Continuidade

Correções atuais locais, sem novo commit/push. O [PR #12](https://github.com/jaimeNafer/mvp-invest/pull/12) contém a entrega anterior; sua CI não comprova esta revisão. Modelo/contrato/dicionário/acordos foram atualizados para a decisão vigente.

Executar `scripts/Start-Lastro.ps1` com `JAVA_HOME` no JDK 27. Configuração MySQL permanece privada em `.local/mysql.properties`. A versão SNAPSHOT foi mantida conforme alteração de Jaime; requisitos oficiais: [Spring Boot 4.2](https://docs.spring.io/spring-boot/4.2-SNAPSHOT/system-requirements.html) e [Gradle](https://docs.gradle.org/current/userguide/compatibility.html).

Próximo trabalho: definir primeiro lançamento, ativos e política decimal, incluindo proteção de contas com histórico. Login/JWT mockados permanecem no frontend; autenticação real externa, importação e motor não entram nesta correção.

## Histórico — entrega anterior de carteiras compartilhadas

Os registros seguintes preservam a cronologia e são substituídos pelo estado vigente acima.

## Estado vigente — 02/11/2026

**Etapa concluída e encerrada por Jaime em 02/11/2026.** Código publicado na branch `codex/carteiras-investimento`, com [PR #12](https://github.com/jaimeNafer/mvp-invest/pull/12) para `main`, aberto para revisão e sem merge automático. O próximo trabalho é especificar o primeiro lançamento; não reabrir este cadastro sem novo requisito.

Cadastro de titulares substituído por **Carteiras** no menu, página, modais, serviços e API. Cada carteira tem nome e instituições selecionadas. Uma conta é única por usuário/instituição e pode compor várias carteiras por `carteira_contas`, sem cópia de dados. O usuário vem exclusivamente de `IUsuarioAtual.id()`, carregado pelo Bearer. Login/JWT/armazenamento continuam nos serviços existentes, sem gestão de usuários no backend.

Criar/editar, criar contas ausentes e atualizar vínculos ocorre em uma transação. Seleção vazia/repetida/inválida é recusada; novo vínculo exige instituição ativa, enquanto vínculo existente inativo pode ser mantido/removido. Nome normalizado até 160 caracteres e único por usuário, inclusive arquivadas. Erros 400/401/404/409; concorrência de criação de conta reverte a requisição conflitante e permite repetir. JPA com `@EntityGraph`, sem exclusão em cascata.

Ativas e arquivadas permanecem juntas; arquivada permite somente reativar. Modal preserva dados após erro, oferece recuperação de falha do catálogo e bloqueia controles durante gravação. Remover instituição remove somente o vínculo; arquivar preserva contas e associações. Catálogo inicial: Binance e BitGet, ativas, país não informado. Sem cadastro independente de contas ou manutenção de instituições pelo usuário.

Contratos atuais: GET `/api/v1/instituicoes`; GET/POST `/api/v1/carteiras`; PUT `/api/v1/carteiras/{id}`; PATCH `/api/v1/carteiras/{id}/situacao`. Rotas de titulares removidas. IDs de resposta são strings. [Contrato completo](09-cadastro-de-carteiras.md), [modelagem](10-modelagem-de-carteiras.md) e [dicionário](11-dicionario-de-dados.md).

## Verificações desta entrega

- Frontend: lint, 13 testes React e build/TypeScript aprovados. Cobertura de formulário/catálogo, vazio/erro/nova tentativa, CRUD, arquivadas, contexto, foco, bloqueio durante gravação e preservação dos dados após falha. Ajuste final de texto/estilo conferido por lint e novo build.
- Backend: `check` aprovado, 23 testes (5 unitários e 18 integrados MySQL/Testcontainers), sem falhas ou ignorados. São 11 de carteiras, 2 de V8, 4 da fundação e 1 histórico de titulares até V7, além dos unitários do contexto.
- Conferidos compartilhamento do ID de conta em várias carteiras, conta distinta de outro usuário, isolamento/404, remoção de vínculos, arquivamento, instituições inativas, conflitos/rollback e requisições simultâneas. Duas criações concorrentes da mesma conta retornam 201/409; nova tentativa funciona sem duplicação.
- Migrations históricas até V7 preservadas nos testes. V7 → V8 cobre ativos preservados, cadastros vazios, seed, constraints, FKs, comentários, reaplicação sem mudanças e bloqueio antes de alterar tabelas quando existem cadastros antigos.
- Pacote integrado `bootJar -PincludeFrontend` aprovado. V8 aplicada no MySQL local após nova conferência: titulares, contas e instituições com zero registros, aplicação parada e backup privado em `.local/banco-antes-carteiras-v8-20261102-012734.sql`. Flyway validou as oito migrations e atualizou 7 → 8.
- Banco local: cinco tabelas de negócio, 23/23 campos comentados; catálogo Binance/BitGet e ausência de titulares. Conta Binance reutilizada em duas carteiras criadas pelo navegador. Remoção do vínculo BitGet manteve sua conta no banco com zero associações.
- Navegador: criação com duas instituições, segunda carteira com Binance, arquivamento permanecendo na lista, edição desativada/reativação, edição dos vínculos e troca para Eliane com lista separada. Capturas privadas em `.local/carteiras-v8-*.png`.
- HTTP local confirmou frontend atualizado 200, catálogo inicial, carteiras sem IDs em comum entre usuários, ausência de Bearer 401 e rota antiga de titulares 404. A carteira temporária adicional foi removida pelo ID/nome exatos; cadastros criados ou renomeados pelo usuário durante a conferência e todas as contas foram preservados.

## Ambiente e próximos passos

Aplicação integrada em [http://127.0.0.1:8080/](http://127.0.0.1:8080/), iniciada por `scripts/Start-Lastro.ps1`; `-Build` reconstrói. Recarregar abas antigas. MySQL persistente `lastro_local`, configuração privada `.local/mysql.properties`; Docker somente para testes isolados. Não versionar ou imprimir credenciais/backups.

Operações futuras pertencem à conta. Carteiras representam seleções atuais de contas completas, sem divisão por operações, quantidades ou períodos. Consultas de várias carteiras deduplicam contas por ID. Próxima decisão: especificar o primeiro lançamento e detalhar política decimal/ativos antes do livro. Importação, motor, relatórios, backoffice, integração com corretoras e autenticação real permanecem fora desta entrega.

Publicação desta entrega no [PR #12](https://github.com/jaimeNafer/mvp-invest/pull/12); resultados remotos devem ser conferidos nos checks do PR, pois acompanham o último commit publicado. A CI do PR #11 abaixo refere-se ao login. O histórico a seguir registra decisões e verificações anteriores, substituídas pelo estado vigente acima.

## Histórico — cadastro anterior e documentação V5–V7

## Estado vigente

Em 01/11/2026, Jaime aprovou simplificar a modelagem antes do cadastro de contas e confirmou manter os titulares separados por perfil. O titular é a referência dos dados financeiros; contas pertencem ao titular e podem referenciar uma instituição. Espaços deixaram de fazer parte do schema. [Modelo, diagrama e exemplos](10-modelagem-de-carteiras.md).

V1–V4 permanecem imutáveis. V5 migrou a associação antiga para `titulares.perfil_ref`, removeu `espacos`, `espaco_id` e `revisao_livro`, e recriou a FK de contas diretamente para o titular. Preserva IDs, nomes, situações, datas e vínculos. Não criou usuários, credenciais, grupos ou campos de concorrência antecipados.

Novo requisito posterior de Jaime: identificar a requisição pelo Bearer e disponibilizar contexto injetável nas services. `UsuarioAtualFilter` lê o JWT por `LeitorJwtMock` e carrega `IUsuarioAtual` com `id()`, `nome()` e `role()`, em escopo de requisição. `TitularService` obtém a referência diretamente desse contexto; controllers/componentes/serviços frontend não passam ID de perfil. `X-Perfil-Id` foi substituído e não é fallback.

API vigente: `/api/v1/titulares`, com `Authorization: Bearer <token>` obrigatório. `sub` determina o perfil dos registros; `name` e `role` também ficam disponíveis no contexto. Ausência/estrutura inválida retorna 401; entrada de negócio inválida continua 400 e titular inexistente/de outro perfil retorna 404. O mock aceita somente `alg: none`, assinatura vazia e claims textuais válidas, sem comprovar autenticidade ou permissão. Não há gestão de usuários, credenciais ou sessões. [Contrato do contexto](07-acesso-local.md).

A tela mantém ativos e arquivados na mesma lista, modal para adicionar/editar e ícones no padrão visual. Arquivado permanece desativado com somente reativar habilitado. Textos que sugeriam um espaço foram ajustados. Login, JWT e armazenamento continuam separados e mockados no frontend.

## Evidências da revisão

- Lint, 12 testes React, TypeScript/build e 19 testes backend passaram: 5 unitários e 14 MySQL/Testcontainers (4 da fundação, 9 do cadastro/contexto e 1 de atualização V4 → V5). Sem falhas ou testes ignorados. Conferidos ID/nome Unicode/role, tokens malformados, ausência de Bearer, header/corpo sem substituir `sub` e 20 requisições simultâneas de dois usuários.
- O teste de atualização preserva titulares/contas, inclusive arquivado e contas de mesmo nome de titulares diferentes; verifica referências dos perfis, FK, unicidade por titular, ausência das estruturas removidas e reaplicação sem mudanças.
- Pacote integrado atualizado e V5 aplicada no MySQL persistente. Backup privado gerado antes da alteração e renovado com a aplicação parada. Comparação antes/depois confirmou os seis titulares com IDs, referências, nomes, situações e datas preservados; não havia contas no banco de uso.
- Na conferência anterior da V5, perfil 1 manteve dois titulares e perfil 2 manteve quatro, sem IDs em comum. Essa etapa usava o header de perfil, agora substituído pelo Bearer. Não há nova migration ou alteração de dados nesta mudança de contexto.
- Pacote integrado reconstruído após a mudança de contexto, verificações finais sem aviso de API obsoleta e aplicação reiniciada em 8080. HTTP real confirmou o bundle atualizado, dois titulares para o Bearer de Jaime e quatro para o de Eliane, listas sem IDs em comum e `X-Perfil-Id` conflitante sem alterar o usuário. Bearer ausente/malformado e header antigo sozinho retornaram ProblemDetail 401 com `WWW-Authenticate: Bearer`. Nenhum cadastro foi alterado nessa conferência.
- Navegador: login de Jaime e Eliane e troca de perfil conferidos com o novo bundle/API; cada um vê seus titulares preservados, incluindo arquivados. Modal de criação abre/cancela sem gravar. Captura privada em `.local/lastro-titulares-modelagem-v5.png`. Nenhum cadastro real foi alterado na conferência.

## Ambiente e continuidade

Schema vigente em V7: as quatro tabelas de negócio e seus 24 campos têm comentários breves, limitados à finalidade e às opções dos campos de valores restritos. A V7 corrige o detalhamento excessivo da V6; nenhuma migration anterior foi reescrita. Fonte campo a campo: [dicionário de dados vigente](11-dicionario-de-dados.md); o modelo v1 continua histórico.

Verificação desta revisão: `check` e pacote integrado passaram, com os mesmos 19 testes backend (5 unitários e 14 integrados). O teste de atualização cobre V4 → V5 → V6 → V7 e compara dados e metadados ao revisar comentários. Frontend não foi alterado nem teve suas verificações repetidas.

V6 aplicada ao MySQL local após backup privado com a aplicação parada, em `.local/banco-antes-comentarios-v6-*.sql`. Comparação antes/depois confirmou preservação dos dados, tipos, defaults, nulabilidade, collations, índices, FKs, CHECKs e contadores de IDs. Cobertura conferida: titulares 5/5 campos, instituições 5/5, ativos 7/7 e contas 7/7, além dos quatro comentários de tabela. A tabela técnica do Flyway não foi modificada. Aplicação reiniciada e respondendo em 8080.

V7 aplicada após novo backup privado em `.local/banco-antes-comentarios-v7-*.sql`. Confirmados os 24 comentários breves, sem detalhes de mock, classes ou transporte HTTP, com as opções de tipos listadas. Comparação confirmou dados e definições preservados; aplicação respondendo em 8080. Padrão registrado em AGENTS e no acordo Q15.

Aplicação integrada disponível em 8080 por `scripts/Start-Lastro.ps1`; `-Build` reconstrói. Frontend de desenvolvimento usa 5173. Recarregar abas antigas para obter o novo bundle/API. MySQL local `lastro_local` em 127.0.0.1:3306, usuário técnico `lastro_app`, configuração privada em `.local/mysql.properties`. Docker é necessário para os testes isolados.

Backups e referências de comparação ficam somente em `.local/`, inclusive `banco-antes-modelagem-titulares-v5.sql` e uma cópia com data/hora imediatamente anterior à aplicação. Não ler/imprimir seu conteúdo em inspeções gerais ou versionar. Eles não são utilizados pela aplicação.

Próxima fatia: cadastro de contas por titular, reutilizando HTTP, token, armazenamento e componentes existentes. Motor, revisão do histórico por titular e consolidação ainda precisam ser implementados. Grupos salvos ficam adiados; quando necessários, vinculam titulares existentes, sem copiar histórico ou alterar cálculos individuais. [Contrato](09-cadastro-de-carteiras.md).

Alterações locais na branch `codex/planejamento-cadastros`, ainda sem commit/publicação. A CI do PR #11 abaixo refere-se à primeira feature de login, não à revisão de modelagem. Os registros seguintes preservam a cronologia; decisões antigas sobre espaços, associação no frontend e próximos passos foram substituídas pelo estado vigente acima.

## Registro da etapa anterior

Encerrada por Jaime em 01/11/2026. Concluída a tela de login com autenticação mockada no frontend, construção do JWT e armazenamento local. O marco de cadastros e as funcionalidades financeiras continuam pendentes.

## Estado entregue

- Usuários mockados `jaime` e `eliane`, ambos com senha vazia. Login inválido apresenta erro; login e saída não fazem chamadas HTTP.
- JWT com `sub`, `name`, role `acesso_total` e `iat`; header `alg: none`, assinatura vazia. Não contém senha.
- Token salvo em localStorage sob `lastro.token`; sair remove o token e limpa o perfil. Recarregar preserva o token, mas retorna ao formulário sem restauração automática.
- Autenticação real será feita por backend externo específico. O backend de negócio não possui usuários, sessões ou endpoints de acesso.
- O token está disponível para a futura integração HTTP. Ainda não há envio em chamadas de negócio nem validação de permissões implementados.

## Responsabilidades consolidadas

| Arquivo | Responsabilidade |
| --- | --- |
| `frontend/src/Login.tsx` e `App.tsx` | Formulário e estados da interface. |
| `frontend/src/services/autenticacao.ts` | Credenciais do mock e coordenação de geração/armazenamento do token e saída. |
| `frontend/src/services/jwt.ts` | Header, emissão, Base64URL e montagem do JWT mockado. |
| `frontend/src/services/armazenamentoLocal.ts` | Único acesso à localStorage: salvar, obter e remover. |

Regras obrigatórias nas próximas tarefas: [AGENTS.md](../AGENTS.md). Aprendizados e critérios de revisão: [acordos de qualidade](08-acordos-de-qualidade.md). Não esperar novos lembretes de Jaime para aplicar essas regras, inclusive em mocks.

## Evidências já verificadas

- Última alteração de código: lint, 6 testes React e build/TypeScript passaram. Cobrem os dois usuários, erro de credenciais, conteúdo do JWT, armazenamento, saída e ausência de chamadas HTTP.
- Última alteração do backend/schema: 4 testes integrados MySQL/Testcontainers passaram, sem falhas ou testes ignorados. Não foram repetidos nas refatorações exclusivas do frontend.
- Navegador: login, troca de usuário e recarga conferidos com o backend desligado antes das extrações de serviços; os testes automatizados passaram após as extrações.
- V4 aplicada no MySQL persistente após backup privado: controle de usuários removido e cinco tabelas de negócio preservadas. IDs, nomes, revisão e criação dos espaços conferidos. Endpoints antigos de acesso retornam 404.
- Pacote reconstruído e aplicação iniciada em 8080 após a extração do serviço JWT. Vite de desenvolvimento usa 5173.
- Primeira feature publicada na branch codex/login-jwt-mock, com [PR #11](https://github.com/jaimeNafer/mvp-invest/pull/11) integrado em main por Jaime. CI de push e PR aprovada; não foi usado merge automático.

Esta atualização de encerramento altera somente documentação; não representa uma nova execução das suítes acima.

## Ambiente e cuidados de retomada

Executar `scripts/Start-Lastro.ps1` e abrir [Lastro](http://127.0.0.1:8080/); `-Build` reconstrói o pacote. `npm run dev` em `frontend` permite usar o login sem backend, em 5173.

MySQL persistente `lastro_local` em 127.0.0.1:3306, usuário técnico `lastro_app`, configuração privada em `.local/mysql.properties`. Aplicação não usa root. Backups antigos em `.local/` não são utilizados pelo aplicativo: não ler/imprimir em inspeções gerais ou versionar.

V1–V3 são histórico imutável; V4 remove `usuarios`, `membros_espaco` e `espacos.criado_por`. Preservar as estruturas de negócio. Docker é necessário somente para os testes isolados.

Java Corretto 21.0.7, Gradle Wrapper 9.8.2/Groovy, Boot 4.1.2, Node local 22.15.1/npm 10.9.2 (recomendado 22.23.4), React 19.3.1, TypeScript 6.0.3 e Vite 8.3.5. Não alterar instalações globais.

## Próximo passo

Discutir com Jaime os limites da arquitetura de negócio antes de implementar cadastros ou novas camadas: responsabilidades, consultas, critérios para extrair classes e tamanho das entregas. Não reabrir a construção do login sem novo requisito.

Cadastros de titulares/contas, integração das chamadas com o token, contrato de identidade externa, motor financeiro, conferência, relatórios e recuperação permanecem pendentes. [Contrato e operação do acesso](07-acesso-local.md).

## Preparação da primeira publicação

Branch `codex/login-jwt-mock`, destinada a PR para `main`. Código, testes, documentação, sete imagens do protótipo, modelo MySQL descompactado, lockfile e Gradle Wrapper fazem parte da entrega. ZIP duplicado preservado localmente e ignorado, junto com segredos, backups, dependências, builds e metadados.

Verificação anterior ao envio: testes do protótipo, lint, 6 testes React, build/TypeScript e `gradlew check --rerun-tasks` com 4 testes integrados MySQL passaram. A checagem das exclusões confirmou a proteção dos caminhos privados e a inclusão dos arquivos necessários. A [execução remota](https://github.com/jaimeNafer/mvp-invest/actions/runs/41355920250) passou no PR #11, assim como a execução disparada pelo push.

## Publicação verificada

Commit da feature: `9c9c495841a6f88e5d44b64d9999b663c9a94869`. [PR #11](https://github.com/jaimeNafer/mvp-invest/pull/11) integrado por Jaime em `main`, sem merge automático. O merge é `8e50dd0c782dd9f787c10f617bbef09e272f9f94`.

A [CI do PR](https://github.com/jaimeNafer/mvp-invest/actions/runs/41355920250) e a [CI do push](https://github.com/jaimeNafer/mvp-invest/actions/runs/41355918541) passaram: testes do protótipo, instalação e verificações do frontend, build do backend, migrations, testes MySQL/Testcontainers e empacotamento integrado. Não houve necessidade de corrigir o comportamento da aplicação para publicar.

O Git do Windows recusou a credencial salva para push. A publicação foi realizada pela conexão autenticada do GitHub, com hash da árvore conferido contra o commit local. A branch local foi sincronizada ao conteúdo publicado e possui upstream; a referência do commit local original foi preservada. A credencial do terminal não foi alterada: futuros pushes por CLI exigem reautenticação ou uso da conexão do GitHub.

## Início da próxima etapa

Jaime confirmou o merge do PR #11 e pediu o próximo passo. A main local foi atualizada para o merge. A atualização documental posterior ao merge foi preservada por cherry-pick na branch local `codex/planejamento-cadastros`, sem perder arquivos ou publicar código novo de negócio.

Escolhas confirmadas: Spring Data JPA para os primeiros cadastros e espaços separados para Jaime e Eliane, com associação explícita no frontend entre perfil mockado e espaço de negócio. A organização de controller, service, repository e entity foi apresentada como proposta para titulares. Espaço agrupa titulares/contas, sem representar autenticação ou gestão de usuários. Os contratos da API continuam a definir. Consultar [proposta do cadastro de titulares](09-cadastro-de-carteiras.md).


## Separação do domínio fiscal — 07/11/2026

A diretriz arquitetural foi registrada em [Arquitetura](02-arquitetura.md#separação-do-domínio-fiscal--07112026). A resposta de conferência deixa de fornecer `estadoFiscal`; finalidade da saída, dados originais e pendências patrimoniais continuam separados. A evolução seguinte adiciona o reconhecimento dos três eventos especificados; o custo desconhecido segue separado da coerência técnica. Consulte o contrato em [Importação Binance](19-importacao-binance.md#créditos-automáticos-de-recompensas).


## Créditos automáticos — 07/11/2026

O reconhecimento cobre as três descrições documentadas, mantém o custo desconhecido e usa a confirmação global. A migration V15 acrescenta os ativos dos exemplos ao catálogo. Verificados 187 testes unitários de backend, seis testes de integração delimitados (créditos e migrations), build, lint e 121 testes de frontend. A execução integral de integração foi interrompida após registrar falhas em associação e conciliação; duas foram reproduzidas isoladamente e cobram expectativas incompatíveis com os acordos atuais de associação automática e coerência de saída cripto. Há outras falhas não investigadas. Earn completo e disponibilidade de negociação permanecem fora do escopo.

# Associação preservada após edição — 07/11/2026

Edições que alteram somente o motivo preservam a proposta de associação; a comparação usa a versão histórica e a proposta atual. A timeline mantém visível uma proposta desatualizada quando a alteração exige revisão, sem aplicá-la ao livro. A confirmação global atualiza o snapshot selecionado e preserva o histórico. Contrato em [associação de registros](22-associacao-registros-binance.md).

Passaram 197 testes unitários e arquiteturais e os dois cenários integrados focados, incluindo inclusão e atualização de saldo anterior, confirmação, custo, quantidade e reimportação. Na execução da classe de associação, 37 dos 40 testes passaram; falharam `associacaoDeTaxaAnteriorEncerraAjusteSemRepetirQuantidades`, `editarLinhaNaoMudaGrupoAplicadoAntesDeReavaliar` e `manualAteDoisSegundosConfirmaTaxaZeroSemDuplicarEDesfaz`, em expectativas relacionadas ao agrupamento automático. A suíte integral de integração não foi executada nessa verificação. Frontend compilado e pacote integrado gerado. Aplicação reiniciada em 8080 com MySQL persistente; página e timeline responderam HTTP 200 e o vínculo relatado voltou a aparecer. Nenhuma migration nem escrita de negócio foi realizada na verificação local.

## Visão geral: próximo trabalho proposto

Créditos automáticos sem custo e conversões sem valoração preservam quantidades, mas impedem calcular os custos dependentes. A classificação de coerência é independente da disponibilidade financeira. A visão geral também usa uma condição ampla de ausência de classificação para resultado e taxas; revisar a disponibilidade de cada indicador, as parcelas conhecidas e a explicação das causas em entrega futura. Nenhuma mudança de cálculo foi implementada nessa investigação.
