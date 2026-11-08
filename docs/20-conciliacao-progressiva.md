> Estado em 06/11/2026: primeiro ciclo implementado e verificado. O texto abaixo preserva as decisões da revisão. Contrato físico/HTTP em [21](21-contrato-conciliacao.md) e evidências/limites na [retomada](06-retomada.md).

# Importação com conciliação progressiva e timeline — plano revisado

Plano revisado por Jaime em 06/11/2026 e implementado no primeiro ciclo descrito no estado acima. Este documento preserva as decisões aprovadas; contratos e limites efetivos estão em [Conciliação progressiva](21-contrato-conciliacao.md). O fluxo CSV anterior permanece descrito em [Importação CSV](19-importacao-binance.md).

## 1. Objetivo e decisões preservadas

Histórico incompleto será um estado suportado. O investidor poderá receber o que possui, aproveitar as informações disponíveis e resolver lacunas progressivamente, sem exigir reconstrução integral do passado.

- Trabalhar com os XLSX disponíveis, sem exigir novos comprovantes como condição de entrada.
- Preservar registros legíveis e originais, inclusive pendências e conflitos. Arquivos ilegíveis ou inseguros continuam recusáveis.
- Quantidades observadas podem compor quantidades registradas em conferência quando sua interpretação e identidade forem suficientes. Não confundir esse retrato com posição externa comprovada.
- Incluir ajustes provisórios de BRL e cripto, sugeridos na prévia e aceitos em lote. Não criá-los silenciosamente.
- Não presumir custo, taxa, cotação ou natureza econômica. Custo desconhecido não equivale a zero.
- Reaproveitar o motor, componentes, arquitetura e contratos públicos existentes. Não criar segundo motor, workflow genérico, biblioteca de grafos, filas ou novos contextos por antecipação.

## 2. Semântica visível e revisão manual

| Estado automático | Critério |
| --- | --- |
| Verde — Coerente | Passou nas verificações aplicáveis e não depende de lacuna conhecida. Não comprova cobertura integral, autenticidade externa ou regularidade fiscal. |
| Amarelo — Com pendência | Falta informação, classificação ou associação; há ajuste provisório ou dependência de uma lacuna. |
| Vermelho — Precisa de correção | Um conflito impede aproveitar algum efeito com segurança, como data inválida ou saldo insuficiente ainda não explicado. |

Cor sempre acompanha texto. Classificação não se confunde com CONFIRMADA/SUBSTITUIDA/CANCELADA nem com origem importada/manual/sintética.

"Revisada por você" é uma anotação separada, com usuário, instante e versão revisada. Não muda automaticamente a cor ou a disponibilidade dos cálculos. Mudanças posteriores preservam o registro da revisão anterior, sem apresentá-la como revisão da nova versão.

"Resolver depois" mantém a pendência e os impactos; não confirma valores, não descarta registros e não equivale a aceitar um ajuste. Não implementar snooze, notificações ou agenda para essa ação.

## 3. Identidade e granularidade de custódia

Na linguagem do produto, titular é o proprietário dos dados. No modelo atual, ele é identificado por IUsuarioAtual.id()/perfil_ref. Não recriar a antiga entidade titulares, nem usar o identificador do extrato como autenticação.

A entidade contas atual é associação carteira/instituição. Ela não será reutilizada para Spot/Funding. Introduzir o conceito específico de conta de custódia no contexto operacoes, com proprietário, instituição e identidade de origem, sem tela de cadastro independente nesta entrega. Importação identifica a conta; conferência permite resolver associações ambíguas.

- Quantidade e déficit: por usuário, conta de custódia e ativo.
- Custo médio: global por usuário e ativo, preservando a política existente.
- Carteiras: continuam visões da união de instituições, sem propriedade sobre fatos ou duplicação de histórico.
- Operações antigas sem conta explícita ficam em custódia "Não especificada" na sua instituição. Não presumir Spot. Manter somas institucionais e custos anteriores; uma conciliação posterior poderá identificar a conta.
- Uma venda Spot não utiliza automaticamente quantidade Funding ou de custódia não especificada, mesmo que o total institucional seja suficiente.

