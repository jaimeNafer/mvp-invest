# Revisão de implementação do backend

Revisão autorizada em 02/11/2026. O escopo melhora legibilidade e coesão dentro da arquitetura vigente. Pacotes, endpoints, migrations, schema, mensagens e regras financeiras permanecem preservados.

## Problemas e melhorias

| Antes | Depois |
|---|---|
| Edição de carteira calculava vínculos e validava estado no fluxo principal | `validarEdicaoPermitida` e `instituicaoIdsVinculadas` expressam a intenção no domínio |
| Cadastro atribuía o nome na criação e novamente em alterar | Criação seguida de sincronização dos vínculos |
| Sincronização percorria todas as contas a cada instituição | Conjuntos auxiliares preservam vínculos mantidos e evitam repetições |
| Busca obrigatória expunha Optional e tratamento da ausência no usecase | `buscarObrigatoria` no adapter mantém filtro por perfil e exceção específica |
| Resposta individual transformava entidade em lista para usar getFirst | Mapeamento individual direto; enriquecimento em lote continua nas listas |
| Normalização repetia strip do nome | Texto normalizado uma vez, com os mesmos limites e mensagem |
| Gravação do saldo misturava consulta de catálogo e encerramento anterior | Métodos especializados expressam seleção válida e substituição com flush |
| Cálculo agrupado ficava dentro de lambda extensa | `consolidarAtivo` calcula cada grupo com os mesmos decimais e ordenação |
| Variáveis de uma letra escondiam objetos nos mappers | Nomes operacao, posicao, custodia, cadastro, ativo e instituicao |

## Transações e persistência

As fronteiras readOnly antes declaradas nas classes estão explícitas nos métodos de consulta, inclusive contratos públicos. Escritas de carteiras permanecem transacionais. Arquivamento conserva dirty checking e não ganhou save redundante.

Saldo inicial preserva READ_COMMITTED, bloqueio do ponto de partida, repetição verificada antes e depois do bloqueio e flush do saldo substituído antes da inserção. SaveAndFlush continua necessário à tradução dos conflitos dentro dos adapters; o persist/flush do ponto de partida continua distinguindo criação de atualização.

Optional permanece nas consultas em que ausência é válida. O contrato de união das carteiras continua retornando ausência para carteira inexistente/alheia, permitindo ao contexto consumidor definir sua própria exceção. Não há acesso a entidades ou repositories de outro contexto.

## Coleções e ordenação

List permanece nos resultados REST e catálogos. Set continua nos vínculos únicos e na união de instituições, sem promessa de ordenação. Respostas de carteira continuam ordenando instituições por nome e ID; carteiras continuam por nome e ID; posições por ticker e custódias por nome/ID. As coleções gerenciadas não são expostas para mutação.

IDs nulos/não positivos dos cadastros são rejeitados pelas constraints dos DTOs. A revisão de 03/11 padronizou Bean Validation, mantendo @Valid nos endpoints e retirando verificações estruturais duplicadas dos usecases. Nome e textos limitados são normalizados nos records antes da validação. Chamadas internas devem respeitar o contrato validado. Instituições inativas previamente vinculadas podem continuar; novos vínculos inativos são rejeitados.

## Cobertura da revisão

Carteiras, operações, catálogos, acesso, adapters, modelos, mappers e controllers foram inspecionados. Controllers e identificação já tinham responsabilidades delimitadas e foram preservados. Não foram criados frameworks, camadas, interfaces auxiliares ou tabelas.

O inventário de classes alteradas está abaixo; contratos públicos entre contextos permanecem iguais.

## Pendências identificadas, sem mudança funcional

O handler já estava diferente dos acordos e dos testes antes desta revisão: o 401 não inclui WWW-Authenticate, e o log usa ERROR com prefixo de status/mensagem em vez de WARN com a mensagem exata. Código do handler e testes foram preservados. Decisão posterior de Jaime, em 03/11/2026: prevalece a classe atual; testes devem exigir 401 sem WWW-Authenticate e log único em ERROR com status e mensagem. A divergência anterior fica registrada apenas como histórico.

O cálculo existente agrupa por ticker. No catálogo atual os seis ativos têm tickers distintos; eventual inclusão de ativos com ticker coincidente exige avaliar o agrupamento por identidade. Não foi alterado nesta refatoração.

## Verificação

