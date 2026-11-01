# Roteiro de entregas

Estado em 01/11/2026: etapa de tela de login e mock de JWT concluída e encerrada por Jaime. Serviços separados para autenticação, construção do token e armazenamento. Autenticação real será externa; backend sem controle de usuários/sessões. Próximo passo é discutir os limites da arquitetura antes de cadastros. A primeira publicação está no PR #11, com CI remota aprovada. Envio do token nas chamadas HTTP e marcos 3–6 permanecem pendentes. Ordem por dependências, sem calendário fixo.

## Marco 1 — Fundação reproduzível

**Objetivo:** retomar o projeto em qualquer fim de semana sem reconstruir o ambiente.

Backlog e evidências: [F01–F07, baseline técnica e diagnóstico do ambiente](05-marco-1.md). Os resultados verificados estão registrados no início do documento; tarefas remotas não são declaradas concluídas com base somente no build local.

- [ ] Revisar as propostas de arquitetura e registrar versões exatas de Java, Spring Boot, Gradle, Node/npm, React/Vite e MySQL; verificar a instalação local e Docker.
- [x] Organizar `backend/`, `frontend/`, `prototype/` e documentação, preservando os exemplos e imagens.
- [x] Criar projetos mínimos com wrappers/lockfile e instruções para executar em loopback; configuração de exemplo sem credenciais reais.
- [x] Revisar o modelo v1 e a ordem de dependências; gerar as primeiras migrations Flyway para acesso/cadastros. Estruturas financeiras ficam nos respectivos marcos e importação permanece adiada. DDL original não executado no banco pessoal.
- [ ] Configurar testes Java de domínio sem banco, MockMvc, MySQL/Testcontainers e frontend; não incluir H2. Configurar pipeline GitHub Actions para build/test sem dados privados ou acesso ao MySQL pessoal.

**Aceite:** a partir de checkout limpo, instalar dependências, executar aplicações e aplicar migrations em banco vazio de teste seguindo o README. Reinício não recria nem apaga tabelas; migrations repetidas não reaplicam dados. MySQL pessoal só é usado após configuração explícita.

**Aprendizado:** migrations reproduzem a evolução do banco; pipeline verifica o projeto fora do computador do autor.

## Marco 2 — Acesso e cadastros

**Objetivo:** cada usuário trabalhar com dados próprios persistidos.

- [x] Login e saída pelo serviço mockado do frontend, sem dependência de backend/banco. [Responsabilidades atuais](07-acesso-local.md).
- [x] JWT mockado com nome e role `acesso_total`, salvo em `lastro.token` e removido ao sair; serviços próprios para JWT e localStorage, com responsabilidades documentadas.
- [x] Encerrar a etapa de login e consolidar aprendizados de qualidade para as próximas tarefas. [Regras permanentes e exemplos](08-acordos-de-qualidade.md).
- [ ] Integrar o token às futuras chamadas de negócio após definir o contrato; o mock não representa autenticação ou autorização reais.
- [ ] Discutir responsabilidades e critérios de extração de classes antes da próxima expansão do backend. O fluxo de senhas e redefinição foi retirado do escopo.
- [ ] Criar titulares, contas e catálogo inicial dos ativos suportados pelo protótipo. Contas são registros identificados, não texto livre repetido em cada operação.
- [ ] Definir a separação dos cadastros pelo perfil selecionado e o arquivamento de registros referenciados no histórico. Definir o contrato de identidade externa antes da integração; não confiar nos IDs do mock como autorização.
- [ ] Construir shell React com navegação aprovada, identificação do usuário, saída e estados de carregamento/erro/vazio.

**Aceite futuro dos cadastros:** registrar titular/conta, reiniciar e encontrá-los no perfil correspondente. A troca de perfil não mistura listagens. Os contratos de consulta e gravação devem definir o vínculo ao perfil antes da implementação. O mock de acesso não comprova identidade nem autorização. Entrada inválida não grava parcialmente.

**Aprendizado:** o backend de negócio não gerencia autenticação. O serviço mockado no frontend permite evoluir a interface enquanto o backend externo específico permanece fora do escopo atual.

## Marco 3 — Livro e resultados rastreáveis

**Objetivo:** registrar um período real e explicar seus números.

- [ ] Detalhar a política decimal já aprovada (18/8/2 casas, HALF_EVEN e versão): fronteiras de arredondamento, precisão de divisão e resíduos, com fixtures. Documentar os contratos de prévia, confirmação, consulta e correção.
- [ ] Criar migrations do livro, auditoria e projeções, validando evolução a partir do schema do marco anterior.
- [ ] Usar o mesmo motor Java na prévia e na confirmação; não duplicar regras financeiras no React. Tratar respostas de prévia fora de ordem e mudança de revisão antes de salvar.
- [ ] Implementar primeiro saldo inicial/aporte, compra e venda, completos entre React e MySQL; em seguida conversão, retirada, transferência e pagamento de imposto.
- [ ] Implementar taxas nas moedas suportadas, custo médio contínuo, custódia por conta, parcelas de resultado e regras de saldo suficiente.
- [ ] Implementar correção por substituição, cancelamento com motivo e histórico consultável. Recalcular de forma transacional e impedir conflito de revisão e repetição da mesma submissão.
- [ ] Entregar listagem paginada, detalhe do lançamento/cálculo, visão geral, composição e detalhe da posição; preservar o tratamento honesto do gráfico de referências históricas.

