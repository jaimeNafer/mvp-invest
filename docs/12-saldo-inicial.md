# Saldo inicial — contrato implementado

Implementado em 02/11/2026 após autorização de Jaime, com seis ativos e uma data de referência por usuário. API, interface, persistência V9 e cálculo de posição inicial estão disponíveis localmente. Este documento registra o contrato vigente e as escolhas adotadas; não declara compras/vendas ou motor completo entregues.

## Resultado de produto

Registrar uma posição já existente na data de início do acompanhamento, informando instituição, ativo, quantidade e custo histórico total em reais. Consultar os registros e os totais iniciais após reiniciar a aplicação. Corrigir um erro preservando o registro anterior.

Exemplo fictício: Jaime informa 0,01 BTC na Binance, com custo total de R$ 4.000. O lançamento é registrado uma vez e aparece nas carteiras que selecionam Binance. Selecionar duas dessas carteiras não duplica quantidade, custo ou lançamento.

Saldo inicial não representa compra nova, aporte, receita, lucro ou cotação de mercado. Ele informa a base de abertura do recorte; não reconstitui compras, vendas ou resultados anteriores à data escolhida.

## Decisões confirmadas

| Tema | Decisão |
| --- | --- |
| Ativos | BRL, USD, BTC, ETH, USDT e USDC. Catálogo mantido pelo sistema. |
| Data | Uma data de referência por usuário, comum a suas instituições e ativos. A data concreta será informada na aplicação. |
| Propriedade | Lançamento pertence ao usuário/instituição. Usuário obtido exclusivamente de `IUsuarioAtual.id()`. |
| Carteiras | `contas` é vínculo carteira/instituição. Operações não pertencem a `contas.id` ou a uma carteira. |
| Histórico único | Mesmos registros aparecem em várias carteiras; combinações de carteiras contam cada evento uma vez. Usuários permanecem separados. |
| Custo conhecido | Custo histórico total em BRL obrigatório. Desconhecido não pode ser preenchido automaticamente com zero. Zero explícito exige confirmação e justificativa. |
| BRL | Custo do saldo em BRL igual à quantidade. |
| Números | Java/BigDecimal; quantidades até 18 casas, valores BRL persistidos até 8 e apresentação monetária usual com 2, HALF_EVEN. Decimais no transporte como strings. |
| Persistência | MySQL/Flyway/JPA; cadastro, regras, contexto e componentes existentes serão reutilizados. |

## Comportamento desta entrega

As regras seguintes são aplicadas pelo fluxo de saldo inicial. Evoluções de outros tipos de operação exigem seus próprios contratos.

### Data e unicidade

- Data de referência representa a posição imediatamente antes das movimentações do dia escolhido, às 00:00 de `America/Fortaleza`. Guardar data de negócio e instante correspondente em UTC; data deve existir no calendário e não ser futura no fuso do produto.
- Não preencher a primeira data silenciosamente com o dia atual. Informar seu significado antes da confirmação.
- Permitir um saldo inicial confirmado por `(usuário,instituição,ativo)`. Segundo envio com outra chave de requisição para a mesma combinação retorna 409, mesmo quando quantidade/custo coincidem; orientar corrigir o existente.
- Instituições diferentes podem conter o mesmo ativo. Seus registros de abertura usam a mesma data do usuário e contribuem para o custo e a quantidade iniciais globais desse ativo.
- Enquanto houver abertura confirmada, a data comum fica fixa. Correção individual não altera a data.
- Se a data tiver sido informada incorretamente, cancelar as aberturas confirmadas com motivo e registrar novas na data correta. Permitir redefinir o ponto de partida somente sem aberturas confirmadas e sem outros tipos de movimentação; preservar os registros históricos cancelados. Não exigir edição manual do banco.
- Após introduzir compras/vendas, revisar a possibilidade de redefinição do início antes de liberar esses tipos. Não permitir que uma abertura tardia contorne a validação do histórico.

### Entrada e catálogo

