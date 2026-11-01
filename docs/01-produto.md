# Produto e escopo do MVP local

## Problema e hipótese

Manter operações em controles dispersos dificulta entender custos e resultados e organizar informações para o contador. A hipótese é que registrar uma vez e conseguir rastrear os números até cada operação economiza trabalho e aumenta a compreensão do histórico.

Fluxo de valor: **entrar → cadastrar titulares e contas → registrar saldos iniciais e operações → entender resultados → comparar com extratos → exportar informações**.

## Decisões confirmadas

| Tema | Decisão |
| --- | --- |
| Participantes | Jaime e Eliane, alternando no mesmo computador. |
| Ambiente | Somente local; publicação em nuvem fica para outra etapa. |
| Acesso | Usuário e senha recebidos na tela, validados por serviço mockado no frontend com Jaime e Eliane e senha vazia. Autenticação real será externa; backend de negócio sem gestão de usuários. |
| Organização | Cada usuário tem seu espaço, com possibilidade de vários titulares e contas. Sem compartilhamento de espaço. |
| Contas | Poucas, aproximadamente 4–5; Binance, Bybit e Bitget são exemplos iniciais. Treasure/DeFi fica fora. |
| Dados | Livro novo e entrada manual; não importar o JSON antigo nem CSV de corretoras nesta entrega. |
| Volume de referência | Até 5.000 operações para dimensionamento inicial. Interpretação provisória: total do piloto; revisar se necessário. |
| Ponto de partida | Recorte real pequeno, extratos e custos históricos disponíveis. |
| Saldo inicial | Exigir custo histórico conhecido; desconhecido não pode virar zero. Zero explicitamente informado deve ser confirmado e documentado. |
| Correções | Preservar versões anteriores e autoria, recalcular e recusar alterações que deixem saídas posteriores sem saldo. |
| Entrega funcional | Incluir as cinco áreas aprovadas, construídas gradualmente. |
| Prazo e equipe | Sem data fixa; um desenvolvedor, trabalhando conforme disponibilidade. |

## Capacidades da primeira entrega

| Área | Capacidade e evidência de aceite |
| --- | --- |
| Acesso | Login com Jaime ou Eliane pelo serviço mockado do frontend e saída para trocar de perfil. Sem chamadas ao backend. Contrato externo e separação dos cadastros serão detalhados depois. |
| Cadastros | Cadastrar e consultar titulares e contas. Evitar variações de texto que criem contas involuntariamente; arquivar sem apagar o histórico. |
| Operações | Registrar saldo inicial, aporte, compra, venda, conversão, retirada, transferência própria e pagamento de imposto. Validar e persistir antes de apresentar sucesso. |
| Visão geral | Mostrar saldos a custo, resultados e taxas com rastreabilidade. Filtrar o período sem perder o custo formado antes dele. |
| Correções | Consultar versões anteriores, corrigir por substituição e cancelar com motivo; histórico e resultados permanecem coerentes após reinício. |
| Conferência | Registrar quantidade de extrato por titular/conta/ativo/data; mostrar diferenças e pendências e recalculá-las após correções. |
| Organização fiscal | Compilado mensal, posições anuais a custo, pagamentos registrados, CSVs e relatório HTML para impressão/compartilhamento. |
| Dados e backup | Exportar os dados do próprio espaço e comprovar recuperação. Distinguir exportação de dados de backup técnico do MySQL. |

As sete imagens e o protótipo são a referência visual. Login, cadastros estruturados, histórico de alterações e estados de erro/carregamento/vazio precisam de especificação complementar. Textos de armazenamento local no navegador e ações de restaurar demonstração não devem ser copiados para o livro real.

## Limites

- Sem importação CSV, sincronização de APIs, migração do backup v3 ou cotações ao vivo.
- Sem DeFi, staking, rendimentos, derivativos, alavancagem ou transferências entre titulares.
- Sem recomendações de investimento, cálculo de imposto devido, emissão de DARF ou entrega de declarações.
- Sem cadastro público, recuperação automática de senha, convites, permissões compartilhadas ou publicação em nuvem.
- País e referências ajudam na conferência; não determinam automaticamente o enquadramento tributário.
- Quantidade coincidente não comprova custo correto ou completude do histórico. Referências repetidas são suspeitas para revisão, não duplicatas automaticamente descartáveis.

## Como avaliar valor

Usar um recorte real com saldo inicial documentado, negociar, conferir um fechamento e preparar um relatório. Repetir a atualização em sessões posteriores para avaliar manutenção, não apenas o primeiro cadastro.

Registrar tempo de cadastro e atualização, erros encontrados, esforço para explicar um resultado, utilidade do relatório e dependência de ajuda. Comparar com o controle atual dos participantes.

Critério qualitativo para avançar: os dois conseguem manter seu recorte, rastrear os resultados e conferir saldos; as divergências têm explicação; o esforço de manutenção é considerado aceitável. Uma avaliação assistida pelo desenvolvedor deve ser identificada como tal.

Isso valida utilidade local. Não comprova demanda de mercado, disposição a pagar ou adequação para usuários sem assistência. Esses testes pertencem a uma etapa posterior.