Antes de sugerir saldo provisório, consultar registros existentes e apontar possíveis transferências ou representações repetidas. Evidências incluem identificação do movimento, natureza declarada e contas de origem/destino; horário e quantidade isolados não efetivam associação.

O primeiro incremento inclui associação restrita de duas pernas observadas de transferência interna, do mesmo usuário/instituição/ativo e contas distintas, com quantidades opostas equivalentes e confirmação explícita. Essa associação move custódia e preserva custo global, sem gerar aporte ou lucro. Diferenças de quantidade/taxas desconhecidas permanecem pendentes; não expandir para transferências entre instituições, rede ou cálculo de taxas de rede.

## 4. Receber, aproveitar informações e calcular

Recepção conserva os arquivos e os registros antes de completar a conciliação financeira. Isso substitui, no novo fluxo, a regra anterior de persistir somente arquivos financeiramente confirmados. O fluxo CSV legado permanece compatível.

Cada informação participa somente dos efeitos que sustenta:

| Informação ou problema | Aproveitamento |
| --- | --- |
| Conta, ativo, direção, data e quantidade identificados | Movimentação de quantidade registrada, com a qualificação de origem/cobertura aplicável. |
| Entrada cripto sem custo | Quantidade conhecida; custo e seus dependentes permanecem incompletos. |
| Receita de venda conhecida sem custo da saída | Receita disponível; custo utilizado e resultado indisponíveis. |
| Natureza ainda desconhecida | Não inferir compra, venda, aporte, retirada, rendimento ou lucro a partir do sinal da quantidade. |
| Data inválida | Registro preservado, em grupo sem data resolvida; sem posição temporal arbitrária. |
| Sobreposição ambígua | Preservar ocorrências e multiplicidade; não adicionar novo efeito duplicado enquanto a identidade estiver em revisão. |
| Déficit sem ajuste aceito | Conservar registro e diferença a explicar; não apresentar saldo impossível como confiável nem completar o saldo silenciosamente. |

Disponibilidade de quantidade, custo, taxa e resultado é separada da cor geral. Um ajuste pode permitir processar quantidades e ainda deixar custo e resultado pendentes.

A observação original fica intacta. Agrupar linhas em uma operação financeira substitui seus efeitos individuais, sem contar observações e operação consolidada ao mesmo tempo. Desfazer a associação também exige prévia dos efeitos.

Arquivos repetidos são reconhecidos pelo hash; identidade econômica e sobreposição são avaliadas separadamente. Linhas idênticas podem ser execuções legítimas. Não descartar multiplicidade por coincidência de data, moeda e quantidade.

Limites iniciais do novo recebimento: 20 arquivos, 10 MiB somados e 10.000 registros, configuráveis em YAML. Leitura XLSX específica ao formato auditado, sem executar fórmulas ou vínculos externos e sem converter decimais por double. Manter proteções de descompactação. Ativo desconhecido não é cadastrado automaticamente.

## 5. Ajuste como lacuna, não como fato econômico

Exemplo de explicação:

> Faltam registros de entrada de 0,03 BTC para explicar esta venda.
>
> Ajuste provisório: 0,03 BTC, com custo desconhecido.

O ajuste representa uma hipótese técnica de quantidade anterior ausente. Não comprova compra, aporte, transferência, origem de recurso ou saldo externo real.

Persistir: identidade da lacuna, proprietário, conta de custódia, ativo, quantidade, registro-âncora, origem da sugestão, aceite pelo usuário e versões. Operações/indicadores afetados serão derivados pelo processamento na revisão correspondente; não persistir listas redundantes que possam ficar desatualizadas.

