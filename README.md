# Lastro v2 — clareza para decidir

> Seu plano de um lado. O momento do mercado do outro.

O Lastro é um protótipo de acompanhamento de investimentos em cripto. Seu objetivo é ajudar o investidor a entender **o que mudou, como isso afeta suas posições e quais premissas merecem revisão**.

A v2 prioriza leitura visual, pouco texto na primeira camada e explicações sob demanda. A tese permanece no detalhe de cada posição, confrontada com os preços e o contexto do momento.

**Todos os dados são fictícios.** Preços, movimentos macroeconômicos, gráficos e leituras técnicas são cenários demonstrativos. Não há consulta ao mercado, previsão de preços ou execução de ordens.

## O problema que queremos resolver

Uma posição pode estar lucrativa enquanto o ambiente fica mais arriscado. Outra pode seguir dentro dos limites de preço, mas já exigir uma revisão das premissas.

O Lastro organiza três perguntas em uma mesma tela:

| Pergunta | O que o produto apresenta |
| --- | --- |
| Qual era meu plano? | Entrada, invalidação, parcial e limite de risco. |
| Onde estou agora? | Preço, resultado, distâncias e impacto potencial na carteira. |
| O que mudou ao redor? | Juros, petróleo, mercado acionário e leitura técnica. |

O investidor interpreta essas informações e decide. O registro da decisão preserva o contexto daquele momento.

## O que mudou na v2

- Carteira e oportunidades ocupam o centro da navegação.
- A tese é definida ao adicionar uma posição ou intenção de entrada.
- O detalhe mantém o plano visível em um painel lateral no desktop e como primeiro bloco em telas menores.
- **Estado do plano** e **estado do contexto** são avaliações separadas.
- Gráfico ilustrativo, níveis, progresso, indicadores e cores resumem a primeira leitura.
- Explicações de alerta, evidências contrárias e fórmulas ficam em detalhes expansíveis.
- A revisão registra uma decisão; não apaga alertas e não altera saldos ou quantidades.

## Um exemplo concreto

O BTC demonstrativo usa o plano discutido para o produto:

| Premissa | Plano de exemplo |
| --- | --- |
| Entrada | US$ 76.000 |
| Cenário negativo | Encerrar caso perca a referência de US$ 74.000. |
| Limite de risco | Perda planejada de até 2% da carteira nesta posição. |
| Cenário positivo | Realizar 25% da posição em US$ 85.000. |

No cenário **Estresse**, o BTC vale US$ 78.500. A posição de 0,2 BTC apresenta lucro aberto de US$ 500, mas juros e petróleo sobem enquanto o S&P 500 cai.

A tela mostra simultaneamente:

- **Plano: dentro dos níveis.** O preço não atingiu a invalidação nem o objetivo.
- **Contexto: reavaliar.** As condições do exemplo merecem atenção, mesmo com lucro.

Não há contradição: o preço ainda respeita o plano, enquanto o contexto sugere revisar se suas premissas continuam adequadas.

## Experimente cinco cenários

Use **Simular contexto**, no topo. O cenário selecionado atualiza preços, patrimônio, risco e leituras em todas as telas.

| Cenário | O que observar |
| --- | --- |
| Atenção · contexto misto | Níveis preservados com pressão macro moderada. |
| Estresse · aversão a risco | Posição BTC ainda lucrativa e contexto vermelho. |
| Favorável · recuperação | Melhora contextual sem remover os limites do plano. |
| Perda do suporte | Preço abaixo da invalidação; perda simulada pode superar a planejada. |
| Objetivo alcançado | Referência de parcial atingida, sem realizar operação automaticamente. |

As classificações são roteirizadas por cenário. Os exemplos macro incluem intensidade do movimento; não há motor de sinais ao vivo, probabilidades ou validação estatística. A trajetória do gráfico é ilustrativa, não uma série de candles reais.

A referência de data e hora identifica o conjunto demonstrativo. Não indica atualização ao vivo.

## Como usar

1. Abra **Visão geral** para ver patrimônio, resultado, caixa, risco agregado e pontos de atenção.
2. Clique em BTC para confrontar o plano com o cenário.
3. Alterne para **Estresse** e observe lucro e alerta vermelho coexistirem.
4. Expanda a explicação da classificação e a evidência contrária.
5. Use **Reavaliar posição** para registrar sua decisão, com motivo opcional.
6. Edite o plano e confira o recálculo do risco. A versão anterior dos níveis fica no registro.
7. Em **Oportunidades**, acompanhe uma intenção sem incluí-la no patrimônio.

### Adicionar um ativo

Escolha BTC, ETH ou SOL e informe se já possui posição ou quer observar. Defina quantidade, entrada, invalidação, objetivo, percentual da parcial, limite de risco e o motivo em uma frase.

O formulário calcula o risco enquanto você preenche e sinaliza quando supera o limite escolhido. Para posições compradas, a invalidação deve ficar abaixo da entrada e o objetivo acima dela.