| Campo | Regra |
| --- | --- |
| Instituição | ID existente e ativo. O catálogo inicial continua Binance/BitGet. |
| Ativo | ID existente e ativo entre os seis provisionados. |
| Data de referência | Obrigatória na primeira abertura; nas seguintes, deve coincidir com a data comum. |
| Quantidade | Maior que zero; respeitar escala e capacidade do campo e a precisão do ativo. |
| Custo total em BRL | Obrigatório, maior ou igual a zero, até 8 casas; não inferir a partir de cotação atual. |
| Confirmação de custo zero | Obrigatória quando custo for zero; manter distinta de campo em branco. |
| Justificativa de custo zero | Obrigatória para zero, texto normalizado até 300 caracteres. Preservada para consulta. |
| Motivo da correção/cancelamento | Obrigatório nessas ações, texto normalizado até 300 caracteres. |

Catálogo provisionado: `fiat:BRL`, `fiat:USD`, `cripto:BTC`, `cripto:ETH`, `cripto:USDT` e `cripto:USDC`, com códigos únicos, nomes/tickers e situação ativa. FIAT: BRL/USD; CRIPTO: os outros quatro. Precisão de quantidade nesta entrega: 2 casas para FIAT e até 18 para CRIPTO. Essa é uma capacidade de registro gerencial; não é uma regra de blockchain ou de uma rede específica.

BRL tem custo preenchido a partir da quantidade e validado novamente no backend. Para USD, USDT e USDC, informar o custo histórico total em reais, como para os demais ativos. Não assumir paridade entre moedas. Não adicionar câmbio automático ou integração de preços nesta entrega.

Criar saldo exige instituição ativa, não a existência de vínculo com uma carteira. A listagem geral do usuário permite encontrar registros fora das seleções atuais. Adicionar/remover instituições nas carteiras muda somente a visualização.

### Números e resultados

- Entradas canônicas da API usam ponto decimal, sem agrupamento, sinal negativo, notação científica, `NaN` ou `Infinity`. IDs e decimais são strings. Não converter dados financeiros para `Number`, `float` ou `double`.
- `DECIMAL(38,18)` suporta no máximo 20 dígitos inteiros de quantidade; `DECIMAL(38,8)`, 30 dígitos inteiros de custo. Validar antes de gravar. Zeros finais equivalentes podem ser normalizados; excesso efetivo de escala é recusado com 400, nunca truncado ou arredondado na entrada.
- Somar quantidades/custos exatamente com BigDecimal. Custo informado é o valor de referência, não um resultado reconstruído de um custo médio arredondado.
- Posição inicial global por usuário/ativo: `quantidade = soma das quantidades confirmadas`; `custo total = soma dos custos confirmados`; `custo médio = custo total / quantidade`.
- Detalhamento decimal adotado nesta etapa: divisão do custo médio com 36 casas fracionárias e HALF_EVEN, sem limitar a parte inteira por `MathContext` global. Custo médio é derivado, transportado como string e não persistido. Pontos de arredondamento de baixas, taxas e resíduos de compras/vendas serão especificados na entrega correspondente.
- Carteiras não criam custos médios independentes. A posição global considera todas as instituições do usuário, inclusive as ausentes da carteira selecionada. O filtro de carteiras da listagem não deve transformar uma soma parcial em custo médio global.
- Na primeira entrega, a consulta de posição inicial é global, com rótulo explícito de todas as instituições. Detalhes por instituição mostram quantidade e **custo inicial informado**, sem apresentar esse valor como alocação do custo médio global. Rateio de custo em posições filtradas fica para o motor.
- Interface recebe/conserva strings decimais, aceita apresentação pt-BR e converte o texto para o formato canônico sem passar por ponto flutuante. Formatação monetária aplica HALF_EVEN e mantém o valor completo acessível no detalhe.
- Custo positivo inferior a R$ 0,01 deve ser identificado como menor que R$ 0,01, com valor completo no detalhe; não apresentá-lo como custo zero confirmado.

## Fluxo de interface

1. Abrir **Operações**: listagem geral dos saldos iniciais, com filtros por instituição, ativo e carteiras. Situação padrão: confirmados; histórico cancelado/substituído consultável.
2. **Adicionar saldo inicial** abre modal, reutilizando os componentes existentes. Na primeira inclusão, escolher data; nas seguintes, mostrar a data comum. Se não houver saldo confirmado, permitir escolher nova data respeitando as regras acima.
3. Escolher instituição e ativo, informar quantidade/custo. Se custo zero, exigir confirmação e justificativa. Apresentar resumo dos dados antes da confirmação final.
4. Salvar no backend/MySQL, atualizar lista e consulta de posição inicial global. Mostrar saldo/custo a partir dos registros confirmados; não exibir lucro, preço atual ou métricas sem cálculo implementado.
5. **Corrigir** abre modal preenchida e exige motivo. A confirmação cria uma nova versão; o registro anterior continua consultável como substituído. **Cancelar** exige motivo, preserva o fato anterior e o retira dos totais vigentes.

