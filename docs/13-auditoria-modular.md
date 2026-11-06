# Auditoria e refinamento modular do backend

Auditoria de 02/11/2026. O desenho do handler foi simplificado posteriormente por pedido explícito de Jaime; prevalecem as regras vigentes de [tratamento HTTP](02-arquitetura.md#erros-configuração-e-logging). O objetivo é encapsular contextos dentro do mesmo aplicativo Spring Boot/Gradle, preservando schema, contratos HTTP e regras financeiras. Não há novos módulos físicos, mensageria ou microsserviços.

Jaime confirmou **manter JPA nos modelos**, como exceção explícita à independência completa de frameworks. A auditoria não duplica entidades nem apresenta esse desenho como domínio inteiramente puro.

## Diagnóstico da base auditada

| Local | Dependência ou falha encontrada | Correção |
| --- | --- | --- |
| `operacoes/application/usecase/impl/SaldoInicialUseCaseImpl.java` | Usava IAtivoRepository, IInstituicaoRepository e ICarteiraRepository de outros contextos e recebia entidades desses catálogos. | Consome IConsultaAtivos, IConsultaInstituicoes e IConsultaCarteiras, com snapshots escalares. |
| `carteiras/application/usecase/impl/CarteiraUseCaseImpl.java` | Acessava o repository de instituições e recebia Instituicao. | Consulta contrato público do catálogo e entrega IDs ao modelo local. |
| `carteiras/domain/model/Carteira.java` e `Conta.java` | A lógica do vínculo e a associação JPA dependiam de Instituicao. | Modelo recebe IDs; a associação JPA com Carteira permanece interna ao contexto. |
| `operacoes/domain/model/Operacao.java` | Relacionamentos JPA com entidades Ativo e Instituicao de outros contextos. | Mantém instituicaoId e ativoId nas mesmas colunas, preservando FKs existentes. |
| Repositories JPA de carteiras/operações | EntityGraphs e filtros atravessavam relacionamentos de outros contextos. | Graph de carteiras carrega somente contas; filtros de operações usam IDs escalares. Metadados são consultados em lote. |
| `acesso/IUsuarioAtual.java` | Contrato público e implementação estavam no mesmo pacote sem fronteira explícita. | Interface exposta em acesso.application.contract; restante do acesso permanece interno. |
| `exceptions/*` | Exceções específicas estavam globais; a retirada dos construtores de mensagem impedia compilação. | Especializações retornaram a domain.exception dos respectivos contextos, com StandardException. Bases globais redundantes removidas. |
| `exceptions/TratamentoGlobalException.java` | Resposta 500 expunha mensagem interna; logging incluía conteúdo sensível; faltavam classificações de integridade, indisponibilidade e header de acesso. | Handler restrito a negócio/validação/acesso, mensagens da origem e logging da mesma mensagem; conflitos de gravação traduzidos no adapter. Demais erros ficam a cargo do Spring. |

A baseline não compilava, com 29 erros de construção de exceções. Após o reparo mínimo dos construtores, três testes existentes do handler falharam. Antes de isolar os contextos, quatro das seis regras iniciais de ArchUnit falharam, incluindo a ausência dos novos contratos; a regra de ciclos passou. **Não foram encontrados ciclos existentes**, portanto não é atribuída à correção a remoção de um ciclo inexistente.

## Responsabilidade dos contextos

- **acesso:** identifica a requisição e fornece IUsuarioAtual. Mantém a leitura de token autorizada anteriormente; não cadastra usuários nem implementa o futuro provedor externo de autenticação.
- **ativos:** proprietário do catálogo e da indicação dos seis ativos aceitos no saldo inicial. Expõe metadados, inclusive de registros inativos, para consulta de histórico.
- **instituicoes:** proprietário do catálogo de custodiantes e de sua situação. Não expõe seu modelo JPA aos consumidores.
- **carteiras:** administra seleções de instituições e vínculos em contas. Resolve a identidade atual ao consultar as instituições de uma seleção; não revela carteiras de outro usuário.
- **operacoes:** proprietário dos fatos financeiros e do cálculo da posição inicial global. Carteiras são visões desses fatos, não livros independentes.

`contas` já está em carteiras.domain.model e não constitui um contexto separado. Não foi recriado nem renomeado. Posições continuam derivadas de operações; transferi-las para carteiras introduziria propriedade financeira incorreta.

## Estrutura final e APIs públicas

```text
io.github.jaimenafer.lastro/
  acesso/
    application/contract/IUsuarioAtual
    domain/exception/AcessoInvalidoException
    (filter, leitor e contexto internos preservados)
  ativos/ | instituicoes/ | carteiras/ | operacoes/
    api/controller/
    application/
      contract/       somente quando há consumo entre contextos
      usecase/impl/
      dto/
      mapper/
    domain/
      model/
      repository/
      service/        somente onde há cálculo/regra independente
      exception/      somente onde há exceção específica
    infrastructure/persistence/
  exceptions/TratamentoGlobalException
```

A árvore representa o padrão lógico, não a criação de pastas vazias. Configurações futuras seguem config, conforme o acordo existente.

**Somente o pacote exato `<contexto>.application.contract` é público para outros contextos.** Subpacotes, usecases REST, DTOs HTTP, repositories, entidades, adapters e implementações são internos. Public em Java não equivale a API pública do contexto: ArchUnit verifica essa fronteira lógica sem múltiplos módulos Gradle.

| Contexto fornecedor | API pública | Garantia |
| --- | --- | --- |
| acesso | IUsuarioAtual | Identidade da requisição, sem transportar credenciais ou contexto mutável. |
| ativos | IConsultaAtivos e AtivoResumo | Consulta individual/em lote; retorna IDs, códigos e metadados escalares. Disponibilidade do saldo inicial pertence ao catálogo. |
| instituicoes | IConsultaInstituicoes e InstituicaoResumo | Consulta individual/em lote incluindo inativas para histórico; consumidor valida a disponibilidade na gravação. |
| carteiras | IConsultaCarteiras | Optional de conjunto imutável de IDs de instituições, unido sem repetições, limitado ao usuário atual. Carteira inexistente/alheia retorna Optional.empty; o consumidor traduz para seu erro de negócio. |
| operacoes | IImportacaoOperacoes e records escalares de lote | Valida e grava lotes de aportes/compras pelo mesmo motor, preservando usuário, locks e histórico. |

Os novos contratos são síncronos e participam da transação existente. Não lançam exceções internas de outro contexto para comunicar ausência esperada. DTOs de contrato são records com valores escalares, sem JPA, Spring, Jackson ou dependência dos DTOs REST.

## Matriz de dependências permitidas

Linhas são consumidores; colunas são fornecedores. ✓ significa somente application.contract, acessível pela camada application. — significa nenhuma dependência cruzada autorizada.

| Consumidor → fornecedor | acesso | ativos | instituicoes | carteiras | operacoes | importacoes |
| --- | --- | --- | --- | --- | --- | --- |
| acesso | próprio | — | — | — | — | — |
| ativos | — | próprio | — | — | — | — |
| instituicoes | — | — | próprio | — | — | — |
| carteiras | ✓ | — | ✓ | próprio | — | — |
| operacoes | ✓ | ✓ | ✓ | ✓ | próprio | — |
| importacoes | ✓ | ✓ | ✓ | — | ✓ | próprio |

O handler HTTP transversal pode importar **apenas domain.exception** dos contextos, além de suas dependências técnicas. Contextos de negócio não dependem do handler/pacote global. Essa exceção técnica é unidirecional e não constitui compartilhamento de interfaces de negócio. Novos contextos devem ser registrados na matriz; não ficam liberados automaticamente.

Dentro de cada contexto: api → application → domain e infrastructure → contratos internos. Domain não depende de application, infrastructure, api, Spring ou transporte HTTP. Jakarta Persistence e as duas referências Hibernate necessárias aos campos CHAR são permitidas somente em domain.model; Lombok continua responsável pelo código repetitivo.

## Preservação de comportamento e persistência

- Nenhuma migration ou coluna foi criada/alterada. FKs e constraints MySQL continuam garantindo referências e unicidade. Remover um relacionamento Java não remove a FK SQL.
- IDs dos vínculos mantidos, remoção de órfãos somente dentro da carteira, arquivamento e reativação preservados.
- Leitura de metadados em lote por página/lista; não consultar um catálogo por registro da lista. Inatividade do catálogo não oculta fatos históricos nem invalida a repetição de comandos já concluídos.
- READ_COMMITTED, bloqueio do ponto de partida, persist/flush explícito, liberação do marcador antes da substituição, UTC e decimais preservados.
- Identidade segue exclusivamente o contexto da requisição. O backend continua sem cadastro de usuários, emissão de tokens ou sessões.

## Validação arquitetural

ArchUnit core **1.5.1**, somente em testImplementation e executado pelo JUnit existente. A versão suporta bytecode Java 27; não foi acrescentado outro engine de testes. [Releases oficiais](https://github.com/TNG/ArchUnit/releases), [guia oficial](https://www.archunit.org/userguide/html/000_Index.html).

ArquiteturaModularTests verifica bytecode para:

1. Dependências cruzadas somente pelos contratos e pela matriz explícita, com rejeição de novos contextos não registrados.
2. Ausência de ciclos entre pacotes/contextos.
3. Direção das camadas.
4. Independência de Spring/HTTP/serialização no domínio, com a exceção JPA/Hibernate delimitada.
5. Contratos sem entidades/frameworks/DTOs REST e sem acesso a internos do próprio contexto.
6. Handler transversal sem acesso a implementações dos contextos.

As verificações anteriores de interfaces I, ausência de setters/tipos aninhados, DTOs sem entidades e annotations com nomes simples foram preservadas. O teste de annotations continua analisando sintaxe, pois nomes simples e nomes completos têm o mesmo bytecode e ArchUnit não distingue essa escolha de escrita.

ContratosContextosTests acrescenta cenários de união/isolamento de carteiras, consulta em lote de instituições inativas e consulta de metadados históricos de ativos sem habilitar nova abertura.

Validação concluída:

- Compilação e `check` aprovados com **64 testes**: 26 unitários (incluindo sete regras ArchUnit e três testes dos contratos) e 38 integrados MySQL/Testcontainers, sem falhas ou ignorados.
- Testes unitários reexecutados após fortalecer a whitelist do domínio: somente JDK, Lombok, domínio próprio e as annotations/enums JPA e mapeamentos Hibernate explicitamente autorizados.
- Pacote integrado `bootJar -PincludeFrontend` aprovado e aplicação iniciada em localhost:8080 com a configuração privada existente.
- Consultas HTTP dos dois perfis para carteiras, instituições, ativos, saldos e posições retornaram 200; IDs e decimais mantêm strings no REST. Ausência de Bearer retorna 401 com WWW-Authenticate.
- Flyway validou nove migrations e informou schema atualizado, sem migration necessária. Hashes dos snapshots privados das seis tabelas de negócio antes/depois são idênticos. Nenhum registro foi criado/alterado nessa conferência.
- Revisão independente não identificou regressões materiais. Links locais e diff conferidos. Não houve nova execução do frontend, CI, commit, push ou PR nesta tarefa.

Não há pendência bloqueante identificada nesta auditoria. Os limites abaixo são escolhas de escopo e de implantação, não violações ignoradas nos testes.

## Avaliação de Spring Modulith

Não introduzido nesta entrega. Existe linha 2.2 alinhada a Spring Boot 4.2, conforme [anúncio oficial](https://spring.io/blog/2026/08/26/spring-modulith-2-2-m1-2-1-1-2-0-8-and-1-4-13-released/), mas o projeto usa Boot 4.2.0-SNAPSHOT. As necessidades atuais são contratos síncronos e validação estática, já atendidas por ArchUnit. Não há caso concreto para publicação de eventos, outbox ou recursos runtime de descoberta de módulos. Evitamos adicionar mais uma matriz de compatibilidade sem benefício necessário.

## Limites e riscos conhecidos

- O domínio mantém JPA por decisão confirmada; não é uma implementação estrita de Clean Architecture sem frameworks.
- Schema, FKs e transações continuam compartilhados no monólito. Uma eventual extração física exigiria uma decisão própria de persistência e consistência; os contratos não tornam essa migração automática.
- Autenticação continua mockada por escopo anterior. A separação lógica não representa autorização criptográfica ou isolamento contra cliente malicioso.
- Não foram criados contextos futuros, eventos, mensageria ou abstrações para casos inexistentes.

[Arquitetura vigente](02-arquitetura.md) · [Retomada](06-retomada.md) · [Validação](04-validacao.md)

## Importação — 05/11/2026

`importacoes` é responsável por interpretação de arquivos e proveniência; consome somente os contratos públicos de acesso, ativos, instituições e operações. O motor permanece em operações. Consultas por código dos catálogos retornam snapshots; a listagem de recibos usa projeção sem arquivo ou registros. [Contrato](19-importacao-binance.md).
