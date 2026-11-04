# Aporte, compra e venda — plano aprovado em 03/11/2026

## Objetivo e limites

Ciclo manual completo: aporte BRL, compra e venda spot. Criptoativos do catálogo, conforme [elegibilidade vigente](17-catalogo-de-ativos.md); cotação BRL/USDT/USDC, moedas diferentes. Taxa em uma das duas moedas. Quantidade e total bruto do extrato são fatos; preço é derivado. Cotação histórica BRL informada para stablecoins, BRL=1. Data/hora Fortaleza e persistência UTC. Sem taxa em BNB como terceira moeda, transferência, retirada, importação ou tributação.

## Regras

Motor Java único de replay: saldo inicial, aporte, compra e venda. Custo por usuário/ativo; custódia por instituição/ativo. Ordem por instante e ordem estável da raiz da operação. Quantidades/câmbio 18 casas; BRL8; divisão36 HALF_EVEN; saída integral remove custo residual inteiro. Nunca tolerar quantidade negativa. Compra taxa quote: custo bruto+taxa, débito quote de ambos. Compra taxa base: crédito líquido Q-taxa com custo bruto. Venda taxa quote: crédito líquido, receita bruta-taxa. Venda taxa base: débito Q+taxa e receita bruta. Diferença BRL na saída de stablecoin separada do resultado da negociação e da taxa, sem dupla dedução.

Prévia não grava nem reserva; confirmação compara revisão, recalcula e grava sob bloqueio do ponto de partida/READ_COMMITTED. Corrigir substitui versão; cancelar preserva fatos; replay candidato inválido reverte tudo. Idempotência reconhecida antes de revisão; repetições não incrementam revisão. Todas escritas inclusive saldos iniciais participam do replay/revisão.

## Persistência e API

V10 preserva V1–V9, fatos e cadeias. Acrescenta campos de cotação/taxa/referência/ordem/política em operacoes e revisão em pontos_partida. Unicidade de abertura apenas SALDO_INICIAL confirmado. custo_total_brl permanece fato de saldo inicial, nulo nos novos tipos. Projeções calculadas, sem tabela de resultados.

Novas rotas /api/v1/operacoes: GET paginado (situacao CONFIRMADA/TODAS/SUBSTITUIDA/CANCELADA, tipo, instituicaoId, ativoId, carteiraIds, pagina=0,tamanho=50 max100); GET /{id}; POST /previa; POST raiz; POST /{id}/substituicoes; POST /{id}/cancelamento. GET /api/v1/posicoes. IDs e decimais strings; revisão string. GET list retorna {itens,total,pagina,tamanho,revisao,dataReferencia}; posições {revisao,itens}. Respostas enriquecidas com metadados, sem entidades.

CadastroOperacao: {tipo:APORTE|COMPRA|VENDA,instituicaoId,ativoId,ocorridaEm:ISO-offset,quantidade:string,moedaCotacaoId?:string,totalBruto?:string,cotacaoBrl?:string,taxaAtivoId?:string,taxaQuantidade?:string,referencia?:string,motivo?:string}. APORTE: ativo BRL, sem campos de negociação/taxa. Prévia: {acao:CRIAR|SUBSTITUIR|CANCELAR,operacaoId?,operacao?:CadastroOperacao,motivo?}. Confirmação criar/substituir: {revisao:string,operacao:CadastroOperacao}, Idempotency-Key UUID. Cancelar: {revisao:string,motivo}. Prévia retorna {revisao,operacao,calculo,posicoes}; operação contém fatos normalizados, nomes, tipo/situação, substituiId/autoria; calculo com bruto/taxa/custo baixado/parcelas/total BRL. Histórico inativo não apresenta cálculo vigente.

Saldo inicial mantém rotas/payloads, consultas restritas ao tipo. Correções/cancelamentos também validam o livro inteiro. Novos endpoints não corrigem saldo inicial: sua tela existente permanece responsável. Confirmações novas exigem revisão; APIs legadas recalculam sob lock sem exigir campo novo.

## Etapas e verificação

1. Motor e fixtures de domínio. 2. Migration/modelos/repos/API e integração com aberturas. 3. Frontend com prévia obrigatória, proteção de respostas antigas, confirmação/repetição, histórico e posições. 4. Testes API/MySQL, migração histórica, concorrência, frontend, pacote e navegador. 5. Atualizar roteiro/dicionário/retomada.

Aceite: BRL e stablecoins; quatro taxas; saldo insuficiente; saída integral; carteira sem duplicidade; usuário isolado; correção/cancelamento retroativo e rollback; revisão obsoleta; timeout repetido; V10 com dados existentes; 5000 operações sintéticas. Sem limpeza de dados ou alteração de migrations anteriores. Sem publicação automática.

## Contrato implementado e escolhas de integração

