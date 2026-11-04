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
- Decisão vigente: login e senha validados por serviço mockado no frontend, com Jaime e Eliane e senhas vazias. O serviço salva JWT sem assinatura em localStorage (`lastro.token`), com nome e role `acesso_total`; sair remove o token. Por pedido posterior, o backend lê suas claims em um filter e carrega `IUsuarioAtual` por requisição. Essa leitura estrutural não comprova autenticidade; autenticação real será externa.
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
- Esclarecimento vigente de 02/11/2026: `contas` representa a associação N:N entre carteiras e instituições, única por `(carteira_id,instituicao_id)`. IDs distintos nessa tabela identificam vínculos, não históricos financeiros independentes. Carteiras do mesmo usuário com Binance devem apresentar as mesmas operações, registradas uma única vez por usuário/instituição. Operações futuras não referenciam `contas.id` ou carteira como proprietários do histórico; a referência do usuário vem de `IUsuarioAtual.id()` e a instituição do catálogo. Selecionar várias carteiras usa a união de instituições do usuário e conta cada operação uma única vez. Eliane e Jaime mantêm operações separadas, mesmo na mesma instituição.
- `carteiras.perfil_ref` vem de `IUsuarioAtual.id()`, correspondente ao `sub` do Bearer; a conta herda o proprietário pela carteira. Não enviar/ler `X-Perfil-Id` nem passar perfil pelo cliente/controller. JWT atual continua mockado. V1–V7 são imutáveis. Jaime autorizou explicitamente corrigir/reaplicar V8 localmente e zerar carteiras, contas e vínculos após backup; catálogos são preservados. Essa exceção não autoriza reescritas ou limpezas em outros ambientes.
- `IUsuarioAtual` usa escopo de requisição e oferece `id()`, `nome()` e `role()`. Services recebem-no pelo construtor; não acessam servlet, headers ou parser JWT. Não guardar usuário atual em singleton, variável estática ou ThreadLocal manual. O filter atua em `/api` e `/api/**`; páginas/arquivos estáticos permanecem acessíveis sem token. Token ausente ou inválido retorna 401, sem capturar erros de negócio como falhas de acesso.
- Carteira exige ao menos uma instituição. Criar/editar mantém/cria/remove somente seus vínculos em `contas`, em uma transação; arquivar preserva vínculos. Remover vínculos ou arquivar nunca apaga operações, custos ou resultados do usuário/instituição. Reassociar a instituição deve permitir consultar o histórico existente, sem copiar/reimportar lançamentos. Saldo inicial já está implementado; os demais tipos de operação permanecem futuros. Catálogo mantido pelo sistema, seed somente quando ausente. Contratos em `docs/09-cadastro-de-carteiras.md` e modelo em `docs/10-modelagem-de-carteiras.md`.
- Backend vigente em Java 27 e Spring Boot 4.2.0-SNAPSHOT por alteração de Jaime. Definir `JAVA_HOME` no ambiente local; CI usa Java 27. Projeto de execução é `backend/`, com JAR `lastro-0.0.1-SNAPSHOT.jar`, conforme o nome atual do projeto Gradle.
- Saldo inicial implementado em V9: BRL, USD, BTC, ETH, USDT e USDC, com data comum por usuário em `pontos_partida`. Fatos em `operacoes`, independentes de carteiras/vínculos. Custo histórico total em BRL obrigatório; desconhecido não vira zero e zero explícito exige confirmação/justificativa. Correção cria nova versão, cancelamento preserva fatos, e repetição reutiliza a chave do comando. Escritas bloqueiam o ponto de partida na mesma transação, com READ_COMMITTED; constraints protegem unicidade. Instantes Java usam Instant e persistência UTC. Contrato e responsabilidades em `docs/12-saldo-inicial.md`. Compra/venda e motor completo continuam futuros.

## Arquitetura obrigatória do backend — 02/11/2026

