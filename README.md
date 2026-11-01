# Lastro — clareza para suas contas

Aplicação para organizar operações de cripto à vista, entender custos e resultados e preparar informações para conferência com o contador.

## Estado da entrega

Etapa de tela de login e mock de JWT concluída. Serviços separados para autenticação mockada, construção do token e armazenamento local; usuários Jaime e Eliane. O backend não gerencia usuários ou sessões; autenticação real será feita por um backend externo específico. O token está disponível para futura integração HTTP, ainda não implementada. MySQL permanece para o domínio de negócio. Cadastros e motor financeiro ainda não foram implementados.

Acordos obrigatórios de desenvolvimento: [AGENTS.md](AGENTS.md). Planejamento e estado de execução: [docs/README.md](docs/README.md).

## Executar o aplicativo

Em PowerShell 7, na raiz:

```powershell
.\scripts\Start-Lastro.ps1
```

Abra [Lastro](http://127.0.0.1:8080/). O JAR serve frontend e backend juntos. A aplicação usa o MySQL persistente configurado; Docker não é necessário para utilizar o aplicativo.

Para reconstruir o pacote antes de iniciar:

```powershell
.\scripts\Start-Lastro.ps1 -Build
```

Requisitos: Java 21, serviço MySQL ativo e configuração privada. Para build do frontend, Node 22.23.4 recomendado e npm. Gradle é fornecido pelo wrapper.

## Conexão e acessos locais

A configuração particular fica em `.local/mysql.properties`, ignorada pelo Git. Chaves:

```properties
lastro.database.url=jdbc:mysql://127.0.0.1:3306/lastro_local?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true
lastro.database.username=lastro_app
lastro.database.password=
```

Na instalação deste computador, `lastro_local` e `lastro_app` já foram criados e as migrations aplicadas. O usuário da aplicação tem permissões apenas nesse schema; a credencial administrativa não é usada pelo backend nem foi salva no projeto.

Para entrar, informe `jaime` ou `eliane` e deixe a senha vazia. O serviço mockado valida os dados no frontend, sem consultar o backend/MySQL. Entrar salva um JWT mockado na localStorage (`lastro.token`), com nome e role `acesso_total`; sair remove o token e limpa o perfil. Atualizar a página mantém o token, mas retorna ao formulário de login. [Responsabilidades e operação](docs/07-acesso-local.md).

`LASTRO_CONFIG_PATH` pode indicar outro arquivo privado. Variáveis `LASTRO_DB_URL`, `LASTRO_DB_USER` e `LASTRO_DB_PASSWORD` sobrescrevem os valores privados. Spring Boot não carrega `.env` automaticamente. Nunca versionar credenciais ou dados reais.

## Desenvolvimento com frontend separado

Backend, dentro de `backend`:

```powershell
.\gradlew.bat bootRun
```

Frontend, em outro terminal, dentro de `frontend`:

```powershell
npm ci
npm run dev
```

Abra [frontend de desenvolvimento](http://127.0.0.1:5173/). O login funciona mesmo com o backend desligado. Vite encaminha futuras chamadas de negócio em `/api` ao backend persistente em 8080. Ambos escutam em loopback. Não execute `bootRun` e o JAR simultaneamente na mesma porta.

## Verificar o projeto

Na raiz:

```powershell
node --test prototype/tests/ledger.test.cjs
```

Dentro de `frontend`:

```powershell
npm ci
npm run lint
npm test
npm run build
```

Dentro de `backend`, com Docker Desktop ativo:

```powershell
.\gradlew.bat check bootJar -PincludeFrontend
```

`check` inclui testes de migrations e integridade do banco com MySQL/Testcontainers. Os testes de acesso ficam no frontend. Esses testes desabilitam a importação da configuração privada e não usam o banco persistente. Docker indisponível faz a suíte integrada falhar; não há H2 nem descarte silencioso dos testes. Em Linux/CI, usar `bash gradlew`.

`localTestRun` existe apenas para verificações isoladas e não preserva dados. Para o fluxo real, usar a conexão persistente.

## Organização

- `backend/`: Java 21, Spring Boot, Gradle e Flyway/MySQL.
- `frontend/`: React, TypeScript, Vite, Biome e testes de componentes.
- `prototype/`: protótipo original, testes e capturas de referência preservados.
- `docs/`: escopo, arquitetura, acordos, entregas e validação.
- `scripts/`: execução local.
- `lastro-modelo-mysql-v1/`: proposta histórica do banco; seus scripts não são as migrations da aplicação.

O workflow [.github/workflows/verify.yml](.github/workflows/verify.yml) verifica builds e testes sem deploy ou credenciais pessoais. O resultado remoto deve ser consultado nos checks do pull request; validação local não substitui a execução da CI.

## Arquivos no Git

Versionar código, testes, migrations, documentação, lockfile npm, Gradle Wrapper e as sete imagens de referência do protótipo. O modelo MySQL descompactado permanece como referência histórica.

O `.gitignore` mantém fora do repositório configurações privadas em `.local/`, arquivos de ambiente pessoais, backups locais, dependências, caches, builds, cobertura, logs e metadados de ferramentas. O ZIP original do modelo é uma cópia duplicada: permanece no computador e não é enviado. Não ignorar genericamente imagens, SQL ou JAR, pois existem referências, migrations e arquivos do wrapper necessários nessas extensões.

Próximo passo: discutir o controle da arquitetura antes de implementar cadastros reais de titulares e contas por perfil. Importação CSV, recomendações de investimentos e cálculo automático de tributos permanecem fora do MVP atual.
