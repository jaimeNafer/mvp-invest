# Validação, testes e riscos

## Estratégia

Três evidências diferentes são necessárias: cálculo correto para casos conhecidos, aplicação funcionando de ponta a ponta e benefício percebido pelos participantes. Uma não substitui as outras.

Usar dados fictícios nos testes automatizados e manter dados reais fora do Git. Casos pequenos devem ter valores esperados calculados e revisados independentemente da função testada. O motor JavaScript pode ajudar na comparação, mas não será o único oráculo.

## Camadas de teste propostas

| Camada | Ferramentas propostas | Evidência |
| --- | --- | --- |
| Domínio | JUnit e BigDecimal, sem contexto Spring | Custos, resultados, taxas, arredondamento e invariantes. |
| API | MockMvc | Contratos e validação das futuras APIs de negócio; não há endpoints de acesso no backend. |
| Persistência/transação | MySQL em Testcontainers, Flyway e MockMvc quando necessário | SQL real, constraints, rollback, revisões e migrations. H2 não substitui esta evidência. |
| Interface | Vitest e React Testing Library | Formulários, erros, carregamento, filtros e limpeza após logout. |
| Fluxo completo | Playwright, como proposta para poucos fluxos críticos | Login → operação → resultado → conferência → exportação; troca de usuário. |
| Volume e recuperação | Histórico sintético e banco descartável | Até 5.000 eventos, estabilidade dos totais e restauração. |

## Matriz inicial de cenários

### MockMvc e banco respondem perguntas diferentes

MockMvc exercita o processamento Spring MVC sem iniciar um servidor HTTP real. Ele não obriga a substituir os serviços por mocks: um teste pode usar MockMvc com contexto completo e persistência real em MySQL descartável. Já um teste focado no controller pode simular o caso de uso para verificar somente entrada, resposta e segurança.

Proposta para manter a suíte simples: testes unitários sem banco para o motor; testes focados de API quando houver contrato relevante; testes de integração com MockMvc, Spring Boot e MySQL/Testcontainers para persistência, migrations, isolamento e rollback. Não criar o mesmo teste nas três camadas sem uma pergunta distinta.

Decisão confirmada por Jaime em 01/11/2026: não incluir H2 nesta etapa, pois domínio não precisa de banco e persistência será validada em MySQL/Testcontainers. Não criar dependência, perfil ou schema H2. Reavaliar somente diante de uma necessidade concreta. Containers de teste não usam as credenciais nem as tabelas do MySQL pessoal.

