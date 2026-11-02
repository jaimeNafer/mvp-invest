# Acesso pelo frontend e autenticação externa futura

Etapa de tela de login e mock de JWT concluída e encerrada por Jaime em 01/11/2026. Este documento descreve o contrato entregue. Aprendizados e regras a aplicar nas próximas tarefas: [acordos de qualidade](08-acordos-de-qualidade.md).

Decisão vigente de Jaime em 01/11/2026: o backend deste projeto contém regras e persistência de negócio. Autenticação será responsabilidade de outro backend específico. Enquanto essa integração não existe, o frontend usa um serviço mockado com os dois usuários.

Substitui as decisões anteriores de autenticação local e de identificação persistente por sessão no backend.

## Como entrar

| Usuário | Senha do mock |
| --- | --- |
| `jaime` | Deixar vazia |
| `eliane` | Deixar vazia |

A tela recebe usuário e senha e chama `entrar(login, senha)` em `frontend/src/services/autenticacao.ts`. O serviço devolve uma Promise com `id`, `login` e `name`, sem expor o campo de senha ao componente. Credenciais diferentes retornam erro genérico. Não existe consulta ao MySQL ou chamada HTTP nesse fluxo.

As senhas vazias mantêm a decisão anterior de Jaime. Não são credenciais reais ou proteção de acesso. Os dados do mock são públicos no bundle do frontend.

O perfil exibido fica no estado React. Por solicitação posterior de Jaime, entrar também grava um JWT mockado na chave `lastro.token` da localStorage. Sair remove o token, limpa o perfil e o formulário. Recarregar a página preserva o token, mas continua retornando ao formulário: restauração automática do login não foi solicitada.

O token tem header `{"alg":"none","typ":"JWT"}`, payload com `sub` (ID do usuário), `name` (Jaime ou Eliane), `role` (`acesso_total`) e `iat` (instante de emissão em segundos). Usa os três segmentos JWT, com assinatura vazia, sem dependências adicionais. Não contém senha.

É um JWT sem assinatura e sem expiração. O cliente HTTP compartilhado envia `Authorization: Bearer <token>`. O backend lê as claims para identificar a requisição e separar carteiras/contas por usuário, sem usar `X-Perfil-Id`. O token continua mockado e manipulável: essa leitura não comprova autenticidade ou permissão. A integração externa substituirá a emissão e deverá validar assinatura, emissor, público destinatário, validade e permissões. Não há sessão HTTP ou cookie de autenticação.

## Limite de responsabilidade

- Tela: recebe campos, apresenta carregamento/erro e chama o serviço.
- Serviço frontend: implementa o acesso mockado atual. Na integração futura, passa a chamar o backend externo sem espalhar dados dos usuários ou regras de autenticação pelos componentes.
- Serviço `jwt`: `gerarTokenMock(claims)` constrói header, inclui `iat`, codifica e monta o JWT sem assinatura. Recebe `sub`, `name` e `role`; não conhece usuários mockados, componentes ou armazenamento. Autenticação define as claims e coordena sua geração e persistência.
- Serviço `armazenamentoLocal`: concentra leitura, gravação e remoção da localStorage, com `obter(chave)`, `salvar(chave, valor)` e `remover(chave)`. É reutilizável e não conhece JWT ou autenticação. O serviço de autenticação decide a chave e o conteúdo do token e delega a persistência.
- Backend: filter identifica a requisição a partir do Bearer e disponibiliza contexto injetável. Não cadastra/consulta usuários, processa senhas, emite tokens, mantém sessões ou expõe login/logout. Regras financeiras não ficam no filter ou no leitor JWT.

A integração externa e a validação real de acesso serão definidas antes de substituir o mock. Não implementar gestão de usuários ou infraestrutura preventiva neste repositório. A leitura do Bearer e o contexto por requisição foram autorizados explicitamente por Jaime, substituindo a proibição anterior de qualquer identificação no backend.

Carteiras e contas contêm `perfil_ref`, referência do usuário proprietário. A `CarteiraService` obtém essa referência por `UsuarioAtual.id()`, que corresponde ao `sub` do token. O valor não é recebido do corpo, query string, header de perfil ou parâmetro do controller. [Modelagem e limites](10-modelagem-de-carteiras.md).

## Contexto do usuário por requisição

Fluxo: `Authorization: Bearer` → `UsuarioAtualFilter` → `LeitorJwtMock` → `UsuarioAtual` → service → repository.

