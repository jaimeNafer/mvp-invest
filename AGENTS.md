# Acordos de desenvolvimento do Lastro

## Aplicação local nas entregas — 07/11/2026

- Ao entregar alterações utilizáveis, iniciar ou atualizar o Lastro integrado em `http://127.0.0.1:8080/`, com o frontend e o backend da versão entregue e o MySQL persistente configurado. Verificar que a aplicação responde e informar o resultado.
- Preservar os dados existentes, inspecionar o processo que ocupa a porta antes de substituí-lo e executar as migrations necessárias com as precauções usuais. Se algum impedimento impossibilitar a inicialização ou a verificação, informar claramente na entrega.

## Separação do domínio fiscal — 07/11/2026

- O núcleo do Lastro trata operações, posições, custos, desempenho e patrimônio. Importação e conferência não exigem dados exclusivamente tributários nem atribuem situações fiscais.
- Preço médio, custo de aquisição e lucro/prejuízo são conceitos de investimento. Recompensas, rendimentos e airdrops são eventos patrimoniais, sem exigência de valoração fiscal na importação; preservar os dados originais e a disponibilidade dos cálculos patrimoniais.
- Um eventual módulo fiscal futuro poderá consumir o domínio de investimentos, sem dependência inversa. Não implementar agora cálculos tributários, DARF, declarações, tabelas ou serviços fiscais antecipados.

## Associação e importação Binance — 07/11/2026

- `Binance Convert` e `Convert` representam uma conversão entre ativos distintos, não uma movimentação neutra. O vínculo exige combinação única; sem valoração confiável, preservar as quantidades e a disponibilidade separada de custos/resultados. Não deduzir preço do custo anterior nem apresentar resultado fictício. Parcelas do mesmo ativo e papel são somadas antes da baixa de custo; registros de origem e confirmação global são preservados. [Contrato](docs/22-associacao-registros-binance.md).

- Uma saída de cripto importada pode ser tecnicamente coerente com finalidade desconhecida. A finalidade é uma proposta versionada, aplicada na confirmação global; a conferência não atribui situação fiscal.
- `Withdraw fee is included` sem valor preserva a saída total, registra detalhamento de taxa pendente e não presume taxa zero. Uma taxa posteriormente informada é parte da saída, sem novo débito. Não presumir quantidade recebida em outro destino.

- Depósito BRL positivo e retirada BRL negativa importados são classificados automaticamente quando não há indícios de transferência. Contrapartidas compatíveis entre contas dentro da janela configurada permanecem para conferência. Criptoativos não são classificados como compra ou venda a partir de depósito/retirada.
- Retirada BRL reduz custódia e saldo, sem gerar resultado realizado. A classificação é proposta até a confirmação global; edições manuais e vínculos de transferência prevalecem sobre classificações automáticas.

- Classificação de coerência verifica dados e integridade financeira. Aguardando confirmação global é um estado de aplicação separado e não gera pendência de coerência em operação completa.

- O Histórico mostra o vínculo automático como uma operação antes da confirmação financeira. Referências fortes distintas são avaliadas separadamente mesmo quando os horários coincidem.
- A associação manual fica no card aberto, com rádio apenas quando há vários destinos e sem seleção inicial. Trocar de destino substitui a escolha anterior. Seleção de linhas adicionais, papéis e campos financeiros ficam recolhidos em opções de conferência.

- Operações agrupadas preservam linhas originais e usam número interno estável `OP-N`, distinto de referências da corretora. Cada linha produz efeito uma única vez: grupo e linhas não podem compor simultaneamente o livro.
- Reconhecimento automático usa `lastro.conciliacao.janela-associacao` em `application.yml` (inicialmente 2s), medindo do primeiro ao último instante, sem encadeamento. Exige usuário, instituição, conta, tipo/direções compatíveis e uma combinação única; execuções do mesmo ativo/papel são somadas exatamente. IDs numéricos recorrentes não identificam ordens. Ambiguidades ficam pendentes e são escolhidas por rádio sem seleção inicial. Associação manual respeita a mesma janela configurada.
- Taxa não informada na importação é considerada zero. Taxa em terceiro ativo mantém débito quantitativo e pendência financeira, sem valoração inventada.
- CSV e XLSX usam a área e o pipeline progressivo comuns. Leitores de formato produzem componentes rastreáveis; pagamentos, recebimentos e taxas derivados da mesma linha física têm identidades distintas sem perder sua proveniência.
- Salvar associação grava proposta; somente a confirmação global substitui efeitos e aplica mudanças de ajustes atomicamente. Correção e desfazimento reprocessam o histórico vigente, preservando versões e autoria. Contrato em docs/22-associacao-registros-binance.md.

