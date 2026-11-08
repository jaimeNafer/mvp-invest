# Importação de operações Binance

## Escopo

O fluxo visível do produto recebe arquivos CSV e XLSX pela mesma área **Operações → Importações**. Ambos seguem recebimento, timeline, associação, prévia global e confirmação. O parser específico do formato normaliza as linhas para o pipeline comum; para o relatório enriquecido, o CSV e o XLSX aceitam o mesmo cabeçalho e separam envio, recebimento e taxa em linhas de origem distintas. O arquivo original, a linha física, os campos e o componente permanecem rastreáveis. O XLSX de movimentações continua aceito no mesmo pipeline.

O pipeline progressivo não equivale a suporte financeiro para qualquer evento Binance. Aporte, compra/venda, conversões, taxas em BNB, transferências, retiradas e outros eventos de Earn e distribuições dependem das regras e classificações do histórico. Itens sem interpretação completa permanecem pendentes; não são convertidos em eventos financeiros inventados. A API estrita legada continua disponível para compatibilidade, mas não integra a experiência atual da interface.

O objetivo é construir histórico auditável. Importar não representa conciliação integral das contas nem apuração fiscal. Taxa ausente é considerada zero. Posição anterior e custo desconhecidos permanecem sujeitos a conferência.

## Fluxo de uso

Em **Operações → Importações**, selecionar um ou mais arquivos CSV/XLSX. Os arquivos são preservados para conferência e suas linhas aparecem na timeline. Negociações reconhecidas formam uma única operação consolidada; execuções originais permanecem acessíveis. Casos ambíguos exigem seleção explícita de uma candidata. A associação apenas salva uma proposta; efeitos financeiros entram no livro após a confirmação global.

A prévia global não grava efeitos financeiros. Apresenta associações, quantidades, taxas, registros originais e ajustes pendentes. A confirmação revalida a revisão, aplica os grupos e os ajustes aceitos atomicamente e preserva idempotência. A paginação visual não seleciona subconjuntos para gravação.

Erros preservam arquivo e decisões enquanto a página permanece aberta. Uma resposta incerta conserva a chave e o comando para repetição. Não iniciar outra carga para resolver uma confirmação incerta.

## Interpretação e precisão

- Cabeçalho: `id,datetime_tz_BRT,type,label,market_model_type,order_type,sent_amount,sent_currency,sent_value_BRL,sent_address,received_amount,received_currency,received_value_BRL,received_address,fee_amount,fee_currency,fee_value_BRL`.
- UTF-8, com BOM opcional; CSV com aspas. O número de registro inclui o cabeçalho e independe das quebras de linha em campos entre aspas.
- Datas `yyyy-MM-dd-HH:mm:ss` em UTC−03 convertidas para UTC. Ordenação cronológica estável, preservando a ordem do arquivo nos empates.
- Depósito: quantidade efetivamente recebida em BRL, sem moeda enviada, taxa explicitamente zero.
- Relatório enriquecido: linhas de envio são pagamentos, linhas recebidas são recebimentos e taxas são débitos, todas com a mesma origem física. Taxa não informada é considerada zero.
- Execuções parciais são somadas exatamente por ativo e papel, dentro da janela configurada de associação. IDs numéricos repetidos não são usados como chave de negociação.
- Quantidade recebida e referência original são preservadas. Valores auxiliares `*_value_BRL` não substituem movimentações efetivas.
- Decimais exatos, sem ponto flutuante. A validação do catálogo e o motor existentes determinam precisão, disponibilidade, custos e saldo suficiente.

Limites padrão: 10 MiB e 5.000 eventos; configuração em YAML. Arquivos acima do limite são rejeitados integralmente.

## Identidade, repetição e histórico

Hash SHA-256 identifica os bytes originais. A identidade do evento é distinta: formato e referência de origem, dentro do usuário. Conteúdo financeiro normalizado compara tipo, instituição, ativo, instante UTC, quantidade, principal, cotação e taxa. Formatação decimal e avaliações auxiliares não criam novos fatos.

As chaves UUIDv5 da carga inicial são reconhecidas. Recibos estabelecem proveniência para exportações sobrepostas posteriores. Uma referência igual sem identidade comprovada bloqueia a carga para conferência; não se elimina ou duplica silenciosamente uma possível correspondência.

O reconhecimento considera a identidade de arquivo e componente de origem, inclusive se depois corrigido ou cancelado. Importar novamente não reativa esse fato nem desfaz versões posteriores. Cada perna derivada da mesma linha física recebe identidade própria, sem perder o número de linha original.

A mesma chave de confirmação e o mesmo comando retornam o recibo anterior antes de validar revisão. Reutilizar a chave com dados diferentes retorna 409. Arquivo já confirmado com outra chave retorna 409, orientando a consultar seu recibo; não se criam aliases de comandos.

## Responsabilidades e transações

`importacoes` interpreta o formato, resolve metadados pelos contratos públicos, coordena a carga e conserva arquivo, decisões, versão do leitor e proveniência. `operacoes.application.contract.IImportacaoOperacoes` oferece prévia e confirmação em lote; somente esse contexto normaliza comandos financeiros, valida o histórico e grava operações.

