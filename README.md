# Lastro — sua carteira precisa de um motivo, um plano e uma memória

> Entenda o que você tem, por que comprou e o que precisa acontecer para mudar de decisão.

O Lastro é uma proposta de aplicativo para acompanhar investimentos em cripto conectando **carteira, tese de investimento, plano de risco e diário de decisões**.

A experiência começa com uma pergunta simples: **se você abrisse sua carteira hoje, conseguiria explicar por que ainda mantém cada posição?**

O objetivo é tornar essa resposta fácil de encontrar, revisar e comparar com o que você realmente fez.

**Estado atual:** protótipo navegável com dados fictícios. Este repositório demonstra uma experiência de produto; ainda não comprova demanda, disposição a pagar ou melhora de resultados financeiros.

## 1. O problema que queremos resolver

Imagine esta situação:

Você compra um ativo depois de estudar um cenário. Define mentalmente um limite de perda, pensa em realizar parte do lucro e decide esperar uma confirmação antes de aumentar a posição.

Dias depois, o preço muda. Você lê uma opinião diferente, vê uma oportunidade em outro ativo e tenta lembrar exatamente o que havia planejado.

A pergunta deixa de ser apenas “quanto estou ganhando?” e passa a ser:

- Por que entrei nessa posição?
- O que mudou desde a compra?
- Estou seguindo meu plano ou adaptando a explicação ao preço?
- Essa compra adicional já estava prevista?
- Quando vendi, respeitei meu critério ou agi por impulso?
- O que posso aprender com essa operação?

O problema que o Lastro pretende atacar é a **distância entre ter informações e conseguir usá-las de forma consistente nas próprias decisões**.

Um saldo informa o tamanho da posição. Para entender a decisão, precisamos também do seu contexto.

## 2. O que é uma tese de investimento?

“Tese” é o nome dado à explicação de por que um investimento faz sentido para você e em quais condições essa explicação deixa de valer.

Ela pode ser curta e escrita em linguagem comum:

> “Quero exposição a este ativo enquanto o cenário que estudei continuar válido. Vou manter uma posição limitada, revisar no fechamento diário e reduzir a exposição se o critério de invalidação acontecer.”

Uma tese útil responde a quatro perguntas:

| Pergunta | O que registrar |
| --- | --- |
| Por que tenho esse ativo? | A oportunidade ou o motivo da exposição. |
| O que sustenta essa decisão? | As condições que você espera observar. |
| O que faria mudar de ideia? | Os critérios de invalidação e revisão. |
| Qual é o próximo passo? | A ação ou observação que já está planejada. |

A proposta do Lastro é colocar essas respostas ao lado da posição correspondente, para que o investidor não precise reconstruir seu raciocínio a cada revisão.

## 3. A experiência que estamos propondo

**Carteira → tese → plano → decisão → revisão.**

1. **Veja a carteira:** posições, caixa e distribuição do patrimônio.
2. **Abra um ativo:** encontre o motivo da exposição e os critérios do plano.
3. **Revise o contexto:** compare o que havia previsto com o que está observando.
4. **Registre uma decisão:** explique o que decidiu e por quê.
5. **Volte ao histórico:** acompanhe como seu raciocínio mudou ao longo do tempo.

O valor pretendido está na continuidade desse ciclo. Cada registro ajuda a dar contexto à próxima revisão.

### Um exemplo prático

Considere uma posição fictícia em BTC:

| Momento | Uso pretendido do Lastro |
| --- | --- |
| Antes da compra | Registrar a motivação, o limite da exposição e a invalidação. |
| Durante a posição | Consultar o plano antes de aumentar, reduzir ou manter. |
| Depois de uma realização parcial | Registrar o motivo e a relação com o plano original. |
| Quando o cenário mudar | Documentar o que deixou de valer e a decisão tomada. |
| Na revisão posterior | Ler a sequência e identificar onde houve disciplina, dúvida ou improviso. |

O sistema organiza o raciocínio do investidor. A decisão continua sendo dele.

## 4. O que o usuário ganha com isso?

### Clareza antes de agir

Ter o plano junto da posição pode facilitar uma pausa útil: “Essa ação faz sentido diante do que eu mesmo defini?”

### Memória das decisões

O diário preserva a justificativa escrita naquele momento. Isso permite revisar o raciocínio original, sem depender apenas da lembrança posterior.

### Uma rotina de revisão

A carteira pode funcionar também como uma lista de decisões a acompanhar: quais posições precisam de atenção e qual é o próximo passo de cada uma.

### Material para aprender com a própria experiência

