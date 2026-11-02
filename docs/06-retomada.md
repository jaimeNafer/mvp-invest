# Retomada — carteiras e contas compartilhadas

## Estado vigente — 02/11/2026

**Etapa concluída e encerrada por Jaime em 02/11/2026.** Solicitada a publicação do código em PR para `main`, sem merge automático. O próximo trabalho é especificar o primeiro lançamento; não reabrir este cadastro sem novo requisito.

Cadastro de titulares substituído por **Carteiras** no menu, página, modais, serviços e API. Cada carteira tem nome e instituições selecionadas. Uma conta é única por usuário/instituição e pode compor várias carteiras por `carteira_contas`, sem cópia de dados. O usuário vem exclusivamente de `UsuarioAtual.id()`, carregado pelo Bearer. Login/JWT/armazenamento continuam nos serviços existentes, sem gestão de usuários no backend.

Criar/editar, criar contas ausentes e atualizar vínculos ocorre em uma transação. Seleção vazia/repetida/inválida é recusada; novo vínculo exige instituição ativa, enquanto vínculo existente inativo pode ser mantido/removido. Nome normalizado até 160 caracteres e único por usuário, inclusive arquivadas. Erros 400/401/404/409; concorrência de criação de conta reverte a requisição conflitante e permite repetir. JPA com `@EntityGraph`, sem exclusão em cascata.

Ativas e arquivadas permanecem juntas; arquivada permite somente reativar. Modal preserva dados após erro, oferece recuperação de falha do catálogo e bloqueia controles durante gravação. Remover instituição remove somente o vínculo; arquivar preserva contas e associações. Catálogo inicial: Binance e BitGet, ativas, país não informado. Sem cadastro independente de contas ou manutenção de instituições pelo usuário.

Contratos atuais: GET `/api/v1/instituicoes`; GET/POST `/api/v1/carteiras`; PUT `/api/v1/carteiras/{id}`; PATCH `/api/v1/carteiras/{id}/situacao`. Rotas de titulares removidas. IDs de resposta são strings. [Contrato completo](09-cadastro-de-carteiras.md), [modelagem](10-modelagem-de-carteiras.md) e [dicionário](11-dicionario-de-dados.md).

## Verificações desta entrega

- Frontend: lint, 13 testes React e build/TypeScript aprovados. Cobertura de formulário/catálogo, vazio/erro/nova tentativa, CRUD, arquivadas, contexto, foco, bloqueio durante gravação e preservação dos dados após falha. Ajuste final de texto/estilo conferido por lint e novo build.
- Backend: `check` aprovado, 23 testes (5 unitários e 18 integrados MySQL/Testcontainers), sem falhas ou ignorados. São 11 de carteiras, 2 de V8, 4 da fundação e 1 histórico de titulares até V7, além dos unitários do contexto.
- Conferidos compartilhamento do ID de conta em várias carteiras, conta distinta de outro usuário, isolamento/404, remoção de vínculos, arquivamento, instituições inativas, conflitos/rollback e requisições simultâneas. Duas criações concorrentes da mesma conta retornam 201/409; nova tentativa funciona sem duplicação.
- Migrations históricas até V7 preservadas nos testes. V7 → V8 cobre ativos preservados, cadastros vazios, seed, constraints, FKs, comentários, reaplicação sem mudanças e bloqueio antes de alterar tabelas quando existem cadastros antigos.
- Pacote integrado `bootJar -PincludeFrontend` aprovado. V8 aplicada no MySQL local após nova conferência: titulares, contas e instituições com zero registros, aplicação parada e backup privado em `.local/banco-antes-carteiras-v8-20261102-012734.sql`. Flyway validou as oito migrations e atualizou 7 → 8.
- Banco local: cinco tabelas de negócio, 23/23 campos comentados; catálogo Binance/BitGet e ausência de titulares. Conta Binance reutilizada em duas carteiras criadas pelo navegador. Remoção do vínculo BitGet manteve sua conta no banco com zero associações.
- Navegador: criação com duas instituições, segunda carteira com Binance, arquivamento permanecendo na lista, edição desativada/reativação, edição dos vínculos e troca para Eliane com lista separada. Capturas privadas em `.local/carteiras-v8-*.png`.
- HTTP local confirmou frontend atualizado 200, catálogo inicial, carteiras sem IDs em comum entre usuários, ausência de Bearer 401 e rota antiga de titulares 404. A carteira temporária adicional foi removida pelo ID/nome exatos; cadastros criados ou renomeados pelo usuário durante a conferência e todas as contas foram preservados.