Resultado final registrado após a execução das suítes. Falhas preexistentes não devem ser ocultadas nem convertidas em testes ignorados.

## Complemento: validação dos DTOs

CadastroCarteira, CadastroSaldoInicial e CancelamentoSaldoInicial receberam constraints e normalização apropriadas. SituacaoCarteira já estava adequado. Não foi criada infraestrutura genérica de validação. Quantidades/custos continuam JsonNode para impedir coerção de números JSON em texto; parsing e precisão decimal permanecem especializados. Data continua sendo convertida estritamente para LocalDate; limite temporal usa o fuso de negócio.

## Classes modificadas

- `backend/src/main/java/io/github/jaimenafer/lastro/ativos/application/mapper/AtivoMapper.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/ativos/application/usecase/impl/AtivoUseCaseImpl.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/ativos/application/usecase/impl/ConsultaAtivosImpl.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/carteiras/application/dto/CadastroCarteira.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/carteiras/application/usecase/impl/CarteiraUseCaseImpl.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/carteiras/application/usecase/impl/ConsultaCarteirasImpl.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/carteiras/domain/model/Carteira.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/carteiras/domain/model/Conta.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/carteiras/domain/repository/ICarteiraRepository.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/carteiras/infrastructure/persistence/CarteiraPersistenceAdapter.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/instituicoes/application/mapper/InstituicaoMapper.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/instituicoes/application/usecase/impl/ConsultaInstituicoesImpl.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/instituicoes/application/usecase/impl/InstituicaoUseCaseImpl.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/operacoes/api/controller/SaldoInicialController.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/operacoes/application/dto/CadastroSaldoInicial.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/operacoes/application/dto/CancelamentoSaldoInicial.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/operacoes/application/mapper/SaldoInicialMapper.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/operacoes/application/usecase/impl/IdentidadeSolicitacaoSaldo.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/operacoes/application/usecase/impl/SaldoInicialUseCaseImpl.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/operacoes/application/usecase/impl/ValidacaoSaldoInicial.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/operacoes/domain/model/Operacao.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/operacoes/domain/model/PontoPartida.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/operacoes/domain/repository/IOperacaoRepository.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/operacoes/domain/service/CalculoPosicaoInicial.java`
- `backend/src/main/java/io/github/jaimenafer/lastro/operacoes/infrastructure/persistence/OperacaoPersistenceAdapter.java`
- `backend/src/test/java/io/github/jaimenafer/lastro/LombokModelosTests.java`
- `backend/src/test/java/io/github/jaimenafer/lastro/ValidacaoDtosTests.java` (novo).

## Resultado da verificação — 03/11/2026

- Compilação de produção e testes concluída.
- 32 testes unitários/arquiteturais executados: 30 aprovados, 2 falhas preexistentes em TratamentoGlobalExceptionTests; nenhum ignorado.
- 38 testes de integração com MySQL descartável aprovados, incluindo concorrência, atomicidade, migrações e isolamento por usuário.
- Três novos testes de DTOs/endpoints aprovados: limites após normalização, IDs/coleções/cancelamento e rejeição antes do usecase em criação, substituição e cancelamento.
- Novo teste de sincronização dos vínculos aprovado, preservando objetos mantidos e unicidade.
- O check dessa execução ficou vermelho pelas duas divergências do handler. O contrato foi posteriormente confirmado por Jaime e os testes ajustados; ver verificação posterior.
- Revisão independente da refatoração principal não identificou regressões concretas. Complemento Bean Validation revisado localmente; revisor adicional indisponível por capacidade.
- Nenhuma alteração em migrations, schema ou dados locais; sem reinício da aplicação, publicação ou commit. Frontend não foi alterado nem revalidado nesta tarefa.

## Contrato do handler confirmado — 03/11/2026

Jaime confirmou o comportamento vigente da classe: respostas 401 sem WWW-Authenticate e log descritivo único em ERROR com status HTTP e mensagem da origem. Os testes foram ajustados para verificar esse contrato; a implementação do handler não foi alterada.

`gradlew check --offline --no-daemon --console=plain` aprovado: 70 testes, sendo 32 unitários/arquiteturais e 38 integrados MySQL, sem falhas, erros ou testes ignorados. As duas divergências registradas na execução anterior estão resolvidas. Sem alteração em schema, migrations ou dados, sem publicação e sem reinício da aplicação.
