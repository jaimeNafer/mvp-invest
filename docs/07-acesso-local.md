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

É um JWT sem assinatura e sem expiração, usado somente no mock do frontend, disponível para a futura integração das chamadas ao backend. O envio HTTP ainda não foi implementado. Não comprova identidade ou permissão e não deve ser aceito pelas futuras APIs como autorização real. A integração externa substituirá sua emissão pelo token do serviço responsável. Não há sessão HTTP ou cookie de autenticação.

## Limite de responsabilidade

- Tela: recebe campos, apresenta carregamento/erro e chama o serviço.
- Serviço frontend: implementa o acesso mockado atual. Na integração futura, passa a chamar o backend externo sem espalhar dados dos usuários ou regras de autenticação pelos componentes.
- Serviço `jwt`: `gerarTokenMock(claims)` constrói header, inclui `iat`, codifica e monta o JWT sem assinatura. Recebe `sub`, `name` e `role`; não conhece usuários mockados, componentes ou armazenamento. Autenticação define as claims e coordena sua geração e persistência.
- Serviço `armazenamentoLocal`: concentra leitura, gravação e remoção da localStorage, com `obter(chave)`, `salvar(chave, valor)` e `remover(chave)`. É reutilizável e não conhece JWT ou autenticação. O serviço de autenticação decide a chave e o conteúdo do token e delega a persistência.
- Backend de negócio: nenhuma criação/consulta de usuários, senha, identificação, sessão, login/logout ou endpoint de autenticação. Nenhuma regra financeira implementada no serviço mockado.

A integração externa, os contratos de identidade e a forma de validar acesso às APIs serão discutidos antes de implementar. IDs do mock não comprovam identidade nem definem autorização de negócio. Não implementar infraestrutura preventiva de autenticação neste repositório.

## Banco e migrations

V1–V3 permanecem como histórico imutável. V4 remove `usuarios`, `membros_espaco` e a coluna/FK `espacos.criado_por`. A tabela de credenciais já havia sido removida por V3. Espaços, titulares, instituições, ativos e contas são estruturas de negócio e são preservados.

Antes de aplicar V4, foi preservado um backup privado em `.local/banco-antes-remover-usuarios.sql`. Não ler/imprimir backups ou o antigo arquivo de senhas em inspeções gerais, nem versioná-los. Não são usados pelo login.

A conexão técnica ao MySQL continua privada em `.local/mysql.properties`; sua senha é independente do login da aplicação. O backend usa `lastro_app` com acesso somente a `lastro_local`, não root.

## Execução e verificação

`npm run dev` em `frontend` permite usar o login mesmo com o backend desligado. [Frontend](http://127.0.0.1:5173/).

Para o pacote integrado, usar `scripts/Start-Lastro.ps1` e acessar [Lastro](http://127.0.0.1:8080/); `-Build` reconstrói o pacote. Nesse modo, o backend serve os arquivos estáticos, mas não processa o login.

Testes React exercitam o serviço local real, entrada dos dois usuários, conteúdo do JWT, gravação/remoção na localStorage, erro de credenciais e ausência de chamadas HTTP. Testes MySQL isolados verificam migrations, ausência das estruturas de controle de usuários e integridade das estruturas de negócio.