Não inventar ocorridaEm de transação. Distinguir instante de criação/aceite, âncora e posição técnica no processamento. Na timeline, apresentar antes da dependência: "Inserido para explicar saldo anterior". Não forçar o ajuste a caber no contrato de uma compra ou aporte com data real obrigatória.

Sugerir somente o déficit mínimo necessário, depois de procurar correspondências. Uma associação confirmada que explique o saldo tem precedência sobre a criação do ajuste. Havendo apenas hipótese não confirmada, oferecer associação ou continuidade provisória, deixando a incerteza explícita.

BRL tem equivalência nominal em reais, mas seu ajuste não vira capital aportado. Cripto sem custo informado permanece sem custo; não usar zero ou cotação atual. Saldo anterior informado pelo usuário é alternativa à reconstrução do passado, com origem manual e disponibilidade do custo registradas.

### Ciclo de vida determinístico

1. Recalcular a necessidade a partir dos registros observados, reais ou informados pelo usuário, sem usar ajustes anteriores para justificar a própria necessidade.
2. Gerar propostas de quantidade mínima em ordem estável e correlacioná-las às lacunas já registradas. A identidade usa proprietário/conta/ativo e âncora estável, não o valor ou instante de execução do recálculo.
3. Comparar propostas com ajustes ativos: manter, reduzir, substituir, aumentar ou encerrar. Reprocessamento inalterado não cria versão, ajuste ou revisão adicionais.
4. Exibir todas as mudanças de ajustes aceitos antes da confirmação. Entrada real nova e redução/encerramento do ajuste pertencem ao mesmo comando transacional.
5. Preservar versões anteriores, mas retirar seu efeito vigente. Um ajuste aceito não permanece ativo se deixar de ser necessário.

Prévia é somente leitura. Confirmação usa revisão, idempotência e bloqueio existentes; reconhece repetição antes de rejeitar revisão antiga e confere novamente após serialização. Mudança do histórico exige nova prévia e novo aceite explícito; nunca aplicar silenciosamente uma proposta recalculada diferente.

Rejeitar proposta não apaga o registro de origem e não interrompe o restante da ferramenta. O efeito que continua sem base fica pendente, com seus dependentes identificados.

## 6. Custos desconhecidos e indicadores

Preservar o motor único e a política decimal/UTC. Custo desconhecido não entra em soma, divisão ou preço médio como zero. Enquanto a posição global do ativo tiver custo desconhecido, seu custo médio e as saídas dependentes ficam incompletos. Receita conhecida continua visível.

Ao zerar integralmente a posição, compras posteriores com custo conhecido podem formar nova base conhecida; resultados antigos incompletos não passam a ser conhecidos. Correção da origem reprocessa os dependentes. Saldo inicial existente não pode ser contado novamente ao receber observações anteriores à abertura.

Ajustes sintéticos não entram em aportes, receitas, negociações reais, taxas reais, resultados ou sua contagem. Sua posição técnica não gera crescimento patrimonial atribuído a uma operação real. Gráficos e saldos que usam sua quantidade recebem "Com ajustes provisórios".

Separar subtotal calculável de total completo. Se faltar base para o total, não apresentar o subtotal como patrimônio integral. Qualificar cada indicador afetado e concentrar a explicação num resumo, evitando avisos repetidos em todos os componentes. Composição de custo e séries parciais devem explicar sua base.

O percentual de registros coerentes será secundário, sem alegação de acurácia financeira. Considerar registros atuais canônicos da timeline, excluindo ajustes sintéticos, cópias comprovadas e versões inativas. Não deduplicar registros para melhorar a porcentagem. Agrupamento visual não altera o denominador; a conciliação que resolve identidades/eventos pode alterá-lo e atualiza os contadores.

Dar destaque ao impacto: "3 pendências afetam o custo de BTC e o resultado de 2 vendas". Contar registros/indicadores afetados sem prometer que todos serão resolvidos por uma única correção quando houver outras causas.

