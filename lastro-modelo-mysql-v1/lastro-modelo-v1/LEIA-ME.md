# Lastro — modelagem inicial de dados v1

Preparado em 01/11/2026. **20 tabelas, MySQL 8.0.16+ ou 8.4, InnoDB e utf8mb4.** Este é um modelo físico inicial para o MVP com backend e frontend, baseado nas funcionalidades aprovadas. Não altera o protótipo e não é uma migração de uma base existente.

## Importar no MySQL Workbench

Para obter o diagrama sem precisar criar as tabelas num servidor:

1. Abra **File → New Model**.
2. No modelo aberto, use **File → Import → Reverse Engineer MySQL Create Script…**.
3. Escolha `01_schema_lastro.sql` e avance no assistente. Se disponível, marque a opção de colocar os objetos importados em um diagrama.
4. Se o diagrama não abrir, crie **Add Diagram**, expanda **Catalog → lastro_mvp → Tables** e arraste as tabelas. Use **Arrange → Autolayout** para organizar.
5. Salve com **File → Save Model As…** para gerar seu `.mwb`.

O arquivo entregue é SQL DDL importável; não é um `.mwb` binário pronto. O Workbench gera o arquivo nativo depois da importação. Caminho oficial: https://dev.mysql.com/doc/workbench/en/wb-reverse-engineer-create-script.html

Para criar um banco real, abra uma conexão MySQL, abra o SQL e execute em uma instalação nova. O script cria o schema `lastro_mvp`, sem DROP, sem desabilitar FKs e sem apagar dados. Se esse schema já tiver tabelas, use outro nome numa cópia do script ou uma instância vazia. Ele falha em tabelas já existentes de propósito, evitando fingir uma migração.

**Não execute a carga fictícia em produção.** Para estudar os exemplos, execute `02_exemplo_ficticio.sql` depois do schema em um banco novo. Consulte os resultados com `03_consultas_exemplo.sql`.

## Arquivos

| Arquivo | Uso |
| --- | --- |
| `01_schema_lastro.sql` | DDL completo com PKs, FKs, índices, comentários e CHECKs. |
| `02_exemplo_ficticio.sql` | Aporte, compra, venda, taxa, snapshots e comparação de saldo. |
| `03_consultas_exemplo.sql` | Carteira a custo, mensal, imposto registrado, conciliação e comparação anual. |
| `DICIONARIO.md` | Definição de todos os campos, incluindo precisão e origem. |
| `REGRAS_DO_BACKEND.md` | Invariantes transacionais e política de cálculo a implementar. |
| `DIAGRAMA.md` | Visões menores dos relacionamentos, em Mermaid. |
| `validar_modelo.py` e `modelo_meta.json` | Validação auxiliar de chaves, tipos e restrições; não são uma migração nem runtime do produto. |

## Avaliação da modelagem que você enviou

Há boas escolhas que foram mantidas conceitualmente: arquivos de origem, operações, moedas e histórico de registros. Eu remodelaria os pontos seguintes:

| Na imagem | Na proposta | Por quê |
| --- | --- | --- |
| `usuarios` com CPF/CNPJ | Usuário de acesso separado de `titulares` | Login e dono dos investimentos são conceitos diferentes. Um usuário pode controlar dois titulares sem misturar custos. |
| `carteira` ligada à corretora | `espacos` → `titulares` → `contas` | A carteira consolidada reúne várias corretoras e wallets; a custódia continua por conta. |
| `ativos` contém quantidade, preço médio e lucro | `ativos` é catálogo; `posicoes_calculadas` guarda snapshots | Saldo/resultado é consequência do histórico, não atributo cadastral da moeda. |
| `ticker` como chave de moeda | ID interno e `codigo` canônico único; ticker pode repetir | Evita colisão de símbolos e dependência de nomes de provedores. |
| Operação tem uma única taxa | Taxa é movimento de saída, com ativo, conta, quantidade e câmbio | Suporta mais de uma taxa, inclusive BNB ou taxa de rede. |
| Entrada/saída com datas no mesmo registro | Um instante por evento, com múltiplas pernas | Compra e venda em datas diferentes são eventos diferentes, e não uma posição acoplada. |
| Operação depende de arquivo | Linha de importação opcional | Registro manual não exige CSV; importação mantém sua origem. |
| `registros.finalidade` em texto | `movimentos.papel` e `direcao` estruturados | Permite distinguir ativo, liquidação, taxa, caixa e transferência. |
| Valores monetários com precisões diferentes | Quantidades/preços `DECIMAL(38,18)`; totais BRL `DECIMAL(38,8)` | Evita inteiro de lucro e reduz perda de precisão. O backend usa BigDecimal e exibe reais em 2 casas. |
| Exclusão lógica genérica | Eventos substituídos/cancelados e auditoria | Uma correção precisa permitir entender por que o resultado mudou. |
| `cache` de preço/câmbio | Câmbio histórico preservado no movimento | Cotação atual não pode sobrescrever o valor usado para calcular um evento antigo. Cache de mercado pode ser adicionado separadamente. |