- Aplicar o padrão por contexto de `docs/02-arquitetura.md`: domain/model, repository, service, exception; application/usecase (interfaces e impl), dto, mapper; infrastructure/persistence; api/controller. Criar apenas pacotes necessários.
- API depende de interfaces de usecases; aplicação depende de contratos/modelos do domínio. Domínio não importa Spring, aplicação, infraestrutura ou API; modelos JPA são exceção consciente de persistência, sem entidades duplicadas.
- Repositories Spring Data e consultas ficam nos adapters de infraestrutura. Contratos de domínio não expõem JpaRepository, Page, Pageable ou Specification. Integrações futuras ficam em infrastructure/clients atrás de interfaces da aplicação.
- Controllers somente binding, validação estrutural, delegação e HTTP. Usecases coordenam transação/contexto/regras; mappers de aplicação transformam os dados dentro da transação. Nenhum contrato da API/usecase expõe entidades JPA.
- Não criar camada de services de aplicação paralela aos usecases. Serviços de domínio expressam cálculos/regras independentes. Helpers puros não exigem interfaces; fronteiras injetadas nos usecases exigem interfaces.
- Annotations Java usam sempre nome simples com import explícito, como @Service e @Transactional. Não escrever annotations com nome totalmente qualificado. ArquiteturaTests verifica as árvores sintáticas de todos os fontes Java em backend/src, incluindo testes, sem confundir comentários ou strings com annotations.
- Todo tipo de produção ocupa arquivo próprio, sem classes/records/enums/interfaces aninhados. Nomear métodos pelas regras validadas; não espalhar condições compostas nem usar generics ou try/catch sem necessidade.
- Exceções especializadas no contexto, sem HTTP; ProblemDetail e logging centralizados em TratamentoGlobalException. Filter encaminha erro de acesso ao resolver global, preservando 401 e sem capturar erros downstream. Não classificar erros de persistência no handler. Conflitos de unicidade usados atualmente são traduzidos em exceções de negócio descritivas no adapter de gravação; demais falhas propagam ao Spring.
- Logs sem tokens, credenciais, payloads, SQL ou valores financeiros. Uma emissão em ERROR, com status HTTP e mensagem de negócio/validação; o detail da resposta preserva somente a mensagem da origem. O handler não captura falhas técnicas inesperadas nem cria mensagens genéricas.
- Configuração versionada em application.yml; preservar import privado .local/mysql.properties e variáveis. Classes @Configuration e métodos @Bean em config, quando existentes.
- Preservar transações, READ_COMMITTED, locks, inserção explícita do ponto de partida, ordem de flush da substituição, UTC, decimais e idempotência. Migrations e schema não mudam nesta refatoração.
- Essas regras substituem a organização anterior de controller/service/repository no mesmo pacote; não justificam infraestrutura genérica ou interfaces extras por antecipação. Executar os testes arquiteturais nas próximas entregas.

## Lombok, interfaces e mutabilidade — padrão obrigatório

- Toda interface própria do backend começa com I seguido de nome em PascalCase: ICarteiraUseCase, ICarteiraRepository, ICarteiraJpaRepository, IUsuarioAtual e ILeitorToken. Implementações preservam nomes descritivos sem prefixo I; não renomear interfaces de bibliotecas.
- Usar Lombok para boilerplate: @RequiredArgsConstructor com dependências final, @Getter somente para acessos necessários e @NoArgsConstructor com acesso protegido em entidades JPA ou privado em utilitários. Não escrever getters/construtores de atribuição manualmente.
- Não usar @Data ou @Setter indiscriminadamente, nem gerar equals/hashCode/toString sobre relacionamentos JPA. Records continuam adequados aos DTOs e resultados imutáveis, sem Lombok redundante.
- Entidades JPA mantêm mutabilidade controlada por operações do domínio; não tornar campos persistentes final por conveniência do Lombok. Fábricas criar/vincular/iniciar/registrarSaldoInicial preservam regras de estado inicial, autoria e UTC. Não substituir inicialização de negócio por construtor irrestrito de todos os campos.
- Não gerar getter que exponha coleção gerenciada mutável. Carteira.contasVinculadas() devolve snapshot via Set.copyOf; manter esse comportamento, mesmo que exija método explícito.
- Exceções que só repassam mensagem usam Lombok @StandardException. Construtores com semântica própria, como a mensagem fixa de AcessoInvalidoException, podem permanecer explícitos; não mover regras para contornar a anotação.
- backend/lombok.config copia @Qualifier para os parâmetros dos construtores gerados. Preservar essa configuração e conferir inicialização Spring; não depender de nomes de parâmetros para resolver beans ambíguos.
- Executar os testes arquiteturais de prefixo I/ausência de setters, o teste da coleção protegida e os testes integrados ao evoluir esse padrão.