Os ativos disponíveis são limitados ao catálogo demonstrativo. A quantidade pode ser ajustada; isso não representa uma compra ou venda.

### Registrar uma decisão

As opções incluem manter, aguardar confirmação, planejar redução, revisar proteção, planejar encerramento e planejar parcial.

Cada registro guarda preço, cenário, classificações e plano daquele momento. A decisão não elimina alertas contextuais e não muda quantidade ou caixa. Operações efetivamente executadas não são conciliadas neste protótipo.

## Entenda os números de risco

| Medida | Cálculo e significado |
| --- | --- |
| Resultado aberto | (Preço atual − entrada) × quantidade. |
| Perda planejada | (Entrada − invalidação) × quantidade. |
| Risco da posição na carteira | Perda planejada ÷ patrimônio atual simulado. |
| Do preço atual até a invalidação | Máximo entre preço atual − invalidação e zero, multiplicado pela quantidade. Inclui devolução de lucro. |
| Risco planejado agregado | Soma das perdas planejadas das posições ÷ patrimônio atual simulado. |

**Limite de 2% da carteira é diferente de queda de 2% do ativo.** O tamanho da posição determina o impacto no patrimônio.

O limite é individual por posição. A visão geral apresenta a soma dos riscos planejados. Essa soma não é VaR, não modela correlações e não garante perda máxima. Taxas, gaps e slippage não estão incluídos.

O caixa pode ser ajustado em **Minha carteira** e altera o denominador do risco. USDC é tratado como USD 1:1 na demonstração. Intenções em observação não entram no patrimônio nem no risco agregado; seu risco individual é uma estimativa sobre o patrimônio existente.

## Por que explorar esta ideia?

O valor proposto é facilitar decisões quando o investidor se sente travado: preço, plano e contexto aparecem juntos, com explicações acessíveis e sem exigir leitura longa na primeira camada.

A tese funciona como referência para interpretar o momento. A interação principal é acompanhar a situação e comparar cenários. A documentação da decisão é uma consequência desse fluxo.

Uma assinatura é uma hipótese futura se houver utilidade recorrente. Ainda precisamos comprovar se as pessoas entendem os alertas, voltam ao produto, tomam decisões mais informadas e consideram pagar. Não há promessa de melhora de rentabilidade.

### O que validar agora

- A pessoa consegue explicar por que há lucro e alerta vermelho simultaneamente?
- Consegue localizar entrada, invalidação e parcial sem ajuda?
- Entende o risco sobre a carteira e a diferença para devolução de lucro?
- Encontra rapidamente as evidências e os limites da leitura?
- Adiciona uma intenção com pouco esforço?
- Voltaria para acompanhar uma posição real?

O experimento deve orientar se aprofundamos, simplificamos ou mudamos a proposta. Elogios à interface, sozinhos, não comprovam demanda.

## Estado do produto e próximos passos possíveis

| Disponível na demonstração | Ainda não implementado |
| --- | --- |
| Carteira e oportunidades locais | Corretoras e importação de operações |
| Plano por posição e risco calculado | Preços e indicadores ao vivo |
| Cinco cenários macro e de preço fictícios | Motor de avaliação com fontes reais |
| Revisão e histórico com contexto | Notificações e monitoramento contínuo |
| Persistência no navegador | Contas, sincronização e backup em servidor |
| Interface visual responsiva | IA para sugerir teses alinhadas ao perfil |

A IA é uma possibilidade futura: sugerir planos e explicitar premissas para o usuário escolher e ajustar. Não há integração ou análise por IA nesta versão.

## Executar localmente

Sem dependências e sem build. Com Python 3, na pasta que contém o index.html:

```sh
python -m http.server 8000
```

Abra [http://localhost:8000](http://localhost:8000) em um navegador moderno.

```text
index.html           Estrutura da interface
styles.css           Estilos responsivos
app.js               Dados, regras demonstrativas e interações
tests/risk.test.cjs   Verificação dos cálculos e transições
README.md            Proposta, roteiro e limites
```

No checkout usado pelo Sites, os três arquivos públicos ficam em dist/; no GitHub ficam na raiz.

### Verificar cálculos

Com Node.js instalado:

```sh
node --check app.js
node tests/risk.test.cjs
```

O teste cobre patrimônio sem intenções, resultado, perda planejada, devolução de lucro, alerta contextual com lucro, invalidação, objetivo, excesso de risco, preservação de contexto passado e validação de níveis.

### Dados locais e restauração

A v2 usa a chave lastro-v2 no localStorage e preserva separadamente a chave lastro-v1. Não há migração automática dos dados antigos. O histórico não é sincronizado entre dispositivos ou origens.

Use **Restaurar demonstração** para voltar aos exemplos da v2, após confirmação. Limpar os dados do navegador também remove os registros locais. Não use o protótipo como armazenamento único de informações financeiras.
