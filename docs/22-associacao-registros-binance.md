# Associação de registros complementares da Binance

Evolução em andamento em 07/11/2026. Estado e verificações na [retomada](06-retomada.md). Complementa a [conciliação progressiva](21-contrato-conciliacao.md) e agora atende o pipeline comum de recebimento CSV/XLSX. A validação integrada MySQL e o ensaio visual permanecem pendentes.

## Reconhecimento e decisão

Uma operação agrupada reúne linhas do mesmo usuário, instituição e conta de custódia. Seus IDs de origem e valores permanecem preservados. O número `OP-123` identifica o grupo no Lastro, nunca uma ordem da Binance.

O reconhecimento automático usa `lastro.conciliacao.janela-associacao`, inicialmente `2s`. Mede do primeiro ao último instante do grupo; candidatos fora da janela não se encadeiam. Exige usuário, instituição, conta de custódia, tipo/família e direções compatíveis, uma combinação completa única e papéis interpretáveis. Execuções parciais do mesmo ativo e papel são somadas em decimal exato antes do pareamento. Taxas explicitamente identificadas são incluídas somente quando há uma única negociação compatível no intervalo. IDs numéricos repetidos, como os da exportação Binance, não identificam uma negociação. Referências fortes conflitantes, negociações distintas e sobreposições impedidas não são combinadas silenciosamente.

Referências fortes distintas são avaliadas separadamente na mesma conta, mesmo quando os horários coincidem. O Histórico apresenta a operação identificada antes da confirmação, com as linhas originais no card. A confirmação global continua necessária para aplicar os efeitos financeiros.

Quando há mais de um destino candidato, o card do registro pendente exibe as opções de rádio sem seleção inicial. Trocar o destino substitui a escolha anterior. A busca por número, a seleção de linhas adicionais, os papéis e os campos financeiros ficam em **Opções de conferência** no mesmo card. A escolha preenche a proposta para prévia e gravação.

Os tipos reconhecidos são `Binance Convert`, `Convert`, `Buy Crypto With Fiat`, `Transaction Buy`, `Transaction Spend`, `Transaction Sold`, `Transaction Revenue`, `Transaction Fee`, `Buy`, `Sell`, `Fee` e `Trading Fee`. Tipos desconhecidos exigem atribuição manual de papéis; não provam uma compra ou venda. Famílias conhecidas conflitantes permanecem pendentes.

Na associação manual, o intervalo total entre a primeira e a última linha respeita a mesma janela configurada, inclusive o limite. São apresentados horário completo, diferença temporal, referência e origem. Selecionar linhas, atribuir papéis, buscar grupo por número, acrescentar ou retirar linhas e desfazer exigem prévia. Menos de duas linhas em um grupo existente equivale a propor seu desfazimento.

## Quantidades, custo e aplicação

O principal conhecido é separado do custo final. Uma taxa não informada na importação é considerada zero quando não há linha de taxa próxima que possa pertencer à negociação e ainda precise de vínculo. Taxas em terceiro ativo ou em várias moedas são vinculadas e debitadas quantitativamente uma única vez, com cálculo financeiro pendente. Não há inclusão automática de ativos no catálogo. Uma taxa de ativo ainda não identificado pode ser vinculada, mas sua quantidade não é contabilizada até a identificação; a cobertura permanece incompleta.

Grupos incompletos preservam as quantidades documentadas; custo final e resultados afetados continuam indisponíveis. Negociações completas do par suportado usam o MotorLivro existente. Quantidades de execuções são somadas exatamente por papel e ativo; seus registros individuais não são descartados.

Salvar a proposta não modifica saldos. Ajustes aceitos continuam visíveis e contados como ativos até sua confirmação de redução ou encerramento. A confirmação global aplica grupos inequívocos e propostas manuais na mesma transação que as linhas e mudanças de ajustes aceitas. A projeção seleciona o grupo ou suas linhas, nunca ambos. Grupos usam como instante econômico o menor instante de suas linhas; instantes individuais permanecem disponíveis.

A coerência expressa a consistência dos dados e do cálculo, independentemente da confirmação global. Uma compra inequívoca em BRL, com quantidades e valor conhecidos e sem pendências financeiras, é coerente antes da aplicação. A tela informa separadamente que a confirmação global está pendente. Saldos insuficientes, referências conflitantes e dados financeiros incompletos permanecem sujeitos às validações existentes.

Alterar uma linha exige reavaliar sua associação. Enquanto isso, a versão aplicada do grupo permanece congelada e a alteração é apresentada como pendência. Desfazer reprocessa as linhas no histórico vigente, sem restaurar cálculos antigos. Ajustes que mudem continuam exigindo o aceite correspondente na prévia.

## API

Rotas sob `/api/v1/conciliacao/associacoes`:

| Método e caminho | Finalidade |
| --- | --- |
| `GET /candidatos?registroId=registro:N` | Linhas próximas, grupo existente, revisão e avisos de ambiguidade. |
| `GET /{numero}` | Consultar grupo por número `OP-N`, seus papéis e informações financeiras conhecidas. |
| `POST /previa` | Validar seleção e devolver interpretação e pendências, sem gravar. |
| `POST /propostas` | Salvar proposta versionada com revisão, hash da prévia e `Idempotency-Key`. |

