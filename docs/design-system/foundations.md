# Fundamentos

Os tokens CSS iniciais estão em `frontend/src/styles.css`, em `:root`. Usam as cores já presentes na interface: fundo (`--cor-fundo`), superfície (`--cor-superficie`), navegação (`--cor-sidebar`), texto (`--cor-texto`, `--cor-texto-secundario`), borda (`--cor-borda`), ação primária e hover, foco, erro e sucesso. Estes tokens preservam os valores anteriores; outras cores e dimensões ainda precisam de consolidação.

A fonte existente é a fonte do sistema. Prefira espaçamentos de 4, 8, 12, 16, 24 e 32 px conforme o contexto e controles de pelo menos 36 px no desktop e 44 px para toque. Não altere valores financeiros ou arredondamento: reutilize `services/decimais.ts`, `ValorMonetario` e os formatadores existentes. Valores indisponíveis não são zero; valores parciais devem ser identificados.
