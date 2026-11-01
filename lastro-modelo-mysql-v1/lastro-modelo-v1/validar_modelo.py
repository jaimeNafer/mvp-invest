"""Validacao estrutural auxiliar. NAO substitui executar em MySQL/Workbench.
Traduz apenas os tipos/DDL para SQLite em memoria para exercitar FKs e CHECKs.
SQLite nao valida a precisao DECIMAL do MySQL; o alvo continua MySQL 8.0.16+.
"""
from pathlib import Path
import sqlite3,re,json,decimal
P=Path(__file__).resolve().parent
meta=json.loads((P/'modelo_meta.json').read_text())
byname={t['name']:t for t in meta}
for t in meta:
    assert len(t['name'])<=64
    colmap={c[0]:c[1] for c in t['cols']}
    for cols,parent,refs in t['fks']:
        p=byname[parent]; pc={c[0]:c[1] for c in p['cols']}
        unique=[tuple(k[1]) for k in p['keys'] if k[0] in ('UNIQUE','PRIMARY')]
        if 'id' in pc:unique.append(('id',))
        assert tuple(refs) in unique,(t['name'],parent,'FK deve apontar chave unica explicita')
        for childcol,parentcol in zip(cols,refs):
            assert childcol in colmap and parentcol in pc
            typ=lambda x:re.sub(r'\s+(NOT NULL|NULL|AUTO_INCREMENT).*','',x)
            assert typ(colmap[childcol])==typ(pc[parentcol]),(t['name'],childcol,'tipos FK diferentes')
        assert parent in byname
sql=(P/'01_schema_lastro.sql').read_text()
assert sql.count('CREATE TABLE ')==len(meta)
assert not re.search(r'\bDROP\s+(TABLE|SCHEMA|DATABASE)\b',sql,re.I)
assert 'FOREIGN_KEY_CHECKS' not in sql
conn=sqlite3.connect(':memory:');conn.execute('PRAGMA foreign_keys=ON')
conn.create_function('DAYOFMONTH',1,lambda s:int(s[8:10]) if s else None)
conn.create_function('REPEAT',2,lambda s,n:s*n)
conn.create_function('UNHEX',1,lambda s:bytes.fromhex(s))
for t in meta:
    lines=[]
    for name,typ,desc in t['cols']:
        options=None
        m=re.match(r'ENUM\((.*?)\)(.*)',typ)
        if m:options=m[1];typ='TEXT'+m[2]
        typ=re.sub(r'\bUNSIGNED\b|\bAUTO_INCREMENT\b|COLLATE utf8mb4_bin','',typ)
        typ=typ.replace('CURRENT_TIMESTAMP(6)','CURRENT_TIMESTAMP')
        typ=re.sub(r'\s+ON UPDATE CURRENT_TIMESTAMP','',typ)
        typ=re.sub(r'\b(?:TINYINT|BIGINT)\b','INTEGER',typ)
        typ=re.sub(r'\bDATETIME\(6\)','TEXT',typ)
        typ=re.sub(r'\b(?:VARBINARY|BINARY)\(\d+\)','BLOB',typ)
        typ=typ.replace('JSON','TEXT')
        lines.append('`'+name+'` '+typ)
        if options:lines.append(f'CHECK (`{name}` IN ({options}))')
    if any(c[0]=='id' for c in t['cols']):lines.append('PRIMARY KEY (id)')
    for kind,cols in t['keys']:
        if kind in ('PRIMARY','UNIQUE'):lines.append(('PRIMARY KEY' if kind=='PRIMARY' else 'UNIQUE')+'('+','.join(cols)+')')
    for cols,parent,refs in t['fks']:lines.append('FOREIGN KEY ('+','.join(cols)+') REFERENCES '+parent+'('+','.join(refs)+') ON DELETE RESTRICT ON UPDATE RESTRICT')
    lines+=['CHECK ('+x+')' for x in t['checks']]
    # SQLite requires column definitions before table constraints.
    columns=[x for x in lines if x.startswith('`')];constraints=[x for x in lines if not x.startswith('`')]
    conn.execute('CREATE TABLE '+t['name']+'('+','.join(columns+constraints)+')')
fixture=(P/'02_exemplo_ficticio.sql').read_text()
fixture=re.sub(r'^--.*$','',fixture,flags=re.M)
fixture=re.sub(r'^\s*(?:USE .*?;|SET .*?;)','',fixture,flags=re.M)
fixture=fixture.replace('START TRANSACTION;','BEGIN;')
conn.executescript(fixture)
assert conn.execute('PRAGMA foreign_key_check').fetchall()==[]
assert conn.execute('SELECT resultado_total_brl FROM resultados_operacoes WHERE operacao_id=3').fetchone()[0]==191
assert conn.execute('SELECT custo_total_brl FROM posicoes_calculadas WHERE ativo_id=2').fetchone()[0]==2406
assert abs(conn.execute('SELECT quantidade FROM saldos_contas WHERE ativo_id=2').fetchone()[0]-.006)<1e-12
# All negative assertions run in savepoints; no changed fixtures are retained.
def rejected(sql,args=()):
    conn.execute('SAVEPOINT invalid_case')
    try:conn.execute(sql,args)
    except sqlite3.IntegrityError:pass
    else:raise AssertionError('Deveria rejeitar: '+sql)
    finally:conn.execute('ROLLBACK TO invalid_case');conn.execute('RELEASE invalid_case')
rejected('UPDATE movimentos SET conta_id=2 WHERE id=2') # conta de outro titular
rejected('UPDATE movimentos SET quantidade=-1 WHERE id=2')
rejected("UPDATE movimentos SET direcao='ENTRADA' WHERE id=4") # taxa nao e credito
rejected('UPDATE operacoes SET preco_unitario=0 WHERE id=2')
rejected('UPDATE operacoes SET idempotencia=(SELECT idempotencia FROM operacoes WHERE id=2) WHERE id=3')
rejected('UPDATE resultados_operacoes SET resultado_total_brl=192 WHERE operacao_id=3')
rejected('DELETE FROM titulares WHERE id=1')
conn.executescript("INSERT INTO espacos(id,nome,criado_por) VALUES(2,'Outro espaco',1);INSERT INTO membros_espaco(espaco_id,usuario_id,papel) VALUES(2,1,'ADMIN');INSERT INTO titulares(id,espaco_id,nome) VALUES(3,2,'Outro titular');INSERT INTO contas(id,espaco_id,titular_id,nome,tipo) VALUES(4,2,3,'Outra conta','CARTEIRA');")
rejected('UPDATE movimentos SET conta_id=4 WHERE id=2') # outro espaco
# A decimal policy example uses Decimal, never binary float, for financial arithmetic.
D=decimal.Decimal
assert (D('4000')+D('10'))/D('.01')==D('401000')
assert D('1800')-D('5')-D('.004')*D('401000')==D('191')
print(f'PASS: {len(meta)} tabelas; FKs de chaves unicas e tipos; fixture; isolamento de espaco/titular; idempotencia; CHECKs; RESTRICT; calculo ilustrativo Decimal.')
print('Limite: DDL nativo ainda precisa ser executado em MySQL 8.0.16+/8.4 e importado no Workbench.')
