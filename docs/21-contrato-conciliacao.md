# Contrato de implementação — conciliação progressiva

A evolução de associação de negociações multilinhas está documentada em [associação de registros](22-associacao-registros-binance.md). A limitação histórica abaixo sobre promoção de uma linha isolada permanece: somente o grupo com contrapartidas explicitamente vinculadas pode substituir suas linhas por uma negociação.

Especificação do primeiro ciclo completo de [conciliação progressiva](20-conciliacao-progressiva.md). Branch codex/conciliacao-progressiva. Documento escrito antes dos endpoints; evidências de implementação serão registradas na retomada.

## Estados e fronteiras

Estado automático: COERENTE, PENDENTE, CORRECAO_NECESSARIA. Origem: IMPORTADA, MANUAL, SINTETICA. Revisão manual tem versão, autor e instante separados. Quantidade, custo e resultado têm disponibilidades independentes; nulo não é zero. Ajuste não recebe data econômica inventada: ancoraId determina posição técnica.

Importacoes guarda arquivos e registros de origem; operacoes recebe snapshots escalares e possui custódias, registros normalizados, ajustes e cálculos. Operacoes não importa implementações de importacoes. Contas de custódia não são contas de carteiras. Legados pertencem à custódia Não especificada, sem inferir Spot.

## HTTP

IDs e decimais são strings. Instantes são ISO com offset/UTC. Somente Bearer/IUsuarioAtual determina proprietário. Leituras consistentes e confirmações READ_COMMITTED sob lock do ponto de partida. Escritas usam Idempotency-Key UUID; mesmo comando retorna resultado anterior antes de validar revisão, payload divergente conflita.

| Método/rota | Entrada | Saída |
| --- | --- | --- |
| POST /api/v1/importacoes/recebimentos | Multipart arquivos repetidos, até20arquivos/10MiBtotal/10000registros | 201: arquivos com id/nomeArquivo/hash/registros/reconhecido/erro; recebidos,reconhecidos,problemas,revisao. |
| GET /api/v1/conciliacao/timeline | pagina=0,tamanho=50(max100),estado,ativoId,contaId | itens,total,pagina,tamanho,revisao,resumo,proximaPendenciaId,proximaPendenciaPagina. |
| GET /api/v1/conciliacao/contas | sem proprietário no cliente | id,nome,instituicaoId,instituicaoNome. |
| PUT /api/v1/conciliacao/registros/{id} | revisao,versao,data,contaId,ativoId,quantidade,custoTotalBrl,natureza,motivo; campos opcionais de negociação | RegistroTimeline atualizado; não força estado verde. |
| POST /api/v1/conciliacao/registros/{id}/revisao | revisao,versao | RegistroTimeline com anotação de revisão, sem resolver lacuna. |
| POST /api/v1/conciliacao/saldos-anteriores | revisao,ancoraId,contaId,ativoId,data,quantidade,custoTotalBrl(nullable),motivo | revisao. Saldo declarado manualmente, não ajuste automático. |
| POST /api/v1/conciliacao/previa | revisao | revisao,previaId,registrosParaAplicar,ajustes,correspondencias,resumo. |
| POST /api/v1/conciliacao/confirmacoes | revisao,previaId,aceitarLacunaIds,aceitarCorrespondenciaIds | revisao. |

RegistroTimeline: id,data(nullable),criadoEm,tipo,descricao,instituicaoId/instituicaoNome,contaId/contaNome,ativoId(nullable)/ativoTicker,quantidade(nullable),custoTotalBrl(nullable),estado,origem,revisadoPor/revisadoEm(nullable),versao,pendencias[{id,mensagem,impacto}],ancoraId(nullable),camposOriginais,aplicado,natureza. IDs canônicos: registro:N,operacao:N,ajuste:N.

Naturezas editáveis: OBSERVADA,SALDO_ANTERIOR,APORTE,COMPRA,VENDA. Negociação usa moedaCotacaoId,totalBruto,cotacaoBrl,taxaAtivoId,taxaQuantidade,taxaInformada. Campos ainda desconhecidos podem ser salvos, mas não transformados em valores zero; não promover informação incompleta para cálculo completo.

Resumo: total,coerentes,pendentes,inconsistentes,ajustesAtivos,percentualCoerencia,registrosAfetados,vendasAfetadas,ativosAfetados,mensagem. Percentual é secundário, sobre registros canônicos atuais, sem ajustes/cópias/versões inativas.

Ajuste de prévia: lacunaId,ancoraId,contaId/contaNome,instituicaoNome,ativoId/ativoTicker,quantidadeSugerida,custoTotalBrl(nullable),acao(CRIAR/MANTER/REDUZIR/ENCERRAR/ALTERAR),quantidadeAnterior(nullable). Custo BRL é nominal; origem continua provisória.

