# Regras que o banco, sozinho, não implementa

O DDL organiza o armazenamento. Ele não implementa o motor gerencial nem uma obrigação tributária. Use transações, autorização e BigDecimal no backend Java/Kotlin. Autorização não pode depender de um `espaco_id` livre enviado pelo navegador.

## 1. Fonte de verdade

`operacoes` + `movimentos` + `pagamentos_imposto` contêm os fatos. `resultados_*`, `posicoes_calculadas` e `saldos_contas` são projeções reconstruíveis. Não atualizar um saldo manualmente para fazer uma diferença desaparecer.

Confirmadas entram no livro. Rascunhos não entram. Para corrigir uma confirmada:

1. Abrir transação e bloquear a linha de `espacos` para ordenar revisões.
2. Conferir permissão e versão esperada da revisão.
3. Criar operação substituta com os novos movimentos; manter a original sem alterar quantidade, data, preço ou taxa.
4. Marcar a anterior como SUBSTITUIDA, vincular `substitui_operacao_id` na nova, confirmar a nova e registrar a auditoria na mesma transação. Uma operação não pode substituir a si mesma; a cadeia não pode ter ciclos.
5. Incrementar `revisao_livro` e invalidar o uso de projeções antigas. Cancelamento também incrementa a revisão. Contas/titulares/arquivos podem ser arquivados sem remover o histórico.
6. Processar o histórico afetado. Antes de confirmar definitivamente, validar que a correção não deixa saídas posteriores sem quantidade disponível. A política inicial recomendada é rejeitar correções que inviabilizem operações posteriores e explicar o conflito.

O banco impede dois sucessores diretos para a mesma operação, mas não verifica todos os ciclos de uma cadeia. O serviço precisa fazê-lo. A autoria e as mudanças de status devem ser auditadas; não expor DELETE genérico de histórico na API. Usar permissões distintas para migração, escrita financeira e auditoria append-only. O JSON de auditoria deve ser minimizado; não replicar CPF aberto, tokens ou arquivos nele.

## 2. Pernas por tipo

| Evento | Movimentos esperados |
| --- | --- |
| SALDO_INICIAL | ENTRADA / SALDO_INICIAL, com quantidade e custo inicial conhecido ou explicitamente pendente. |
| APORTE | ENTRADA / CAIXA. Não é lucro. |
| COMPRA | ENTRADA / ATIVO + SAIDA / LIQUIDACAO + zero ou mais SAIDA / TAXA. |
| VENDA | SAIDA / ATIVO + ENTRADA / LIQUIDACAO + zero ou mais SAIDA / TAXA. |
| CONVERSAO | ENTRADA / ATIVO recebido + SAIDA / LIQUIDACAO entregue + taxas. |
| RETIRADA | SAIDA / CAIXA. Movimento de capital separado do lucro; moeda pode ter variação cambial própria. |
| TRANSFERENCIA | SAIDA e ENTRADA / TRANSFERENCIA com mesmo ativo, mesma quantidade e contas diferentes do mesmo titular; taxa separada. |
| IMPOSTO | Um `pagamentos_imposto`. Se EXTERNO, não cria movimento de carteira. Se CARTEIRA, uma SAIDA / CAIXA na moeda paga; preservar quantidade e cotação. |

As pernas são **brutas**. Exemplo: venda de 1.800 BRL com taxa de 5 BRL registra entrada 1.800 e saída TAXA 5, não entrada 1.795 mais outra saída 5. Quantidade líquida em extrato pode precisar ser convertida para bruta pelo importador, sem perder a linha original.

Na confirmação, o serviço valida em conjunto:

- Tipo, número e sentido das pernas; presença de um único ATIVO principal em compra/venda/conversão.
- Correspondência com `ativo_preco_id`/`moeda_preco_id`; quantidade de liquidação = quantidade negociada × preço, dentro da política decimal documentada.
- Cotação, valor em reais e fonte coerentes. Para BRL, cotação deve ser 1 e valor precisa corresponder à quantidade. Não inferir dólar ou USDC de hoje para operação passada.
- Todas as contas do mesmo titular e espaço. O banco já bloqueia grande parte desses vínculos com FKs compostas, mas autorização continua na API.
- Saldo suficiente na conta de saída e no consolidado, considerando as taxas. Sem alavancagem, saldo negativo ou transferência entre titulares neste MVP.
- Taxas no ativo recebido não podem consumir toda a quantidade. Taxa no ativo vendido pode consumir unidades adicionais; taxa na moeda de recebimento reduz o caixa. Outras moedas de taxa exigem saldo e cotação histórica próprios.
- Um SALDO_INICIAL não cria lucro nem aporte novo. `NULL` é custo desconhecido; 0 é um custo explicitamente informado como zero e deve pedir conferência da origem.
- País da instituição e referências podem estar ausentes no cadastro, mas aparecem como pendências; não determinar tratamento tributário somente com esses campos.
- Para IMPOSTO, valor registrado, moeda, cotação, competência e origem de débito precisam ser coerentes. Há no máximo um pagamento por operação; a FK não confirma sozinha o tipo IMPOSTO.

CHECKs não conseguem validar somas e cardinalidade entre diversas linhas. Essas regras não foram disfarçadas como se já estivessem implementadas em SQL.

## 3. Custo, câmbio e taxas

Média móvel ponderada por **espaço + titular + ativo**, com custódia separada por conta. Ordenar por instante UTC, `ordem_livro` e ID, preservando desempates nas substituições. A política é gerencial e deve ser revisada com profissional especializado antes de uso como base de apuração.

