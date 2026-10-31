# Lastro — MVP 4: entender, organizar e prestar contas

## Promessa

Entender os próprios números e ter os registros organizados para as obrigações fiscais fazem parte do mesmo contexto. Manter um histórico contínuo, sem refazer o controle mensal. A plataforma facilita a vida do investidor e o trabalho do contador; não substitui sua análise especializada.

## Público e escopo

Investidores brasileiros de cripto à vista, poucas contas, moedas de reserva e titulares separados. Primeiro provar registro, explicação e conferência. Não expandir para derivativos ou DeFi.

## Entregue

1. Resumo operacional: custo dos saldos, resultado realizado, taxas e negociações.
2. Composição rastreável: vendas + câmbio/conversões/rede = realizado; acesso aos cálculos por lançamento.
3. Conferência: pendências acionáveis e comparação manual de quantidades por titular/conta/ativo no fim de uma data.
4. Comparações recalculadas ao corrigir histórico; exportação com valores digitados e diferenças.
5. Histórico completo preserva o custo ao trocar o período. Resumo mensal e exportações apoiam a conferência com contador.
6. Detalhe do ativo foca quantidade, custo, conta e lançamentos formadores.
7. Estratégias, recomendações e cenários macro removidos da experiência. Cotações simuladas por cenário removidas; referências vêm dos lançamentos.

## Critérios de aceite

- Não oferecer compra/venda, stop, alvo, alocação ou cenário macro.
- Não apresentar referência histórica como preço de mercado atual ou como rentabilidade.
- Permitir rastrear a soma realizada aos lançamentos e às parcelas do cálculo.
- Comparar sem ajustar operações automaticamente; diferenças e coincidências não certificam custos ou impostos.
- Recalcular comparações após correção dos eventos, sem confiar em um status antigo salvo.
- Referências repetidas são suspeitas para revisão, nunca duplicatas confirmadas automaticamente.
- Preservar dados e backups existentes; novas comparações são opcionais no formato compatível.
- Continuar distinguindo taxas já consideradas de pagamentos de imposto registrados.

## Experimento de 30 dias

Recrutar 8–12 investidores além do fundador, observar um histórico real conciliável e obter revisão de um contador com experiência em cripto. Medir:

- Tempo até o primeiro resultado compreendido e saldo comparado.
- Divergências encontradas, causa e tempo para resolver.
- Esforço semanal, pedidos de suporte e uso espontâneo.
- Utilidade da exportação para o contador.
- Disposição de pagar, testada com oferta concreta.

Meta exploratória: pelo menos três usuários pagando e mantendo o próprio controle. Não é garantia estatística de mercado; serve para decidir o próximo investimento.

## Próximas etapas condicionais

1. Um importador CSV de corretora: arquivos reais, prévia, mapeamento, taxas, saldo anterior, identificação de duplicações e confirmação antes da escrita. Não confundir com restauração de backup.
2. Piloto assistido: descobrir lacunas de histórico sem transformar suporte manual em custo inviável.
3. Produção: autenticação, isolamento de titulares/contas, banco, histórico de alterações, recuperação, privacidade e cálculo decimal.
4. Revisão contábil independente da política de custos e câmbio; regras fiscais apenas em projeto separado, se houver demanda e capacidade de manutenção.

## Guardas de produto

Não transformar coincidência de quantidade em selo de carteira validada. Não esconder lacunas atrás de números precisos. Não usar IA para inferir custos ausentes ou prometer imposto correto. Um extrato digitado não foi autenticado. Histórico incompleto pode produzir preço médio incorreto mesmo com saldo coincidente.

## Organização fiscal e clareza visual

Entregue: compilado mensal com custo vendido; detalhamento das operações; saldos anuais a custo de aquisição por titular/ativo, com custódia; pagamentos de imposto; exportações CSV e relatório HTML para compartilhamento/impressão. O ano escolhido é o da data Até; a posição anual usa todo o histórico até 31/12, mesmo se o período for apenas um mês. Ano em andamento aparece como prévia. Sem histórico anterior não é saldo zero certificado.

Ícones apenas em métricas e colunas compactas. Orientações gerais, filtros, subtítulos e avisos usam texto breve visível. Suprimir o excesso de ícones não deve esconder as limitações de uso dos números.

A importação CSV futura usará o mesmo livro e os mesmos relatórios. O fluxo manual já deve entregar esse valor hoje. Continuam fora do escopo recomendações de investimentos, regras tributárias automáticas e emissão de DARF.