Referências: [MockMvc](https://docs.spring.io/spring-framework/reference/testing/mockmvc.html) e [integração Spring Boot/Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html).

### Cenários

Cada linha poderá gerar mais de um teste. Completar esta matriz antes de implementar a regra correspondente; adicionar regressão quando surgir um defeito relevante.

| ID | Cenário | Resultado esperado |
| --- | --- | --- |
| T01 | Saldo inicial conhecido, ausente, zero explícito e BRL | Conhecido aceito; ausente recusado; zero confirmado e sinalizado; BRL com custo igual à quantidade. |
| T02 | Compras sucessivas e venda parcial/total | Média móvel correta, baixa proporcional e consumo do custo residual no encerramento. |
| T03 | Compra e venda usando stablecoin | Separar resultado do ativo e da moeda de liquidação; compra pode ter resultado cambial. |
| T04 | Taxa em BRL, ativo recebido/vendido e terceiro ativo | Saldo e custo coerentes; sem desconto duplo; terceiro ativo precisa de saldo e cotação histórica. |
| T05 | Conversão | Um evento com pernas coerentes; preservar a composição de resultado. |
| T06 | Transferência própria e taxa de rede | Principal preserva custo; custódia muda; taxa não aparece duas vezes no resultado. |
| T07 | Aporte, retirada e imposto externo/da carteira | Capital não vira lucro; imposto externo não debita carteira; moeda pode ter resultado cambial próprio. |
| T08 | Saída sem saldo, inclusive taxa e conta errada | Rejeitar tudo sem gravação parcial ou saldo negativo. |
| T09 | Mesmo ativo em titulares e usuários diferentes | Custos separados; visão conjunta do espaço soma sem misturar médias; outro espaço inacessível. |
| T10 | Mesmo instante e eventos fora de ordem de cadastro | Ordem determinística; filtros processam histórico anterior antes da apresentação. |
| T11 | Correção/cancelamento retroativo válido e inválido | Recalcular ou recusar atomicamente; preservar versão anterior, motivo e autoria. |
| T12 | Reenvio da requisição e revisão antiga | Não duplicar fatos; rejeitar conflito de edição com orientação para atualizar. |
| T13 | Comparação de saldo e correção posterior | Diferença atualizada na data informada, sem ajuste automático. |
| T14 | Pendências e mesma referência em vários fills | Sinalizar para revisão; nunca excluir automaticamente. |
| T15 | Ano anterior sem histórico, posição encerrada e ano corrente | Ausência não vira zero certificado; zero sustentado pelo histórico é distinto; ano atual é prévia. |
| T16 | Decimais, divisão periódica e limites de escala | Valores reproduzíveis conforme política versionada; excesso rejeitado; transporte sem Number financeiro. |
| T17 | Login pelo serviço mockado, erro de credenciais, saída e recarga | Jaime/Eliane com senha vazia; credenciais incorretas recusadas; saída/recarga limpam o perfil; nenhuma chamada HTTP. |
| T18 | Separação de cadastros por perfil — contrato pendente | Definir e testar junto aos cadastros; o mock do frontend não comprova identidade; autenticação externa será integrada depois. |
| T19 | CSV/HTML e entradas com caracteres especiais | Totais consistentes; texto não executa script nem fórmula de planilha. |
| T20 | JSON inválido/incompatível e recuperação válida | Falha sem alteração parcial; recuperação mantém isolamento, fatos, auditoria e resultados recalculados. |
| T21 | Migrations em MySQL vazio e banco já migrado | Estrutura criada corretamente; segundo início não altera indevidamente o banco. |
| T22 | Históricos sintéticos crescentes até 5.000 eventos | Paginação correta, totais independentes da página e resultados reproduzíveis. |
| T23 | Prévia no backend, edição, respostas fora de ordem e confirmação após mudança do livro | Mesmo motor na prévia/confirmação; nenhuma gravação na prévia; edição substitui o evento; resposta antiga não sobrescreve dados atuais; revisão desatualizada exige atualização. |

Não prometer cobertura de todos os casos possíveis. A matriz cobre as regras conhecidas e deve evoluir com o histórico real.

### Exemplo financeiro de referência para T02

Fixture fictícia em BRL, com um titular e uma conta; usar strings decimais na implementação:

1. Aporte de R$ 10.000.
2. Compra de 0,01 BTC ao preço unitário de R$ 400.000, com taxa de R$ 10 em BRL. Custo incorporado: R$ 4.010; caixa: R$ 5.990.
3. Venda de 0,004 BTC ao preço unitário de R$ 450.000, com taxa de R$ 5 em BRL. Receita bruta: R$ 1.800; líquida: R$ 1.795; custo baixado: R$ 1.604; resultado da venda: R$ 191.
4. Posição final: 0,006 BTC com custo de R$ 2.406 e caixa de R$ 7.785. Taxas informativas: R$ 15, já consideradas; não descontar novamente do resultado de R$ 191.

Esse caso tem resultados exatos e verifica a ligação entre fatos, custos, caixa e resultado. Não cobre divisões periódicas, outras moedas de taxa ou política tributária; usar casos adicionais para essas regras.

## Ensaio de volume

Gerar eventos válidos, com múltiplas contas/titulares, taxas e correções. Conferir um fechamento conhecido também na massa grande; não medir velocidade sobre eventos financeiramente inválidos.

Medir listagem filtrada, confirmação com recálculo, visão geral e exportação anual, registrando equipamento e versão do banco. Metas provisórias a calibrar: listagem em até 1 segundo e confirmação com recálculo em até 5 segundos no computador do piloto. Ultrapassagem exige investigação e registro antes do piloto, não uma mudança automática para arquitetura distribuída.

## Riscos e resposta planejada

| Risco | Resposta |
| --- | --- |
| Digitação de histórico grande inviabiliza o uso | Validar recorte pequeno e medir esforço; importação CSV é uma próxima decisão, não requisito escondido. |
| Aparência correta esconde cálculo errado | Fixtures independentes, rastreabilidade e revisão contábil quando disponível; não certificar apuração tributária. |
| Modelo de 20 tabelas aumenta escopo | Revisar para as capacidades atuais; não construir importadores ou telas só porque existem tabelas propostas. |
| Perda de dados no computador | Backup técnico, exportação e recuperação testada em banco separado; cópia protegida fora do disco principal. |
| Troca de usuário revela dados | Saída limpa o perfil em memória. Não tratar o mock como proteção; definir contrato externo e separação dos dados antes das APIs de negócio. |
| Pausas longas dificultam retomada | Tarefas pequenas e notas de retomada com próximo passo explícito. |
| Só os fundadores gostarem da solução | Registrar limite da evidência; validar usuários externos e pagamento apenas em etapa posterior. |

## Registro de uma sessão real

```text
Participante e data:
Recorte e contas cobertas (sem dados sensíveis no Git):
Tarefa realizada:
Tempo aproximado e comparação com controle atual:
Resultado conferido / divergência e explicação:
Ajuda necessária:
Utilidade percebida e dificuldade principal:
Próxima ação:
```

## Checklist de saída do piloto local

- [ ] Jaime e Eliane completaram o fluxo principal em seus espaços.
- [ ] Houve manutenção do histórico em sessões posteriores, não apenas demonstração inicial.
- [ ] Resultados e saldos do recorte foram conferidos, com divergências explicadas.
- [ ] CSVs/relatório foram avaliados quanto à utilidade; revisão por contador registrada se ocorrer, sem presumir que já aconteceu.
- [ ] Backup e recuperação foram demonstrados sem usar o banco real como ambiente de teste destrutivo.
- [ ] Esforço de atualização e dependência de ajuda foram registrados.
- [ ] Próximo investimento escolhido a partir dessas evidências.