- Compra incorpora custo e taxas elegíveis. Venda baixa proporcionalmente o custo anterior e calcula receita líquida menos esse custo.
- Usar uma moeda de reserva, por exemplo USDC, pode gerar resultado próprio de câmbio/conversão. Essa parcela precisa ser separada da venda do ativo principal.
- Taxa em outro token pode baixar seu próprio custo e gerar variação. Transferência própria preserva custo do principal; taxa de rede não deve ser somada ao resultado duas vezes.
- `resultados_operacoes.custo_baixado_brl` e `custo_incorporado_brl` descrevem o **ativo principal**. Custos da moeda de liquidação e das taxas ficam em `resultados_movimentos`; não somar tudo e chamar de custo vendido do ativo principal.
- Em uma compra pode haver resultado de uso da moeda de pagamento. Não presumir que compra significa resultado zero.
- Em uma venda, a taxa alocada à receita líquida não é reconhecida outra vez como despesa de rede. As parcelas de resultado das pernas devem reconciliar com o total da operação.
- `valor_referencia_brl` mantém evidência bruta. Não exigir que ele seja igual à receita líquida calculada da venda.

Usar BigDecimal em quantidade e valor, inclusive ao desserializar o CSV/JSON. Não usar `Double` intermediário. Persistir totais BRL com 8 casas, quantidades e preços com 18; escolher e versionar modo/etapas de arredondamento (ex.: HALF_EVEN) antes da produção. Arredondar para 2 casas na exibição não permite arredondar cada custo intermediário para centavos. Se a precisão suportada não comportar o ativo, rejeitar com explicação, nunca truncar silenciosamente.

## 4. Execuções e relatórios

Ao iniciar um cálculo, capturar uma revisão consistente do livro, seu método e data limite. Uma execução de data limite setembro processa compras de meses/anos anteriores; o filtro mensal só é aplicado depois, para apresentar resultados.

Persistir, numa unidade consistente, operações processadas, resultados por movimento, posições e saldos de conta. Só marcar CONCLUIDO após verificar cobertura e reconciliação. Em cálculos incompletos, preencher PENDENTE/NULL e explicar a lacuna; não certificar zero. A publicação da execução e seus detalhes precisa ser atômica. Não editar os resultados de uma execução concluída.

As consultas de exemplo excluem snapshots cuja revisão difere de `espacos.revisao_livro`. Após corrigir um evento antigo, reprocessar também as datas de comparação relevantes. Não reutilizar um selo de quantidade coincidente salvo anteriormente.

Para saldos anuais, comparar snapshots até 31/12 anterior e 31/12 escolhido, ambos da revisão atual. Um registro de saldo inicial em 01/01 não prova o saldo de 31/12 anterior. Ausência de posição anterior não é saldo zero certificado. Um ativo encerrado pode manter posição zero explícita quando há histórico para sustentá-la. Não gravar relatórios mensais manuais como uma nova fonte de verdade.

O resumo de pagamentos usa data de pagamento. A competência é um atributo separado. Relatórios apresentam registros e pendências, não isenção, imposto devido ou código de declaração inferido automaticamente.

## 5. Importação futura

Guardar arquivo e sua hash, lote, versão do mapeador e linhas originais. O usuário revisa a prévia antes de confirmar. Operação manual não precisa de arquivo. Uma linha pode originar mais de um evento se o formato assim exigir; a correspondência deve permanecer explícita.

A chave `idempotencia` é SHA-256 de uma identidade estável do item/requisição, produzida pelo serviço, e é única no espaço. Para CSV, incluir conta, identidade canônica do evento (trade/fill externo quando disponível), versão de normalização aplicável e discriminador se houver vários eventos na linha. Não usar apenas o número da linha ou a hash do arquivo: extratos sobrepostos precisam reconhecer o mesmo evento em arquivos diferentes.

`referencia_externa` não é única: uma ordem pode ter diversos fills legítimos. `fingerprint` da linha serve para apontar candidatos, não para apagar ou ignorar operações automaticamente. Confirmar lote e seus eventos numa transação ou em lotes idempotentes bem definidos. Retry não pode duplicar movimento. Cancelamento e correção são decisões explícitas, não efeitos colaterais de reimportar.

## 6. Acesso e privacidade

A associação `membros_espaco` permite acesso ao espaço inteiro. Se futuramente houver compartilhamento de somente um titular com contador, adicionar concessões específicas; não alegar que esse escopo já existe. Todas as rotas precisam testar associação ativa e papel. FKs não implementam isolamento de leitura nem Row-Level Security no MySQL.

Usuários usam OIDC na proposta; se a decisão for login próprio, adicionar credenciais com hash de senha adequado, verificação e recuperação em módulo específico. Documentos fiscais cifrados são opcionais. A chave de criptografia e a chave do HMAC ficam fora do banco, com gestão e rotação próprias. Sem segredos de APIs de corretora no MVP.

Datas DATETIME são UTC por convenção do serviço; configurar conexões em UTC. `data_local` e `fuso_origem` devem corresponder ao instante real, calculados na aplicação. O banco não verifica essa correspondência. CNPJ e país da instituição são cadastro, não classificação tributária automática.

Ativos têm identidade canônica. Um símbolo igual em dois tokens não implica o mesmo ativo. Redes/wrapped tokens e a decisão de equivalência exigirão uma política explícita; não consolidar automaticamente por ticker.

## 7. Escopo deliberadamente adiado

Não há tabelas de sinais, stops, recomendações, rentabilidade de mercado, alíquotas, isenções, DARF calculado ou compensações tributárias. Não há cache genérico misturado aos fatos financeiros. Cotações atuais, autenticação adicional, regras fiscais e integrações poderão ter módulos próprios após a validação do MVP.