## Fronteiras modulares obrigatórias

- A decisão de manter JPA nos modelos foi reconfirmada nesta auditoria; não duplicar entidades por pureza arquitetural. Anotações JPA e os tipos Hibernate de mapeamento são exceção somente em domain.model.
- A API pública de um contexto é exclusivamente seu pacote exato application.contract. Repositories, entidades, DTOs REST, mappers, usecases e implementações não são contratos externos, mesmo quando public em Java.
- Entre contextos, somente application pode consumir application.contract, seguindo a matriz em docs/13-auditoria-modular.md: carteiras → acesso/instituicoes; operacoes → acesso/ativos/instituicoes/carteiras. acesso, ativos e instituicoes não consomem outros contextos. Novos contextos/dependências exigem atualizar matriz e testes.
- Modelos não referenciam entidades de outro contexto. Conta/Operacao usam instituicaoId e ativoId escalares; relações JPA internas são permitidas. Preservar as FKs e colunas SQL existentes.
- Catálogos externos retornam snapshots escalares, incluindo inativos para histórico. Consultar metadados em lote nas listagens, sem consulta por registro. Contrato de carteiras resolve usuário atual e retorna união imutável de IDs, sem expor entidade/repository.
- Exceções de negócio ficam em domain.exception do contexto. exceptions contém somente tratamento HTTP transversal e não é dependência de regras de negócio. O handler pode acessar as exceções dos contextos, sem importar outras implementações internas.
- ArquiteturaModularTests com ArchUnit deve continuar verificando matriz, ciclos, encapsulamento dos contratos, camadas e a exceção JPA delimitada. Não usar freezes ou ignorar violações para tornar a suíte verde.
- Monólito permanece em um projeto Gradle. Spring Modulith foi avaliado e adiado por falta de benefício necessário; não adicionar eventos, outbox ou contextos futuros sem caso concreto.
- Essas regras substituem a permissão anterior de compartilhar repositories de domínio e associações JPA entre contextos. Manter contratos REST, semântica financeira, transações e migrations.

## Handler HTTP mínimo — revisão de Jaime

- TratamentoGlobalException limita-se aos erros de negócio, validação de entrada/parâmetros e acesso já usados no código atual. Não adicionar handlers preventivos para erros técnicos ou catch genérico de Exception/RuntimeException.
- Respostas são ResponseEntity<ProblemDetail>, nunca Object. O método responder recebe somente mensagem e HttpStatus, sem HttpHeaders. Por decisão de Jaime em 03/11/2026, o 401 não envia WWW-Authenticate.
- Mensagens descritivas são definidas onde a exceção de negócio é lançada ou nas constraints de validação; o detail retorna a mensagem da origem, e o log ERROR acrescenta contexto de status no formato `Ocorreu um erro, status: <status>, mensagem: <mensagem>`. Não substituir por mensagens genéricas no handler.
- Não manter lista de constraints, nomes de índices, códigos SQL ou método duplicidadeConhecida no handler, nem deslocar essa lista para um utilitário. Adapters traduzem somente a violação UNIQUE reconhecida pelo Hibernate nos pontos de gravação existentes, com mensagem do contexto; outras falhas propagam sem serem reclassificadas como negócio.
- Essas regras substituem os tratamentos técnicos e fallback genérico anteriormente adicionados ao handler. Respostas técnicas padrão permanecem responsabilidade do Spring. Executar os testes de conflito/concorrência para preservar os fluxos atuais.

## Aplicação obrigatória nas próximas tarefas

