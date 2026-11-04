import type { CamposOperacao as Campos } from "./services/cadastroOperacao";
import type { Instituicao } from "./services/instituicoes";
import type { TipoNegociacao } from "./services/operacoes";
import type { Ativo } from "./services/saldosIniciais";

type Props = {
  campos: Campos;
  ativos: Ativo[];
  instituicoes: Instituicao[];
  cancelar: boolean;
  corrigindo: boolean;
  bloqueado: boolean;
  alterar: (campos: Partial<Campos>) => void;
};
export default function CamposOperacao({
  campos,
  ativos,
  instituicoes,
  cancelar,
  corrigindo,
  bloqueado,
  alterar,
}: Props) {
  const brl = ativos.find((a) => a.codigo === "fiat:BRL");
  const bases = ativos.filter((a) => a.tipo === "CRIPTO");
  const cotacoes = ativos.filter(
    (a) => ["fiat:BRL", "cripto:USDT", "cripto:USDC"].includes(a.codigo) && a.id !== campos.ativoId,
  );
  const moedasTaxa = ativos.filter(
    (a) => a.id === campos.ativoId || a.id === campos.moedaCotacaoId,
  );

  return (
    <fieldset className="saldo-campos" disabled={bloqueado}>
      {!cancelar && (
        <>
          <label htmlFor="neg-tipo">Tipo de operação</label>
          <select
            id="neg-tipo"
            value={campos.tipo}
            onChange={(e) =>
              alterar({
                tipo: e.target.value as TipoNegociacao,
                taxaAtivoId: "",
                taxaQuantidade: "",
              })
            }
          >
            <option value="APORTE">Aporte em reais</option>
            <option value="COMPRA">Compra</option>
            <option value="VENDA">Venda</option>
          </select>
          <label htmlFor="neg-instituicao">Instituição</label>
          <select
            id="neg-instituicao"
            required
            value={campos.instituicaoId}
            onChange={(e) => alterar({ instituicaoId: e.target.value })}
          >
            <option value="">Selecione</option>
            {instituicoes.map((i) => (
              <option key={i.id} value={i.id} disabled={!i.ativo}>
                {i.nome}
                {!i.ativo ? " (inativa)" : ""}
              </option>
            ))}
          </select>
          {campos.tipo === "APORTE" ? (
            <p>Ativo: BRL — Real</p>
          ) : (
            <>
              <label htmlFor="neg-ativo">Ativo</label>
              <select
                id="neg-ativo"
                required
                value={campos.ativoId}
                onChange={(e) =>
                  alterar({
                    ativoId: e.target.value,
                    moedaCotacaoId: "",
                    taxaAtivoId: "",
                    taxaQuantidade: "",
                  })
                }
              >
                <option value="">Selecione</option>
                {bases.map((a) => (
                  <option key={a.id} value={a.id} disabled={!a.ativo}>
                    {a.ticker}
                    {!a.ativo ? " (inativo)" : ""}
                  </option>
                ))}
              </select>
            </>
          )}
          <label htmlFor="neg-data">Data e hora (Fortaleza, UTC−03:00)</label>
          <input
            id="neg-data"
            type="datetime-local"
            step="1"
            required
            value={campos.data}
            onChange={(e) => alterar({ data: e.target.value })}
          />
          <label htmlFor="neg-quantidade">Quantidade</label>
          <input
            id="neg-quantidade"
            inputMode="decimal"
            autoComplete="off"
            required
            value={campos.quantidade}
            onChange={(e) => alterar({ quantidade: e.target.value })}
          />
          {campos.tipo !== "APORTE" && (
            <>
              <label htmlFor="neg-moeda">Moeda de cotação</label>
              <select
                id="neg-moeda"
                required
                value={campos.moedaCotacaoId}
                onChange={(e) =>
                  alterar({ moedaCotacaoId: e.target.value, taxaAtivoId: "", taxaQuantidade: "" })
                }
              >
                <option value="">Selecione</option>
                {cotacoes.map((a) => (
                  <option key={a.id} value={a.id} disabled={!a.ativo}>
                    {a.ticker}
                    {!a.ativo ? " (inativa)" : ""}
                  </option>
                ))}
              </select>
              <label htmlFor="neg-total">Total bruto</label>
              <input
                id="neg-total"
                inputMode="decimal"
                autoComplete="off"
                required
                value={campos.totalBruto}
                onChange={(e) => alterar({ totalBruto: e.target.value })}
              />
              {campos.moedaCotacaoId === brl?.id ? (
                <p className="field-hint">Cotação BRL: R$ 1.</p>
              ) : (
                <>
                  <label htmlFor="neg-cotacao">Cotação histórica em reais por unidade</label>
                  <input
                    id="neg-cotacao"
                    inputMode="decimal"
                    autoComplete="off"
                    required
                    value={campos.cotacaoBrl}
                    onChange={(e) => alterar({ cotacaoBrl: e.target.value })}
                  />
                </>
              )}
              <label htmlFor="neg-taxa-ativo">Moeda da taxa</label>
              <select
                id="neg-taxa-ativo"
                value={campos.taxaAtivoId}
                onChange={(e) => alterar({ taxaAtivoId: e.target.value, taxaQuantidade: "" })}
              >
                <option value="">Sem taxa</option>
                {moedasTaxa.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.ticker}
                  </option>
                ))}
              </select>
              {campos.taxaAtivoId && (
                <>
                  <label htmlFor="neg-taxa">Quantidade da taxa</label>
                  <input
                    id="neg-taxa"
                    inputMode="decimal"
                    autoComplete="off"
                    required
                    value={campos.taxaQuantidade}
                    onChange={(e) => alterar({ taxaQuantidade: e.target.value })}
                  />
                </>
              )}
            </>
          )}
          <label htmlFor="neg-referencia">Referência (opcional)</label>
          <input
            id="neg-referencia"
            maxLength={300}
            value={campos.referencia}
            onChange={(e) => alterar({ referencia: e.target.value })}
          />
        </>
      )}
      {(corrigindo || cancelar) && (
        <>
          <label htmlFor="neg-motivo">
            {cancelar ? "Motivo do cancelamento" : "Motivo da correção"}
          </label>
          <textarea
            id="neg-motivo"
            required
            maxLength={300}
            value={campos.motivo}
            onChange={(e) => alterar({ motivo: e.target.value })}
          />
        </>
      )}
    </fieldset>
  );
}