O histórico pode ajudar a investigar se uma decisão respeitou o plano, quais critérios foram úteis e quais precisam melhorar.

**Esses são benefícios pretendidos. O protótipo ainda não mede redução de impulsividade, economia de tempo ou melhoria de desempenho.**

## 5. Para quem faz sentido começar?

O público inicial proposto é o **investidor individual em cripto que acompanha suas posições ativamente e já tenta trabalhar com algum plano**.

Alguém que mantém poucas posições, consulta análises, faz revisões e deseja entender melhor suas próprias decisões é um perfil útil para testar a ideia.

O encaixe parece menor para quem faz aportes automáticos e raramente revisa posições, ou para quem procura apenas uma cotação rápida.

Esse recorte é uma hipótese de público. Precisamos observar quem usa o produto de verdade antes de ampliá-lo.

## 6. Por que alguém usaria em vez de uma planilha, notas ou um chat?

O Lastro precisa merecer esse lugar na rotina. Organizar tudo manualmente em ferramentas existentes é uma alternativa válida.

| Alternativa | O que precisamos demonstrar para justificar a troca |
| --- | --- |
| Planilha | Que consultar o plano e registrar uma decisão exige menos esforço. |
| Aplicativo de notas | Que a ligação entre posição, tese e histórico facilita a revisão. |
| Conversas em um chat | Que o contexto fica mais fácil de localizar e acompanhar ao longo do tempo. |
| Acompanhamento apenas por saldo | Que o registro do processo é útil o suficiente para ser mantido. |

**Nosso diferencial proposto é a conexão entre posição e raciocínio.** Ainda precisamos comprovar que essa conexão entrega valor superior ao esforço de preenchê-la.

Um formulário bonito, sozinho, não justifica um produto.

## 7. Por que investir tempo nesta ideia?

### Existe uma experiência concreta para explorar

O produto já pode ser usado como demonstração. Isso permite conversar sobre ações específicas: abrir uma posição, revisar o plano e registrar uma decisão.

### O primeiro aprendizado pode vir de um escopo pequeno

Podemos investigar o valor do fluxo principal antes de construir integrações, automações ou uma comunidade. O protótipo permite descobrir quais partes fazem falta e quais podem ser removidas.

### Há afinidade entre o problema e quem está construindo

O projeto nasce da combinação entre experiência em desenvolvimento de software e vivência com acompanhamento de investimentos. Essa proximidade facilita formular perguntas e reconhecer atritos reais.

Proximidade com o problema ajuda a começar; usuários externos precisam confirmar se ele também importa para outras pessoas.

### Existe uma hipótese de uso recorrente

Se o aplicativo participar das revisões e decisões do investidor, pode haver motivo para voltar. Essa recorrência precisa ser observada: gostar de uma demonstração é diferente de adotar uma rotina.

### Existe uma possibilidade de receita a testar

Uma assinatura pode fazer sentido se o serviço entregar valor contínuo. Antes de definir preço, precisamos descobrir qual benefício faria alguém pagar e continuar usando.

**O investimento de tempo defensável agora é um experimento com limites e critérios de continuidade.** Ainda não há evidência suficiente para assumir meses de desenvolvimento ou retorno financeiro.

## 8. O que poderia virar um negócio?

Uma possibilidade futura é oferecer um plano gratuito para experimentar o fluxo e uma assinatura para quem utiliza o produto com frequência.

| Possibilidade futura | Hipótese de valor |
| --- | --- |
| Histórico e revisões mais completos | Facilitar o acompanhamento contínuo das decisões. |
| Lembretes configuráveis | Ajudar o usuário a manter sua rotina de revisão. |
| Relatórios do processo de decisão | Apoiar a identificação de padrões no próprio histórico. |
| Importação de operações | Reduzir o esforço manual para manter a carteira. |
| Compartilhamento opcional de teses | Permitir discussão e aprendizado com outras pessoas. |

Esses recursos **não estão implementados** e não constituem um compromisso de desenvolvimento. Devem ser priorizados a partir de uso observado e conversas com potenciais clientes.

Também será necessário estimar custos de operação, aquisição, suporte e retenção para avaliar a viabilidade econômica.

## 9. O risco principal da ideia

O maior risco é o usuário reconhecer a importância de documentar decisões e, mesmo assim, não querer fazer isso.

Outras perguntas decisivas:

- O preenchimento é rápido o suficiente?
- O usuário consulta os registros antes de decidir?
- O diário continua útil depois da novidade inicial?
- Existe um benefício percebido que justifique pagamento?
- A necessidade central é organizar decisões ou apenas consolidar operações?

