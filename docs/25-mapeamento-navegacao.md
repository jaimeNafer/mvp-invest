# Mapeamento da navegação do Lastro

## Fase 1 — inventário

Hoje o menu tem Visão geral, Meu perfil, Carteiras e Operações. A área Operações abre em Importações e alterna entre o histórico de conciliação, Negociações e Saldos iniciais. `Home` e `Operacoes` guardam a seleção em estado React.

| Página atual | Funcionalidades existentes | Responsabilidade e destino propostos | Componentes reutilizáveis | Ajuste e risco de compatibilidade |
| --- | --- | --- | --- | --- |
| Visão geral | Custo, resultado, taxas, aportes, patrimônio diário, posições e avisos de conferência | Visão geral concentra acompanhamento financeiro. Ação de conferência leva à fila de exceções | `VisaoGeral`, `PatrimonioDiario`, `ComposicaoCustos`, `SaldosVisaoGeral` | Resumo, avisos e série diária hoje recebem datas. Aplicar o escopo de instituições da carteira nos três fluxos e preservar a consulta global, inclusive para instituições sem carteira vinculada. |
| Carteiras | Criar, editar, arquivar e reativar carteiras. Editar vínculos com instituições | Carteiras concentra organização e fornece atalhos para a visão financeira, o histórico e a importação no contexto selecionado | `Carteiras`, `CarteiraForm` e catálogo de instituições | Cadastro exige pelo menos uma instituição. Orientar nome e seleção de instituição no mesmo formulário, mantendo uma única gravação. Remover vínculo ou arquivar mantém o histórico global. |
| Operações → Negociações | Consultar, cadastrar, corrigir e cancelar operações. Filtrar por carteira. Consultar posição atual | Operações concentra histórico econômico e manutenção. Posição atual fica na Visão geral | `Negociacoes`, `OperacaoForm`, `DetalheOperacao` e filtros existentes | A listagem atual pagina entidades `Operacao`. Grupos progressivos vêm da timeline. Uma projeção de leitura deve combinar as duas origens antes de filtrar, ordenar e paginar, preservando identificadores e evitando a duplicação entre grupos e pernas. |
| Operações → Saldos iniciais | Consultar, cadastrar, corrigir e cancelar o ponto de partida | Ação contextual em Operações, com fluxo próprio para saldos iniciais | `SaldosIniciais`, `SaldoInicialForm` | Preservar regras de custo, data, versão e cancelamento. |
| Operações → Importações | Enviar CSV/XLSX, consultar recibo, conferir registros, aplicar efeitos e pedir cotações | Importações recebe arquivos e apresenta resultado e histórico. Conferência resolve exceções. Operações consulta o histórico econômico aplicado | `RecebimentoHistorico`, `Historico`, `useHistorico`, `PreviaConciliacao`, `RegistroHistorico`, `AssociacaoRegistros`, `ValoracaoHistorico` | O envio progressivo tem apenas POST de recebimento. A listagem de `Importacoes` usa um contrato anterior com persistência própria. Expor recibos progressivos em leitura, preservar os recibos anteriores e usar a proveniência existente para ligar arquivos e operações. Manter confirmação global, retentativa e recuperação do comando atual. |
| Meu perfil e rodapé | Dados do perfil e saída | Meu perfil e Sair no rodapé | `Home` e fluxo de saída existente | Manter proteção durante comandos e preservar autenticação atual. |

## Modelo vigente e decisão necessária

Carteiras agrupam instituições. Quando duas carteiras incluem a Binance, ambas exibem o mesmo histórico da Binance daquele usuário. A consulta por carteira existente resolve seus vínculos para um conjunto de instituições. O histórico financeiro pertence ao usuário e à instituição. Arquivos progressivos preservam sua identidade de origem e não têm carteira proprietária.

**Opção recomendada — carteira como contexto de navegação.** Selecionar a carteira na importação limita as instituições elegíveis e permite voltar com o filtro aplicado. A origem da operação continua sendo a instituição e o arquivo. As operações de uma instituição compartilhada aparecem nas carteiras vinculadas e entram uma vez no consolidado. Registros anteriores continuam acessíveis sem atribuição retroativa.

**Opção com atribuição exclusiva.** Associar cada operação a uma carteira proprietária muda a regra vigente para instituições compartilhadas. Exige definir o que acontece com operações e arquivos antigos, reimportações em outra carteira e remoção de vínculos. A implementação requer modelagem e migração incrementais após essas decisões.

## Navegação proposta

**Visão geral · Carteiras · Operações · Importações · Conferência**, com **Meu perfil** e **Sair** no rodapé. Os atalhos passam o contexto por parâmetros de navegação para carteira, arquivo ou registro. A conferência abre nas exceções e mantém o acesso contextual aos registros relacionados. Depois da confirmação, o recibo conduz a Operações ou às pendências existentes.

## Decisão e execução

Manter a carteira como contexto por instituições e usar o consolidado global como acesso aos registros legados.

1. Separar as cinco áreas em `Home`, preservar o bloqueio dos comandos e transportar o contexto por parâmetros de navegação.
2. Reutilizar a projeção da conferência no histórico econômico. Filtrar e paginar uma única sequência com as operações persistidas, grupos e registros importados aplicados.
3. Aplicar o conjunto de instituições da carteira ao resumo e à série diária no backend. Calcular com o mesmo livro.
4. Separar envio e recibos de arquivos das ações de conferência. Manter confirmação global e acesso aos dados de origem.
5. Verificar navegação, primeiro acesso, escopo compartilhado, histórico aplicado e regressões dos fluxos existentes.

## Contratos e compatibilidade

- `GET /api/v1/operacoes/historico` retorna uma sequência paginada de operações cadastradas e registros importados aplicados. Um grupo aplicado aparece uma vez e inclui suas linhas originais. Aceita os filtros de operação e `arquivoId`. A listagem anterior e os comandos de cadastro, substituição e cancelamento continuam disponíveis.
- `GET /api/v1/importacoes/recebimentos` lista os metadados paginados dos recebimentos progressivos do perfil atual, sem carregar o conteúdo dos arquivos. Os recibos de relatórios anteriores continuam disponíveis em uma seção recolhida. O histórico de recebimentos mostra data, instituição e número de registros. O sistema ainda não registra um estado final por arquivo nem conserva a carteira escolhida no envio como atributo do arquivo.
- `GET /api/v1/conciliacao/timeline` aceita `estado=PENDENCIAS` para a fila de trabalho e `registroId` para abrir uma operação específica com suas linhas relacionadas. A confirmação global permanece no fluxo existente e pode ser aberta a partir do recibo de importação.
- As consultas de Visão geral, avisos e patrimônio diário aceitam `carteiraId` opcional. A seleção usa apenas vínculos do perfil autenticado. O consolidado segue lendo todos os fatos do perfil, inclusive os de instituições sem vínculos ativos.
- O primeiro cadastro reúne nome e instituição no mesmo formulário. O processamento de arquivos desta jornada usa os formatos Binance já suportados. Carteiras podem reunir outras instituições para consultar e registrar operações.