Correspondência: id,registroIds,descricao,contaOrigem,contaDestino,quantidade,ativoTicker. Primeiro incremento limita a duas pernas opostas, explícitas e confirmadas de transferência interna da mesma instituição/usuário/ativo; sem associação por mera coincidência de horário.

## Recebimento e aplicação

Depósitos e retiradas importados em BRL, com sinal compatível e sem indícios de transferência, são classificados respectivamente como `APORTE` e `RETIRADA`. Uma contrapartida de mesmo ativo e quantidade oposta em outra conta, dentro da janela configurada, mantém os registros para conferência. Indicações explícitas de transferência também impedem a classificação automática. Movimentações de cripto não são tratadas como compra ou venda por esses rótulos.

Uma linha importada `Withdraw` de cripto com quantidade negativa é reconhecida como `SAIDA_CRIPTO`, sem atribuir uma venda ou destino. Na timeline, `estado` indica consistência técnica, `finalidadeCripto` indica `DESCONHECIDA`, `CARTEIRA_PROPRIA`, `TERCEIROS` ou `OUTRA`. A finalidade é editada por PUT com revisão, versão e idempotência, e aplicada na confirmação global.

Saídas sem contraparte comprovada reduzem a custódia de origem sem inventar entrada no destino ou resultado realizado. Totais de custo e resultado dependentes permanecem incompletos; o histórico de custos anteriores é preservado. A indicação de saldo anterior insuficiente é distinta da coerência técnica. A observação de taxa incluída sem valor produz `taxaDetalhamentoPendente`; a quantidade da saída não é alterada quando o detalhamento for informado posteriormente.

A classificação automática é avaliada nas propostas ainda não editadas, inclusive nos registros já recebidos. A confirmação global aplica os efeitos e conserva a versão aplicada. A timeline informa `aguardaConfirmacao` separadamente de `aplicado`, pois uma versão anterior pode estar aplicada enquanto a nova classificação espera confirmação. Edições manuais e transferências já vinculadas têm precedência. Retirada debita BRL sem gerar resultado realizado e exige saldo disponível, com o mesmo tratamento de lacuna do livro. Os registros originais permanecem disponíveis.

Receber preserva os registros e permite consultá-los, inclusive inválidos. Não apaga os arquivos anteriores nem equivale a afirmar que todos os efeitos já estão incorporados ao livro.

Desde 07/11/2026, o recebimento CSV/XLSX e o acompanhamento da timeline são apresentados numa única área. A consolidação de negociações multilinhas e critérios de associação estão no [contrato complementar Binance](22-associacao-registros-binance.md); eles atualizam a limitação inicial documentada acima sem remover o histórico deste primeiro ciclo.

A prévia considera registros novos e dados corrigidos. Aplicar novos efeitos e modificar ajustes anteriores acontece numa confirmação atômica; evita somar entrada real ao ajuste que ela substitui. Rejeitar nova sugestão de crédito não impede aproveitar movimentos: o déficit permanece explícito e os cálculos afetados ficam indisponíveis. Reduzir/encerrar ajuste já aceito exige o aceite mostrado para aplicar os registros que o substituem. Resolver depois mantém registros e impactos.

A necessidade de ajustes é calculada sem os ajustes antigos. Histórico inalterado não cria versões ou revisões adicionais. Revisão obsoleta exige nova prévia e novo aceite; não recalcular silenciosamente outra proposta no POST.

Recebimentos por arquivo/hash são idempotentes. Fórmulas, macros, vínculos externos, arquivos corrompidos e limites de descompactação são tratados antes dos efeitos. Linha inválida permanece com seus valores originais e mensagens sem dados sensíveis nos logs. Mesmo arquivo não cria novas ocorrências; sobreposição de arquivos diferentes não elimina multiplicidade silenciosamente.

## Persistência V13

Adição de arquivos recebidos/proveniência em importacoes; contas_custodia, registros_conferencia, ajustes_conciliatorios versionados e comandos_conciliacao em operacoes. Todas as tabelas por usuário, sem usuários/senhas/titulares novos e sem entidades JPA cruzando contextos. Referências de origem são escalares. Campos e tabelas terão COMMENT breve; atualizar o dicionário. V1–V12 imutáveis.

Registros têm original imutável e proposta editável versionada, marca de aplicação e revisão manual separada. Ajustes persistem âncora, lacuna, versão, quantidade, autor/aceite e estado ativo; não um horário fictício de transação. Impactos e estados são derivados por replay, sem listas de dependências persistidas.

## Compatibilidade e limites

CSV legado preservado. Os endpoints de posições/visão geral passam a indicar parcialidade e uso de ajustes quando houver conferência; históricos completos preservam números existentes. Custos ausentes e totais incompletos trafegam como null, acompanhados de subtotais calculáveis e motivos.

