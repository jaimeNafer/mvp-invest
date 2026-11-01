# Lastro — clareza e organização para suas contas

> **Próxima etapa: MVP local com React e Spring Boot.** O escopo, as decisões e o roteiro estão no [índice de planejamento](../docs/README.md). O restante deste README descreve o protótipo atual; backend e persistência MySQL ainda não estão implementados.

O Lastro organiza operações de cripto para **entender os resultados e preparar os dados para suas obrigações fiscais**. Registre uma vez, acompanhe custos e saldos e compartilhe um compilado claro com seu contador. A plataforma não substitui a análise de um contador especializado.

## Para quem

Investidores brasileiros de cripto à vista, com poucas contas, operações em reais ou stablecoins e necessidade de separar titulares. O protótipo começa com exemplos fictícios; os lançamentos próprios são manuais.

## O diferencial que estamos validando

**Do total ao lançamento, do lançamento ao documento.**

- **Resultado explicado:** separa vendas de ativos de câmbio, conversões e rede; permite abrir os lançamentos e seus cálculos.
- **Fila de conferência:** aponta país ausente, referência de ordem ausente, custo inicial zero e referências repetidas. O usuário investiga e corrige; não há exclusões automáticas.
- **Comparação com extrato:** registre quantidade, conta, titular, data e referência do documento. O Lastro compara com o saldo calculado no fim desse dia e mostra a diferença. Uma correção posterior no histórico recalcula a comparação.
- **Histórico contínuo:** custos anteriores continuam compondo os resultados mesmo quando a tela mostra só um mês.
- **Titulares separados:** a visão conjunta soma valores sem compartilhar preço médio entre pessoas.

Essa proposta é uma hipótese competitiva, não uma validação de mercado. A utilidade e a disposição de pagar ainda serão testadas com investidores e contadores.

## Organização fiscal: a recompensa pelo registro

Uma tela reúne, por período e titular:

- Resumo mensal de valores vendidos, custo das quantidades vendidas, resultados de vendas, câmbio/conversões/rede, taxas e pagamentos de imposto registrados.
- Operações detalhadas com quantidade, preço, moeda, câmbio histórico, conta, país da instituição, referência, custo e resultado, com acesso ao cálculo.
- Posição anual por ativo e titular, comparando quantidade e custo de aquisição no fim do ano anterior e do ano escolhido, incluindo as contas em que há saldo.
- Pagamentos de imposto com data, mês de referência, origem e comprovante informado.
- CSVs mensal, anual e de operações, além de um relatório HTML autossuficiente para compartilhar ou imprimir pelo navegador.

O período De–Até controla o resumo mensal e as operações. O saldo anual usa **todo o histórico até 31/12 do ano da data Até**. O seletor de ano permite aplicar o ano inteiro ao período. Ano em andamento é uma prévia, não um fechamento definitivo. Ausência de histórico anterior não comprova saldo zero. Saldos são a custo de aquisição; não há enquadramento automático em fichas/códigos de declaração.

Taxas já integram os cálculos. Pagamentos de imposto são registros manuais, nunca uma estimativa de obrigação. Os relatórios não calculam DARF nem entregam declarações.

## Comece por um período que você consiga conferir

1. Registre um saldo inicial com custo de aquisição conhecido ou um aporte.
2. Cadastre compras, vendas, conversões e taxas, com as cotações históricas em reais.
3. Abra **Ver composição** para entender o resultado e **Ver cálculo** para chegar ao lançamento.
4. Em **Conferência**, informe o saldo total de um ativo em uma conta no fim do dia, conforme seu extrato. Inclua quantidades bloqueadas em ordens, não só o saldo disponível.
5. Investigue diferenças e pendências. Uma referência repetida pode ser uma ordem com várias execuções, e não um lançamento duplicado.
6. Abra Organização fiscal, confira o mês ou ano, revise os saldos anuais e baixe o relatório ou CSVs para seu contador. Preserve também um backup JSON.

## O que cada número significa

| Número | Significado |
| --- | --- |
| Custo dos saldos | Custo de aquisição do que ainda está na carteira, incluindo taxas elegíveis. |
| Resultado realizado | Resultado das operações do período; inclui câmbio/conversões e rede. Taxas já consideradas, impostos pagos separados. |
| Taxas | Total informativo de taxas em reais. Não descontar novamente do resultado. |
| Valor de referência | Quantidade pela última cotação registrada nas operações. Pode estar desatualizado; não é valor de mercado. |
| Comparação de saldo | Quantidade do Lastro menos quantidade digitada do extrato, por conta, titular, ativo e data. |
| Pagamento de imposto | Valor pago e cadastrado pelo usuário. Não é imposto calculado ou devido. |

Coincidir quantidade **não comprova custo, completude ou correção fiscal**. Ausência de pendências significa somente que as checagens implementadas não apontaram problemas. O Lastro não acessa nem autentica o extrato digitado.

## Limites desta versão

- Não recomenda ativos, entradas, saídas, stops ou alocações. Estratégias e alertas macro foram retirados da interface.
- Não calcula imposto, não emite DARF e não entrega declarações. O resumo mensal organiza dados para conferência.
- Não possui cotações ao vivo. O gráfico usa referências dos lançamentos e não representa rentabilidade histórica de mercado.
- Não importa arquivos de corretoras nem sincroniza APIs nesta versão. Restaurar JSON é recuperar um backup do Lastro.
- O histórico é local ao navegador: sem conta de usuário, sincronização ou backup automático em servidor. O acesso ao site pode ter uma barreira do provedor de hospedagem, mas isso não sincroniza o livro.
- Cálculos gerenciais em `Number` do JavaScript; precisam de validação contábil independente e aritmética decimal antes de produção.
- Fora do escopo: futuros, alavancagem, staking, rendimentos, pools/DeFi e transferências entre titulares.

A versão de produto é MVP 4. A chave de armazenamento e o formato de backup continuam v3 para preservar compatibilidade. Dados antigos de estratégias/revisões permanecem no backup, sem funcionalidade ativa.

## Executar e verificar

Sem dependências de execução. Sirva a pasta que contém `index.html`, `app.js`, `ledger.js` e `styles.css` por HTTP:

```sh
python3 -m http.server 8000
node --test tests/ledger.test.cjs
```

O motor fica em `ledger.js`; a interface em `app.js`. Os testes cobrem custo médio, taxas, câmbio, contas, titulares, filtros, composição dos resultados, comparações históricas e pendências.

A infraestrutura de hospedagem usa uma cópia desses arquivos em `dist/`.

## Próximo investimento

Validar o fluxo com históricos reais e autorização dos participantes. Priorizar uma primeira importação CSV com prévia e prevenção de duplicações se o esforço manual bloquear o uso. Expandir somente quando houver evidência de tempo economizado, números conciliáveis e uso recorrente.

## Diretriz de interface

Explicações gerais ficam em frases curtas dentro das seções. Ícones de informação são reservados para métricas e cabeçalhos de tabela com pouco espaço. Não converter automaticamente subtítulos, orientações de formulário ou rodapés em ícones.
