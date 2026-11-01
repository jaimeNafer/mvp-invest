# Acordos de qualidade e aprendizados da primeira entrega

Consolidado em 01/11/2026, ao concluir a tela de login e o mock de JWT. Estas regras se aplicam às próximas tarefas, inclusive a código mockado. As instruções operacionais ficam em [AGENTS.md](../AGENTS.md); o funcionamento da entrega está em [acesso no frontend](07-acesso-local.md).

## Aprendizados que passam a ser padrão

| ID | Aprendizado | Aplicação obrigatória nas próximas tarefas |
| --- | --- | --- |
| Q01 | Cada responsabilidade precisa ter um lugar claro. | Componentes apresentam dados e recebem ações; serviços executam e coordenam comportamentos. Formatação de token e acesso ao armazenamento ficam em serviços próprios. O backend de negócio não gerencia autenticação de usuários. |
| Q02 | Simplicidade exige separação, além de pouco código. | Usar a menor estrutura que mantenha responsabilidades compreensíveis. Não concentrar tudo em um arquivo para reduzir a quantidade de classes; não criar camadas, interfaces, filtros ou endpoints para necessidades futuras. |
| Q03 | Código mockado também precisa de qualidade. | Isolar dados e comportamentos simulados atrás do serviço correspondente. Manter contratos claros e organização que permita trocar a implementação, sem espalhar o mock por telas ou regras de negócio. |
| Q04 | Um mecanismo reutilizável deve ter implementação centralizada. | Toda operação de localStorage passa por `armazenamentoLocal`; toda construção de JWT mockado passa por `jwt`. Novos consumidores reutilizam esses serviços. Não duplicar codificação, persistência ou acesso direto à API do navegador. |
| Q05 | Cada classe, consulta e endpoint precisa atender ao escopo atual. | Explicar sua utilidade no fluxo entregue. Escrever consultas claras; usar joins e abstrações quando houver necessidade concreta. Não generalizar um detalhe pequeno para uma infraestrutura completa. |
| Q06 | O código deve expressar sua intenção. | Usar nomes claros e funções com responsabilidade definida. Evitar comentários explicativos; registrar decisões e justificativas em documentação. Preservar licenças e diretivas necessárias. |
| Q07 | A entrega incremental mantém a qualidade da experiência. | Textos de produto, estados úteis e controles funcionais. Sem avisos de construção, restrições nominais como “Acesso reservado a Jaime e Eliane”, menus fictícios ou números inventados. |
| Q08 | Reduzir escopo não autoriza substituir o fluxo de negócio por uma demonstração. | Persistência financeira usa MySQL real. O mock de acesso é uma exceção explícita de escopo, até a integração externa. Não estender essa exceção a outras funcionalidades por conveniência. |
| Q09 | Qualidade precisa de evidência proporcional à mudança. | Executar lint, typecheck/build e testes pertinentes ao comportamento alterado. Não ignorar testes, suprimir alertas ou forçar dependências para obter sucesso. Evitar testes que apenas repitam a implementação sem verificar um comportamento relevante. |
| Q10 | A evolução deve preservar dados e decisões. | Não reescrever migrations aplicadas; evoluir por novas migrations. Preservar alterações existentes e segredos fora do Git. Atualizar o estado atual e distinguir o que foi verificado do que continua proposto. |

Esses aprendizados não dependem de novos lembretes de Jaime. Cabe ao agente aplicá-los durante a implementação e revisar o próprio trabalho antes de apresentar a entrega.

## Exemplo concreto da organização aprovada

| Parte | Responsabilidade | Limite |
| --- | --- | --- |
| `Login.tsx` e `App.tsx` | Formulário, estados visuais e perfil exibido. | Não constroem JWT nem acessam localStorage diretamente. |
| `services/autenticacao.ts` | Validar credenciais do mock, definir claims e coordenar geração, armazenamento e saída. | Não implementa Base64URL nem operações de armazenamento do navegador. |
| `services/jwt.ts` | Construir o JWT mockado: header, emissão, codificação e montagem. | Não conhece usuários, telas ou persistência. |
| `services/armazenamentoLocal.ts` | Salvar, obter e remover valores por chave. | Não conhece JWT, autenticação ou regras de negócio. |
| Backend de negócio | Persistência e regras do domínio financeiro a implementar. | Não cria usuários, senhas, sessões ou endpoints de autenticação. |

Essa separação é o exemplo aprovado para a necessidade atual. Não obriga a criar um serviço para cada função ou copiar a mesma quantidade de arquivos em todas as funcionalidades.

## Decisões vigentes da entrega encerrada

- Login e saída exclusivamente no frontend, com `jaime` e `eliane` e senhas vazias.
- JWT mockado com `sub`, `name`, role `acesso_total` e `iat`, salvo na chave `lastro.token` e removido ao sair.
- Token sem assinatura (`alg: none`), destinado a apoiar a futura integração das chamadas ao backend. Envio HTTP e validação de acesso ainda não foram implementados.
- Recarregar a página preserva o token, mas não restaura automaticamente o perfil.
- Autenticação real será responsabilidade de um backend externo específico, a implementar depois.
- Banco de negócio persistente, com controle de usuários removido por V4 e estruturas de negócio preservadas.

## Revisão a aplicar em cada tarefa

Antes de implementar, identificar o comportamento esperado, as responsabilidades envolvidas e o que permanece fora da entrega. Consultar os serviços existentes antes de criar uma nova implementação.

Antes de concluir, conferir se a tela contém apenas apresentação, se serviços respeitam seus limites, se há código duplicado ou infraestrutura antecipada, se o mock mantém a organização acordada e se os estados de sucesso/erro e a documentação correspondem ao comportamento real. Executar apenas as verificações pertinentes e registrar seus resultados.

Essa revisão não cria um novo ciclo de aprovação. Escolhas rotineiras autorizadas devem avançar diretamente. A conversa sobre limites da arquitetura de negócio permanece o próximo passo antes de expandir o backend.

## Decisões substituídas

A autenticação local com senhas no MySQL e a identificação por sessão no backend foram retiradas do escopo. Também foi substituída a concentração da criação do JWT e das operações de localStorage em autenticação. Esses desenhos não devem ser reintroduzidos por referência a notas históricas.