Se as pessoas precisarem de insistência constante para usar o fluxo, teremos de simplificar, mudar o recorte ou reconsiderar a proposta.

Também precisamos preservar um limite de interpretação: **um plano bem documentado pode estar errado**. O aplicativo não garante proteção patrimonial nem lucro.

## 10. Como avaliar a ideia usando este protótipo

### Primeiro: experimentar individualmente

Escolha um dos ativos demonstrativos e percorra este roteiro:

1. Abra a tese e leia os critérios.
2. Edite o motivo da posição e o próximo passo.
3. Registre uma decisão com sua justificativa.
4. Abra o diário e procure essa decisão.
5. Volte depois e avalie se o registro ajudou a recuperar o contexto.

A pergunta principal é: **“Eu teria motivo para voltar a isso na minha próxima revisão?”**

### Depois: observar outras pessoas

Como proposta de experimento, podemos apresentar o protótipo a cinco investidores do perfil inicial e pedir que realizem o mesmo fluxo com um cenário fictício.

Observe o que conseguem fazer sem explicação, o que ignoram e quais informações procuram espontaneamente.

Perguntas úteis:

- Como você resolve isso hoje?
- Qual foi a última situação em que sentiu falta desse contexto?
- O que aqui facilitaria sua próxima revisão?
- Que parte parece trabalho desnecessário?
- O que precisaria existir para você usar novamente?
- Em quais condições você consideraria pagar?

### Critérios propostos para decidir o próximo passo

| Evidência observada | Possível decisão |
| --- | --- |
| Pessoas voltam espontaneamente e consultam seus registros | Aprofundar o fluxo e testar uso recorrente. |
| Existe interesse, mas o preenchimento impede o uso | Simplificar a experiência antes de adicionar funções. |
| O interesse se concentra em outra necessidade | Investigar uma mudança de foco. |
| Há elogios, mas nenhum uso concreto após o teste | Reavaliar a proposta antes de ampliar o investimento. |

Cinco testes oferecem aprendizado inicial, não uma comprovação estatística de mercado. Intenção de pagar também precisa ser distinguida de pagamento real.

## 11. O que existe hoje

| Recurso | Estado |
| --- | --- |
| Visão geral do patrimônio e distribuição | Demonstrativo, com valores fixos. |
| Posições em BTC, ETH e SOL; caixa em USDC | Carteira fictícia. |
| Teses por ativo | Exemplos pré-carregados. |
| Edição do motivo, invalidação, níveis e próximo passo | Funcional no navegador. |
| Diário de decisões por ativo | Funcional no navegador. |
| Marcar uma tese como revisada | Funcional, com registro no diário. |
| Persistência e restauração dos exemplos | localStorage do navegador. |
| Layout adaptável | Implementado em CSS. |
| Preços ao vivo, corretoras e execução de ordens | Não implementados. |
| Contas, sincronização e armazenamento em servidor | Não implementados. |
| Análise por IA, notificações e comunidade | Não implementadas. |
| Assinaturas e cobrança | Não implementadas. |

O protótipo permite explorar uma direção de produto. Ainda não é um serviço pronto para operar com dados reais de clientes.

## 12. Como executar localmente

Não há dependências nem etapa de build. É necessário Python 3 e um navegador moderno.

Na pasta do repositório, execute:

```sh
python -m http.server 8000
```

Abra [http://localhost:8000](http://localhost:8000).

### Roteiro de navegação

- **Visão geral:** patrimônio demonstrativo, posições e lembrete de revisão.
- **Minha carteira:** distribuição e acesso aos ativos.
- **Teses de investimento:** motivo, condições e próximo passo de cada posição.
- **Diário de decisões:** registros e opção “Restaurar demonstração”.

As alterações ficam apenas no navegador e na origem em que o aplicativo foi aberto. Limpar os dados do navegador remove esses registros. Não há sincronização ou backup em servidor.

### Estrutura

```text
index.html   Interface, estilos, dados fictícios e interações
README.md    Proposta de valor, hipóteses e instruções
.gitignore   Exclusões básicas do repositório
```

### Validação técnica realizada

A sintaxe JavaScript foi verificada. A navegação para uma tese, o registro de uma decisão e a edição do plano foram conferidos no navegador. O layout possui regras responsivas; não foi feita verificação em aparelho móvel.

## A proposta em uma frase

**O Lastro quer tornar cada posição compreensível e cada decisão revisável.**

Essa é a razão para experimentar a ideia: descobrir se organizar o raciocínio de investimento resolve uma dificuldade pela qual alguém voltaria ao aplicativo — e, eventualmente, pagaria.
