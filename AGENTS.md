# Acordos de desenvolvimento do Lastro

Estas instruções se aplicam ao repositório inteiro. Foram solicitadas por Jaime em 01/11/2026 e devem ser lidas antes de implementar ou revisar mudanças. Manter este arquivo atualizado com novos acordos explícitos.

## Qualidade desde a primeira entrega

- Desenvolvimento incremental reduz o escopo da entrega, não o padrão de qualidade do código ou da experiência.
- Implementar cada funcionalidade com estrutura sustentável e critérios de aceite. Não introduzir atalhos provisórios que exijam reescrever a funcionalidade depois.
- Priorizar nomes claros, funções com responsabilidade definida e separação entre apresentação, aplicação, domínio e persistência. Extrair responsabilidades quando isso melhorar a compreensão; não criar camadas sem necessidade.
- Evitar comentários explicativos no código. Melhorar nomes e estrutura para que o código expresse sua intenção. Contexto, justificativas, limitações e procedimentos pertencem à documentação.
- Não contornar problemas com supressões de lint, dependências forçadas, desativação de validações ou testes ignorados. Resolver a causa. Preservar avisos legais/licenças e diretivas necessárias de ferramentas.
- Não reescrever migrations já aplicadas apenas para adequar comentários ou estilo; evoluir o schema por novas migrations.
- Por pedido de Jaime, documentar tabelas e campos de negócio com metadados `COMMENT` no MySQL e manter `docs/11-dicionario-de-dados.md` atualizado. Isso não altera o acordo de evitar comentários explicativos no código Java/TypeScript. Para adicionar/alterar comentários de schema já aplicado, usar nova migration preservando tipos, defaults, nulabilidade, collations, índices, vínculos e dados.
- Comentários do schema devem ser breves, objetivos e apropriados à produção: descrever apenas a finalidade do campo ou tabela e listar opções de campos com valores restritos. Não incluir mocks, etapas de desenvolvimento, classes, transporte HTTP, justificativas ou explicações sobre o que o campo não representa. Contexto de implementação pertence à documentação técnica, fora dos comentários e descrições dos campos.
- Manter formatação consistente e executar testes pertinentes ao comportamento alterado.
- Aplicar o mesmo padrão de organização e separação de responsabilidades ao código mockado. Mocks substituem uma implementação dentro do escopo acordado; não dispensam contratos claros ou serviços reutilizáveis.

## Interface compatível com produção

- Telas, textos, estados e controles entregues devem ser apropriados a uma versão final de produção, mesmo quando poucas funcionalidades estiverem disponíveis.
- Não expor bastidores do desenvolvimento na interface: fases, marcos, roadmap, avisos de construção, referências a mocks/protótipos ou detalhes de stack e ambiente sem utilidade para a decisão do usuário.
- Não inserir mensagens como “Acesso reservado a Jaime e Eliane”. Os perfis conhecidos pertencem ao serviço de acesso, não aos textos da tela de login.
- Decisão vigente: login e senha validados por serviço mockado no frontend, com Jaime e Eliane e senhas vazias. O serviço salva JWT sem assinatura em localStorage (`lastro.token`), com nome e role `acesso_total`; sair remove o token. Por pedido posterior, o backend lê suas claims em um filter e carrega `UsuarioAtual` por requisição. Essa leitura estrutural não comprova autenticidade; autenticação real será externa.
- Exibir somente controles funcionais. Funcionalidades futuras ficam no planejamento, sem menus fictícios, botões sem ação ou promessas na tela.
- Não inventar métricas, saldos ou estados vazios sem consultar a fonte de dados correspondente. Um histórico ainda não implementado não equivale a uma carteira vazia.
- Usar linguagem de produto: orientação de uso e mensagens de erro claras, úteis e acionáveis. Restrições de escopo, instruções de desenvolvimento e operação local ficam no README e em `docs/`.
- O protótipo preservado é uma referência histórica. Sua linguagem de demonstração não deve ser copiada automaticamente para a nova aplicação.
- Em Carteiras, manter ativas e arquivadas na mesma listagem, sem abas por situação. Arquivadas ficam visualmente desativadas, com somente reativar habilitado. Criação e edição acontecem em modal, com nome e instituições; ícones e modal seguem o padrão visual da aplicação.

## Entregas com valor real

- Fatiar por funcionalidades completas e utilizáveis, não por demonstrações técnicas apresentadas como entrega funcional.
- Dados e regras de negócio usam MySQL persistente; não substituir esse fluxo por dados simulados para simplificar. Exceção explícita de Jaime: a camada de autenticação fica mockada no frontend até a integração com um serviço externo.
- Bancos descartáveis e mocks de negócio ficam nos testes isolados. O mock de autenticação no frontend é uma exceção de escopo autorizada; não o apresentar como autenticação real.
- O backend de negócio não cadastra usuários, processa senhas, emite tokens ou mantém sessões de login. A leitura do Bearer e o contexto por requisição foram autorizados explicitamente por Jaime; não recriar tabelas/endpoints de controle de usuários. Migrações aplicadas permanecem imutáveis.
- Resolver dependências necessárias ao fluxo, incluindo configuração local e credenciais, em vez de adiar o requisito e entregar um substituto provisório.
- Avançar diretamente no trabalho autorizado; pedir somente informações ou decisões que realmente dependam do usuário. Não repetir ciclos de aprovação para escolhas rotineiras.

## Registro e continuidade

