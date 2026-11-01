# Marco 1 — Fundação reproduzível: tarefas de execução

> Registro histórico da fundação. O estado vigente é login mockado no frontend com autenticação externa futura; as referências abaixo a Security, status, identificação local e credenciais descrevem a implementação anterior e não devem ser reintroduzidas. Consulte [identificação atual](07-acesso-local.md) e [acordos](08-acordos-de-qualidade.md).

Preparado e implementado parcialmente em 01/11/2026. A entrega é uma base executável e verificável, ainda sem funcionalidades financeiras. A lista de tarefas abaixo conserva os critérios de aceite; o estado atual distingue resultados locais e pendências externas.

Atualização posterior no mesmo dia: a conexão de uso já está no MySQL persistente `lastro_local`, por usuário exclusivo `lastro_app`, com migrations e os dois acessos criados. Login/logout e isolamento foram confirmados após reinício. Os registros abaixo sobre uso somente descartável descrevem a fase da fundação; o procedimento atual está em [acesso persistente](07-acesso-local.md). Atualização de publicação: a CI foi [executada com sucesso](https://github.com/jaimeNafer/mvp-invest/actions/runs/41355920250) no PR #11. Os registros de ausência de execução abaixo descrevem o encerramento histórico da fundação.

## Estado de execução

| Tarefa | Estado e evidência |
| --- | --- |
| F01 | Ambiente local suficiente para build e testes; Java 21.0.7, Node 22.15.1/npm 10.9.2 e Docker verificados. Patch Node recomendado em `.nvmrc` não instalado globalmente; credenciais/conexão ao MySQL pessoal não utilizadas. |
| F02 | Estrutura criada; quatro arquivos, testes e sete imagens preservados em `prototype/`. Testes antigos passam a partir da raiz. |
| F03 | Backend executa em loopback, status público e demais rotas protegidas; 3 testes MockMvc passaram. Login ainda não implementado. |
| F04 | Frontend React executa em loopback e foi observado conectado à API real no navegador. Typecheck, lint Biome, 4 testes e build passaram. |
| F05 | V1 acesso e V2 cadastros aplicadas no MySQL descartável; sem usuários ou operações reais. Reexecução de Flyway sem novas migrations verificada. |
| F06 | 4 testes integrados passaram no MySQL 8.0.42/Testcontainers: migrations, constraint de titular/espaço, rollback, precisão de ativo e segurança HTTP. |
| F07 | Workflow com actions fixadas por commit e instruções locais criados; `clean check bootJar -PincludeFrontend` passou e o JAR foi observado no navegador servindo React e API juntos. CI no GitHub não executada: nenhuma mudança foi enviada. |

Ferramentas do frontend: TypeScript 6.0.3, plugin React 6.1.2, Vitest 5.0.5 e Biome 2.6.4, com dependências exatas no lockfile. TypeScript 7 não foi adotado devido à incompatibilidade com o lint inicialmente avaliado. A cadeia desse lint apresentou avisos de segurança e foi substituída pelo Biome; a instalação final reportou zero vulnerabilidades. Não foram usados `--force` ou `--legacy-peer-deps`.

## 1. Ambiente: fatos verificados

| Ferramenta | Evidência local | Ação |
| --- | --- | --- |
| Java | Corretto 21.0.7 no PATH | Compilar para Java 21; conferir patch de manutenção antes do bootstrap. Não alterar o JDK dos projetos da empresa. |
| Node/npm | Node 22.15.1 e npm 10.9.2 no PATH | Node atende ao mínimo documentado do Vite; planejar patch atualizado na linha 22 para este projeto. |
| MySQL | Cliente e executável do servidor 8.0.42 em `C:/Program Files/MySQL/MySQL Server 8.0/bin/` | Instalação confirmada, conexão e servidor ativo ainda não verificados. Não são necessárias credenciais para planejar. |
| Gradle | Instalação global não é requisito | Usar Gradle Wrapper versionado no backend, conforme decisão de Jaime. |
| Docker | Docker Desktop instalado pelo usuário; cliente/engine 29.9.2, contexto `desktop-linux`, Linux/amd64. `hello-world` executou com sucesso em 01/11/2026. | Nesta sessão foi necessário incluir `C:/Users/jaime/AppData/Local/Programs/DockerDesktop/resources/bin` no PATH somente do processo para localizar o auxiliar de credenciais. Conferir herança de PATH no terminal/IDE usado no projeto. |

A inspeção inicial não iniciou serviços, não instalou ferramentas e não conectou ao banco. Após Jaime instalar/iniciar Docker Desktop, hello-world e MySQL/Testcontainers executaram com sucesso. Nenhum PATH global foi alterado. O MySQL pessoal não foi acessado; os bancos usados nos testes e na execução manual de desenvolvimento são descartáveis.

## 2. Baseline técnica do bootstrap

As versões de bibliotecas abaixo estão fixadas nos arquivos do projeto e passaram por resolução de dependências, testes e builds locais. Distribuições globais do usuário não foram substituídas. Node 22.23.4 permanece recomendado para CI; a verificação local usou 22.15.1, também dentro da faixa suportada.

| Componente | Baseline proposta | Como fixar |
| --- | --- | --- |
| Java | Linguagem/runtime 21; instalação encontrada 21.0.7 | `java.version=21`; registrar distribuição e patch efetivamente usados no README e CI. |
| Spring Boot | 4.1.2 | Plugin Gradle com versão exata e BOM Spring Boot para dependências. Java 21 está dentro da faixa suportada documentada. |
| Gradle | 9.8.2, fornecido pelo gerador oficial Spring Initializr | Wrapper com distribuição e checksum; nenhuma instalação global obrigatória. Groovy DSL. |
| Node | 22.23.4 LTS | Arquivo de versão do projeto e CI; preparar uso por projeto para não interferir no trabalho. |
| npm | 10.9.2, já disponível | Registrar a versão usada; lockfile versionado e `npm ci` nas instalações reproduzíveis. Validar junto ao Node alvo. |
| React / React DOM | 19.3.1, iguais entre si | Dependências diretas e lockfile. |
| Vite | 8.3.5 | Dependência de desenvolvimento e lockfile. |
| TypeScript e plugin React | 6.0.3 e 6.1.2 | Versões exatas e lockfile; typecheck/build verificados. |
| MySQL | 8.0.42, alinhado ao executável instalado | Mesma versão no container de integração, sem usar tag `latest`; registrar imagem resolvida/digest. |
| Flyway, JPA, driver e bibliotecas de teste | Gerenciadas pelo BOM Spring Boot quando disponíveis | Registrar resolução Gradle; sobrescrever somente por necessidade demonstrada. Incluir suporte MySQL do Flyway. |

Vite 8 documenta Node 20.19+ ou 22.12+; mesmo assim, a compatibilidade do template e plugins será comprovada pelo build. Não confundir existência de uma versão com validação de toda a combinação.

## 3. Backlog ordenado

### F01 — Fechar ambiente e dependências

**Dependências:** nenhuma. **Tamanho:** pequeno, exceto eventual instalação de Docker.

- Localizar Docker ou registrar a configuração necessária; verificar cliente e engine, não somente o ícone do aplicativo.
- Confirmar Java 21 e Node/npm do projeto; selecionar patch sem alterar configurações globais compartilhadas desnecessariamente.
- Registrar versões de TypeScript/plugin React no bootstrap e as versões transitivas gerenciadas pelo BOM.
- Para o MySQL pessoal, confirmar servidor/porta/schema somente quando a execução local exigir; usar configuração fora do Git.

**Aceite:** versões documentadas; Docker consegue executar um container descartável; o teste MySQL pode usar credenciais próprias geradas para o container, sem acesso ao banco pessoal. Caso Docker ainda não funcione, tarefas independentes podem avançar, mas F06 continua pendente.

### F02 — Organizar o repositório e preservar a referência

**Dependências:** nenhuma. **Tamanho:** pequeno.

- Mover os quatro arquivos do protótipo para `prototype/` e as sete capturas para `prototype/screenshots/`, com nomes legíveis.
- Manter os testes JavaScript antigos em `prototype/tests/` e ajustar somente seus caminhos para continuarem executáveis.
- Manter o pacote de modelagem original como referência histórica; não confundir seu DDL com migrations ativas.
- Atualizar links e instruções afetados; configurar ignores para `target/`, `dist/`, caches, arquivos locais e credenciais.

**Aceite:** protótipo continua abrindo por HTTP e seus testes continuam executando; nenhuma imagem ou referência se perde. Nenhuma regra financeira é reescrita nesta tarefa.

### F03 — Criar o backend mínimo

**Dependências:** F01 para versões; F02 para localização. **Tamanho:** pequeno.

- Gradle com Groovy DSL, Java 21, pacote-base proposto `io.github.jaimenafer.lastro`, nome do projeto `lastro-backend`, aplicação única.
- Incluir MVC, validação, JPA, Security, Flyway/MySQL e dependências de teste compatíveis com a linha Boot escolhida; evitar adicionar clientes externos.
- Versionar `gradlew`, `gradlew.bat`, arquivos de `gradle/wrapper/`, scripts de build e exemplo de configuração. Backend em `127.0.0.1:8080`; segredos somente em configuração local/ambiente.
- Expor somente um endpoint mínimo de status, sem informações sensíveis, para verificar comunicação. Demais rotas protegidas por padrão; o login funcional pertence ao marco 2.
- Incluir teste de contrato do status e de bloqueio de uma rota protegida. Não criar CRUD financeiro vazio nem entidades sem uso.

**Aceite:** wrapper compila/testa; aplicação inicia com banco configurado; status responde; nenhuma rota financeira é aberta por uma configuração temporária de `permitAll` geral.

### F04 — Criar o frontend mínimo

**Dependências:** F01 e F02; integração depende de F03. **Tamanho:** pequeno.

- React/TypeScript com Vite, `strict` habilitado, npm e lockfile. Sem React Compiler, SSR ou gerenciador global adicional neste marco.
- Vite em `127.0.0.1:5173`, porta estrita e proxy `/api` para o backend.
- Tela mínima com identidade visual de referência e estado da comunicação: carregando, disponível ou falhou. Não apresentar a carteira fictícia como dados persistidos.
- Criar cliente HTTP central; manter credenciais do banco exclusivamente no backend. Variáveis `VITE_*` são públicas ao navegador.
- Configurar typecheck, lint e teste de componente que verifica os estados observáveis da conexão.

**Aceite:** `npm ci`, typecheck, lint, testes e build passam. Navegador consulta o status via proxy; falha do backend é apresentada sem travar a tela.

### F05 — Primeiras migrations de acesso e cadastros

**Dependências:** F03 e revisão das relações do modelo. **Tamanho:** médio; dividir acesso e cadastros em tarefas se necessário.

- Criar schema dedicado `lastro_local` somente na configuração/provisionamento local; Flyway atua no schema configurado, sem `CREATE SCHEMA`/`USE` fixo herdado do script original.
- Migrations iniciais para identidade interna, credencial local, espaços/vínculos e cadastros necessários. Usuário sem obrigação de emissor/sujeito OIDC.
- Preservar isolamento e relações de conta/titular/espaço; sem tabelas/FKs de importação ou CPF obrigatório.
- Não incluir usuários reais ou hashes de senha em migration; rotina de provisionamento pertence ao marco 2.
- Hibernate valida, não cria/atualiza schema. Carga fictícia somente em fixture de teste, fora das migrations locais de uso real.

**Aceite:** migration aplica em MySQL vazio; nova inicialização não reaplica; constraints rejeitam vínculos inválidos; nenhum banco existente é apagado ou convertido automaticamente.

### F06 — Testes integrados isolados

**Dependências:** Docker operacional, F03 e F05. **Tamanho:** pequeno/médio.

- Usar MySQL/Testcontainers com versão alinhada ao desenvolvimento e Flyway real; sem H2 e sem credenciais do MySQL pessoal.
- Garantir isolamento entre testes; verificar aplicação das migrations, constraints e rollback relevante. Testes de API usam MockMvc quando verificam comportamento HTTP.
- Se a engine estiver indisponível, falhar com diagnóstico na suíte integrada; não marcar sucesso com todos os testes de banco ignorados.
- Separar testes unitários e de integração em tarefas Gradle, conectando `integrationTest` a `check` para que a verificação completa não omita os testes MySQL.

**Aceite:** a suíte cria, migra, testa e descarta seu banco. Uma falha proposital relevante é detectada. Nenhuma conexão depende de `lastro_local` ou dados reais.

### F07 — Build, CI e instruções de retomada

**Dependências:** F03–F06. **Tamanho:** médio; separar pipeline e execução local se necessário.

- Backend: `./gradlew clean check bootJar` no CI e `.\gradlew.bat clean check bootJar` no PowerShell; `check` inclui testes integrados. Execução de desenvolvimento por `bootRun`. Frontend: `npm ci`, typecheck, lint, testes sem modo interativo e build.
- GitHub Actions em runner com Docker, versões alinhadas e permissões mínimas. Nenhum deploy ou segredo do MySQL pessoal. Fixar actions por commit ao implementar.
- Documentar desenvolvimento com dois processos e procedimento de uso local com build estático do React servido pelo backend. A API não pode cair no fallback HTML das rotas React.
- Documentar configuração de banco, comandos PowerShell, portas, diagnóstico de falhas e como retomar o trabalho. Não presumir que Spring Boot carrega `.env` automaticamente.

**Aceite:** build limpo funciona conforme documentação; pipeline executa de fato os testes de banco; pacote local entrega frontend e API na mesma origem, com escuta restrita ao computador. A CI só estará comprovada após execução real no GitHub.

## 4. Ordem prática e saída do marco

F01 e F02 primeiro; depois F03/F04, F05, F06 e F07. Uma pessoa executa uma tarefa por vez e registra o próximo passo. Não há compromisso de concluir um marco inteiro em um fim de semana.

O marco termina quando outro checkout consegue reproduzir build, testes, migrations e comunicação React/API. Não termina apenas porque existem diretórios ou porque a página abre. Login funcional, cadastros na interface e livro financeiro pertencem aos marcos seguintes.

## Fontes para as versões e compatibilidade

- [Spring Boot 4.1.2: requisitos](https://docs.spring.io/spring-boot/system-requirements.html).
- [Spring Boot: instalação com Gradle e uso do wrapper](https://docs.spring.io/spring-boot/installing.html).
- [Node 22.23.4 LTS](https://nodejs.org/en/blog/release/v22.23.4).
- [Vite: requisitos e template TypeScript](https://vite.dev/guide/).
- [Vite 8.3.5: release oficial](https://github.com/vitejs/vite/releases/tag/v8.3.5).
- [React: releases oficiais](https://github.com/react/react/releases).