## Ambiente e próximos passos

Aplicação integrada em [http://127.0.0.1:8080/](http://127.0.0.1:8080/), iniciada por `scripts/Start-Lastro.ps1`; `-Build` reconstrói. Recarregar abas antigas. MySQL persistente `lastro_local`, configuração privada `.local/mysql.properties`; Docker somente para testes isolados. Não versionar ou imprimir credenciais/backups.

Operações futuras pertencem à conta. Carteiras representam seleções atuais de contas completas, sem divisão por operações, quantidades ou períodos. Consultas de várias carteiras deduplicam contas por ID. Próxima decisão: especificar o primeiro lançamento e detalhar política decimal/ativos antes do livro. Importação, motor, relatórios, backoffice, integração com corretoras e autenticação real permanecem fora desta entrega.

Publicação desta entrega solicitada em PR para `main`; os resultados remotos devem ser conferidos nos checks desse PR. A CI do PR #11 abaixo refere-se ao login. O histórico a seguir registra decisões e verificações anteriores, substituídas pelo estado vigente acima.

## Histórico — cadastro anterior e documentação V5–V7

## Estado vigente

Em 01/11/2026, Jaime aprovou simplificar a modelagem antes do cadastro de contas e confirmou manter os titulares separados por perfil. O titular é a referência dos dados financeiros; contas pertencem ao titular e podem referenciar uma instituição. Espaços deixaram de fazer parte do schema. [Modelo, diagrama e exemplos](10-modelagem-de-carteiras.md).

V1–V4 permanecem imutáveis. V5 migrou a associação antiga para `titulares.perfil_ref`, removeu `espacos`, `espaco_id` e `revisao_livro`, e recriou a FK de contas diretamente para o titular. Preserva IDs, nomes, situações, datas e vínculos. Não criou usuários, credenciais, grupos ou campos de concorrência antecipados.

Novo requisito posterior de Jaime: identificar a requisição pelo Bearer e disponibilizar contexto injetável nas services. `UsuarioAtualFilter` lê o JWT por `LeitorJwtMock` e carrega `UsuarioAtual` com `id()`, `nome()` e `role()`, em escopo de requisição. `TitularService` obtém a referência diretamente desse contexto; controllers/componentes/serviços frontend não passam ID de perfil. `X-Perfil-Id` foi substituído e não é fallback.

API vigente: `/api/v1/titulares`, com `Authorization: Bearer <token>` obrigatório. `sub` determina o perfil dos registros; `name` e `role` também ficam disponíveis no contexto. Ausência/estrutura inválida retorna 401; entrada de negócio inválida continua 400 e titular inexistente/de outro perfil retorna 404. O mock aceita somente `alg: none`, assinatura vazia e claims textuais válidas, sem comprovar autenticidade ou permissão. Não há gestão de usuários, credenciais ou sessões. [Contrato do contexto](07-acesso-local.md).

A tela mantém ativos e arquivados na mesma lista, modal para adicionar/editar e ícones no padrão visual. Arquivado permanece desativado com somente reativar habilitado. Textos que sugeriam um espaço foram ajustados. Login, JWT e armazenamento continuam separados e mockados no frontend.

## Evidências da revisão

- Lint, 12 testes React, TypeScript/build e 19 testes backend passaram: 5 unitários e 14 MySQL/Testcontainers (4 da fundação, 9 do cadastro/contexto e 1 de atualização V4 → V5). Sem falhas ou testes ignorados. Conferidos ID/nome Unicode/role, tokens malformados, ausência de Bearer, header/corpo sem substituir `sub` e 20 requisições simultâneas de dois usuários.
- O teste de atualização preserva titulares/contas, inclusive arquivado e contas de mesmo nome de titulares diferentes; verifica referências dos perfis, FK, unicidade por titular, ausência das estruturas removidas e reaplicação sem mudanças.
- Pacote integrado atualizado e V5 aplicada no MySQL persistente. Backup privado gerado antes da alteração e renovado com a aplicação parada. Comparação antes/depois confirmou os seis titulares com IDs, referências, nomes, situações e datas preservados; não havia contas no banco de uso.
- Na conferência anterior da V5, perfil 1 manteve dois titulares e perfil 2 manteve quatro, sem IDs em comum. Essa etapa usava o header de perfil, agora substituído pelo Bearer. Não há nova migration ou alteração de dados nesta mudança de contexto.
- Pacote integrado reconstruído após a mudança de contexto, verificações finais sem aviso de API obsoleta e aplicação reiniciada em 8080. HTTP real confirmou o bundle atualizado, dois titulares para o Bearer de Jaime e quatro para o de Eliane, listas sem IDs em comum e `X-Perfil-Id` conflitante sem alterar o usuário. Bearer ausente/malformado e header antigo sozinho retornaram ProblemDetail 401 com `WWW-Authenticate: Bearer`. Nenhum cadastro foi alterado nessa conferência.
- Navegador: login de Jaime e Eliane e troca de perfil conferidos com o novo bundle/API; cada um vê seus titulares preservados, incluindo arquivados. Modal de criação abre/cancela sem gravar. Captura privada em `.local/lastro-titulares-modelagem-v5.png`. Nenhum cadastro real foi alterado na conferência.

## Ambiente e continuidade

Schema vigente em V7: as quatro tabelas de negócio e seus 24 campos têm comentários breves, limitados à finalidade e às opções dos campos de valores restritos. A V7 corrige o detalhamento excessivo da V6; nenhuma migration anterior foi reescrita. Fonte campo a campo: [dicionário de dados vigente](11-dicionario-de-dados.md); o modelo v1 continua histórico.

Verificação desta revisão: `check` e pacote integrado passaram, com os mesmos 19 testes backend (5 unitários e 14 integrados). O teste de atualização cobre V4 → V5 → V6 → V7 e compara dados e metadados ao revisar comentários. Frontend não foi alterado nem teve suas verificações repetidas.

V6 aplicada ao MySQL local após backup privado com a aplicação parada, em `.local/banco-antes-comentarios-v6-*.sql`. Comparação antes/depois confirmou preservação dos dados, tipos, defaults, nulabilidade, collations, índices, FKs, CHECKs e contadores de IDs. Cobertura conferida: titulares 5/5 campos, instituições 5/5, ativos 7/7 e contas 7/7, além dos quatro comentários de tabela. A tabela técnica do Flyway não foi modificada. Aplicação reiniciada e respondendo em 8080.

V7 aplicada após novo backup privado em `.local/banco-antes-comentarios-v7-*.sql`. Confirmados os 24 comentários breves, sem detalhes de mock, classes ou transporte HTTP, com as opções de tipos listadas. Comparação confirmou dados e definições preservados; aplicação respondendo em 8080. Padrão registrado em AGENTS e no acordo Q15.

Aplicação integrada disponível em 8080 por `scripts/Start-Lastro.ps1`; `-Build` reconstrói. Frontend de desenvolvimento usa 5173. Recarregar abas antigas para obter o novo bundle/API. MySQL local `lastro_local` em 127.0.0.1:3306, usuário técnico `lastro_app`, configuração privada em `.local/mysql.properties`. Docker é necessário para os testes isolados.

Backups e referências de comparação ficam somente em `.local/`, inclusive `banco-antes-modelagem-titulares-v5.sql` e uma cópia com data/hora imediatamente anterior à aplicação. Não ler/imprimir seu conteúdo em inspeções gerais ou versionar. Eles não são utilizados pela aplicação.

Próxima fatia: cadastro de contas por titular, reutilizando HTTP, token, armazenamento e componentes existentes. Motor, revisão do histórico por titular e consolidação ainda precisam ser implementados. Grupos salvos ficam adiados; quando necessários, vinculam titulares existentes, sem copiar histórico ou alterar cálculos individuais. [Contrato](09-cadastro-de-carteiras.md).

Alterações locais na branch `codex/planejamento-cadastros`, ainda sem commit/publicação. A CI do PR #11 abaixo refere-se à primeira feature de login, não à revisão de modelagem. Os registros seguintes preservam a cronologia; decisões antigas sobre espaços, associação no frontend e próximos passos foram substituídas pelo estado vigente acima.

## Registro da etapa anterior

Encerrada por Jaime em 01/11/2026. Concluída a tela de login com autenticação mockada no frontend, construção do JWT e armazenamento local. O marco de cadastros e as funcionalidades financeiras continuam pendentes.

## Estado entregue

- Usuários mockados `jaime` e `eliane`, ambos com senha vazia. Login inválido apresenta erro; login e saída não fazem chamadas HTTP.
- JWT com `sub`, `name`, role `acesso_total` e `iat`; header `alg: none`, assinatura vazia. Não contém senha.
- Token salvo em localStorage sob `lastro.token`; sair remove o token e limpa o perfil. Recarregar preserva o token, mas retorna ao formulário sem restauração automática.
- Autenticação real será feita por backend externo específico. O backend de negócio não possui usuários, sessões ou endpoints de acesso.
- O token está disponível para a futura integração HTTP. Ainda não há envio em chamadas de negócio nem validação de permissões implementados.

## Responsabilidades consolidadas

| Arquivo | Responsabilidade |
| --- | --- |
| `frontend/src/Login.tsx` e `App.tsx` | Formulário e estados da interface. |
| `frontend/src/services/autenticacao.ts` | Credenciais do mock e coordenação de geração/armazenamento do token e saída. |
| `frontend/src/services/jwt.ts` | Header, emissão, Base64URL e montagem do JWT mockado. |
| `frontend/src/services/armazenamentoLocal.ts` | Único acesso à localStorage: salvar, obter e remover. |

Regras obrigatórias nas próximas tarefas: [AGENTS.md](../AGENTS.md). Aprendizados e critérios de revisão: [acordos de qualidade](08-acordos-de-qualidade.md). Não esperar novos lembretes de Jaime para aplicar essas regras, inclusive em mocks.

## Evidências já verificadas

- Última alteração de código: lint, 6 testes React e build/TypeScript passaram. Cobrem os dois usuários, erro de credenciais, conteúdo do JWT, armazenamento, saída e ausência de chamadas HTTP.
- Última alteração do backend/schema: 4 testes integrados MySQL/Testcontainers passaram, sem falhas ou testes ignorados. Não foram repetidos nas refatorações exclusivas do frontend.
- Navegador: login, troca de usuário e recarga conferidos com o backend desligado antes das extrações de serviços; os testes automatizados passaram após as extrações.
- V4 aplicada no MySQL persistente após backup privado: controle de usuários removido e cinco tabelas de negócio preservadas. IDs, nomes, revisão e criação dos espaços conferidos. Endpoints antigos de acesso retornam 404.
- Pacote reconstruído e aplicação iniciada em 8080 após a extração do serviço JWT. Vite de desenvolvimento usa 5173.
- Primeira feature publicada na branch codex/login-jwt-mock, com [PR #11](https://github.com/jaimeNafer/mvp-invest/pull/11) integrado em main por Jaime. CI de push e PR aprovada; não foi usado merge automático.

Esta atualização de encerramento altera somente documentação; não representa uma nova execução das suítes acima.

## Ambiente e cuidados de retomada

Executar `scripts/Start-Lastro.ps1` e abrir [Lastro](http://127.0.0.1:8080/); `-Build` reconstrói o pacote. `npm run dev` em `frontend` permite usar o login sem backend, em 5173.

MySQL persistente `lastro_local` em 127.0.0.1:3306, usuário técnico `lastro_app`, configuração privada em `.local/mysql.properties`. Aplicação não usa root. Backups antigos em `.local/` não são utilizados pelo aplicativo: não ler/imprimir em inspeções gerais ou versionar.

V1–V3 são histórico imutável; V4 remove `usuarios`, `membros_espaco` e `espacos.criado_por`. Preservar as estruturas de negócio. Docker é necessário somente para os testes isolados.

Java Corretto 21.0.7, Gradle Wrapper 9.8.2/Groovy, Boot 4.1.2, Node local 22.15.1/npm 10.9.2 (recomendado 22.23.4), React 19.3.1, TypeScript 6.0.3 e Vite 8.3.5. Não alterar instalações globais.

## Próximo passo

Discutir com Jaime os limites da arquitetura de negócio antes de implementar cadastros ou novas camadas: responsabilidades, consultas, critérios para extrair classes e tamanho das entregas. Não reabrir a construção do login sem novo requisito.

Cadastros de titulares/contas, integração das chamadas com o token, contrato de identidade externa, motor financeiro, conferência, relatórios e recuperação permanecem pendentes. [Contrato e operação do acesso](07-acesso-local.md).

## Preparação da primeira publicação

Branch `codex/login-jwt-mock`, destinada a PR para `main`. Código, testes, documentação, sete imagens do protótipo, modelo MySQL descompactado, lockfile e Gradle Wrapper fazem parte da entrega. ZIP duplicado preservado localmente e ignorado, junto com segredos, backups, dependências, builds e metadados.

Verificação anterior ao envio: testes do protótipo, lint, 6 testes React, build/TypeScript e `gradlew check --rerun-tasks` com 4 testes integrados MySQL passaram. A checagem das exclusões confirmou a proteção dos caminhos privados e a inclusão dos arquivos necessários. A [execução remota](https://github.com/jaimeNafer/mvp-invest/actions/runs/41355920250) passou no PR #11, assim como a execução disparada pelo push.

## Publicação verificada

Commit da feature: `9c9c495841a6f88e5d44b64d9999b663c9a94869`. [PR #11](https://github.com/jaimeNafer/mvp-invest/pull/11) integrado por Jaime em `main`, sem merge automático. O merge é `8e50dd0c782dd9f787c10f617bbef09e272f9f94`.

A [CI do PR](https://github.com/jaimeNafer/mvp-invest/actions/runs/41355920250) e a [CI do push](https://github.com/jaimeNafer/mvp-invest/actions/runs/41355918541) passaram: testes do protótipo, instalação e verificações do frontend, build do backend, migrations, testes MySQL/Testcontainers e empacotamento integrado. Não houve necessidade de corrigir o comportamento da aplicação para publicar.

O Git do Windows recusou a credencial salva para push. A publicação foi realizada pela conexão autenticada do GitHub, com hash da árvore conferido contra o commit local. A branch local foi sincronizada ao conteúdo publicado e possui upstream; a referência do commit local original foi preservada. A credencial do terminal não foi alterada: futuros pushes por CLI exigem reautenticação ou uso da conexão do GitHub.

## Início da próxima etapa

Jaime confirmou o merge do PR #11 e pediu o próximo passo. A main local foi atualizada para o merge. A atualização documental posterior ao merge foi preservada por cherry-pick na branch local `codex/planejamento-cadastros`, sem perder arquivos ou publicar código novo de negócio.

Escolhas confirmadas: Spring Data JPA para os primeiros cadastros e espaços separados para Jaime e Eliane, com associação explícita no frontend entre perfil mockado e espaço de negócio. A organização de controller, service, repository e entity foi apresentada como proposta para titulares. Espaço agrupa titulares/contas, sem representar autenticação ou gestão de usuários. Os contratos da API continuam a definir. Consultar [proposta do cadastro de titulares](09-cadastro-de-carteiras.md).