| Parte | Responsabilidade |
| --- | --- |
| `LeitorJwtMock` | Ler a estrutura JWT/Base64URL/JSON do contrato mockado e extrair `sub`, `name` e `role`. Aceita somente `alg: none` com assinatura vazia; não valida identidade ou implementa criptografia. |
| `UsuarioAtualFilter` | Executar uma vez na requisição às APIs, carregar o contexto antes do controller/service e responder 401 para Bearer ausente ou inválido. |
| `UsuarioAtual` | Contexto com escopo de requisição, injetável pelo construtor. Expõe `id()`, `nome()` e `role()`, sem disponibilizar o token bruto. |
| Services de negócio | Usar o contexto injetado para obter a referência responsável. Não ler servlet/headers, decodificar JWT ou receber ID de perfil pelos controllers. |

O filter cobre `/api` e `/api/**`. Página inicial, arquivos estáticos e despacho de erro não exigem token. A leitura ocorre antes da cadeia de negócio; falhas de validação/persistência não são capturadas e convertidas em erro de token.

Claims obrigatórias textuais e não vazias: `sub` até 120 caracteres, `name` até 160 e `role` até 120. Tokens acima de 8192 caracteres, estrutura inválida, Base64URL/JSON inválidos, claims inválidas e formatos incompatíveis com o mock retornam 401 com ProblemDetail e `WWW-Authenticate: Bearer`. A mensagem não reproduz token ou detalhes internos. O contrato mockado não implementa expiração ou autorização por role; `role()` apenas disponibiliza a informação recebida.

O contexto é criado por requisição, sem singleton de dados do usuário, estado estático ou ThreadLocal manual. Ao terminar a requisição, o escopo é encerrado pelo Spring. Não há contexto de usuário disponível para tarefas de background fora de uma requisição; ao criar essa capacidade, definir explicitamente seus dados de entrada.

Referências da implementação: [escopo de requisição do Spring](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/context/annotation/RequestScope.html), [OncePerRequestFilter](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/filter/OncePerRequestFilter.html) e [JSON/Jackson do Spring Boot](https://docs.spring.io/spring-boot/reference/features/json.html).

## Banco e migrations

V1–V4 permanecem como histórico imutável. V4 removeu `usuarios`, `membros_espaco` e a coluna/FK `espacos.criado_por`; credenciais já haviam sido removidas por V3. V5 remove espaços/`espaco_id`/`revisao_livro`, preserva titulares e contas e migra a associação antiga para `perfil_ref`. A referência não é um cadastro de usuários ou uma credencial.

V8 substitui titulares por carteiras e contas do usuário. `carteiras.perfil_ref` e `contas.perfil_ref` usam a mesma referência externa; os registros são separados pelo contexto. Carteiras reutilizam contas por instituição, sem nova responsabilidade de autenticação no backend de negócio.

Antes de aplicar V4, foi preservado um backup privado em `.local/banco-antes-remover-usuarios.sql`. Não ler/imprimir backups ou o antigo arquivo de senhas em inspeções gerais, nem versioná-los. Não são usados pelo login.

A conexão técnica ao MySQL continua privada em `.local/mysql.properties`; sua senha é independente do login da aplicação. O backend usa `lastro_app` com acesso somente a `lastro_local`, não root.

## Execução e verificação

`npm run dev` em `frontend` permite usar o login mesmo com o backend desligado. [Frontend](http://127.0.0.1:5173/).

Para o pacote integrado, usar `scripts/Start-Lastro.ps1` e acessar [Lastro](http://127.0.0.1:8080/); `-Build` reconstrói o pacote. Nesse modo, o backend serve os arquivos estáticos, mas não processa o login.

Testes React exercitam o serviço local real, entrada dos dois usuários, conteúdo do JWT, gravação/remoção na localStorage, erro de credenciais e ausência de chamadas HTTP. Testes MySQL isolados verificam migrations, ausência das estruturas de controle de usuários e integridade das estruturas de negócio.

Verificação do contexto: 5 testes unitários de leitura/filter e 14 testes MySQL passaram, incluindo 20 requisições simultâneas de dois usuários, claims Unicode, tokens malformados, substituição indevida do perfil, erros de negócio preservados e arquivos estáticos independentes de token. Os 12 testes frontend, lint, TypeScript e build passaram. A API real em 8080 foi conferida com Bearers dos dois perfis e respostas 401 para acesso sem token/inválido, sem alterar dados.