## Orquestração e subagentes — 06/11/2026

- Usar modelos proporcionais à tarefa. Delegar ao `gpt-6-luna` buscas delimitadas, análise de logs, documentação e alterações simples com requisitos claros quando houver benefício concreto. Inicialmente, manter no máximo um subagente ativo por vez, sem subdelegação. Esta regra substitui a orientação anterior de paralelizar frentes.
- Cada delegação deve fornecer objetivo, arquivos relevantes e permitidos, limites, contratos necessários e critério de aceite. Solicitar resultado curto com evidências; enviar somente o contexto necessário, preferindo referências a arquivos a transcrições completas.
- Manter no agente principal decisões arquiteturais, regras financeiras, concorrência e integração. Revisar os resultados sem repetir integralmente a investigação delegada; aprofundar somente lacunas, contradições ou riscos concretos.
- Se `gpt-6-luna` estiver indisponível ou a ferramenta não permitir selecioná-lo, informar Jaime. Não substituir silenciosamente por um modelo mais caro, inclusive quando um perfil instalado fixar outro modelo.
- Evitar delegações triviais cujo custo supere o trabalho, análises duplicadas e conflitos de edição. Economia de tokens e tempo é um objetivo, não uma garantia.
- Aplicar AGENTS.md e os acordos do Lastro a todos os agentes. Perfis especializados não autorizam mudanças de arquitetura, escopo, permissões ou divulgação de dados; orientações fiscais exigem evidências e fontes oficiais pertinentes.
- O catálogo VoltAgent foi instalado no ambiente pessoal em `~/.codex/agents/`, separado do código da aplicação. Usar os perfis adequados quando disponíveis; não declarar que um perfil foi carregado ou validado em execução apenas porque seu arquivo está instalado. A integração final e a declaração de conclusão permanecem responsabilidade do orquestrador.

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

## Visão geral e densidade visual — 03/11/2026

- Priorizar apresentação compacta: espaços/preenchimento de 12–16 px, cabeçalhos baixos, controles mínimos de 36 px em desktop e 44 px no toque. Não ocultar valores nem criar vários painéis de rolagem vertical. Visão geral com seis ativos e custódia resumida deve caber em 1366×768.
- Consolidação global por usuário: saldos até a data final, resultados/taxas/aportes do período, mantendo fatos anteriores para o custo. Nova consulta somente leitura usa REPEATABLE_READ para snapshot consistente; comandos mantêm READ_COMMITTED e bloqueios.
- Totais e percentuais financeiros calculados no backend. React apresenta valores exatos e ignora respostas canceladas. Não confundir custo com preço de mercado ou rentabilidade. Série mensal, valoração e rentabilidade foram priorizadas como evoluções, ainda não implementadas. Contrato em docs/18-visao-geral.md.


## Consolidação financeira — evolução de 04/11/2026

- Resultado realizado mensal começa em zero no período selecionado; meses sem operações mantêm o acumulado. Último ponto e soma por ativo reconciliam exatamente com o resumo, reutilizando as parcelas do motor sem descontar taxas novamente.
- Realização pertence ao ativo efetivamente realizado: ativo vendido ou moeda gasta em compra com USDT/USDC. Aportes e abertura não são lucro; custo não é valor de mercado.
- Saldos em moeda são quantidades registradas, sem informação sobre bloqueios externos. Conferência apresenta somente avisos de revisão, não erros presumidos: custo zero declarado e referência repetida no mesmo usuário/instituição. Custo desconhecido continua rejeitado.
- A ampliação do painel permite rolagem natural em 1366×768, substituindo o aceite anterior de todos os blocos caberem na altura da tela. Preservar blocos compactos, controles legíveis e ausência de rolagens verticais independentes.
- Valores financeiros permanecem decimais exatos no backend e strings no frontend. Number é permitido somente para geometria do gráfico; alternativa tabular mantém acesso aos valores completos. Detalhes e avisos não exigem novas tabelas. [Contrato](docs/18-visao-geral.md).

## Importação auditável — 05/11/2026

