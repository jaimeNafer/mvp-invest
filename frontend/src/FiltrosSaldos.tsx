import type { Carteira } from "./services/carteiras";
import type { Instituicao } from "./services/instituicoes";
import type { Ativo, FiltrosSaldo } from "./services/saldosIniciais";
type Props = {
  instituicoes: Instituicao[];
  ativos: Ativo[];
  carteiras: Carteira[];
  filtros: FiltrosSaldo;
  onFiltrar: (f: Partial<FiltrosSaldo>) => void;
};
export default function FiltrosSaldos({
  instituicoes,
  ativos,
  carteiras,
  filtros,
  onFiltrar,
}: Props) {
  return (
    <>
      <div className="saldo-filtros">
        <label>
          Instituição
          <select
            aria-label="Filtrar instituição"
            value={filtros.instituicaoId}
            onChange={(e) => onFiltrar({ instituicaoId: e.target.value })}
          >
            <option value="">Todas</option>
            {instituicoes.map((i) => (
              <option key={i.id} value={i.id}>
                {i.nome}
              </option>
            ))}
          </select>
        </label>
        <label>
          Ativo
          <select
            aria-label="Filtrar ativo"
            value={filtros.ativoId}
            onChange={(e) => onFiltrar({ ativoId: e.target.value })}
          >
            <option value="">Todos</option>
            {ativos.map((a) => (
              <option key={a.id} value={a.id}>
                {a.ticker}
              </option>
            ))}
          </select>
        </label>
        <label>
          Situação
          <select
            aria-label="Filtrar situação"
            value={filtros.situacao}
            onChange={(e) => onFiltrar({ situacao: e.target.value })}
          >
            <option value="CONFIRMADA">Confirmados</option>
            <option value="TODAS">Todo o histórico</option>
            <option value="SUBSTITUIDA">Substituídos</option>
            <option value="CANCELADA">Cancelados</option>
          </select>
        </label>
      </div>
      {carteiras.length > 0 && (
        <fieldset className="saldo-carteiras">
          <legend>Carteiras</legend>
          <p className="field-hint">
            Sem seleção, todas as instituições. Uma instituição presente em várias carteiras é
            considerada uma vez.
          </p>
          {carteiras.map((c) => (
            <label key={c.id} className="check-label">
              <input
                type="checkbox"
                checked={filtros.carteiraIds.includes(c.id)}
                onChange={(e) =>
                  onFiltrar({
                    carteiraIds: e.target.checked
                      ? [...filtros.carteiraIds, c.id]
                      : filtros.carteiraIds.filter((id) => id !== c.id),
                  })
                }
              />
              {c.nome}
              {!c.ativo && " (arquivada)"}
            </label>
          ))}
        </fieldset>
      )}
    </>
  );
}
