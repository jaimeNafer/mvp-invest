# Design System — Regras de desenvolvimento

Todo desenvolvimento de interface deve consultar `docs/design-system/`, reutilizar componentes existentes, usar tokens semânticos disponíveis, respeitar os padrões de página, manter consistência, evitar estilos locais arbitrários e duplicação, preservar acessibilidade, validar responsividade e documentar exceções justificadas.

É proibido criar um novo padrão visual quando já existir um padrão equivalente no Design System. Se um componente não atender à necessidade, avalie composição, depois extensão de variante; justifique novos componentes, atualize a documentação e crie testes correspondentes. Não crie variantes apenas para resolver problemas locais de layout. Consulte os acordos do `AGENTS.md` da raiz.

Para botões, campos textuais, selects, composição de rótulos e cabeçalhos de página, use `Button`, `Input`, `Select`, `FormField` e `PageHeader` conforme `docs/design-system/components.md`. Preserve controles nativos e associações acessíveis; não crie substitutos locais equivalentes. Migre usos legados apenas no escopo da tarefa.