- Ler estes acordos e consultar `docs/08-acordos-de-qualidade.md` antes de implementar. É responsabilidade do agente aplicar os aprendizados, sem esperar novos lembretes ou correções de Jaime.
- Antes de codificar, identificar o comportamento esperado, o escopo atual, os responsáveis por cada comportamento e os serviços existentes que podem ser reutilizados.
- Durante a implementação, manter apresentação, coordenação, mecanismos reutilizáveis e regras de negócio nos lugares apropriados. Extrair responsabilidades concretas; não criar infraestrutura por antecipação nem concentrar tudo para reduzir a quantidade de arquivos.
- Antes de apresentar a entrega, revisar responsabilidades misturadas, duplicação, acesso direto à localStorage fora do serviço próprio, construção de JWT fora de `jwt.ts`, complexidade sem necessidade e linguagem da interface. Corrigir dentro do escopo autorizado, sem aguardar a revisão do usuário.
- Executar verificações proporcionais à mudança e atualizar contratos, estado da entrega e nota de retomada. Declarar somente o que foi implementado e comprovado. Essa revisão não exige novos ciclos de aprovação para escolhas rotineiras.

## Clean Code na implementação — 02/11/2026

- Métodos públicos dos usecases mostram o fluxo de negócio. Extrair validações compostas e transformações relevantes para métodos nomeados pela intenção, sem fragmentar instruções triviais. Invariantes permanecem no domínio.
- Nomes representam o objeto: carteira, cadastro, operacao, instituicaoIds. Preferir tipos explícitos no fluxo quando tornarem a leitura mais clara; var é adequado quando o tipo é evidente.
- Buscas obrigatórias retornam o modelo concreto e o adapter lança a exceção específica, sempre preservando filtro por proprietário. Optional permanece quando ausência é válida, como catálogo, idempotência e ponto de partida ainda não iniciado.
- Demarcar transações por operação na aplicação. Preservar dirty checking, READ_COMMITTED, bloqueios e flush necessários à atomicidade, às constraints e à tradução de conflitos. Não remover transações com base apenas no número de entidades alteradas.
- Mapeamento individual não cria lista de um elemento. Listagens enriquecem metadados em lote pelos contratos públicos; não consultar catálogo por registro.
- List representa resultados ordenados; Set representa unicidade ou auxilia consultas. Não depender da iteração de HashSet/Set.copyOf. Preservar ordenação explícita dos DTOs, snapshots imutáveis, cascades e orphanRemoval.
- Refatorações preservam regras, mensagens, precedência das validações, REST, schema e migrations. Defeitos funcionais identificados são registrados separadamente, sem correção silenciosa.
- Não adicionar factories, builders, interfaces, helpers genéricos ou novas camadas para satisfazer estilo. Revisar todo o backend não exige modificar classes já adequadas.

## Validação estrutural de DTOs — 03/11/2026

- Usar Jakarta Bean Validation para obrigatoriedade, tamanho, formato e limites numéricos compatíveis com o tipo do campo. Não repetir nas usecases validações já garantidas na entrada.
- Todo RequestBody de entrada com constraints deve ser validado com @Valid no controller; testar que entradas inválidas não alcançam o usecase.
- Preservar constraints dos elementos das coleções: List<@NotNull @Positive Long>. Manter declaração compacta, sem quebrar cada token do genérico em linha separada; mensagens personalizadas permanecem explícitas.
- Evitar redundância: @NotEmpty já rejeita coleção nula e vazia; @NotBlank já rejeita texto nulo/vazio/branco. @Positive não rejeita null e exige @NotNull quando o ID é obrigatório.
- Normalização de textos pode ficar no construtor compacto do record quando o limite é aplicado ao texto normalizado. Esse construtor tem comportamento e não é boilerplate a substituir por Lombok.
- Duplicidade, existência no catálogo, disponibilidade de vínculo, precisão por ativo e obrigatoriedade condicional de motivo/justificativa continuam regras de negócio.
- Parsing estrito de datas e decimais JSON não deve ser substituído por coerção permissiva. @NotNull em JsonNode não valida o tipo ou NullNode: o mapper mantém rejeição de números JSON, e a conversão decimal mantém precisão exata. Não acrescentar annotations incompatíveis com JsonNode nem alterar contrato para simplificar validação.

