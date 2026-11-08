import { useId, useState } from "react";
import { moeda } from "./services/decimais";
import type { PontoPatrimonioDiario } from "./services/visaoGeral";

function dataLegivel(data: string) {
  return data.split("-").reverse().join("/");
}

export default function PatrimonioDiario({
  pontos,
  carregando = false,
  erro = "",
  carregado = false,
  onAbrir,
  onTentarNovamente,
}: {
  pontos: PontoPatrimonioDiario[];
  carregando?: boolean;
  erro?: string;
  carregado?: boolean;
  onAbrir?: () => void;
  onTentarNovamente?: () => void;
}) {
  const tituloId = useId();
  const [pagina, setPagina] = useState(0);

  const tamanhoPagina = 15;
  const ultimaPagina = Math.max(0, Math.ceil(pontos.length / tamanhoPagina) - 1);
  const paginaAtual = Math.min(pagina, ultimaPagina);
  const exibidos = pontos
    .slice()
    .reverse()
    .slice(paginaAtual * tamanhoPagina, (paginaAtual + 1) * tamanhoPagina);

  return (
    <section className="overview-panel overview-patrimony" aria-labelledby={tituloId}>
      <details
        onToggle={(evento) => {
          if (evento.currentTarget.open) onAbrir?.();
        }}
      >
        <summary id={tituloId}>Evolução diária do patrimônio</summary>
        <p className="overview-note">
          Valores estimados pelos preços disponíveis em cada data. Ativos sem preço não são tratados
          como zero.
        </p>
        <p className="overview-note">
          A consulta cobre até 12 meses por vez. Ajuste as datas da visão geral para consultar outro
          intervalo.
        </p>
        {carregando && (
          <p role="status" className="overview-note">
            Consultando a evolução do patrimônio…
          </p>
        )}
        {erro && (
          <div className="overview-error">
            <p role="alert">{erro}</p>
            <button type="button" className="secondary-button" onClick={onTentarNovamente}>
              Tentar novamente
            </button>
          </div>
        )}
        {carregado && pontos.length === 0 && !erro && (
          <p className="overview-note">Não há dados patrimoniais neste período.</p>
        )}
        {pontos.length > 0 && (
          <>
            <div className="table-scroll">
              <table aria-label="Série diária do patrimônio">
                <thead>
                  <tr>
                    <th>Data</th>
                    <th>Valor total</th>
                    <th>Valor conhecido</th>
                    <th>Cobertura</th>
                  </tr>
                </thead>
                <tbody>
                  {exibidos.map((ponto) => (
                    <tr key={ponto.data}>
                      <td>{dataLegivel(ponto.data)}</td>
                      <td>
                        {ponto.valorTotalBrl === null ? (
                          <span>Valor total indisponível</span>
                        ) : (
                          moeda(ponto.valorTotalBrl)
                        )}
                      </td>
                      <td>{moeda(ponto.valorConhecidoBrl)}</td>
                      <td>
                        {ponto.ativosSemPreco === 0 ? (
                          <span>{ponto.ativosComPreco} ativos com preço</span>
                        ) : (
                          <span>
                            {ponto.ativosComPreco} com preço · {ponto.ativosSemPreco} sem preço
                          </span>
                        )}
                        {!ponto.quantidadesCompletas && <small> Quantidades incompletas</small>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {ultimaPagina > 0 && (
              <nav className="patrimony-pagination" aria-label="Paginação do patrimônio diário">
                <button
                  type="button"
                  className="secondary-button"
                  disabled={paginaAtual === 0}
                  onClick={() => setPagina(paginaAtual - 1)}
                >
                  Mais recentes
                </button>
                <span>
                  Página {paginaAtual + 1} de {ultimaPagina + 1}
                </span>
                <button
                  type="button"
                  className="secondary-button"
                  disabled={paginaAtual === ultimaPagina}
                  onClick={() => setPagina(paginaAtual + 1)}
                >
                  Mais antigas
                </button>
              </nav>
            )}
          </>
        )}
      </details>
    </section>
  );
}