Prioridade de "Próxima pendência": maior número de registros afetados, depois gravidade, depois a âncora mais antiga e identificador estável. Reusar os vínculos apurados pelo replay; não criar um simulador de pontuação ou grafo genérico. A timeline mantém ordem cronológica; o botão navega para a pendência prioritária e explica o critério.

Relatórios fiscais não existem no escopo atual. Registrar desde já que futuras exportações separarão fatos, ajustes e pendências; não construir relatório/exportador fiscal nesta entrega nem prometer aptidão fiscal dos dados.

## 7. Experiência compacta

Uma timeline principal em Operações, com filtros por estado, ativo, instituição/conta e ação Importar. Paginação, agrupamento por dia e resumo de pendências. Estados acompanhados de texto e controles acessíveis por teclado/toque.

Detalhes e correções expandem na própria linha. Reaproveitar formulários existentes, sem criar páginas intermediárias ou modais encadeadas. Oferecer poucas ações contextualizadas: Informar saldo anterior, Associar a um registro, Corrigir dados e Resolver depois. A ação de revisar é separada da resolução financeira.

Agrupar visualmente dependências da mesma lacuna, preservando identificadores, origens e detalhes de cada registro. Agrupar pendências semelhantes não permite presumir causa ou custo únicos.

Na Visão geral: resumo de impactos e acesso direto à timeline filtrada. Ajustes e informações ainda desconhecidas têm tratamentos distintos: quantidade pode estar calculável com origem provisória, enquanto custo/resultado continuam indisponíveis.

## 8. Arquitetura, contratos e dados

- importacoes cuida do arquivo, leitura e proveniência; operacoes possui registros normalizados, contas de custódia, lacunas, ajustes e cálculo. Consumir contratos públicos sem dependência inversa/ciclo.
- Acrescentar models/repositories/usecases/DTOs/mappers necessários nos contextos existentes. Interfaces I, Lombok, tipos em arquivos próprios e validação estrutural seguem os acordos vigentes.
- Separar fatos, decisões e qualificações derivadas. A anotação manual de revisão não é um setter de estado financeiro.
- Nova V13 aditiva, ou migrations novas sequenciais se a implementação for dividida. Não alterar V1–V12. Backfill legado somente para custódia não especificada; não inferir Spot/Funding nem limpar dados.
- Preservar as respostas do histórico completo. Dados incompletos exigem contratos tipados e explícitos de disponibilidade/subtotais, com atualização conjunta do frontend e sem preenchimento de compatibilidade por zero.

### Capacidades públicas a especificar antes de codificar

| Capacidade | Contrato mínimo |
| --- | --- |
| Receber arquivos | Retornar arquivos/registros recebidos, reconhecidos e com problemas; preservar legíveis e distinguir recebimento de efeito financeiro. |
| Timeline | Lista paginada com origem, estado automático, revisão manual, pendências, impactos e ações disponíveis; dados de conta e âncora sem expor entidades. |
| Salvar conferência | Dados/associações informados, versão e autoria; aceitar incompletude explicitamente, sem promover automaticamente a fato coerente. |
| Prévia de conciliação | Revisão, interpretação das correspondências, efeitos aproveitáveis e mudanças propostas de ajustes, com identidade estável. |
| Confirmar conciliação | Mesma proposta explicitamente aceita, revisão e chave de idempotência; aplicar fatos, associações e versões de ajustes atomicamente. |
| Visão geral/posições/detalhes | Disponibilidade por informação, subtotais calculáveis, uso de ajustes e causas/impactos; sem total completo falso. |

Definir nomes, requests/responses e exemplos dessas rotas no contrato de implementação antes de construir os endpoints. Esta revisão não declara que o contrato HTTP completo já esteja fechado. O CSV legado e seus endpoints atuais permanecem documentados no contrato 19, sem mudança nesta tarefa documental.

## 9. Incrementos e aceite

