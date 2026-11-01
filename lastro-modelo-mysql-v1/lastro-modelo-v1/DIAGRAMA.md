# Relacionamentos — Lastro

O SQL é a fonte do modelo físico. Estes diagramas resumem relações; os campos e FKs compostas estão no DDL e no dicionário. Todas as FKs usam RESTRICT.

## Acesso e titularidade

```mermaid
erDiagram
    USUARIOS ||--o{ MEMBROS_ESPACO : participa
    ESPACOS ||--o{ MEMBROS_ESPACO : autoriza
    ESPACOS ||--o{ TITULARES : organiza
    TITULARES ||--o{ CONTAS : possui
    INSTITUICOES o|--o{ CONTAS : identifica
```

## Operação e origem

```mermaid
erDiagram
    ARQUIVOS ||--o{ IMPORTACOES : documenta
    IMPORTACOES ||--o{ LINHAS_IMPORTACAO : preserva
    LINHAS_IMPORTACAO o|--o{ OPERACOES : origina
    OPERACOES ||--o{ MOVIMENTOS : contem
    CONTAS ||--o{ MOVIMENTOS : custodia
    ATIVOS ||--o{ MOVIMENTOS : identifica
    OPERACOES ||--o| PAGAMENTOS_IMPOSTO : registra
    OPERACOES o|--o| OPERACOES : substitui
```

O mínimo de pernas e sua combinação são validados pelo backend ao confirmar. Operação manual não tem linha de importação; imposto externo não tem movimento de carteira. Os documentos de imposto e conciliação também podem apontar para `arquivos`.

## Projeções e conferência

```mermaid
erDiagram
    ESPACOS ||--o{ EXECUCOES_CALCULO : versiona
    EXECUCOES_CALCULO ||--o{ RESULTADOS_OPERACOES : produz
    OPERACOES ||--o{ RESULTADOS_OPERACOES : rastreia
    RESULTADOS_OPERACOES ||--o{ RESULTADOS_MOVIMENTOS : detalha
    MOVIMENTOS ||--o{ RESULTADOS_MOVIMENTOS : explica
    EXECUCOES_CALCULO ||--o{ POSICOES_CALCULADAS : consolida
    EXECUCOES_CALCULO ||--o{ SALDOS_CONTAS : distribui
    CONTAS ||--o{ CONCILIACOES_SALDO : confere
```

`conciliacoes_saldo` guarda o valor informado do extrato. Diferença e situação são calculadas contra um snapshot da mesma data e revisão, sem FK para um resultado antigo congelado. A auditoria do espaço registra alterações; não é mostrada como FK polimórfica para cada entidade.

## Caminho da informação

**Operação original → movimentos → execução versionada → resultado/posição → relatório e conferência.**

A interface pode continuar simples. A estrutura interna guarda o detalhe necessário para entender resultados e organizar os dados fiscais sem recadastro mensal.
