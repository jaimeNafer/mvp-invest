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
            {pontos.length > 1 && <GraficoPatrimonio pontos={pontos} />}
            <div className="table-scroll">
              <table aria-label="Série diária do patrimônio">
                <thead>
                  <tr>
                    <th>Data</th>
                    <th>Valor total</th>
                    <th>Valor conhecido</th>
                    <th>Aportes líquidos acumulados</th>
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
                      <td>
                        {ponto.valorTotalBrl === null && ponto.ativosComPreco === 0
                          ? "Indisponível"
                          : moeda(ponto.valorConhecidoBrl)}
                      </td>
                      <td>
                        {ponto.aportesLiquidosBrl === undefined ? (
                          "—"
                        ) : ponto.aportesLiquidosBrl === null ? (
                          ponto.aportesLiquidosConhecidosBrl == null ? (
                            "Indisponível"
                          ) : (
                            <>{moeda(ponto.aportesLiquidosConhecidosBrl)} conhecidos</>
                          )
                        ) : (
                          moeda(ponto.aportesLiquidosBrl)
                        )}
                      </td>
                      <td>
                        {ponto.ativosSemPreco === 0 ? (
                          <span>{ponto.ativosComPreco} ativos com preço</span>
                        ) : (
                          <span>
                            {ponto.ativosComPreco} com preço · {ponto.ativosSemPreco} sem preço
                          </span>
                        )}
                        {!ponto.quantidadesCompletas && <small> Quantidades incompletas</small>}
                        {ponto.aportesLiquidosBrl === null && (
                          <small>Aportes líquidos incompletos</small>
                        )}
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

function GraficoPatrimonio({ pontos }: { pontos: PontoPatrimonioDiario[] }) {
  const valoresPatrimonio = pontos.map((ponto) => ponto.valorTotalBrl);
  const valoresAportes = pontos.map((ponto) => ponto.aportesLiquidosBrl ?? null);
  const textos = [...valoresPatrimonio, ...valoresAportes].filter(
    (valor): valor is string => valor !== null,
  );
  if (textos.length === 0) return null;
  const casas = Math.max(...textos.map((valor) => valor.split(".")[1]?.length ?? 0));
  const unidades = (valor: string) => {
    const negativo = valor.startsWith("-");
    const [inteira, fracao = ""] = (negativo ? valor.slice(1) : valor).split(".");
    const magnitude = BigInt(inteira + fracao.padEnd(casas, "0"));
    return negativo ? -magnitude : magnitude;
  };
  const todos = textos.map(unidades);
  let minimo = todos[0];
  let maximo = todos[0];
  for (const valor of todos.slice(1)) {
    if (valor < minimo) minimo = valor;
    if (valor > maximo) maximo = valor;
  }
  const amplitude = maximo - minimo || 1n;
  const x = (indice: number) => 10 + (indice / (pontos.length - 1)) * 620;
  const y = (valor: string) =>
    150 - Number(((unidades(valor) - minimo) * 130_000n) / amplitude) / 1_000;
  const caminho = (serie: (string | null)[]) => {
    let iniciado = false;
    return serie
      .map((valor, indice) => {
        if (valor === null) {
          iniciado = false;
          return "";
        }
        const comando = iniciado ? "L" : "M";
        iniciado = true;
        return `${comando}${x(indice)},${y(valor)}`;
      })
      .filter(Boolean)
      .join(" ");
  };
  const temAportes = valoresAportes.some((valor) => valor !== null);
  return (
    <div className="patrimony-chart">
      <svg
        viewBox="0 0 640 164"
        preserveAspectRatio="none"
        role="img"
        aria-label="Evolução diária do patrimônio e aportes líquidos"
      >
        <title>Evolução diária do patrimônio e aportes líquidos</title>
        <desc>
          Os trechos sem valor completo aparecem como lacunas. Consulte os valores na tabela.
        </desc>
        <path
          d={caminho(valoresPatrimonio)}
          className="chart-patrimony"
          aria-label="Patrimônio estimado"
        />
        {temAportes && (
          <path
            d={caminho(valoresAportes)}
            className="chart-contributions"
            aria-label="Aportes líquidos acumulados"
          />
        )}
      </svg>
      <div className="patrimony-chart-legend">
        <span>Patrimônio estimado</span>
        {temAportes && <span>Aportes líquidos acumulados</span>}
      </div>
      {valoresPatrimonio.some((valor) => valor === null) && (
        <p className="overview-note">Os trechos sem valor completo aparecem como lacunas.</p>
      )}
    </div>
  );
}
