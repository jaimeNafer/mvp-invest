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
| Q11 | Uma mudança de interface precisa funcionar de ponta a ponta. | Conferir estilos no navegador e o contrato da API em execução. Alterar a listagem exige verificar seus estados reais, incluindo arquivados, sem depender somente de mocks dos testes. Reutilizar o padrão visual em modais e ícones. |
| Q12 | Propriedade e visualização são responsabilidades distintas. | `contas` é vínculo carteira/instituição. Operações futuras pertencem ao usuário/instituição e aparecem em várias carteiras sem cópia. Remover vínculos/arquivar não exclui fatos e consultas combinadas não os contam duas vezes. |
| Q13 | Informações do usuário pertencem ao contexto da requisição. | Filter carrega as claims do Bearer; services recebem `IUsuarioAtual` por injeção, sem ler headers/servlet ou decodificar JWT. Não transmitir ID de perfil separado do token nem armazenar contexto em singleton, estático ou ThreadLocal manual. Testar alternância e concorrência entre requisições. |
| Q14 | O significado dos dados deve acompanhar o schema. | Documentar tabelas/colunas de negócio com `COMMENT` no MySQL e atualizar o dicionário vigente. Comentários de metadados são documentação solicitada, sem substituir nomes claros no código. Evoluir por nova migration preservando dados e definições; não copiar campos de modelos históricos sem validar o escopo atual. |
| Q15 | Comentários de dados devem servir ao uso em produção. | Descrever somente a finalidade, de forma breve e objetiva; listar opções de campos com valores restritos. Não incluir mocks, classes, headers, estágio do projeto, justificativas ou comparações com outros conceitos. Manter detalhes de implementação nos documentos técnicos. |

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
- Token sem assinatura (`alg: none`), enviado como Bearer pelo cliente HTTP. O backend lê `sub`, `name` e `role` e disponibiliza contexto por requisição; o cadastro usa `sub` como referência do perfil. `X-Perfil-Id` foi substituído. A leitura do mock não comprova autenticidade; validação real de acesso permanece futura.
- Recarregar a página preserva o token, mas não restaura automaticamente o perfil.
- Autenticação real será responsabilidade de um backend externo específico, a implementar depois.
- Banco de negócio persistente, com controle de usuários removido por V4 e estruturas de negócio preservadas.

## Revisão a aplicar em cada tarefa

Antes de implementar, identificar o comportamento esperado, as responsabilidades envolvidas e o que permanece fora da entrega. Consultar os serviços existentes antes de criar uma nova implementação.

Antes de concluir, conferir se a tela contém apenas apresentação, se serviços respeitam seus limites, se há código duplicado ou infraestrutura antecipada, se o mock mantém a organização acordada e se os estados de sucesso/erro e a documentação correspondem ao comportamento real. Executar apenas as verificações pertinentes e registrar seus resultados.

Essa revisão não cria um novo ciclo de aprovação. Escolhas rotineiras autorizadas devem avançar diretamente. A conversa sobre limites da arquitetura de negócio permanece o próximo passo antes de expandir o backend.

Esclarecimento final de Jaime em 02/11/2026: carteiras são seleções de instituições; `contas` é a associação N:N. Binance em duas carteiras apresenta as mesmas operações do usuário. Essa decisão substitui a interpretação anterior de contas com históricos independentes. [Modelo vigente](10-modelagem-de-carteiras.md).

Carteira possui vínculos únicos por instituição, alterados em transação. IDs mantidos identificam vínculos, sem propriedade sobre os fatos financeiros. Operações futuras usam usuário/instituição; excluir vínculo ou arquivar carteira não apaga histórico. Consultas de várias carteiras selecionam a união das instituições do usuário e contam cada operação uma vez.

V1–V7 são imutáveis. Jaime autorizou corrigir/reaplicar V8 localmente e zerar carteiras, contas e vínculos após backup, preservando catálogos. Exceção restrita a esta recuperação; futuras evoluções compartilhadas usam nova migration. Comentários do schema descrevem somente finalidade e opções aplicáveis.

## Decisões substituídas