Entrada da prévia: `revisao`, `numero`/`versao` opcionais, `linhas:[{id,versao,papel}]`, `taxaZeroConfirmada`, `cotacaoBrl` opcional e `desfazer`. Papéis: `RECEBIMENTO`, `PAGAMENTO`, `TAXA`, `NAO_DEFINIDO`. A gravação acrescenta `previaId`. IDs e decimais são strings; custos desconhecidos são nulos.

A timeline apresenta uma entrada por `grupoNumero` ou, antes de atribuir o número, por `grupoAutomatico`. As parcelas agregadas em `pagamentosGrupo` e `recebimentosGrupo` permitem apresentar origem e destino sem calcular valores no navegador. `linhasPorGrupo` inclui as linhas originais dos grupos da página, mesmo quando apenas uma delas corresponde ao filtro. A operação confirmada compõe uma única negociação no livro, sem criar cópias na tabela legada de operações. Na prévia, a operação consolidada mostra quantidades recebidas, pagas, taxas e suas linhas originais, incluindo arquivo, aba e número da linha.

A resposta contém número/versão, linhas, recebimentos, pagamentos, taxas, natureza, principal conhecido em BRL, custo final quando calculável, pendências e resoluções propostas. `taxaZeroConfirmada` e `cotacaoBrl` preservam decisões ao editar. A prévia global de conciliação acrescenta `grupos`, incluídos na confirmação em lote, sem aceite individual para cada grupo inequívoco.

Proprietário vem exclusivamente de IUsuarioAtual. Validação retorna 400, versão/revisão/conflito de vínculo retorna 409 e grupo/linha inexistente ou alheio retorna 404. Repetição do mesmo comando devolve o resultado anterior antes de recusar revisão antiga.

## Persistência e responsabilidades

V14 adiciona grupos, reservas de linhas e histórico de propostas, sem alterar migrations anteriores. A reserva abrange a união das linhas propostas e aplicadas até a confirmação; impede reaproveitar uma linha em outro grupo durante uma edição pendente. Desfazer e confirmar libera a reserva, preservando o histórico.

Uma alteração apenas do motivo de uma linha não invalida sua associação: a versão anterior é comparada à proposta atual e a versão da linha é atualizada na projeção. A confirmação global registra a atualização da proposta antes de aplicá-la. Alterações nos demais campos continuam sujeitas à revisão do grupo; enquanto isso, a timeline preserva a proposta visível e o livro mantém somente a versão anteriormente aplicada. Os registros originais não são modificados.

O agrupador de domínio interpreta papéis e compatibilidade; a aplicação coordena usuário, snapshots, persistência e DTOs; adapters mantêm JPA e constraints. Os controllers apenas validam e delegam. Não há novo motor financeiro, fila ou contexto genérico de workflows.

Critérios de aceite incluem exemplo SOL/BRL, múltiplas execuções, taxa externa pendente, ambiguidades sem escolha inicial, limite temporal configurável e inclusivo, IDs numéricos repetidos, CSV/XLSX pelo mesmo pipeline, isolamento, edição/desfazimento de grupos aplicados, idempotência, concorrência e rollback. Dados reais não entram em fixtures, logs ou Git.


## Conversões entre ativos — 07/11/2026

`Binance Convert` e o tipo `Convert` do relatório enriquecido são interpretados como contrapartidas de uma operação, respeitando a mesma instituição, conta e janela configurada. Um candidato concorrente impede a associação automática; referências fortes diferentes são avaliadas separadamente; sem referência forte, componentes de linhas físicas distintas do relatório são avaliados separadamente. IDs numéricos recorrentes continuam insuficientes como identidade. Execuções múltiplas dos dois lados exigem referência forte comum; os totais são somados exatamente por ativo e papel. As linhas originais permanecem disponíveis no grupo, antes e depois da confirmação global.

Quando uma perna é BRL, seu valor determina o principal. Na conversão entre criptoativos, uma cotação em reais por unidade da moeda de cotação permite calcular custo e resultado com o motor existente. Para BNB → SOL, essa moeda é o BNB enviado; para uma venda por USDT/USDC, é a moeda recebida. A importação só propõe essa cotação a partir de valores em BRL da mesma linha física, com moedas e quantidades coerentes, valores não conflitantes e sem taxa não zero. Caso contrário, a cotação permanece desconhecida e pode ser informada na associação. Não são consultadas cotações externas.

Sem valoração, a associação pode ser coerente e confirmada; as parcelas de pagamento, recebimento e taxa atualizam quantidades uma única vez. Execuções de mesma função e ativo são somadas antes da baixa de custo. Quando a taxa utiliza um dos ativos da conversão, seu efeito quantitativo é combinado ao desse ativo, preservando os valores originais e evitando lacunas artificiais de ordem. O custo de origem continua reprocessável; o custo do ativo recebido e o resultado dependente permanecem indisponíveis. A falta de preço não é pendência de vínculo. Dados financeiros contraditórios, saldo insuficiente e taxas em terceiro ativo continuam sujeitos às validações pertinentes.
