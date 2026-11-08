# Componentes fundamentais

Os componentes ficam em `frontend/src/` e usam elementos HTML nativos. A migração das telas é gradual; estilos legados permanecem para usos ainda não migrados.

## Button

`<Button variant="primary" type="submit">Salvar</Button>`. Use `variant="secondary"` para ações secundárias e `variant="icon"` para ações apenas com ícone. A variante padrão é `primary` e o tipo padrão é `button`. Passe `disabled` durante bloqueios e `aria-label` descritivo quando não houver texto. Os estilos existentes definem dimensões, foco, hover e estado desabilitado; não introduza tamanhos para resolver layout local.

## Input e Select

`<Input id="nome" value={nome} onChange={aoAlterar} />` e `<Select id="ativo" value={ativo} onChange={aoAlterar}>…</Select>` preservam propriedades e comportamento nativos. Ambos usam largura integral, altura mínima de 44 px, tipografia do sistema, borda, foco, desabilitação e indicação visual de `aria-invalid`. Input destina-se a campos textuais, numéricos e datas; checkboxes, rádios e uploads mantêm seus padrões próprios. Não há variantes ou tamanhos adicionais sem necessidade comprovada.

## FormField

```tsx
<FormField id="nome" label="Nome" hint="Como deseja identificar a carteira?" error={erro}>
  <Input id="nome" aria-invalid={!!erro} aria-describedby={erro ? "nome-hint nome-error" : "nome-hint"} />
</FormField>
```

O `id` do controle deve corresponder ao do campo. Associe os IDs `-hint` e `-error` presentes ao controle com `aria-describedby`; use `aria-invalid` quando inválido. A validação e o anúncio de erros dinâmicos pertencem ao formulário. Use `fieldset` e `legend` para grupos de controles.

## PageHeader

`<PageHeader id="page-title" eyebrow="CADASTROS" title="Carteiras" description="Organize suas contas." />` compõe o padrão existente: título `h1`, identificação opcional e descrição opcional. Referencie o ID em `aria-labelledby` da seção quando aplicável. Não coloque ações ou regras de negócio no cabeçalho sem necessidade real.

## Outros componentes

`Modal`, `Icone`, `AjudaCampo`, `ValorMonetario` e `ResultadoMonetario` continuam reutilizáveis. Filtros, tabelas, paginação e padrões específicos de domínio permanecem fora desta etapa. Valide alterações em teclado, leitores de tela e tamanhos de tela afetados; testes automatizados não substituem essa verificação.
