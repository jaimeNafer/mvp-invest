# Lastro — decisões com contexto

Protótipo navegável para explorar uma plataforma de acompanhamento de investimentos em cripto, conectando carteira, tese e decisões.

## Executar localmente

Não há dependências nem etapa de build. Com Python 3 instalado:

```sh
python -m http.server 8000
```

Abra http://localhost:8000 no navegador.

## Funcionalidades

- Visão geral do patrimônio e distribuição de uma carteira demonstrativa.
- Posições em BTC, ETH e SOL e acesso às respectivas teses.
- Edição da motivação, invalidação, níveis de referência e próximo passo.
- Diário de decisões associado a cada ativo.
- Registro de revisão de tese.
- Persistência local via localStorage e restauração dos exemplos pelo diário.
- Layout adaptável a telas menores.

## Escopo da demonstração

Todos os valores, preços, gráficos e cenários são fictícios e estáticos. O protótipo não executa ordens, não integra corretoras, não consulta preços ao vivo e não possui análise por IA. Alterações ficam apenas no navegador e não são sincronizadas entre dispositivos.

O nome e os fluxos são provisórios e servem à descoberta do produto.

## Validação realizada

Navegação da visão geral para a tese, registro de uma decisão e edição do plano verificados no navegador. O fluxo de salvar decisões exibiu o registro na tese correspondente.
