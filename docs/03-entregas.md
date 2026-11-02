# Roteiro de entregas

Estado em 02/11/2026: cadastro de carteiras e execução Java 27 implementados. Esclarecimento vigente: `contas` associa carteiras e instituições; operações serão únicas por evento, pertencentes ao usuário/instituição e compartilhadas na visualização das carteiras. PR #12 integrado em `main`; revisão Java 27/V8/modelagem publicada no [PR #13](https://github.com/jaimeNafer/mvp-invest/pull/13), branch `codex/java27-vinculos-carteiras`. [Retomada](06-retomada.md).

**Etapa de carteiras concluída por Jaime em 02/11/2026**, originalmente publicada no PR #12. O novo requisito altera a propriedade das contas e a execução em Java 27; a [modelagem vigente](10-modelagem-de-carteiras.md) substitui a anterior. Pendências de ativos/identidade externa não estão incluídas nesta correção.

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
- [x] Integrar o token às chamadas de carteiras e instituições pelo cliente HTTP; o mock não representa autenticação ou autorização reais.
- [x] Obter a identificação do Bearer no backend por filter e contexto por requisição injetável nas services; substituir `X-Perfil-Id`, mantendo emissão mockada e autenticação real futura.
- [x] Alinhar JPA e responsabilidades. Decisão vigente substitui titulares por carteiras; senhas permanecem fora do backend de negócio.
- [x] Criar, consultar, editar, arquivar e reativar carteiras no MySQL. Modal com nome e instituições. [Contrato e verificações](09-cadastro-de-carteiras.md).
- [x] Cadastro de associações entre carteiras e instituições em `contas`, com unicidade por carteira/instituição e transação na edição. IDs diferentes representam vínculos; o histórico financeiro futuro não será duplicado por carteira.
- [x] Disponibilizar catálogo inicial de instituições: Binance e BitGet, por migration e sem tela de manutenção.
- [ ] Definir os ativos suportados e provisionar o catálogo para o primeiro lançamento; a tabela existente é preservada.
- [x] Separar carteiras por `perfil_ref` de `UsuarioAtual.id()`; contas herdam o proprietário via carteira. V8 exige cadastros antigos vazios e backup, preservando V1–V7 e catálogos.
- [ ] Aplicar o contexto aos próximos cadastros e definir o contrato de identidade externa antes da integração real; não confiar nos IDs do mock como autorização.
- [x] Entregar navegação entre Meu perfil e Carteiras, identificação do perfil, saída e estados de carregamento/erro/vazio no cadastro.

**Aceite dos cadastros:** vínculos separados pela carteira do usuário, com uma instituição por vínculo. Editar mantém IDs preservados e altera somente os vínculos daquela carteira; arquivar os mantém. Operações futuras do usuário/instituição são únicas e sobrevivem à remoção de vínculos. Falhas não gravam parcialmente.

**Aprendizado:** o backend de negócio não gerencia autenticação. O serviço mockado no frontend permite evoluir a interface enquanto o backend externo específico permanece fora do escopo atual.

## Marco 3 — Livro e resultados rastreáveis

**Objetivo:** registrar um período real e explicar seus números.

- [ ] Detalhar a política decimal já aprovada (18/8/2 casas, HALF_EVEN e versão): fronteiras de arredondamento, precisão de divisão e resíduos, com fixtures. Documentar os contratos de prévia, confirmação, consulta e correção.
- [ ] Criar migrations do livro, auditoria e projeções, validando evolução a partir do schema do marco anterior.
- [ ] Usar o mesmo motor Java na prévia e na confirmação; não duplicar regras financeiras no React. Tratar respostas de prévia fora de ordem e mudança de revisão antes de salvar.
- [ ] Implementar primeiro saldo inicial/aporte, compra e venda, completos entre React e MySQL; em seguida conversão, retirada, transferência e pagamento de imposto.
- [ ] Implementar taxas, custo médio contínuo por usuário/ativo, custódia por usuário/instituição, parcelas de resultado e saldo suficiente; nunca vincular o histórico ao ID da associação da carteira.
- [ ] Implementar correção por substituição, cancelamento com motivo e histórico consultável. Recalcular de forma transacional e impedir conflito de revisão e repetição da mesma submissão.
- [ ] Entregar listagem paginada, detalhe do lançamento/cálculo, visão geral, composição e detalhe da posição; preservar o tratamento honesto do gráfico de referências históricas.

**Aceite:** fixtures conferidas batem nos valores esperados; trocar o filtro mensal não altera custo histórico. Uma correção antiga válida atualiza resultados; uma inválida não muda o livro. Salvar duas vezes a mesma requisição não duplica operação. Tudo permanece após reiniciar.

**Aprendizado:** teste do cálculo prova uma regra; teste do fluxo prova que essa regra foi conectada corretamente à aplicação.

## Marco 4 — Conferência

**Objetivo:** comparar registros com evidências externas sem fabricar ajustes.

- [ ] Implementar comparação por usuário/conta/ativo/data e referência textual do documento, sem upload nesta etapa.
- [ ] Criar migration das comparações de saldo e validar atualização do banco existente.
- [ ] Entregar fila de pendências: país/referência ausente, custo zero e referência repetida; links para investigar os lançamentos.
- [ ] Atualizar diferenças após mudanças no histórico; exportar as comparações autorizadas.

**Aceite:** usar saldo total do fim do dia, incluindo bloqueados. Alterar operação anterior recalcula a comparação. Diferença não cria operação automaticamente; referência repetida não apaga evento; coincidência não recebe selo de validação fiscal.

## Marco 5 — Organização fiscal e recuperação

**Objetivo:** aproveitar o histórico fora da aplicação e recuperar os dados.

- [ ] Entregar resumo mensal e posições anuais por usuário/ativo, com custo e custódia; ano corrente aparece como prévia.
- [ ] Entregar operações detalhadas e impostos pagos, separando pagamento de obrigação calculada.
- [ ] Entregar CSVs e relatório HTML autossuficiente com conteúdo escapado, impressão e totais compatíveis com a interface.
- [ ] Revisar o contrato de JSON versionado, implementar exportação/restauração das contas do usuário e testar falhas sem perda parcial.
- [ ] Documentar backup MySQL e executar recuperação em banco separado; adaptar os textos de Dados e backup ao novo armazenamento.

**Aceite:** posição anual usa todo o histórico até a data relevante; ausência de histórico anterior não inventa saldo zero. Exportações reproduzem os filtros aplicáveis. Recuperação preserva fatos, titularidade, conferências e rastreabilidade. Arquivo inválido não altera dados. Consolidação é gerencial, sem misturar cálculos individuais ou compensar tributos entre usuários.

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
