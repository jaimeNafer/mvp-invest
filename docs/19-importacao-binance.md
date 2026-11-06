# Importação de operações Binance

## Escopo

A primeira entrega aceita o relatório CSV enriquecido de eventos usado na carga inicial, com depósitos BRL (`Deposit/FIAT`) e compras em BRL (`Buy/OCBS` e `Buy/CONVERT`). Não equivale a suporte a qualquer exportação da Binance. Arquivos de movimentações XLSX, vendas, conversões cripto/cripto, taxas em BNB, transferências, retiradas, Earn e distribuições continuam fora deste formato.

O objetivo é construir histórico auditável. Importar não representa conciliação integral das contas nem apuração fiscal. Não se inferem posição anterior, taxa ausente ou custo desconhecido como zero.

## Fluxo de uso

Em **Operações → Importar arquivo**, selecionar o CSV, confirmar que os depósitos são recursos próprios e que as taxas OCBS estão incluídas no valor enviado. Quando não existe ponto de partida, declarar posição anterior zerada; havendo posição anterior, cadastrar o saldo inicial antes da importação.

A prévia não grava dados. Apresenta registros, valores, quantidades, diagnósticos e contagens de novos/reconhecidos. Um registro inválido impede a confirmação de todo o arquivo. O recibo conserva a relação entre registro de origem e operação. A paginação visual não seleciona subconjuntos para gravação.

Erros preservam arquivo e decisões enquanto a página permanece aberta. Uma resposta incerta conserva a chave e o comando para repetição. Não iniciar outra carga para resolver uma confirmação incerta.

## Interpretação e precisão

- Cabeçalho: `id,datetime_tz_BRT,type,label,market_model_type,order_type,sent_amount,sent_currency,sent_value_BRL,sent_address,received_amount,received_currency,received_value_BRL,received_address,fee_amount,fee_currency,fee_value_BRL`.
- UTF-8, com BOM opcional; CSV com aspas. O número de registro inclui o cabeçalho e independe das quebras de linha em campos entre aspas.
- Datas `yyyy-MM-dd-HH:mm:ss` em UTC−03 convertidas para UTC. Ordenação cronológica estável, preservando a ordem do arquivo nos empates.
- Depósito: quantidade efetivamente recebida em BRL, sem moeda enviada, taxa explicitamente zero.
- OCBS: principal = BRL enviado − taxa BRL. O motor debita principal e taxa uma única vez.
- CONVERT: apenas compras com BRL enviado e taxa explicitamente zero. Não generaliza conversões entre criptomoedas.
- Quantidade recebida e referência original são preservadas. Valores auxiliares `*_value_BRL` não substituem movimentações efetivas.
- Decimais exatos, sem ponto flutuante. A validação do catálogo e o motor existentes determinam precisão, disponibilidade, custos e saldo suficiente.

Limites padrão: 10 MiB e 5.000 eventos; configuração em YAML. Arquivos acima do limite são rejeitados integralmente.

## Identidade, repetição e histórico

Hash SHA-256 identifica os bytes originais. A identidade do evento é distinta: formato e referência de origem, dentro do usuário. Conteúdo financeiro normalizado compara tipo, instituição, ativo, instante UTC, quantidade, principal, cotação e taxa. Formatação decimal e avaliações auxiliares não criam novos fatos.

As chaves UUIDv5 da carga inicial são reconhecidas. Recibos estabelecem proveniência para exportações sobrepostas posteriores. Uma referência igual sem identidade comprovada bloqueia a carga para conferência; não se elimina ou duplica silenciosamente uma possível correspondência.

O reconhecimento considera o fato original, inclusive se depois corrigido ou cancelado. Importar novamente não reativa esse fato nem desfaz versões posteriores.

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

1. Diagnóstico e reconstrução comprovada das movimentações XLSX de Eliane, preservando multiplicidade e sobreposições.
2. Conversões adicionais, taxas em BNB e movimentações de custódia, cada capacidade com regras financeiras próprias antes da carga.
3. Earn/distribuições e conciliação integral por período, conforme evidências e políticas aprovadas.

Esses marcos não autorizam adaptar tipos de operação para aceitar linhas incompatíveis. Uma importação parcial dos tipos suportados não comprova cobertura do histórico inteiro.

## Skills e verificações

Utilizar planejamento, TDD, depuração sistemática, revisão e verificação já disponíveis. Nenhuma instalação adicional é pré-requisito. Uma revisão específica de segurança de uploads pode usar `security-threat-model`; regras tributárias exigem fontes oficiais e vigência aplicável, não confiança em uma skill genérica.

Testes usam dados sintéticos: parser, precisão, saldo insuficiente, história posterior, identidade, cancelamento/correção, atomicidade, revisão, concorrência, isolamento e interface. Arquivos reais, hashes e evidências privadas permanecem em `.local/`, fora de Git/CI/logs.