Estados obrigatórios: carregamento, vazio real, erro e nova tentativa. Erro conserva campos. Envio bloqueia controles/fechamento e mantém a chave de requisição enquanto o resultado for incerto. Após timeout, orientar consultar/repetir a mesma solicitação, sem criar outra chave automaticamente.

Nenhum menu de compra/venda/importação ou ação sem funcionamento é incluído. Os textos do fluxo descrevem o uso do produto, sem expor stack, ambiente ou etapas de desenvolvimento.

## Contrato HTTP

Todas as rotas exigem o Bearer atual e isolamento por `IUsuarioAtual.id()`. Nenhuma entrada recebe perfil, usuário proprietário ou ID de vínculo `contas`.

| Método e rota | Finalidade |
| --- | --- |
| `GET /api/v1/ativos` | Catálogo de leitura, ordenado por ticker/ID; IDs, código, ticker, nome, tipo, precisão de quantidade e situação. |
| `GET /api/v1/saldos-iniciais` | Listagem paginada do usuário; filtros opcionais `instituicaoId`, `ativoId`, `carteiraIds` e `situacao`. |
| `GET /api/v1/saldos-iniciais/{id}` | Registro e referências de sua correção/cancelamento, somente do usuário atual. |
| `POST /api/v1/saldos-iniciais` | Confirmar abertura; primeira gravação retorna 201 e Location para o novo recurso. |
| `POST /api/v1/saldos-iniciais/{id}/substituicoes` | Corrigir abertura confirmada, com motivo; 201 para a nova versão. |
| `POST /api/v1/saldos-iniciais/{id}/cancelamento` | Cancelar abertura confirmada com `{motivo}`; 200 com estado cancelado. |
| `GET /api/v1/posicoes-iniciais` | Quantidade/custo inicial globais por ativo e detalhes por instituição, apenas do usuário atual. Sem filtro de carteira nesta etapa. |

Entrada de criação; valores fictícios, IDs ilustrativos:

```json
{
  "instituicaoId": "1",
  "ativoId": "3",
  "dataReferencia": "2026-10-01",
  "quantidade": "0.01",
  "custoTotalBrl": "4000.00",
  "custoZeroConfirmado": false,
  "justificativaCustoZero": null
}
```

Substituição usa esses campos e `motivo`. Proibir campos de custo zero contraditórios com custo positivo. Data deve respeitar o ponto de partida comum; instituição/ativo podem ser corrigidos, desde que não conflitem com outra abertura confirmada.

Resposta de registro: `id`, `instituicaoId`, `instituicaoNome`, `ativoId`, `ativoTicker`, `dataReferencia`, `quantidade`, `custoTotalBrl`, `situacao`, `substituiId`, `justificativaCustoZero`, `motivoAlteracao`, `criadoEm` e `alteradoEm`. Situações: `CONFIRMADA`, `SUBSTITUIDA`, `CANCELADA`. Timestamps ISO UTC; IDs/decimais em strings. O fato anterior preserva os valores informados.

Listagem: envelope com `itens`, `pagina`, `tamanho`, `total`, `dataReferencia` e `possuiSaldosConfirmados`. Os dois últimos representam o usuário inteiro, independentemente dos filtros/página. Padrão: 50 registros por página, máximo 100, ordenação por data/ID. Totais de posição usam todos os confirmados, nunca só a página.

Filtro de várias carteiras usa a união de suas instituições para o usuário. Carteira inexistente/de outro usuário retorna 404; não usar instituições da carteira alheia. Carteira arquivada pode ser consultada quando selecionada explicitamente; não concede permissão sobre outro usuário.

### Falhas, repetição e concorrência

