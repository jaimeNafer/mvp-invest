# Planejamento do Lastro — MVP local

Atualizado em 02/11/2026. Responsável pelo desenvolvimento: Jaime.

## Objetivo desta documentação

Transformar o protótipo aprovado em uma aplicação local com frontend, backend e banco, validando se manter um histórico real no Lastro ajuda Jaime e Eliane a entender resultados, conferir saldos e preparar informações para o contador.

Esta é a base de planejamento e acompanhamento. A fundação Spring Boot/React já foi criada; o estado de cada capacidade é indicado abaixo. As decisões confirmadas estão separadas das propostas técnicas que serão detalhadas antes de cada implementação.

| Documento | Pergunta que responde |
| --- | --- |
| [Auditoria modular](13-auditoria-modular.md) | Quais fronteiras são públicas, quais dependências são permitidas e como são verificadas? |
| [Backlog de ideias futuras](ideias-futuras/README.md) | Quais oportunidades podemos avaliar depois do MVP? |
| [Produto e escopo](01-produto.md) | Para quem estamos construindo, qual problema resolvemos e o que entra? |
| [Arquitetura e decisões](02-arquitetura.md) | Como as partes se relacionam e por que escolher esse desenho? |
| [Roteiro de entregas](03-entregas.md) | Em qual ordem construir e como comprovar que cada etapa terminou? |
| [Validação e testes](04-validacao.md) | Como verificar os números, a aplicação e a utilidade prática? |
| [Marco 1 detalhado](05-marco-1.md) | Quais são as primeiras tarefas, suas dependências e a evidência de conclusão? |
| [Nota de retomada](06-retomada.md) | O que foi verificado e qual é a próxima tarefa concreta? |
| [Acesso local](07-acesso-local.md) | Como funciona o serviço mockado e qual responsabilidade será externa? |
| [Acordos de qualidade](08-acordos-de-qualidade.md) | Qual padrão de código e experiência deve ser preservado em todas as entregas? |
| [Cadastro de carteiras](09-cadastro-de-carteiras.md) | Como usar o cadastro, quais são seus contratos e como foi verificado? |
| [Modelagem de carteiras](10-modelagem-de-carteiras.md) | Como carteiras selecionam o mesmo histórico de instituições sem duplicar operações? |
| [Dicionário de dados vigente](11-dicionario-de-dados.md) | Qual o significado, tipo, default e relacionamento de cada campo do banco atual? |
| [Saldo inicial](12-saldo-inicial.md) | Como registrar a abertura, corrigir/cancelar e conferir sua posição inicial? |

## Situação atual

- Etapa de tela de login e mock de JWT concluída e encerrada por Jaime em 01/11/2026. Autenticação, JWT e localStorage possuem serviços próprios. Os aprendizados passam a ser critérios obrigatórios de implementação e revisão, conforme [acordos de qualidade](08-acordos-de-qualidade.md).
- Existe um protótipo funcional preservado em `prototype/`. Ele calcula e registra dados no navegador, mas inicia com exemplos fictícios.
- Existem testes do motor JavaScript. Eles são uma referência de comportamento, não uma validação contábil independente.
- A proposta original MySQL com 20 tabelas foi preservada. As primeiras migrations próprias de acesso/cadastros estão em `backend/src/main/resources/db/migration/` e foram verificadas em MySQL 8.0.42 descartável.
- As sete capturas PNG estão em `prototype/screenshots/`, com nomes legíveis. O protótipo executável complementa os estados e interações que as imagens não mostram.
- Spring Boot/Gradle e React/TypeScript estão estruturados. Login mockado no frontend; autenticação real será externa. O backend lê o Bearer em um filter e disponibiliza `IUsuarioAtual` por requisição. Carteiras e contas usam JPA/MySQL, separadas por `IUsuarioAtual.id()`, sem `X-Perfil-Id`. Saldo inicial está implementado; demais operações e motor completo permanecem pendentes.
- Verificações atuais e evidências estão na [nota de retomada](06-retomada.md). A CI foi executada com sucesso no [PR #11](https://github.com/jaimeNafer/mvp-invest/pull/11), incluindo protótipo, frontend e backend/MySQL.
- MySQL local `lastro_local`, Java 27 e V8 reaplicada após autorização/backup na etapa anterior. Esclarecimento vigente: `contas` é associação N:N; operações futuras do usuário/instituição serão as mesmas nas carteiras que a selecionam, sem duplicação. Início por `scripts/Start-Lastro.ps1`; projeto em `backend/`. [Retomada](06-retomada.md).
- O `PLAN.md` da raiz documenta a fase anterior do protótipo. Para a nova etapa local, prevalecem as decisões confirmadas nesta pasta; o experimento anterior com prazo de 30 dias e usuários externos não é o compromisso desta entrega.

## Como conduzir o trabalho

1. Trabalhar por marcos, sem calendário fixo. A disponibilidade é principalmente em finais de semana e feriados.
2. Escolher uma tarefa pequena com resultado observável; incluir frontend, backend e testes quando fizerem parte do fluxo.
3. Registrar decisões com contexto, escolha, motivo e consequência. Uma mudança de ideia deve atualizar o registro, não deixar instruções conflitantes.
4. Encerrar cada sessão anotando o que funciona, a próxima tarefa e os bloqueios. Não registrar dados financeiros ou credenciais nessas notas.
5. Revisar mudanças pelo Git e executar as verificações relevantes antes de considerar a tarefa concluída.

## Termos usados

| Termo | Significado no Lastro |
| --- | --- |
| MVP | Menor entrega completa que permite testar a hipótese de valor escolhida. |
| Critério de aceite | Evidência observável que permite dizer que uma entrega funciona. |
| Marco | Conjunto de tarefas que deixa uma capacidade utilizável. |
| Usuário | Perfil retornado hoje pelo serviço mockado do frontend; no futuro, pelo backend externo de autenticação. |
| Referência do perfil | Identificador externo do usuário proprietário das carteiras e contas; não é credencial. |
| Carteira | Seleção de instituições para visualizar os investimentos do usuário. |
| Conta | Nome atual da associação entre carteira e instituição; seu ID identifica o vínculo. |
| Instituição | Entrada do catálogo de custodiantes, compartilhada entre usuários. |
| Projeção | Saldo ou resultado calculado a partir do histórico e que pode ser reconstruído. |
| Migration | Alteração versionada da estrutura do banco. |

## Entrega atual e próximo passo

[Saldo inicial](12-saldo-inicial.md) implementado: seis ativos, data comum por usuário, custo conhecido, histórico de correções/cancelamentos, repetição segura e posição global. V9 aplicada localmente, 44 testes backend e 20 frontend aprovados. Evidências e limites na [retomada](06-retomada.md). Próximo passo é especificar compra/venda, taxas, custódia e arredondamentos de baixas; implementação e publicação remota serão tarefas posteriores.

- [Revisão Clean Code do backend](14-clean-code-backend.md).

- [Enums de domínio e compatibilidade](15-enums-de-dominio.md).