### Incremento 1 — primeiro ciclo completo

Importar histórico → detectar lacuna por usuário/conta/ativo → procurar correspondência interna → aceitar ajuste BRL ou cripto em lote → consultar impactos → informar/importar entrada real ou saldo anterior → prever redução/encerramento → confirmar e reprocessar.

Incluir persistência, idempotência, versões, motor com informações desconhecidas, UI compacta e impacto no painel. Esse ciclo é o primeiro marco de validação, antes de ampliar facilidades de edição ou heurísticas de associação. Não declarar encerrada a entrega com apenas um leitor de arquivos ou uma demonstração visual.

### Ampliação depois do ciclo validado

Ampliar associações de múltiplas linhas, edição assistida, agrupamentos e ergonomia do histórico completo. As etapas posteriores não dispensam os requisitos essenciais do ciclo inicial: não duplicar, não inventar custos, separar custódias e preservar originais.

### Testes obrigatórios

- BRL e cripto: sugestão mínima, custo ausente, receita conhecida, resultado incompleto e isolamento dos indicadores não afetados.
- Funding com saldo e Spot deficitária: não consumir Funding automaticamente. Correspondência interna confirmada explica o movimento antes de ajuste externo; custo global preservado.
- Legado em custódia não especificada: preservar dados/resultados anteriores e exigir associação explícita quando necessária, sem inventar conta de origem.
- Ajuste aceito reduzido ou encerrado após entrada retroativa. Mesmos dados reprocessados sem multiplicação de ajustes, versões ou revisão.
- Aceite, rejeição e Resolver depois: originais preservados, sem preenchimento silencioso nem saldo impossível apresentado como confiável.
- Quantidade disponível com custo/resultado pendentes; custo zero explícito permanece distinto de desconhecido. Saída integral e retomada de base conhecida.
- Registro revisado manualmente que continua amarelo/vermelho; alteração posterior não herda indevidamente a revisão da versão anterior.
- Ajustes excluídos de aportes, receitas, negociações, resultados e contagens reais; gráficos não atribuem aporte/rentabilidade à posição técnica.
- Prévia obsoleta, comandos concorrentes, repetição após resposta incerta e rollback de fatos/associações/ajustes na mesma transação.
- Sobreposição sem perda de multiplicidade; associação sem efeito duplicado; agrupamento visual preserva vínculos e prioridade explicável.
- Conta/ativo/data inválidos ou não identificados: preservar informação recebida, sem efeito arbitrário. Saldo inicial não contado novamente.
- Isolamento entre perfis, regressão do CSV e dos históricos completos, 10.000 registros, reinício e acessibilidade.

Testes públicos sintéticos, expectativas independentes, MySQL descartável, ArchUnit, lint/build e cobertura existente, sem reduzir mínimos ou ignorar falhas. Ensaio com dados reais somente privado, sem limpeza ou correções automáticas do banco pessoal.

## 10. Avaliação dos ajustes recebidos

Incorporados: semântica Coerente versus revisão manual; ajuste como lacuna; disponibilidades separadas; proibição de distorção de indicadores; ciclo determinístico/idempotente; impacto antes de percentual; ações contextuais e primeiro fluxo completo.

Incorporados com adequação ao Lastro: granularidade por conta exige novo conceito de custódia, pois contas atual tem outra finalidade; titular significa proprietário por perfil_ref, sem recriar titulares. Posição técnica usa âncora, sem fabricar data de transação.

Delimitados para evitar expansão: associação inicial somente de transferência interna observável e confirmada; não implementar motor geral de correspondências. Regras para relatórios fiscais são um limite futuro, sem novos relatórios agora. Disponibilidade e impacto são derivados pelo motor, sem armazenamento redundante de listas.

Contratos HTTP e modelagem física foram definidos no documento 21 para a implementação. O trecho de avaliação acima registra a revisão original; o estado de execução e as verificações estão na retomada.