- 400: campos, calendário, escala, capacidade, instituição/ativo inexistente ou inativo e confirmação de zero inválidos. 401: Bearer ausente/inválido. 404: registro ou carteira inexistente/de outro usuário. 409: abertura confirmada já existente, data comum incompatível, chave reutilizada com outro comando ou disputa de alteração. Erros legíveis via ProblemDetail, sem detalhes internos.
- Criação e substituição exigem `Idempotency-Key` UUID, única por usuário. O fingerprint do comando inclui rota, registro de origem e valores normalizados. Chave ausente/inválida: 400.
- Mesma chave e mesmo comando: primeira execução 201; repetição concluída 200, com o mesmo ID e o estado atual desse recurso. Não criar outra versão ou confirmar outra abertura. Mesma chave com comando diferente: 409.
- Equivalências numéricas e de texto normalizado, como `4000.00` e `4000.000`, não tornam uma repetição um comando diferente. Justificativas/motivos diferentes são alterações de conteúdo.
- Identificar repetição concluída antes de revalidar a data comum atual, a situação da versão de origem ou a situação atual do catálogo: retornar um resultado já gravado não representa uma nova operação.
- Cancelamento é idempotente pelo recurso: repetir com o mesmo motivo normalizado retorna o estado cancelado, sem novo efeito; motivo diferente ou tentativa de cancelar versão substituída retorna 409.
- Cadastro, chave de requisição, verificação/definição da data comum e gravação são uma transação. Correção cria a nova versão e substitui a anterior atomicamente; cancelamento altera situação/metadados atomicamente. Falha reverte tudo.
- Serializar mudanças do mesmo usuário pelo ponto de partida; unicidade também protegida no banco. Duas primeiras aberturas concorrentes não podem estabelecer datas diferentes. Disputa pode retornar 409 após rollback; repetição usa a mesma chave.
- Corrigir/cancelar uma mesma versão simultaneamente admite apenas uma alteração efetiva; a outra retorna 409. Usar bloqueio transacional e revalidar situação, sem aceitar a leitura antiga do navegador como autoridade.

## Persistência e responsabilidades

V9 acrescenta as estruturas abaixo e provisiona os seis ativos, sem reescrever V1–V8. A migration valida códigos/tickers existentes antes das alterações persistentes e interrompe em incompatibilidade. Não sobrescreve catálogo ou cadastros. Comentários breves de tabelas/colunas estão no [dicionário vigente](11-dicionario-de-dados.md).

| Estrutura | Responsabilidade e motivo |
| --- | --- |
| `pontos_partida` | Uma linha por `perfil_ref`, com data comum, criação/alteração e autoria. Representa o início do acompanhamento financeiro; permite bloquear o mesmo usuário e impedir datas concorrentes divergentes. Não cadastra usuários ou autentica credenciais. |
| `operacoes` | Fatos financeiros; único tipo implementado nesta entrega: `SALDO_INICIAL`. Usuário, instituição, ativo, data/instante, quantidade, custo, situação, justificativa, encadeamento de versões, motivo e metadados de autoria. Sem FK para carteira ou `contas`. |

`pontos_partida` resolve a regra de uma data por usuário, explicitamente escolhida por Jaime. Sem essa coordenação, duas requisições poderiam ler ausência de abertura e salvar datas incompatíveis. A criação concorrente da primeira linha deve respeitar unicidade e rollback; novas tentativas encontram a linha existente e a bloqueiam. Não adicionar sessão, tabela de autenticação ou cache de usuário para essa finalidade.

Em `operacoes`, preservar fatos quantitativos das versões antigas. Situação e metadados de ciclo de vida podem ser atualizados; substituição cria outro ID e referencia a versão anterior. Registrar quem/quando criou ou alterou, a partir do contexto e do relógio do servidor. Guardar chave/fingerprint do comando de criação/substituição no mesmo registro; não criar infraestrutura genérica de idempotência antecipadamente.

Garantir uma versão confirmada por usuário/instituição/ativo por constraint MySQL, permitindo versões históricas. Implementação: marcador gerado com valor 1 para confirmada e NULL para as demais situações, com índice único sobre perfil/instituição/ativo/marcador. Testes MySQL exercitam cancelamento, substituição e concorrência; uma consulta `exists` sozinha não garante unicidade.

Na substituição da mesma combinação, liberar a confirmação anterior antes de inserir a nova versão, dentro da mesma transação e com o bloqueio mantido. Qualquer falha precisa restaurar integralmente o estado confirmado anterior; conferir também a ordem de flush do JPA nos testes.

Controller cuida de binding/validação estrutural/HTTP e delega à interface ISaldoInicialUseCase. A implementação coordena contexto, transação e ciclo de vida; validação e identidade de solicitação têm helpers específicos, mappers produzem DTOs e adapters JPA implementam contratos do domínio. Cálculo de posição inicial fica no backend Java, isolando soma/divisão da camada HTTP. Extrair somente as responsabilidades efetivas; não criar motor de compra/venda, interfaces por classe ou projeções persistidas antes de existir esse comportamento.