A autenticação local com senhas no MySQL e a identificação por sessão no backend foram retiradas do escopo. Também foi substituída a concentração da criação do JWT e das operações de localStorage em autenticação. Esses desenhos não devem ser reintroduzidos por referência a notas históricas.

A proibição anterior de qualquer identificação no backend foi revisada pelo pedido de contexto injetável a partir do Bearer. A emissão/validação real de credenciais continua externa; não confundir leitura do token mockado com autenticação efetiva.

## Padrão de arquitetura obrigatório

A refatoração autorizada em 02/11/2026 adota contextos com domain/application/infrastructure/api, mantendo JPA nos modelos. A especificação permanente, exemplos e checklist estão em [Arquitetura](02-arquitetura.md#padrão-arquitetural-obrigatório--decisão-de-02112026).

Controllers delegam a interfaces de usecases e não mapeiam entidades. Usecases coordenam transações e produzem DTOs por mappers. Domínio não importa Spring Data ou camadas externas; adapters realizam persistência. Serviços de domínio cuidam de cálculos independentes. Cada tipo de produção ocupa arquivo próprio. Tratamento de exceções e logs é global; modelos não carregam informações de transporte HTTP. Configuração versionada em YAML, mantendo o arquivo privado de conexão compatível.

A revisão de cada tarefa deve conferir esses limites e executar ArquiteturaTests. Interfaces são obrigatórias nas fronteiras autorizadas, sem criar uma interface por helper nem uma camada de services duplicando usecases.

## Lombok e nomes de interfaces

Interfaces próprias começam com I; implementações mantêm o nome do papel. Preferir @RequiredArgsConstructor para injeção com campos final, @Getter seletivo e @NoArgsConstructor protegido para JPA/privado para utilitários. Records não precisam de Lombok. Exceções de repasse usam @StandardException; comportamento próprio de construção deve ser preservado.

Não introduzir setters ou @Data por conveniência. Fábricas e operações nomeadas preservam invariantes; coleções de entidades são expostas por cópias protegidas. Copiar @Qualifier nos construtores gerados, conforme backend/lombok.config. [Regras e exemplos completos](02-arquitetura.md#lombok-e-convenção-de-interfaces).

## Encapsulamento entre contextos

O refinamento modular substitui a permissão anterior de compartilhar repositories e entidades JPA entre contextos. Somente o pacote exato application.contract é API pública; consumidores usam snapshots escalares e consultas em lote. Domain.repository é interno, mesmo sendo uma interface. Relações JPA ficam dentro do contexto; referências externas usam IDs mantendo FKs SQL.

Exceções específicas pertencem a domain.exception; o pacote global exceptions contém apenas o handler HTTP. Seguir a [matriz explícita e a exceção JPA reconfirmada](13-auditoria-modular.md) e executar ArquiteturaModularTests. Novos contextos não ficam automaticamente autorizados. Não adicionar abstrações globais de negócio, eventos, módulos Gradle ou Spring Modulith sem necessidade concreta.

## Simplificação do tratamento HTTP

Por revisão explícita de Jaime, o advice trata apenas negócio, validação e acesso atualmente utilizados, com ResponseEntity<ProblemDetail> e mensagem descritiva originada na exceção/constraint. Retornar a mensagem da origem no detail e registrar uma vez em ERROR, incluindo status e mensagem no formato `Ocorreu um erro, status: <status>, mensagem: <mensagem>`. Não recriar listas de unicidades, classificação SQL ou catch genérico no handler. O helper responder não recebe headers; o 401 não envia WWW-Authenticate, conforme decisão de Jaime em 03/11/2026. Conflitos reais de gravação são traduzidos na persistência por tipo UNIQUE do Hibernate, sem listas ou nova infraestrutura genérica.

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

## Catálogo e elegibilidade dos ativos

O catálogo mantido pelo sistema determina os criptoativos elegíveis, sem listas de tickers repetidas nos repositories, validadores ou telas. Classificação e capacidade são expostas por snapshots escalares no contrato público entre contextos; situação ativa é validada nas novas gravações, preservando histórico e idempotência. Cotação continua BRL/USDT/USDC; abertura também aceita BRL/USD. Inclusões são migrations compatíveis, sem sobrescrever registros existentes. [Contrato vigente](17-catalogo-de-ativos.md).