**Aceite:** fixtures conferidas batem nos valores esperados; trocar o filtro mensal não altera custo histórico. Uma correção antiga válida atualiza resultados; uma inválida não muda o livro. Salvar duas vezes a mesma requisição não duplica operação. Tudo permanece após reiniciar.

**Aprendizado:** teste do cálculo prova uma regra; teste do fluxo prova que essa regra foi conectada corretamente à aplicação.

## Marco 4 — Conferência

**Objetivo:** comparar registros com evidências externas sem fabricar ajustes.

- [ ] Implementar comparação por titular/conta/ativo/data e referência textual do documento, sem upload nesta etapa.
- [ ] Criar migration das comparações de saldo e validar atualização do banco existente.
- [ ] Entregar fila de pendências: país/referência ausente, custo zero e referência repetida; links para investigar os lançamentos.
- [ ] Atualizar diferenças após mudanças no histórico; exportar as comparações autorizadas.

**Aceite:** usar saldo total do fim do dia, incluindo bloqueados. Alterar operação anterior recalcula a comparação. Diferença não cria operação automaticamente; referência repetida não apaga evento; coincidência não recebe selo de validação fiscal.

## Marco 5 — Organização fiscal e recuperação

**Objetivo:** aproveitar o histórico fora da aplicação e recuperar os dados.

- [ ] Entregar resumo mensal e posições anuais por titular/ativo, com custo e custódia; ano corrente aparece como prévia.
- [ ] Entregar operações detalhadas e impostos pagos, separando pagamento de obrigação calculada.
- [ ] Entregar CSVs e relatório HTML autossuficiente com conteúdo escapado, impressão e totais compatíveis com a interface.
- [ ] Revisar o contrato de JSON versionado, implementar exportação/restauração do próprio espaço e testar falhas sem perda parcial.
- [ ] Documentar backup MySQL e executar recuperação em banco separado; adaptar os textos de Dados e backup ao novo armazenamento.

**Aceite:** posição anual usa todo o histórico até a data relevante; ausência de histórico anterior não inventa saldo zero. Exportações reproduzem os filtros aplicáveis. Recuperação preserva fatos, conferências e rastreabilidade; não mistura espaços. Arquivo inválido não altera dados.

## Marco 6 — Validação local

**Objetivo:** responder se o MVP entrega valor antes de investir em nuvem ou importadores.

- [ ] Executar matriz de cenários e ensaio sintético até 5.000 operações; registrar equipamento, tempos e eventuais limitações.
- [ ] Cada participante mantém um recorte real pequeno com custos conhecidos, compara saldos e gera um compilado.
- [ ] Registrar divergências, causas, esforço, dúvidas e uso assistido; corrigir defeitos que impedem o fluxo.
- [ ] Repetir o ciclo de atualização em sessões posteriores e comparar com o controle atual.
- [ ] Decidir a próxima etapa com evidências: melhorar o fluxo manual, adicionar um primeiro CSV ou preparar acesso remoto.

**Aceite:** checklist de validação preenchido; nenhuma divergência material sem explicação; recuperação comprovada; avaliação de utilidade e esforço registrada pelos dois participantes. Se faltar evidência, continuar o piloto em vez de declarar validação comercial.

## Critério comum de conclusão de tarefa

- Comportamento demonstrável, incluindo erro e estado vazio quando aplicáveis.
- Testes relevantes executados; separação dos registros conforme o contrato da funcionalidade e nenhum dado/segredo privado em fixtures, logs ou Git.
- Migration, contrato e documentação atualizados quando afetados.
- Sem botão que aparente funcionar enquanto usa apenas dados fictícios; exemplos em ambiente separado do livro real.
- Nota curta de retomada: resultado, verificações, próxima tarefa e pendências.

## Modelo de tarefa para GitHub

```text
Título: verbo + capacidade a entregar
Marco:
Problema e resultado esperado:
Comportamento incluído:
Fora desta tarefa:
Dependências:
Critérios de aceite:
Como verificar:
Decisões ou riscos relevantes:
```

Este roteiro não cria issues automaticamente. Ao iniciar um marco, decompor somente o trabalho próximo em tarefas pequenas; detalhar tudo antecipadamente aumenta o custo de revisar decisões ainda não testadas.
