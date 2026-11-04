# Criptoativos disponíveis pelo catálogo

Decisão aprovada por Jaime: saldo inicial e negociações utilizam o catálogo mantido pelo sistema, sem listas fixas de tickers nas telas, repositories ou validações de base negociada. Esta entrega não cadastra dados reais nem cria tela de manutenção do catálogo.

## Disponibilidade e contratos

- Compra/venda aceita ativos classificados como `CRIPTO`; novas gravações exigem disponibilidade no catálogo. Nome, ticker ou prefixo textual do código não substituem essa classificação.
- Saldo inicial admite os criptoativos e as moedas `fiat:BRL` e `fiat:USD`.
- Moedas de cotação continuam limitadas a `fiat:BRL`, `cripto:USDT` e `cripto:USDC`, diferentes do ativo principal. Taxas continuam nas moedas do par.
- `GET /api/v1/ativos` mantém campos e IDs textuais; passa a listar todo o catálogo, por ticker e ID, inclusive inativos para histórico. Formulários filtram o escopo e desabilitam inativos.
- O snapshot interno `AtivoResumo` inclui `boolean cripto` e mantém `permiteSaldoInicial`. O contexto de ativos calcula classificação e capacidade a partir do modelo; operações consome somente o contrato público, sem importar o enum de domínio de outro contexto. Capacidade e situação ativa são informações distintas.
- Acrescentar outro criptoativo ao catálogo não exige mudar listas de elegibilidade Java/React. A manutenção permanece por migration revisada; isso não libera novos tipos de evento, pares ou taxas em terceira moeda.

## Provisionamento por V11

| Código | Ticker | Nome |
| --- | --- | --- |
| `cripto:SOL` | SOL | Solana |
| `cripto:LINK` | LINK | Chainlink |
| `cripto:NEAR` | NEAR | NEAR Protocol |
| `cripto:LDO` | LDO | Lido DAO |
| `cripto:ARKM` | ARKM | Arkham |
| `cripto:RONIN` | RONIN | Ronin |
| `cripto:PYTH` | PYTH | Pyth Network |
| `cripto:TIA` | TIA | Celestia |

São inseridos somente registros ausentes, como `CRIPTO`, ativos e com 18 casas de registro. Essa precisão é a capacidade gerencial do Lastro, não um limite de rede, blockchain ou ordem de corretora. O símbolo RONIN corresponde à identificação da [Binance para Ronin](https://www.binance.com/en/support/announcement/detail/dfea1e2ae7fd475196b215e673193c63), preservando o símbolo do histórico examinado.

Registros compatíveis existentes conservam ID, nome e situação, inclusive se inativos. Incompatibilidades de código/ticker, tipo ou precisão interrompem a migration antes da inserção persistente; não corrigir ou reativar registros silenciosamente. V1–V10 permanecem imutáveis. V11 não acrescenta tabelas ou campos persistentes.

## Verificação e operação

O aceite exige ciclo de abertura, compra, venda e posição para os oito ativos; um criptoativo sintético adicional deve funcionar sem alteração de código de elegibilidade. Verificar taxa, precisão, saldo suficiente, inativos, histórico e repetição idempotente após inativação. Testes históricos permanecem fixados nas versões que caracterizam; os testes novos cobrem V10→V11, catálogo parcial/inativo, colisões e reaplicação.

Antes de aplicar localmente, gerar backup privado e capturar fatos/cadastros. Comparar os dados após a migration e conferir o catálogo no navegador sem cadastrar operações fictícias no livro pessoal. Evidências finais são registradas na [retomada](06-retomada.md).

Sem carga de Eliane, importação, novos pares, BNB como terceira moeda de taxa, Earn, airdrops, transferências ou preço/câmbio automático. Esses itens continuam no [backlog](ideias-futuras/README.md).

## Evidências de conclusão — 03/11/2026

Backend aprovado em check e pacote integrado: 55 testes unitários/arquiteturais e 68 integrações MySQL. Oito testes de migration foram reexecutados com alvo histórico V11. Frontend: 46 testes, lint, typecheck/build; protótipo: 1 teste. Testes RED demonstraram a ausência de ativo adicional no catálogo, a seleção indevida de moeda fiduciária no formulário e a comparação de tipo insensível à caixa; correções verificadas.

V11 aplicada localmente após backup; fatos/cadastros, metadados e ativos anteriores preservados por comparações privadas, incluindo respostas de ambos os perfis. Reinício sem reaplicação; catálogo e formulários conferidos no navegador sem gravar operações. Nenhum dado real entrou nas fixtures ou documentação versionada.
