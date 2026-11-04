# Ordens com várias execuções e taxas distintas

Identificada na revisão do backlog em 03/11/2026. Estado: **a avaliar**, sem aprovação de implementação ou inclusão na próxima entrega.

## Oportunidade

Uma ordem pode aparecer no extrato dividida em execuções com quantidades, preços, horários e taxas distintos. Avaliar como facilitar o registro fiel desses dados e sua conferência, preservando a explicação dos custos e resultados.

## Avaliação necessária

- Analisar um extrato real fornecido para essa finalidade e identificar como ordem, execuções e taxas são representadas.
- Verificar se o lançamento manual de cada execução, com referência à mesma ordem, já atende ao uso.
- Avaliar se agrupamento de consulta ou entrada assistida reduziria esforço sem perder a sequência temporal ou o detalhamento das taxas.
- Conferir a soma de quantidades, valores e taxas contra o documento e identificar os riscos de duplicação e arredondamento.

## Evidência do diagnóstico documental — 03/11/2026

As exportações examinadas representam negociações por linhas de compra, pagamento, receita, venda e taxa. Várias linhas podem compartilhar o mesmo segundo. Observação ou referência parcial não oferece identificação suficiente para parear automaticamente todas as pernas.

Avaliar o uso de comprovantes com IDs de ordem e execução antes de reconstruir eventos. Preservar execuções distintas com os mesmos valores e não consolidar só por horário. A comparação de exportações sobrepostas também preserva a multiplicidade dentro de cada arquivo; identidade de arquivo/linha e identidade de negócio são conceitos diferentes.

O diagnóstico foi concluído, mas o agrupamento financeiro permanece não aprovado. Dependências de proveniência e demais produtos estão nos [complementos de históricos](../historico-completo-corretoras/README.md). Não foi implementada importação ou consolidação.

## Limites da implementação futura

Não presumir consolidação automática de execuções. Referência de ordem repetida, isoladamente, não prova duplicidade de operação. Importação CSV permanece adiada e será planejada a partir de um formato concreto, caso o fluxo validado justifique sua inclusão.

Decidir escopo, dependências e critérios de aceite somente após essa avaliação. Não antecipar modelo de persistência, interfaces ou tratamento tributário.

[Índice do backlog](../README.md) · [Roteiro](../../03-entregas.md)