- A tela de login e emissão do JWT mockado foram encerradas por Jaime em 01/11/2026. O novo requisito autoriza consumir o Bearer no backend, sem reabrir o fluxo de credenciais: `UsuarioAtualFilter` carrega contexto e services o consultam por injeção.
- Registrar decisões arquiteturais e de produto em `docs/`, distinguindo decisões confirmadas e propostas.
- Atualizar a nota de retomada e o roteiro ao concluir uma entrega, sem declarar funcionalidades, testes ou execução de CI que ainda não foram comprovados.
- Preservar alterações existentes do usuário e manter segredos e dados reais fora do Git, dos testes e dos logs.
- No versionamento, preservar as sete imagens de referência, o protótipo, o modelo MySQL descompactado, migrations, lockfile e Gradle Wrapper. Ignorar o ZIP duplicado do modelo, configurações privadas, backups locais e artefatos gerados; não criar exclusões genéricas de imagens, SQL ou JAR.

## Proporção e controle da arquitetura

- Autenticação real será externa. No backend de negócio, identificação da requisição fica centralizada no filter, leitor de token e contexto injetável, sem controle de usuários/senhas/sessões. O frontend mantém o serviço assíncrono de login para futura integração externa.
- Resolver a necessidade atual com consultas claras e a menor estrutura compreensível. Não criar camadas, interfaces, serviços ou configurações por antecipação. Quantidade de classes não é critério de qualidade.
- Centralizar todo acesso à localStorage no serviço reutilizável `frontend/src/services/armazenamentoLocal.ts`. Autenticação e demais consumidores usam esse serviço; ele não conhece JWT, usuários ou regras de negócio.
- Centralizar a construção do JWT mockado em `frontend/src/services/jwt.ts`, incluindo header, emissão, codificação e montagem. Autenticação valida os dados do mock, define as claims e coordena geração e armazenamento. Manter essas responsabilidades separadas mesmo no código mockado.
- Antes de expandir o backend para a próxima funcionalidade, discutir com Jaime as responsabilidades, a organização e os critérios para extrair novas classes. Não tratar propostas anteriores como arquitetura já aprovada.
- Esclarecimento vigente de 02/11/2026: `contas` representa a associação N:N entre carteiras e instituições, única por `(carteira_id,instituicao_id)`. IDs distintos nessa tabela identificam vínculos, não históricos financeiros independentes. Carteiras do mesmo usuário com Binance devem apresentar as mesmas operações, registradas uma única vez por usuário/instituição. Operações futuras não referenciam `contas.id` ou carteira como proprietários do histórico; a referência do usuário vem de `UsuarioAtual.id()` e a instituição do catálogo. Selecionar várias carteiras usa a união de instituições do usuário e conta cada operação uma única vez. Eliane e Jaime mantêm operações separadas, mesmo na mesma instituição.
- `carteiras.perfil_ref` vem de `UsuarioAtual.id()`, correspondente ao `sub` do Bearer; a conta herda o proprietário pela carteira. Não enviar/ler `X-Perfil-Id` nem passar perfil pelo cliente/controller. JWT atual continua mockado. V1–V7 são imutáveis. Jaime autorizou explicitamente corrigir/reaplicar V8 localmente e zerar carteiras, contas e vínculos após backup; catálogos são preservados. Essa exceção não autoriza reescritas ou limpezas em outros ambientes.
- `UsuarioAtual` usa escopo de requisição e oferece `id()`, `nome()` e `role()`. Services recebem-no pelo construtor; não acessam servlet, headers ou parser JWT. Não guardar usuário atual em singleton, variável estática ou ThreadLocal manual. O filter atua em `/api` e `/api/**`; páginas/arquivos estáticos permanecem acessíveis sem token. Token ausente ou inválido retorna 401, sem capturar erros de negócio como falhas de acesso.
- Carteira exige ao menos uma instituição. Criar/editar mantém/cria/remove somente seus vínculos em `contas`, em uma transação; arquivar preserva vínculos. Remover vínculos ou arquivar nunca apaga operações, custos ou resultados do usuário/instituição. Reassociar a instituição deve permitir consultar o histórico existente, sem copiar/reimportar lançamentos. Operações ainda não foram implementadas. Catálogo mantido pelo sistema, seed somente quando ausente. Contratos em `docs/09-cadastro-de-carteiras.md` e modelo em `docs/10-modelagem-de-carteiras.md`.
- Backend vigente em Java 27 e Spring Boot 4.2.0-SNAPSHOT por alteração de Jaime. Definir `JAVA_HOME` no ambiente local; CI usa Java 27. Projeto de execução é `backend/`, com JAR `lastro-0.0.1-SNAPSHOT.jar`, conforme o nome atual do projeto Gradle.

## Aplicação obrigatória nas próximas tarefas

- Ler estes acordos e consultar `docs/08-acordos-de-qualidade.md` antes de implementar. É responsabilidade do agente aplicar os aprendizados, sem esperar novos lembretes ou correções de Jaime.
- Antes de codificar, identificar o comportamento esperado, o escopo atual, os responsáveis por cada comportamento e os serviços existentes que podem ser reutilizados.
- Durante a implementação, manter apresentação, coordenação, mecanismos reutilizáveis e regras de negócio nos lugares apropriados. Extrair responsabilidades concretas; não criar infraestrutura por antecipação nem concentrar tudo para reduzir a quantidade de arquivos.
- Antes de apresentar a entrega, revisar responsabilidades misturadas, duplicação, acesso direto à localStorage fora do serviço próprio, construção de JWT fora de `jwt.ts`, complexidade sem necessidade e linguagem da interface. Corrigir dentro do escopo autorizado, sem aguardar a revisão do usuário.
- Executar verificações proporcionais à mudança e atualizar contratos, estado da entrega e nota de retomada. Declarar somente o que foi implementado e comprovado. Essa revisão não exige novos ciclos de aprovação para escolhas rotineiras.