Frontend reutiliza cliente HTTP, serviços de acesso, modal e ícones. Chamadas de ativos/operações ficam em services próprias. Não armazenar dados financeiros em localStorage; chave pendente e formulário permanecem em memória React. Apresentação decimal precisa de tratamento reutilizável e testes, sem reproduzir regras do domínio no React.

## Exemplos com resultados esperados

Todos fictícios, com data comum 01/10/2026. Custo médio abaixo é por usuário/ativo e independente da escolha de carteiras.

| Caso | Entrada | Resultado esperado |
| --- | --- | --- |
| SI01 | Jaime/Binance: 0,01 BTC, custo R$ 4.000 | Quantidade 0,01; custo 4.000; médio 400.000 BRL/BTC; 1 registro. |
| SI02 | Principal e Reserva incluem Binance; visualizar SI01 em ambas/juntas | Mesmo ID nas duas visões; combinação continua com 0,01 BTC e R$ 4.000, sem duplicação. |
| SI03 | Somar a SI01: Jaime/BitGet, 0,02 BTC, custo R$ 10.000 | Total 0,03 BTC; custo R$ 14.000; médio = 1.400.000 / 3, com 36 casas e HALF_EVEN. Não calcular nova média apenas da carteira filtrada. |
| SI04 | Eliane/Binance: 0,005 BTC, custo R$ 1.500 | Posição de Eliane: 0,005 e 1.500; médio 300.000. Nada muda na posição de Jaime. |
| SI05 | Jaime/Binance: 1.234,56 BRL | Custo = 1.234,56. Custo diferente é recusado. |
| SI06 | USD: quantidade 100; custo histórico R$ 500 | Quantidade 100 USD; custo R$ 500; médio 5 BRL/USD, sem buscar cotação. |
| SI07 | USDT: quantidade 100; custo histórico R$ 520 | Médio 5,2 BRL/USDT; não presumir igualdade com USD. |
| SI08 | ETH: quantidade 3; custo R$ 1 | Total permanece 1; médio = 1/3, com 36 casas. Não reconstruir custo multiplicando uma média arredondada. |
| SI09 | Cripto: quantidade positiva; custo 0, confirmado com justificativa | Aceitar e sinalizar custo zero no detalhe. Sem confirmação/justificativa ou custo em branco: 400. |
| SI10 | Custo 1,005 BRL e, separadamente, 1,015 BRL | Preservar valores no banco; apresentação com 2 casas e HALF_EVEN: 1,00 e 1,02. |
| SI11 | Custo positivo 0,00000001 BRL | Preservar; apresentação identifica valor menor que 0,01 e oferece detalhe exato. Não tratar como zero confirmado. |
| SI12 | Quantidade com 19 casas efetivas, FIAT com mais de 2, custo com 9 ou valores acima da capacidade inteira do campo | 400, sem truncamento e sem gravação parcial. |
| SI13 | Quantidade 0/negativa, custo negativo, data inválida/futura, ativo/instituição inválido | 400 e nenhum fato confirmado. |
| SI14 | Repetir SI01 após timeout com a mesma chave/comando | Mesmo ID e um único registro criado; repetição 200. |
| SI15 | Mesma chave com quantidade diferente; ou nova chave para duplicar SI01 | 409; totais e quantidade de registros inalterados. |
| SI16 | Corrigir SI01 isolado para 0,01 BTC e custo R$ 4.200 | Novo ID confirmado; original substituído com valores 0,01/4.000 preservados. Médio vigente 420.000. |
| SI17 | Correção conflita com abertura confirmada de outro ativo/instituição | 409; original continua confirmado e nenhum fato parcial novo. |
| SI18 | Cancelar SI01 isolado, com motivo | Registro histórico preservado; posição inicial vigente desse ativo fica ausente. Repetir cancelamento com mesmo motivo não muda totais. |
| SI19 | Usuário tem abertura em 01/10; tenta outra em 02/10 | 409, sem alterar data comum ou os registros anteriores. |
| SI20 | Duas primeiras solicitações do usuário disputam datas diferentes | Apenas uma data se estabelece; outra requisição reverte com 409. Nenhum fato com data divergente. |
| SI21 | Remover Binance de uma carteira, arquivar ou reassociar | SI01 persiste; outra carteira continua mostrando o mesmo evento; reassociação recupera a visualização sem reimportar. |
| SI22 | Reiniciar aplicação e consultar todas as páginas | Dados, situações e IDs preservados; posição usa todo o histórico confirmado. |