Não adicionar relatórios fiscais, cotações externas, motor genérico de associações, notificações ou limpeza de dados. Testes públicos usam massa sintética e MySQL descartável.


## Disponibilidade e proveniência

`RegistroTimeline` acrescenta `aceitoPor` e `aceitoEm` para ajustes; não reutiliza os campos de revisão manual. A versão corresponde ao ajuste vigente. Os campos originais incluem arquivo, aba e linha de origem, mantendo o conteúdo recebido.

`GET /api/v1/posicoes` acrescenta `quantidadesCompletas`. A disponibilidade da visão geral contém `custoCompleto`, `resultadoCompleto`, `taxasCompletas`, `quantidadesCompletas`, `comAjustes`, `subtotalCustoBrl`, `subtotalResultadoBrl` e `subtotalTaxasBrl`. Custo e resultados totais são nulos quando a cobertura não permite afirmá-los; os subtotais não são apresentados como totais. Linhas sem data não são atribuídas artificialmente a um período. Quantidades conhecidas por ativo permanecem consultáveis, qualificadas quando necessário.

Ações financeiras completas usam as mesmas validações do cadastro manual. Taxa explicitamente zero dispensa moeda da taxa; taxa ausente continua desconhecida. Negociação incompleta mantém pendência automática mesmo com estoque e custo conhecidos. Datas originais são preservadas quando apenas outro campo é editado.

Sobreposições entre arquivos mantêm todas as ocorrências e sua multiplicidade. Referências de evento repetidas, inclusive entre o CSV estrito e os recebimentos, não geram novo efeito automaticamente. Correspondência ambígua não é resolvida pela ordem das linhas. O primeiro ciclo não oferece um motor geral de associação de execuções/contrapartidas.

`historico_propostas` preserva dados anteriores, versão, autor e instante das correções. Reaparecimento de lacuna encerrada continua a sequência de versões do ajuste. Recepção de arquivo anterior preserva a data comum de aberturas existentes.


## Movimentações e propostas financeiras

O snapshot aplicado pode conservar as observações originais enquanto a proposta de negociação continua pendente. `aplicado` informa que há um snapshot quantitativo incorporado; a pendência de alteração e os campos propostos permanecem separados. Repetir a conferência sem novos efeitos não incrementa a revisão.

CSV parcialmente classificado aproveita as pernas documentadas mesmo antes do primeiro aceite. Uma perna identificável pode ser incorporada quando a outra ainda exige catálogo ou correção, mantendo a cobertura global incompleta. Ausência de moeda/quantidade de contrapartida não representa saída zero. Taxa em terceira moeda não é adaptada às moedas do par.

Uma linha isolada de XLSX não é promovida a compra/venda completa: isso poderia repetir o débito que já existe em outra linha. Os dados propostos são preservados, e as observações continuam sustentando as quantidades até existir vínculo explícito das contrapartidas. Reconstrução geral de negociações multilinhas permanece uma evolução separada.

Pendência datada não torna indisponíveis os meses anteriores a ela. Incerteza sem data não recebe mês fictício. Valores por ativo incompletos são nulos, com subtotal de parcelas conhecidas identificado separadamente. A prioridade de pendências conta os dependentes do histórico inteiro, ainda que o filtro da tela esconda parte deles.


## Créditos automáticos

A proposta de crédito automático e seu subtipo são identificados a partir das descrições aceitas e das informações válidas da movimentação importada. São parte da timeline e da confirmação global. A coerência do evento não exige custo, mas o livro mantém a causa de custo desconhecido nas posições e nos cálculos que dependam dele. A classificação automática respeita edições manuais, sobreposição ambígua e versões aplicadas. [Detalhes](19-importacao-binance.md#créditos-automáticos-de-recompensas).


## Aplicações e resgates Simple Earn — 07/11/2026

As descrições `Simple Earn Locked Subscription` e `Simple Earn Flexible Subscription` com quantidade negativa e `Simple Earn Locked Redemption` e `Simple Earn Flexible Redemption` com quantidade positiva são propostas como `MOVIMENTACAO_INTERNA`, quando data, ativo, conta e origem são válidos. A ausência de contraparte não gera débito nem crédito patrimonial, custo, resultado ou ajuste. Nomes desconhecidos e sinais contraditórios não recebem essa classificação. Rendimentos seguem a regra de créditos automáticos.

A proposta permanece na timeline e é aplicada na confirmação global. Uma versão anteriormente aplicada mantém seu efeito até essa confirmação, quando o histórico vigente é reprocessado; edições manuais e ambiguidades de origem preservam sua precedência. Não há representação de posições separadas, disponibilidade, bloqueios ou produtos Earn.
