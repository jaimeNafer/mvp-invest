# Retomada — etapa de login e JWT concluída

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
- Primeira feature publicada na branch codex/login-jwt-mock, com [PR #11](https://github.com/jaimeNafer/mvp-invest/pull/11) aberto para main. CI de push e PR aprovada; sem merge automático.

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

Commit da feature: `9c9c495841a6f88e5d44b64d9999b663c9a94869`. [PR #11](https://github.com/jaimeNafer/mvp-invest/pull/11) aberto de `codex/login-jwt-mock` para `main`, sem merge automático.

A [CI do PR](https://github.com/jaimeNafer/mvp-invest/actions/runs/41355920250) e a [CI do push](https://github.com/jaimeNafer/mvp-invest/actions/runs/41355918541) passaram: testes do protótipo, instalação e verificações do frontend, build do backend, migrations, testes MySQL/Testcontainers e empacotamento integrado. Não houve necessidade de corrigir o comportamento da aplicação para publicar.

O Git do Windows recusou a credencial salva para push. A publicação foi realizada pela conexão autenticada do GitHub, com hash da árvore conferido contra o commit local. A branch local foi sincronizada ao conteúdo publicado e possui upstream; a referência do commit local original foi preservada. A credencial do terminal não foi alterada: futuros pushes por CLI exigem reautenticação ou uso da conexão do GitHub.