Novas confirmações recebem {revisao,operacao}; cancelamento {revisao,motivo}. Revisões, IDs e decimais trafegam como strings. Preço unitário derivado no domínio com 36 casas é enviado em precoUnitario, sem mudar o total bruto informado. OperacaoResponse inclui criadoPor/alteradoPor além dos instantes; versões históricas sem efeito vigente retornam calculo nulo, sem inventar resultado original.

A listagem inclui os quatro tipos e filtra por instituição, ativo envolvido (principal/cotação/taxa), tipo, situação e união de carteiras, sem duplicação. A posição atual é global, com custódia; filtros da lista não alteram o custo do usuário. As APIs legadas continuam restritas a saldos iniciais. Elas validam todo o livro sob o mesmo bloqueio e incrementam revisão; não exigem novo campo de revisão em seus contratos anteriores.

Primeiro aporte define o ponto de partida quando inexistente; prévia nunca cria essa linha. Aportes usam BRL sem taxa, com custo igual à quantidade. Novas operações anteriores à referência ou no futuro são recusadas. Instantes normalizados em microssegundos; empates novos usam próxima ordem, substituições herdam ordem da raiz. Referência repetida não exclui eventos.

A política 1 arredonda bruto em BRL e valor da taxa para 8 casas, sem reduzir a centavos. Compra com taxa na moeda de cotação separa as baixas principal e da taxa; venda separa resultado bruto e efeito da taxa, somando as parcelas uma única vez. custoBaixadoBrl representa o custo de todas as saídas do evento, inclusive moeda utilizada na compra. Saída integral elimina o resíduo de custo; custo médio derivado mantém 36 casas sem MathContext global. Resultados monetários fora de DECIMAL(38,8) são recusados, sem truncamento.

A interface preserva o formulário de saldos iniciais em área própria. Negociações têm prévia após 350ms, descarte de respostas antigas e confirmação explícita. Erro preserva campos; resposta incerta bloqueia edição/fechamento e permite repetir o mesmo corpo e chave. Conflito exige nova prévia e outro clique, sem confirmação automática.

## Verificação e operação local

Evidências finais registradas abaixo. V10 é nova; V1–V9 permanecem imutáveis. A aplicação local foi precedida por backup privado e seguida de comparação dos fatos anteriores. Dados sintéticos dos ensaios ficam em Testcontainers, sem limpeza ou inserção de demonstração no histórico real.

O limite padrão de recursão do MySQL exige cuidado ao preservar cadeias longas de substituição: [WITH e limites de CTE recursiva — MySQL 8.0](https://dev.mysql.com/doc/refman/8.0/en/with.html). A validação da migration inclui histórico acima de mil versões.

A V10 ajusta temporariamente o limite de recursão da sua sessão ao número de fatos existentes e restaura o valor anterior após a propagação da ordem. Não exige mudança global no servidor. O teste de 1.105 versões reproduziu a falha com o limite padrão antes desse ajuste.

## Entrega de negociações concluída — 03/11/2026

Implementados aporte em BRL, compra e venda com BRL/USDT/USDC como cotação, taxas nas moedas do par, prévia/confirmação pelo motor Java, revisão/idempotência, histórico por versões, correção/cancelamento e posição atual global com custódia. Saldo inicial continua acessível e participa do mesmo recálculo ao ser alterado. [Contrato e regras](16-negociacoes.md).

Validação: backend check aprovado com **104 testes** (55 unitários/arquiteturais e 49 integrações MySQL), incluindo 5.000 eventos sintéticos e V9→V10 com 1.105 versões históricas. Frontend aprovado em lint, **44 testes**, typecheck/build; protótipo preservado com **1 teste** aprovado. Pacote integrado reconstruído com o bundle final.

V10 aplicada no MySQL local após backup privado. Comparação de snapshots das seis tabelas confirmou preservação integral dos campos e fatos anteriores; comentários dos campos conferidos, sem ausências. Reinicialização validou dez migrations sem reaplicação. Aplicação integrada disponível em http://127.0.0.1:8080/.

No navegador: acesso, listagem unificada, posição atual, abertura do formulário, campos específicos de aporte e prévia calculada sem gravação. Consultas novas e legadas retornaram 200 para os dois perfis. Nenhuma operação fictícia foi gravada no banco pessoal. Escritas/correções/cancelamentos e concorrência foram exercitados em MySQL descartável nos testes.

Revisões corrigidas: limite temporário da CTE na migração; autoria no DTO; proteção contra saída no primeiro POST pendente; rejeição de consulta com revisões divergentes; moeda negativa HALF_EVEN e duas casas com valor exato acessível. Nenhum achado material aberto dessas revisões.

Branch de publicação: codex/negociacoes-livro. Alterações do backlog anteriores incluídas na entrega; publicação do PR solicitada por Jaime, sem merge automático. BNB permanece confirmado para o futuro. Próximo passo de produto: validar um recorte real do ciclo e escolher os complementos do backlog com base nessa conferência.
