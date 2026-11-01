# Planejamento do Lastro — MVP local

Atualizado em 01/11/2026. Responsável pelo desenvolvimento: Jaime.

## Objetivo desta documentação

Transformar o protótipo aprovado em uma aplicação local com frontend, backend e banco, validando se manter um histórico real no Lastro ajuda Jaime e Eliane a entender resultados, conferir saldos e preparar informações para o contador.

Esta é a base de planejamento e acompanhamento. A fundação Spring Boot/React já foi criada; o estado de cada capacidade é indicado abaixo. As decisões confirmadas estão separadas das propostas técnicas que serão detalhadas antes de cada implementação.

| Documento | Pergunta que responde |
| --- | --- |
| [Produto e escopo](01-produto.md) | Para quem estamos construindo, qual problema resolvemos e o que entra? |
| [Arquitetura e decisões](02-arquitetura.md) | Como as partes se relacionam e por que escolher esse desenho? |
| [Roteiro de entregas](03-entregas.md) | Em qual ordem construir e como comprovar que cada etapa terminou? |
| [Validação e testes](04-validacao.md) | Como verificar os números, a aplicação e a utilidade prática? |
| [Marco 1 detalhado](05-marco-1.md) | Quais são as primeiras tarefas, suas dependências e a evidência de conclusão? |
| [Nota de retomada](06-retomada.md) | O que foi verificado e qual é a próxima tarefa concreta? |
| [Acesso local](07-acesso-local.md) | Como funciona o serviço mockado e qual responsabilidade será externa? |
| [Acordos de qualidade](08-acordos-de-qualidade.md) | Qual padrão de código e experiência deve ser preservado em todas as entregas? |

## Situação atual

- Etapa de tela de login e mock de JWT concluída e encerrada por Jaime em 01/11/2026. Autenticação, JWT e localStorage possuem serviços próprios. Os aprendizados passam a ser critérios obrigatórios de implementação e revisão, conforme [acordos de qualidade](08-acordos-de-qualidade.md).
- Existe um protótipo funcional preservado em `prototype/`. Ele calcula e registra dados no navegador, mas inicia com exemplos fictícios.
- Existem testes do motor JavaScript. Eles são uma referência de comportamento, não uma validação contábil independente.
- A proposta original MySQL com 20 tabelas foi preservada. As primeiras migrations próprias de acesso/cadastros estão em `backend/src/main/resources/db/migration/` e foram verificadas em MySQL 8.0.42 descartável.
- As sete capturas PNG estão em `prototype/screenshots/`, com nomes legíveis. O protótipo executável complementa os estados e interações que as imagens não mostram.
- Spring Boot/Gradle e React/TypeScript estão estruturados. O login usa um serviço mockado no frontend, com Jaime e Eliane. Autenticação real será externa; o backend de negócio não gerencia usuários ou sessões. Cadastros e funcionalidades financeiras ainda não existem.
- Verificações atuais e evidências estão na [nota de retomada](06-retomada.md). A configuração de CI existe; sua execução remota não foi verificada.
- O ambiente de uso está configurado em `lastro_local` no MySQL instalado; estruturas de negócio persistidas; controle de usuários removido por V4. Início por `scripts/Start-Lastro.ps1`. Bancos descartáveis são exclusivos de verificações isoladas.
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
| Espaço | Agrupamento de dados de negócio, sem gestão de usuários ou autenticação neste backend. |
| Titular | Dono dos investimentos; seu custo é calculado separadamente. |
| Conta | Local de custódia de um titular, como uma conta em corretora. |
| Projeção | Saldo ou resultado calculado a partir do histórico e que pode ser reconstruído. |
| Migration | Alteração versionada da estrutura do banco. |

## Próxima conversa

Discutir com Jaime os limites da arquitetura do backend: responsabilidades, consultas, critérios de extração de classes e revisão das próximas mudanças. Fazer esse alinhamento antes de implementar cadastros ou novas camadas. O login está encerrado; o envio do token nas chamadas de negócio permanece para a integração correspondente. A implementação atual está documentada em [acesso no frontend](07-acesso-local.md).
