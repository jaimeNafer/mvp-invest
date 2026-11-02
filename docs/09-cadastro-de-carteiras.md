# Carteiras — contrato e operação

Implementação de 02/11/2026. Substitui o cadastro de titulares. Carteiras são visões das contas do usuário: uma mesma conta pode aparecer em várias carteiras, sem copiar seus dados. [Modelagem e diagrama](10-modelagem-de-carteiras.md).

Etapa concluída e encerrada por Jaime em 02/11/2026, com publicação solicitada em PR para `main`, sem merge automático.

## Uso

Entrar, abrir **Carteiras** e clicar em **Adicionar carteira**. Informar o nome e selecionar pelo menos uma instituição. A gravação cria ou reutiliza as contas do usuário e vincula-as à carteira em uma transação.

A listagem apresenta nome, instituições das contas, situação e ações. Ativas e arquivadas ficam juntas. Adicionar/editar usa modal; arquivadas permanecem visíveis e permitem somente reativar. Arquivar não altera a conta compartilhada nem outras carteiras. Remover uma instituição na edição remove somente o vínculo daquela carteira; selecionar novamente reutiliza a conta anterior.

O catálogo é mantido pelo sistema e inicia com Binance e BitGet. Não existe tela de criação de instituições ou cadastro independente de contas. Instituições inativas não permitem novos vínculos; vínculos existentes podem ser mantidos ou removidos.

Nome obrigatório até 160 caracteres, sem espaços nas extremidades e único por usuário, inclusive entre carteiras arquivadas. A modal preserva nome/seleção em falhas, permite cancelar/fechar/Escape e bloqueia fechamento e campos durante a gravação. Falhas de catálogo/listagem oferecem nova tentativa, sem simular dados vazios. Respostas antigas não substituem consultas mais recentes.

## API

Todas as rotas exigem Bearer, lido pelo filter e disponibilizado em `UsuarioAtual`. A service obtém o usuário pelo contexto, sem ID de perfil no corpo, query ou header separado. O JWT atual permanece mockado; [limites de acesso](07-acesso-local.md).

| Método e rota | Entrada | Resposta |
| --- | --- | --- |
| `GET /api/v1/instituicoes` | — | Catálogo ordenado por nome/ID: `{id,nome,ativo}`. |
| `GET /api/v1/carteiras` | `ativo` opcional | Carteiras do usuário, ordenadas por nome/ID; sem filtro inclui arquivadas. |
| `POST /api/v1/carteiras` | `{nome,instituicaoIds}` | 201 com a carteira e suas contas. |
| `PUT /api/v1/carteiras/{id}` | `{nome,instituicaoIds}` | 200 com nome e vínculos atualizados. |
| `PATCH /api/v1/carteiras/{id}/situacao` | `{ativo}` | 200 com a situação atualizada. |

Resposta de carteira: `{id,nome,ativo,contas:[{id,instituicaoId,instituicaoNome}]}`. IDs são strings nas respostas; o frontend também envia IDs de instituições como strings numéricas positivas. Contas são apresentadas por nome da instituição e ID. Seleção vazia, repetida, inexistente ou novo vínculo inativo é recusado.

Erros: 400 para entrada inválida ou edição de carteira arquivada; 401 para Bearer ausente/inválido; 404 para carteira inexistente/de outro usuário; 409 para conflito de nome ou concorrência. Falhas de gravação revertem toda a transação. Repetir uma requisição que perdeu a disputa pela criação da conta permite reutilizar a conta já criada.

`/api/v1/titulares` foi removido. Catálogo não oferece endpoints de escrita.

## Organização

- `carteiras`: controller com DTOs/validação/erros HTTP, service com transações e regras, repository JPA e entity.
- `contas`: entity e repository para localizar ou criar contas únicas por usuário/instituição. Sem controller ou tela própria.
- `instituicoes`: entity, repository somente de leitura e controller do catálogo. Manutenção por migrations/backoffice futuro.
- JPA usa `@EntityGraph` para carregar contas/instituições necessárias aos DTOs, sem cascade de exclusão.
- Frontend reaproveita modal, ícones, cliente HTTP, serviços de acesso, JWT e armazenamento. Formulário concentra estado/seleção do catálogo; serviços concentram chamadas.

## Banco e implantação

V1–V7 permanecem imutáveis. V8 exige titulares, contas e instituições vazios antes de modificar o schema. O banco local é conferido novamente antes de aplicar, com backup privado; dados inesperados interrompem a atualização, sem conversão automática. A própria migration possui uma verificação antes dos comandos destrutivos.

V8 cria carteiras, contas por usuário/instituição e seus vínculos, remove titulares e os campos antigos das contas, preserva ativos e cadastra Binance/BitGet com país não informado. Comentários do schema descrevem somente a finalidade. [Dicionário vigente](11-dicionario-de-dados.md).

## Verificação e continuidade

Cobertura: criação/edição com uma ou várias instituições, validação, compartilhamento de conta entre carteiras, separação de usuários, remoção de vínculos, arquivo/reativação, instituições inativas, rollback, conflitos e concorrência. Migrações históricas são testadas até V7; atualização V7 → V8 e instalação vazia têm testes próprios, incluindo recusa quando há dados antigos.

Resultados executados e verificação no navegador: [nota de retomada](06-retomada.md). A validação remota da publicação será registrada nos checks do PR desta entrega.

Próxima fatia: definir o primeiro fluxo de operações, vinculado à conta do usuário. Motor, importação, relatórios e backoffice não fazem parte desta entrega.