Na imagem, alguns nomes de FK/índice também parecem herdados de tabelas anteriores (`id_operacao_UNIQUE` em finalidades, por exemplo). Como foi enviada uma imagem, e não o DDL original, não é possível confirmar as restrições efetivas dessa base. A análise é conceitual.

## Organização do modelo

| Bloco | Tabelas |
| --- | --- |
| Acesso e propriedade | usuarios, espacos, membros_espaco, titulares |
| Cadastros e custódia | instituicoes, ativos, contas |
| Documentos/importação futura | arquivos, importacoes, linhas_importacao |
| Fatos financeiros | operacoes, movimentos, pagamentos_imposto |
| Cálculos reproduzíveis | execucoes_calculo, resultados_operacoes, resultados_movimentos, posicoes_calculadas, saldos_contas |
| Conferência e histórico de alterações | conciliacoes_saldo, auditoria |

As 20 tabelas não representam 20 telas. O usuário continuará vendo lançamento simples, carteira, organização fiscal e conferência. Importação e autenticação delegada são preparações arquiteturais; não são funcionalidades já implementadas no aplicativo por este DDL.

## Decisões principais

- **Custo médio por titular/ativo; quantidade por conta.** Transferir entre duas contas do mesmo titular não cria uma venda.
- **Tudo em decimal.** Não usar `double`/`float` no backend nem converter JSON monetário para ponto flutuante. Precisão máxima suportada no MVP: 18 casas de quantidade. Ativos que precisem de mais casas exigem decisão explícita futura.
- **Fatos e resultados separados.** O cadastro não grava lucro manual como verdade. Cálculos ficam associados a uma revisão do livro e versão do motor.
- **Custo desconhecido não é zero.** `custo_inicial_brl = NULL` preserva a pendência. A aplicação não pode publicar um resultado confiável dependente desse custo como se fosse zero.
- **Histórico contínuo.** Um relatório de setembro calcula custo com compras anteriores, mesmo que não apareçam no mês. Não se cadastram novas operações no início de cada mês.
- **Saldos anuais a custo.** Processar até 31/12 do ano anterior e do ano escolhido, e comparar snapshots. Sem snapshot/registro anterior não inventar zero. Ano em andamento é uma prévia.
- **Pagamento de imposto é registro.** O schema não calcula obrigação, não emite DARF e não classifica automaticamente o enquadramento fiscal.
- **Sem recomendações.** Não há tese, stop, alvo, recomendação de compra/venda ou alerta macro.
- **Imutabilidade por serviço/permissões.** O DDL tem RESTRICT e trilha de auditoria, mas sozinho não impede que alguém com privilégio de UPDATE altere fatos. Veja as regras do backend.

## Validação realizada e limite

Foi validada a estrutura de chaves e tipos, a referência a chaves únicas explícitas, a ausência de DDL destrutivo e uma tradução de teste em SQLite em memória. A fixture foi carregada e testes negativos rejeitaram contas de outro titular/espaço, chave idempotente repetida, quantidade negativa, preço zero, taxa de entrada, soma inconsistente e exclusão de titular com histórico.

O exemplo com Decimal resulta em compra de 0,01 BTC por R$ 4.010 de custo, venda de 0,004 BTC com resultado de R$ 191 e saldo de 0,006 BTC com custo de R$ 2.406; caixa final R$ 7.785.

**Não havia MySQL Server ou Workbench disponível neste ambiente. A execução nativa e o reverse engineering visual ainda precisam ser confirmados no seu MySQL/Workbench.** SQLite não comprova a precisão decimal ou toda a sintaxe específica do MySQL. Para repetir a validação auxiliar: `python3 validar_modelo.py`.

## Antes da primeira migration do backend

Confirmar quatro escolhas: autenticação OIDC ou credenciais próprias; regra de arredondamento/versionamento; política de confirmação/correção retroativa; e o primeiro formato de CSV futuro. O desenho propõe OIDC, média gerencial por titular, revisão transacional e arquivos privados. Não inclui execução automática de cálculos, armazenamento de segredos de corretora ou um mecanismo tributário.