Não há chamadas HTTP internas, motor financeiro paralelo, filas ou persistência de rascunhos. O frontend apresenta os valores calculados pelo backend.

A confirmação mantém operações, arquivo, vínculos e recibo na mesma transação `READ_COMMITTED`, com bloqueio do ponto de partida. Valida todo o histórico vigente, incluindo operações posteriores às do arquivo. Avança a revisão uma vez quando há fatos novos; reconhecimento integral não altera a revisão financeira. Falhas revertem a transação inteira. A prévia utiliza uma leitura consistente e é revalidada na confirmação.

V12 acrescenta somente as tabelas de importação. Não reescreve migrations, limpa banco ou altera operações existentes. Aplicação em banco real exige backup prévio; testes usam MySQL descartável.

## API

| Método e rota | Entrada / saída |
| --- | --- |
| `POST /api/v1/importacoes/previa` | Multipart: `arquivo`, `aportesProprios`, `taxasIncluidas`, `posicaoAnteriorZerada`. Retorna hash, revisão, identidade da prévia, novos/reconhecidos, diagnósticos e registros. |
| `POST /api/v1/importacoes` | Mesmo multipart + `hash`, `revisao`, `previaId`; header `Idempotency-Key` UUID. Retorna recibo em 201. |
| `GET /api/v1/importacoes` | `pagina` (0+) e `tamanho` (1–100). Resumos paginados, sem arquivo ou lista de registros. |
| `GET /api/v1/importacoes/{id}` | Recibo e registros com referência e ID da operação, somente do usuário atual. |

Identificação exclusivamente pelo Bearer/IUsuarioAtual. IDs e decimais são strings. Erros de entrada retornam 400; conflitos de identidade, revisão ou saldo retornam 409; recibo inexistente/alheio, 404. Diagnósticos de parsing são retornados na prévia e impedem confirmação.

## Próximas entregas

O fluxo progressivo e a timeline são agora o caminho da interface. Os contratos estritos descritos neste documento continuam disponíveis para compatibilidade; arquivo ou referência já recebidos no fluxo progressivo bloqueiam uma segunda aplicação pelo fluxo estrito, mesmo com outro hash.

1. Diagnóstico e reconstrução comprovada das movimentações XLSX de Eliane, preservando multiplicidade e sobreposições.
2. Conversões adicionais, taxas em BNB e movimentações de custódia, cada capacidade com regras financeiras próprias antes da carga.
3. Earn/distribuições e conciliação integral por período, conforme evidências e políticas aprovadas.

Esses marcos não autorizam adaptar tipos de operação para aceitar linhas incompatíveis. Uma importação parcial dos tipos suportados não comprova cobertura do histórico inteiro.

## Skills e verificações

Utilizar planejamento, TDD, depuração sistemática, revisão e verificação já disponíveis. Nenhuma instalação adicional é pré-requisito. Uma revisão específica de segurança de uploads pode usar `security-threat-model`; regras tributárias exigem fontes oficiais e vigência aplicável, não confiança em uma skill genérica.

Testes usam dados sintéticos: parser, precisão, saldo insuficiente, história posterior, identidade, cancelamento/correção, atomicidade, revisão, concorrência, isolamento e interface. Arquivos reais, hashes e evidências privadas permanecem em `.local/`, fora de Git/CI/logs.


## Créditos automáticos de recompensas

Movimentações importadas válidas, com ativo do catálogo, conta e data identificados e quantidade positiva, são propostas como `CREDITO_AUTOMATICO` para `Megadrop Rewards` (bonificação), `Launchpool Airdrop - System Distribution` (airdrop) e `Simple Earn Locked Rewards` (rendimento). O extrato de movimentações, com variação positiva e conta identificada, é a evidência do crédito patrimonial; o nome isolado não indica disponibilidade de negociação. Dados contraditórios ou incompletos não são automaticamente aplicados. A confirmação global aplica a proposta, preservando originais, autoria e regras de sobreposição.

A entrada não é compra, venda nem resultado realizado. Custo não informado permanece desconhecido e pode tornar indisponíveis preço médio e resultados posteriores; essa indisponibilidade não torna incoerente o reconhecimento do crédito. Não há controle de bloqueio, prazos ou disponibilidade de produtos Earn. `V15` inclui BNB, LISTA e IO no catálogo, sem sobrescrever cadastros.


## Movimentações internas Simple Earn

Aplicações e resgates Locked e Flexible são reconhecidos pelas quatro descrições e sinais previstos no [contrato de conferência](21-contrato-conciliacao.md#aplicações-e-resgates-simple-earn--07112026). Não alteram o saldo patrimonial consolidado nem o custo; recompensas efetivamente creditadas seguem como entradas separadas. A classificação automática permanece uma proposta até a confirmação global. Não há controle de disponibilidade ou posições separadas no Earn.


## Conversões entre ativos

Conversões identificadas no relatório de movimentações ou no relatório enriquecido são vinculadas e apresentadas como uma única operação, com valores enviados, recebidos e originais acessíveis. Quantidades e valoração têm disponibilidades distintas; a confirmação global aplica as mudanças. [Critérios e limites](22-associacao-registros-binance.md#conversões-entre-ativos--07112026).
