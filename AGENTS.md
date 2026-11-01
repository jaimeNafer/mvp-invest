# Acordos de desenvolvimento do Lastro

Estas instruções se aplicam ao repositório inteiro. Foram solicitadas por Jaime em 01/11/2026 e devem ser lidas antes de implementar ou revisar mudanças. Manter este arquivo atualizado com novos acordos explícitos.

## Qualidade desde a primeira entrega

- Desenvolvimento incremental reduz o escopo da entrega, não o padrão de qualidade do código ou da experiência.
- Implementar cada funcionalidade com estrutura sustentável e critérios de aceite. Não introduzir atalhos provisórios que exijam reescrever a funcionalidade depois.
- Priorizar nomes claros, funções com responsabilidade definida e separação entre apresentação, aplicação, domínio e persistência. Extrair responsabilidades quando isso melhorar a compreensão; não criar camadas sem necessidade.
- Evitar comentários explicativos no código. Melhorar nomes e estrutura para que o código expresse sua intenção. Contexto, justificativas, limitações e procedimentos pertencem à documentação.
- Não contornar problemas com supressões de lint, dependências forçadas, desativação de validações ou testes ignorados. Resolver a causa. Preservar avisos legais/licenças e diretivas necessárias de ferramentas.
- Não reescrever migrations já aplicadas apenas para adequar comentários ou estilo; evoluir o schema por novas migrations.
- Manter formatação consistente e executar testes pertinentes ao comportamento alterado.
- Aplicar o mesmo padrão de organização e separação de responsabilidades ao código mockado. Mocks substituem uma implementação dentro do escopo acordado; não dispensam contratos claros ou serviços reutilizáveis.

## Interface compatível com produção

- Telas, textos, estados e controles entregues devem ser apropriados a uma versão final de produção, mesmo quando poucas funcionalidades estiverem disponíveis.
- Não expor bastidores do desenvolvimento na interface: fases, marcos, roadmap, avisos de construção, referências a mocks/protótipos ou detalhes de stack e ambiente sem utilidade para a decisão do usuário.
- Não inserir mensagens como “Acesso reservado a Jaime e Eliane”. Os perfis conhecidos pertencem ao serviço de acesso, não aos textos da tela de login.
- Decisão vigente: login e senha informados na tela e validados por um serviço mockado no frontend, com Jaime e Eliane. As senhas do mock permanecem vazias. A autenticação real será integrada a outro backend específico para essa finalidade. Por pedido posterior de Jaime, o serviço mockado salva um JWT sem assinatura em localStorage (`lastro.token`), com nome e role `acesso_total`; sair remove o token. Não validar esse mock no backend nem tratá-lo como autorização real.
- Exibir somente controles funcionais. Funcionalidades futuras ficam no planejamento, sem menus fictícios, botões sem ação ou promessas na tela.
- Não inventar métricas, saldos ou estados vazios sem consultar a fonte de dados correspondente. Um histórico ainda não implementado não equivale a uma carteira vazia.
- Usar linguagem de produto: orientação de uso e mensagens de erro claras, úteis e acionáveis. Restrições de escopo, instruções de desenvolvimento e operação local ficam no README e em `docs/`.
- O protótipo preservado é uma referência histórica. Sua linguagem de demonstração não deve ser copiada automaticamente para a nova aplicação.

## Entregas com valor real

- Fatiar por funcionalidades completas e utilizáveis, não por demonstrações técnicas apresentadas como entrega funcional.
- Dados e regras de negócio usam MySQL persistente; não substituir esse fluxo por dados simulados para simplificar. Exceção explícita de Jaime: a camada de autenticação fica mockada no frontend até a integração com um serviço externo.
- Bancos descartáveis e mocks de negócio ficam nos testes isolados. O mock de autenticação no frontend é uma exceção de escopo autorizada; não o apresentar como autenticação real.
- O backend de negócio não gerencia usuários, senhas, sessões ou autenticação. Remover seus endpoints/configurações/tabelas de controle de acesso; preservar estruturas de negócio. Migrações aplicadas permanecem imutáveis.
- Resolver dependências necessárias ao fluxo, incluindo configuração local e credenciais, em vez de adiar o requisito e entregar um substituto provisório.
- Avançar diretamente no trabalho autorizado; pedir somente informações ou decisões que realmente dependam do usuário. Não repetir ciclos de aprovação para escolhas rotineiras.

## Registro e continuidade

- A etapa de tela de login e mock de JWT foi encerrada por Jaime em 01/11/2026. Não reabrir ou expandir essa etapa sem novo requisito. O envio do token nas chamadas HTTP ainda pertence à próxima integração.
- Registrar decisões arquiteturais e de produto em `docs/`, distinguindo decisões confirmadas e propostas.
- Atualizar a nota de retomada e o roteiro ao concluir uma entrega, sem declarar funcionalidades, testes ou execução de CI que ainda não foram comprovados.
- Preservar alterações existentes do usuário e manter segredos e dados reais fora do Git, dos testes e dos logs.
- No versionamento, preservar as sete imagens de referência, o protótipo, o modelo MySQL descompactado, migrations, lockfile e Gradle Wrapper. Ignorar o ZIP duplicado do modelo, configurações privadas, backups locais e artefatos gerados; não criar exclusões genéricas de imagens, SQL ou JAR.

## Proporção e controle da arquitetura

- Autenticação é responsabilidade de um backend externo específico, a implementar em outro momento. Não reintroduzir identificação, controle de usuários ou sessões no backend de negócio. O frontend usa uma função de serviço assíncrona para permitir futura substituição pela integração externa.
- Resolver a necessidade atual com consultas claras e a menor estrutura compreensível. Não criar camadas, interfaces, serviços ou configurações por antecipação. Quantidade de classes não é critério de qualidade.
- Centralizar todo acesso à localStorage no serviço reutilizável `frontend/src/services/armazenamentoLocal.ts`. Autenticação e demais consumidores usam esse serviço; ele não conhece JWT, usuários ou regras de negócio.
- Centralizar a construção do JWT mockado em `frontend/src/services/jwt.ts`, incluindo header, emissão, codificação e montagem. Autenticação valida os dados do mock, define as claims e coordena geração e armazenamento. Manter essas responsabilidades separadas mesmo no código mockado.
- Antes de expandir o backend para a próxima funcionalidade, discutir com Jaime as responsabilidades, a organização e os critérios para extrair novas classes. Não tratar propostas anteriores como arquitetura já aprovada.

## Aplicação obrigatória nas próximas tarefas

- Ler estes acordos e consultar `docs/08-acordos-de-qualidade.md` antes de implementar. É responsabilidade do agente aplicar os aprendizados, sem esperar novos lembretes ou correções de Jaime.
- Antes de codificar, identificar o comportamento esperado, o escopo atual, os responsáveis por cada comportamento e os serviços existentes que podem ser reutilizados.
- Durante a implementação, manter apresentação, coordenação, mecanismos reutilizáveis e regras de negócio nos lugares apropriados. Extrair responsabilidades concretas; não criar infraestrutura por antecipação nem concentrar tudo para reduzir a quantidade de arquivos.
- Antes de apresentar a entrega, revisar responsabilidades misturadas, duplicação, acesso direto à localStorage fora do serviço próprio, construção de JWT fora de `jwt.ts`, complexidade sem necessidade e linguagem da interface. Corrigir dentro do escopo autorizado, sem aguardar a revisão do usuário.
- Executar verificações proporcionais à mudança e atualizar contratos, estado da entrega e nota de retomada. Declarar somente o que foi implementado e comprovado. Essa revisão não exige novos ciclos de aprovação para escolhas rotineiras.
