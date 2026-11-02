# Carteiras — contrato e operação

Cadastro entregue em 02/11/2026. Esclarecimento vigente: `contas` representa a associação carteira/instituição; carteiras com Binance mostram as mesmas operações do usuário. O histórico financeiro futuro independe dos IDs desses vínculos. [Retomada](06-retomada.md).

## Uso

Entrar, abrir **Carteiras** e selecionar **Adicionar carteira**. Informar nome e ao menos uma instituição. A gravação cria vínculos em `contas`, únicos por carteira/instituição. Vínculos em carteiras diferentes têm IDs distintos e selecionam o mesmo histórico da instituição para o usuário.

Listagem com nome, instituições, situação e ações. Ativas e arquivadas ficam juntas; arquivadas permitem somente reativação. Criação/edição por modal, com preservação dos dados em erro, recuperação de falha do catálogo e bloqueio de controles enquanto salva.

Editar mantém/cria/remove somente vínculos da carteira. Arquivar os preserva. Operações futuras serão únicas por evento, associadas ao usuário/instituição; remoção de vínculo e arquivamento não apagam lançamentos, custos ou resultados. Reassociar permite consultar o histórico existente.

Nome obrigatório, normalizado nas extremidades, até 160 caracteres e único por usuário, inclusive arquivadas. Instituições inativas não permitem novas contas; contas existentes podem ser mantidas/removidas. Não há cadastro independente de contas ou criação de instituições pelo usuário.

## API

Rotas exigem Bearer; service consulta `UsuarioAtual.id()`, sem ID de perfil em header separado, corpo ou query. Identificação mockada permanece como documentada em [acesso](07-acesso-local.md).

| Método e rota | Entrada | Resposta |
| --- | --- | --- |
| `GET /api/v1/instituicoes` | — | Catálogo `{id,nome,ativo}` ordenado por nome/ID. |
| `GET /api/v1/carteiras` | `ativo` opcional | Carteiras do usuário; sem filtro inclui arquivadas. |
| `POST /api/v1/carteiras` | `{nome,instituicaoIds}` | 201 com carteira e contas. |
| `PUT /api/v1/carteiras/{id}` | `{nome,instituicaoIds}` | 200 com nome e contas atualizados. |
| `PATCH /api/v1/carteiras/{id}/situacao` | `{ativo}` | 200 com situação atualizada. |

Resposta: `{id,nome,ativo,contas:[{id,instituicaoId,instituicaoNome}]}`. IDs de resposta são strings. Seleções vazias, repetidas, inexistentes ou novas seleções inativas retornam 400. Carteira inexistente/de outro usuário retorna 404; Bearer ausente/inválido retorna 401; conflito de nome/concorrência retorna 409. Falha reverte toda a transação.

Rotas de titulares removidas. Catálogo somente de leitura. O contrato HTTP é preservado nesta revisão; muda a propriedade das contas.

## Organização e banco

`CarteiraService` coordena regras/transações; `CarteiraRepository` usa `@EntityGraph` para vínculos/instituições. `Conta` é entidade explícita da associação N:N: referencia carteira/instituição. Remoção de órfãs afeta apenas vínculos. O contrato HTTP mantém o nome `contas`; não implica propriedade de operações por carteira. Sem repository/controller de contas independente.

V8 corrigida exige titulares e contas antigos vazios. Catálogos existentes são preservados; Binance/BitGet são inseridas somente quando ausentes. Não há tabela `carteira_contas` nem `contas.perfil_ref`; unicidade por carteira/instituição. [Dicionário](11-dicionario-de-dados.md).

A reaplicação local foi autorizada com limpeza dos cadastros e backup; não é um procedimento automático para outros bancos. Testes históricos até V7 permanecem preservados. [Resultados e continuidade](06-retomada.md).