## Enums de domínio — 03/11/2026

- Converter String para enum somente para classificações fechadas controladas pela aplicação. Catálogos dinâmicos, códigos/tickers, referências externas, usuários e textos livres continuam strings.
- Enums pertencem ao domain.model do contexto; não criar pacote global nem compartilhar enums internos entre contextos. Constantes em maiúsculas, sem comportamento/atributos/construtores desnecessários.
- Persistir por @Enumerated(EnumType.STRING), nunca ORDINAL. Conferir dados e constraints antes da conversão. Em colunas VARCHAR existentes, preservar o tipo explicitamente com @JdbcTypeCode(SqlTypes.VARCHAR), sem converter o schema em ENUM nativo.
- Manter repositories, filtros internos e comparações tipados. Conversões textuais ficam nas fronteiras apropriadas, preservando nomes JSON, mensagens de erro e regras; TODAS é opção de filtro, não estado persistido.
- Renomear constante persistida exige avaliar compatibilidade e estratégia de migração. Não acrescentar estados ou tipos de operação ainda não implementados.
- Cobrir persistência/leitura de textos existentes, JSON, filtros válidos/inválidos e regras. Inventário e justificativas em docs/15-enums-de-dominio.md.

## Livro de negociações — implementação de 03/11/2026

- Usar MotorLivro para saldo inicial, aporte BRL, compra e venda. Regras/contratos em docs/16-negociacoes.md. Custo global por usuário/ativo, custódia por instituição. Carteiras apenas filtram a união de instituições.
- Prévia não grava nem reserva. Novas confirmações usam revisão e recálculo sob lock do ponto de partida/READ_COMMITTED. Toda escrita financeira, inclusive a API legada de saldo inicial, participa do motor e incrementa revisão somente quando há novo efeito.
- Confirmar a repetição antes de recusar revisão antiga. Após resposta incerta, preservar corpo/chave e impedir edição/saída silenciosa, inclusive durante o primeiro POST pendente. Não persistir informações financeiras em localStorage.
- Aceitar quantidade e total bruto; derivar preço no domínio. Taxas somente nas moedas do par; BNB fica no backlog. Preservar 18/8/36 casas e HALF_EVEN nas fronteiras documentadas, sem dupla dedução de taxa ou quantidade negativa.
- APIs legadas consultam somente SALDO_INICIAL. Posições atuais são derivadas do histórico confirmado; versões inativas preservam fatos, sem apresentar cálculo como vigente. Consultas de lista/posição não podem exibir revisões incompatíveis como um mesmo retrato.
- V10 preserva cadeias e campos anteriores. Testar migração acima de mil versões; ajustar temporariamente a sessão da CTE ao volume de dados e restaurá-la, sem alterar configuração global. Migrations aplicadas continuam imutáveis.

## Elegibilidade dos ativos pelo catálogo — 03/11/2026

- Criptoativos negociáveis são determinados pela classificação CRIPTO no catálogo mantido pelo sistema; não manter listas fixas de tickers/códigos de criptoativos em repositories, validação da base negociada ou seletores. Novas gravações exigem ativos disponíveis; histórico e idempotência preservam o tratamento vigente de inativos.
- Saldo inicial admite CRIPTO e BRL/USD; cotação continua BRL/USDT/USDC e taxa somente nas moedas do par. Usar códigos para identificar moedas fiduciárias/de cotação, sem confundir ticker com identidade.
- AtivoResumo expõe classificação escalar cripto pelo contrato público entre contextos; não importar TipoAtivo do domínio de ativos em operações. REST mantém formato e catálogo ordenado, incluindo inativos.
- Inclusões futuras são migrations revisadas, preservando registros compatíveis e interrompendo colisões antes da inserção. Precisão de 18 casas dos criptoativos é capacidade de registro, não regra de blockchain. Não acrescentar outra whitelist ao incluir outros criptoativos.
- Contrato em docs/17-catalogo-de-ativos.md. A elegibilidade não autoriza novos tipos financeiros ou carga de dados reais.