- Interpretar arquivos no contexto importacoes; validar e gravar fatos somente pelo contrato público de lote de operacoes. Não duplicar regras financeiras ou chamar HTTP interno.
- Manter arquivo, decisões, versão do leitor, origem dos registros e operações na mesma transação. Prévia não grava. Idempotência distingue comando, arquivo e evento; reconhecer fatos originais não desfaz correções/cancelamentos.
- Não adaptar taxas em BNB, movimentações ou custos desconhecidos aos tipos suportados. Evidência insuficiente bloqueia carga; importação não equivale a conciliação ou apuração fiscal.
- Dados e arquivos reais ficam fora de Git, CI e logs. Cenários públicos são sintéticos. Executar testes de atomicidade, sobreposição, concorrência e isolamento. [Contrato](docs/19-importacao-binance.md).

## Conciliação progressiva — diretrizes da revisão de 06/11/2026

- Histórico incompleto é tratado pelo primeiro ciclo de conciliação em docs/20-conciliacao-progressiva.md e contrato docs/21-contrato-conciliacao.md. Receber registros, aproveitar quantidades e calcular custos/resultados são capacidades separadas. O CSV legado permanece estrito e compatível, com proteção contra reaplicação de referências recebidas no fluxo progressivo.
- Usar Coerente, Com pendência e Precisa de correção para verificação automática. Revisada por você é anotação separada, que não elimina uma lacuna nem certifica valores.
- Ajustes provisórios representam lacunas, sem provar fato econômico. Sugerir e confirmar em lote; preservar âncora técnica, origem, aceite e versões, sem fabricar data real ou contabilizar aporte, receita, negociação ou lucro.
- Custódia por conta não reutiliza contas (vínculo carteira/instituição) nem recria titulares. Proprietário vem de IUsuarioAtual; custos permanecem globais por usuário/ativo. Não presumir que o legado é Spot.
- Buscar correspondências antes de sugerir ajustes. Reprocessar sem multiplicar ajustes, apresentar mudanças de ajustes aceitos e aplicar fatos e alterações atomicamente. Ausência de custo não vira zero; pendência não pode ser escondida por revisão manual.
- Priorizar impacto e resolução contextual numa timeline compacta. Percentual de coerência é secundário e não mede acurácia financeira. Relatórios fiscais e motor genérico de associações não fazem parte desta revisão.


## Conciliação progressiva — regras de implementação

- Receber e editar preserva originais, versões anteriores e snapshot aplicado. Somente a confirmação incorpora propostas aproveitáveis e modifica ajustes, na mesma transação; proposta impedida usa o snapshot aplicado também na prévia.
- Não alterar a data comum de uma abertura confirmada ao receber arquivo anterior. Preservar revisões, bloqueios e o custo global por usuário/ativo, com quantidade por conta de custódia.
- Ajustes encerrados continuam na cadeia de versões. Reaparecimento da lacuna utiliza a próxima versão e vínculo anterior; não recriar versão 1.
- Eventos repetidos entre arquivos ou fluxos não podem gerar um novo efeito sem conciliação. Preservar multiplicidade; pares internos exigem exatamente duas pernas permitidas e reciprocamente únicas.
- Informações desconhecidas não viram zero nem estado Coerente. Quantidade, custo, resultado e cobertura têm disponibilidades próprias. Totais incompletos são nulos; subtotais conhecidos são identificados como parciais. Nunca apresentar uma fonte ilegível como carteira vazia.
- Timestamps econômicos permanecem intactos ao editar outros campos. Ajustes usam âncora técnica e aceite real, sem data econômica inventada. Proveniência e versão real chegam à timeline.
- Reutilizar o motor para leituras e validação dos caminhos manuais e importados. Preservar os contratos legados de históricos completos. Testar reabertura, propostas ilegíveis, referências reexportadas, correspondências ambíguas e rollback de confirmação.

- Na importação progressiva, separar observações quantitativas e proposta financeira: dados de negociação incompletos não eliminam as pernas documentadas. Um relatório enriquecido só se expande em envio, recebimento e taxa quando esses componentes constam explicitamente na origem; manter rastreabilidade e não inferir outros eventos.
- Verificar cobertura de todas as pernas, incluindo moeda/quantidade ainda não identificadas. Incerteza datada não contamina automaticamente meses anteriores; prioridade conta dependentes antes de aplicar filtros visuais.