Os exemplos orientam os testes; não representam 22 testes independentes de navegador. Resultados aritméticos foram conferidos com razões inteiras. As suítes executadas cobrem domínio, API/MySQL, concorrência, migrations, formulário, falhas/repetição e decimais, conforme as evidências abaixo.

## Evidências e conclusão

1. Backend `check` aprovado: 44 testes, sendo 7 unitários e 37 integrados. A fatia acrescenta 2 testes de cálculo, 15 de API/transação/concorrência e 3 de V9. Testes históricos até V7 e V8 preservados.
2. Frontend: lint, 20 testes e build aprovados. A fatia acrescenta 5 testes de fluxo e 2 de decimais, incluindo repetição com a mesma chave após resposta incerta e fechamento/reabertura da modal.
3. Pacote integrado `bootJar -PincludeFrontend` reconstruído e servido em 8080. V9 aplicada ao MySQL local após backup privado em `.local/`; nova inicialização validou as nove migrations sem reaplicação. Seis tabelas e 47/47 campos com comentários conferidos.
4. Snapshot privado confirmou preservação integral de carteiras, vínculos e instituições. No navegador: abertura BTC, consulta por duas carteiras sem duplicação, correção, cancelamento e consulta de versões após reinício. Somente as duas versões temporárias criadas para esse ensaio foram removidas; dados cadastrados pelo usuário durante a conferência foram preservados.
5. Instantes usam `Instant`; teste lê também o valor SQL em UTC para impedir deslocamento duplo de fuso. Escritas usam bloqueio do ponto de partida e READ_COMMITTED. Primeira inserção concorrente, mesma chave e disputa correção/cancelamento foram exercitadas.

O MySQL emitiu aviso de depreciação da expressão BINARY usada na guarda de compatibilidade do catálogo da V9. A migration concluiu; o aviso não foi suprimido nem a versão aplicada reescrita. Não há publicação/CI desta alteração nesta tarefa.

Conclusão exige registrar/corrigir/cancelar e consultar saldos reais no MySQL; custos e quantidades preservados; IDs únicos nas visões; nenhum fato duplicado por carteira ou repetição; nenhum acesso a dados de outro usuário. Publicação Git/PR e CI são etapas posteriores, quando solicitadas.

Compra/venda, aporte, retirada, transferência, conversão, taxas, câmbio automático, importação, cotações, relatórios, motor completo e autenticação externa permanecem fora desta primeira fatia. Antes de compras/vendas, especificar prévia/confirmação, custódia, arredondamentos de baixas/resíduos e recálculo retroativo usando os mesmos fatos.

## Aprendizados de planejamento

Critério de aceite é comportamento observável: registrar, repetir, corrigir, reiniciar e obter números conhecidos. Dependências como ativos e data comum entram na fatia porque tornam o fluxo utilizável. Idempotência evita duplicação por falha de rede; unicidade de negócio evita duas aberturas vigentes; são proteções diferentes. Versionamento preserva o que foi informado; arquivamento de uma visão não apaga fatos financeiros. Restringir o escopo evita construir tipos de operação e tabelas que ainda não são necessários.

## Referências técnicas consultadas

O MySQL permite múltiplos NULL em índices únicos, fundamento de manter versões históricas fora da chave da confirmação vigente: [CREATE INDEX — MySQL 8.0](https://dev.mysql.com/doc/refman/8.0/en/create-index.html). A combinação de constraint e ciclo de vida foi exercitada em MySQL descartável.

Divisão com escala/modo de arredondamento explícitos e representação decimal exata: [BigDecimal — Java 27](https://docs.oracle.com/en/java/javase/27/docs/api/java.base/java/math/BigDecimal.html). A escolha de 36 casas intermediárias é uma decisão desta fatia do projeto, não uma regra financeira imposta pela biblioteca.

[Produto](01-produto.md) · [Arquitetura](02-arquitetura.md) · [Roteiro](03-entregas.md) · [Validação](04-validacao.md) · [Modelagem](10-modelagem-de-carteiras.md)
