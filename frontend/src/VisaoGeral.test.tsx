import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";

import { StrictMode } from "react";

import { afterEach, describe, expect, it, vi } from "vitest";

import Home from "./Home";

const usuario = { id: "1", login: "teste", name: "Teste" };

const dados = {
  revisao: "3",

  dataInicio: "2026-10-05",

  dataFim: "2026-10-06",

  dataReferencia: "2026-10-05",

  possuiRegistrosAteFim: true,

  dataSaldos: "2026-10-06",

  saldosMoedas: [
    { ativoId: "1", ativoTicker: "BRL", ativoNome: "Real", quantidade: "947", instituicoes: [] },
  ],

  evolucaoResultado: {
    acumuladoInicialBrl: "0",
    meses: [
      {
        dataInicio: "2026-10-05",
        dataFim: "2026-10-06",
        resultadoBrl: "48",
        acumuladoBrl: "48",
        operacoes: 1,
      },
    ],
  },

  resultadosPorAtivo: [
    {
      ativoId: "2",
      ativoTicker: "BTC",
      ativoNome: "Bitcoin",
      negociacaoBrl: "49",
      moedaBrl: "0",
      taxaBrl: "-1",
      resultadoTotalBrl: "48",
    },
  ],

  conferencia: { custoZeroDeclarado: 0, referenciasRepetidas: 0 },

  resumo: {
    custoSaldosBrl: "1048",

    resultadoRealizadoBrl: "48",

    taxasBrl: "1",

    aportesBrl: "0",

    operacoesNoPeriodo: 1,
  },

  resultado: { negociacaoBrl: "49", moedaBrl: "0", taxaBrl: "-1" },

  posicoes: [
    {
      ativoId: "2",

      ativoTicker: "BTC",

      ativoNome: "Bitcoin",

      quantidade: "1",

      custoMedioBrl: "101",

      custoTotalBrl: "101",

      participacaoPercentual: "9.6374",

      instituicoes: [
        { instituicaoId: "1", instituicaoNome: "Binance", quantidade: "0.6" },

        { instituicaoId: "2", instituicaoNome: "BitGet", quantidade: "0.4" },
      ],
    },
  ],
};

const resposta = (body: unknown) => {
  const response = new Response(JSON.stringify(body), { status: 200 });
  Object.defineProperty(response, "json", { value: async () => body });
  return response;
};

afterEach(() => {
  window.history.replaceState(null, "", "/");
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe("Visão geral", () => {
  it("apresenta resultado mensal e por ativo sem confundir com composição a custo", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(resposta(dados)));

    render(<Home usuario={usuario} onLogout={vi.fn()} />);

    expect(
      await screen.findByRole("heading", { name: "Resultado realizado acumulado" }),
    ).toBeVisible();

    expect(
      screen.getByRole("img", { name: "Evolução mensal do resultado realizado" }),
    ).toBeVisible();

    expect(screen.getByRole("heading", { name: "Resultado por ativo" })).toBeVisible();

    expect(screen.getByText("Saldos em moeda")).toBeVisible();

    expect(screen.getByText("947")).toBeVisible();

    expect(
      screen.getByText("Distribuição pelo custo de aquisição, sem cotação de mercado."),
    ).toBeVisible();
  });

  it("apresenta subtotais parciais sem duplicar o resumo do período", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        resposta({
          ...dados,
          disponibilidade: {
            custo: {
              valorConhecido: "900.50",
              status: "PARCIAL",
              componentesConhecidos: 2,
              componentesDesconhecidos: 1,
              motivo: "Um custo não foi identificado.",
            },
            resultado: {
              valorConhecido: "-12.25",
              status: "PARCIAL",
              componentesConhecidos: 3,
              componentesDesconhecidos: 2,
              motivo: null,
            },
            taxas: {
              valorConhecido: "4.10",
              status: "COMPLETO",
              componentesConhecidos: 2,
              componentesDesconhecidos: 0,
              motivo: null,
            },
            aportesLiquidos: {
              valorConhecido: null,
              status: "INDISPONIVEL",
              componentesConhecidos: 0,
              componentesDesconhecidos: 1,
              motivo: "Aportes não calculáveis.",
            },
          },
          posicoes: [
            { ...dados.posicoes[0], custoTotalBrl: "900.50", participacaoPercentual: "100" },
            {
              ...dados.posicoes[0],
              ativoId: "3",
              ativoTicker: "ETH",
              custoTotalBrl: null,
              participacaoPercentual: null,
            },
          ],
        }),
      ),
    );
    render(<Home usuario={usuario} onLogout={vi.fn()} />);
    expect((await screen.findAllByText("R$ 900,50")).length).toBeGreaterThan(0);
    expect(
      screen.getByText("1 componente desconhecido · Um custo não foi identificado."),
    ).toBeVisible();
    expect(screen.getByText("2 componentes desconhecidos")).toBeVisible();
    expect(screen.getByText("Aportes não calculáveis.")).toBeVisible();
    expect(screen.getByText(/Distribuição da parcela com custo identificado/)).toBeVisible();
    expect(screen.queryByRole("heading", { name: "Resultado do período" })).not.toBeInTheDocument();
    expect(screen.queryByText(/Totais incompletos/)).not.toBeInTheDocument();
  });

  it("atualiza visão geral e patrimônio quando o marcador global muda", async () => {
    vi.useFakeTimers();
    let marcadorConsultado = 0;
    const fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes("/conciliacao/cotacoes/atualizacao")) {
        marcadorConsultado += 1;
        return Promise.resolve(
          resposta({ atualizadaEm: marcadorConsultado === 1 ? null : "2026-10-07T12:00:00Z" }),
        );
      }
      if (url.includes("patrimonio-diario")) return Promise.resolve(resposta([]));
      return Promise.resolve(resposta(dados));
    });
    vi.stubGlobal("fetch", fetch);

    render(<Home usuario={usuario} onLogout={vi.fn()} />);

    await act(async () => {
      for (let indice = 0; indice < 12; indice += 1) await Promise.resolve();
    });

    expect(screen.getByText("R$ 1.048,00")).toBeVisible();
    const consultasPrincipais = () =>
      fetch.mock.calls.filter(
        ([url]) => String(url).includes("/visao-geral") && !String(url).includes("patrimonio"),
      ).length;
    expect(consultasPrincipais()).toBe(1);

    await act(async () => {
      await vi.advanceTimersByTimeAsync(30_000);
      for (let indice = 0; indice < 12; indice += 1) await Promise.resolve();
    });

    expect(
      screen.getByText("Novas cotações carregadas; os cálculos foram atualizados."),
    ).toBeVisible();
    expect(consultasPrincipais()).toBe(2);
    expect(
      fetch.mock.calls.filter(([url]) => String(url).includes("patrimonio-diario")).length,
    ).toBe(0);
    vi.useRealTimers();
  });

  it("consulta avisos somente ao abrir e não os apresenta como erros", async () => {
    const fetch = vi.fn().mockImplementation((url: string) =>
      Promise.resolve(
        resposta(
          url.includes("/avisos")
            ? {
                revisao: "3",
                pagina: 0,
                tamanho: 10,
                total: 1,

                itens: [
                  {
                    operacaoId: "7",
                    ocorridaEm: "2026-10-01T03:00:00Z",
                    instituicaoNome: "Binance",
                    ativoTicker: "BTC",
                    referencia: null,
                    justificativa: "Origem documentada",
                  },
                ],
              }
            : { ...dados, conferencia: { custoZeroDeclarado: 1, referenciasRepetidas: 0 } },
        ),
      ),
    );

    vi.stubGlobal("fetch", fetch);

    render(<Home usuario={usuario} onLogout={vi.fn()} />);

    await screen.findByText("R$ 1.048,00");

    expect(fetch.mock.calls.some(([url]) => String(url).includes("/avisos"))).toBe(false);

    fireEvent.click(
      screen.getByRole("button", { name: /1 saldo inicial com custo zero declarado/ }),
    );

    expect(await screen.findByText("Origem documentada")).toBeVisible();

    expect(screen.getByText(/não comprovam erros/)).toBeVisible();

    expect(fetch.mock.calls.find(([url]) => String(url).includes("/avisos"))?.[0]).toBe(
      "/api/v1/visao-geral/avisos?dataFim=2026-10-06&tipo=CUSTO_ZERO_DECLARADO&pagina=0&tamanho=10",
    );
  });

  it("solicita atualizar painel sem mostrar avisos de outra revisão", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation((url: string) =>
        Promise.resolve(
          resposta(
            url.includes("/avisos")
              ? {
                  revisao: "4",
                  pagina: 0,
                  tamanho: 10,
                  total: 1,

                  itens: [{ operacaoId: "7", justificativa: "Não deve aparecer" }],
                }
              : { ...dados, conferencia: { custoZeroDeclarado: 1, referenciasRepetidas: 0 } },
          ),
        ),
      ),
    );

    render(<Home usuario={usuario} onLogout={vi.fn()} />);

    await screen.findByText("R$ 1.048,00");

    fireEvent.click(
      screen.getByRole("button", { name: /1 saldo inicial com custo zero declarado/ }),
    );

    expect(await screen.findByRole("button", { name: "Atualizar painel" })).toBeVisible();

    expect(screen.queryByText("Não deve aparecer")).not.toBeInTheDocument();
  });

  it("ignora resposta de consulta cancelada sem substituir dados recentes", async () => {
    let concluirAntiga: ((resposta: Response) => void) | undefined;

    const antiga = new Promise<Response>((resolve) => {
      concluirAntiga = resolve;
    });

    const fetch = vi

      .fn()

      .mockReturnValueOnce(antiga)

      .mockImplementation(() => Promise.resolve(resposta(dados)));

    vi.stubGlobal("fetch", fetch);

    render(
      <StrictMode>
        <Home usuario={usuario} onLogout={vi.fn()} />
      </StrictMode>,
    );

    await screen.findByText("R$ 1.048,00");

    await act(async () =>
      concluirAntiga?.(resposta({ ...dados, resumo: { ...dados.resumo, custoSaldosBrl: "999" } })),
    );

    expect(screen.getByText("R$ 1.048,00")).toBeVisible();

    expect(screen.queryByText("R$ 999,00")).not.toBeInTheDocument();

    expect((fetch.mock.calls[0][1] as RequestInit).signal?.aborted).toBe(true);
  });

  it("orienta cadastro sem criar composição quando não há registros", async () => {
    vi.stubGlobal(
      "fetch",

      vi.fn((url: string) =>
        Promise.resolve(
          url.endsWith("/carteiras")
            ? resposta([])
            : resposta({
                ...dados,

                possuiRegistrosAteFim: false,

                posicoes: [],

                resumo: {
                  custoSaldosBrl: "0",

                  resultadoRealizadoBrl: "0",

                  taxasBrl: "0",

                  aportesBrl: "0",

                  operacoesNoPeriodo: 0,
                },
              }),
        ),
      ),
    );

    render(<Home usuario={usuario} onLogout={vi.fn()} />);

    expect(
      await screen.findByText(
        "Nenhum registro até a data final. Consulte outro período ou registre suas operações.",
      ),
    ).toBeVisible();

    expect(await screen.findByRole("button", { name: "Criar carteira" })).toBeEnabled();

    expect(screen.queryByRole("heading", { name: "Composição a custo" })).not.toBeInTheDocument();

    expect(screen.queryByText("R$ 0,00")).not.toBeInTheDocument();
  });

  it("preserva filtros após rejeição e informa cobertura anterior", async () => {
    const fetch = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      if (url.includes("patrimonio-diario")) return Promise.resolve(resposta([]));
      if ((init?.signal as AbortSignal | undefined)?.aborted)
        return Promise.resolve(resposta(dados));
      if (url.includes("dataInicio=2026-10-07"))
        return Promise.resolve(
          new Response(
            JSON.stringify({ detail: "A data inicial não pode ser posterior à data final." }),
            { status: 400 },
          ),
        );
      return Promise.resolve(resposta({ ...dados, dataInicio: "2026-10-01" }));
    });

    vi.stubGlobal("fetch", fetch);

    render(<Home usuario={usuario} onLogout={vi.fn()} />);

    expect(await screen.findByText("O histórico cadastrado começa em 05/10/2026.")).toBeVisible();

    fireEvent.change(screen.getByLabelText("De"), { target: { value: "2026-10-07" } });

    fireEvent.click(screen.getByRole("button", { name: "Aplicar" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "A data inicial não pode ser posterior à data final.",
    );

    expect(screen.getByLabelText("De")).toHaveValue("2026-10-07");

    expect(screen.getByLabelText("Até")).toHaveValue("2026-10-06");

    expect(screen.getByText("R$ 1.048,00")).toBeVisible();
  });

  it("abre após login e apresenta consolidação e custódia expansível", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(resposta(dados)));

    render(<Home usuario={usuario} onLogout={vi.fn()} />);

    expect(await screen.findByRole("heading", { name: "Visão geral" })).toBeVisible();

    expect(await screen.findByText("R$ 1.048,00")).toBeVisible();

    expect(screen.getByText("9,64%")).toHaveAttribute("title", "Percentual completo: 9,6374%");

    expect(
      screen.getByText("9,64%").closest("li")?.querySelector(".composition-track > div"),
    ).toHaveStyle({ width: "9.6374%" });

    expect(screen.getAllByText("Bitcoin").length).toBeGreaterThan(0);

    expect(screen.getByRole("heading", { name: "Composição a custo" })).toBeVisible();

    const custodia = screen.getByText("2 instituições");

    fireEvent.click(custodia);

    expect(screen.getByText("Binance")).toBeVisible();

    expect(screen.getByText("BitGet")).toBeVisible();

    fireEvent.click(screen.getByRole("button", { name: "Meu perfil" }));

    expect(screen.getByRole("heading", { name: "Meu perfil" })).toBeVisible();
  });

  it("aplica o período somente ao enviar os filtros", async () => {
    const patrimonio = [
      {
        data: "2026-10-05",
        valorTotalBrl: null,
        valorConhecidoBrl: "950",
        ativosComPreco: 1,
        ativosSemPreco: 1,
        quantidadesCompletas: true,
      },
      {
        data: "2026-10-06",
        valorTotalBrl: "1100.25",
        valorConhecidoBrl: "1100.25",
        ativosComPreco: 2,
        ativosSemPreco: 0,
        quantidadesCompletas: true,
      },
    ];
    const fetch = vi
      .fn()
      .mockImplementation((url: string) =>
        Promise.resolve(resposta(url.includes("patrimonio-diario") ? patrimonio : dados)),
      );

    vi.stubGlobal("fetch", fetch);

    render(<Home usuario={usuario} onLogout={vi.fn()} />);

    await screen.findByText("R$ 1.048,00");

    fireEvent.change(screen.getByLabelText("De"), { target: { value: "2026-10-01" } });

    expect(fetch).toHaveBeenCalledTimes(3);
    expect(screen.getByText("Evolução diária do patrimônio")).toBeVisible();
    expect(
      fetch.mock.calls.filter(([url]) => String(url).includes("patrimonio-diario")),
    ).toHaveLength(0);
    fireEvent.click(screen.getByText("Evolução diária do patrimônio"));
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(4));
    expect(fetch.mock.calls.find(([url]) => String(url).includes("patrimonio-diario"))?.[0]).toBe(
      "/api/v1/visao-geral/patrimonio-diario?dataInicio=2026-10-05&dataFim=2026-10-06",
    );
    expect(screen.getByText("Valor total indisponível")).toBeVisible();
    expect(screen.getByText("R$ 950,00")).toBeVisible();

    fireEvent.click(screen.getByRole("button", { name: "Aplicar" }));

    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(5));

    expect(
      fetch.mock.calls.find(
        ([url]) => String(url).includes("/visao-geral?") && !String(url).includes("patrimonio"),
      )?.[0],
    ).toBe("/api/v1/visao-geral?dataInicio=2026-10-01&dataFim=2026-10-06");
    expect(
      fetch.mock.calls.filter(([url]) => String(url).includes("patrimonio-diario")),
    ).toHaveLength(1);
  });

  it("limita a consulta patrimonial a um ano e permite tentar novamente após falha", async () => {
    const dadosLongos = { ...dados, dataInicio: "2020-01-01", dataFim: "2026-10-06" };
    const fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes("patrimonio-diario"))
        return fetch.mock.calls.filter(([chamada]) => String(chamada).includes("patrimonio-diario"))
          .length === 1
          ? Promise.resolve(
              new Response(JSON.stringify({ detail: "Falha ao consultar" }), { status: 503 }),
            )
          : Promise.resolve(resposta([]));
      if (url.includes("/conciliacao/cotacoes/atualizacao"))
        return Promise.resolve(resposta({ atualizadaEm: null }));
      return Promise.resolve(resposta(dadosLongos));
    });
    vi.stubGlobal("fetch", fetch);
    render(<Home usuario={usuario} onLogout={vi.fn()} />);
    await screen.findByText("R$ 1.048,00");
    fireEvent.click(screen.getByText("Evolução diária do patrimônio"));
    expect(await screen.findByRole("alert")).toHaveTextContent("Falha ao consultar");
    expect(fetch.mock.calls.find(([url]) => String(url).includes("patrimonio-diario"))?.[0]).toBe(
      "/api/v1/visao-geral/patrimonio-diario?dataInicio=2025-10-06&dataFim=2026-10-06",
    );
    fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));
    await waitFor(() =>
      expect(
        fetch.mock.calls.filter(([url]) => String(url).includes("patrimonio-diario")),
      ).toHaveLength(2),
    );
  });

  it("exibe erro com nova tentativa sem fabricar indicadores", async () => {
    let falharResumo = true;
    const fetch = vi.fn().mockImplementation((url: string) => {
      if (url.endsWith("/carteiras")) return Promise.resolve(resposta([]));
      if (url.includes("/conciliacao/cotacoes/atualizacao"))
        return Promise.resolve(resposta({ atualizadaEm: null }));
      if (falharResumo) {
        falharResumo = false;
        return Promise.reject(new Error("offline"));
      }
      return Promise.resolve(resposta(dados));
    });

    vi.stubGlobal("fetch", fetch);

    render(<Home usuario={usuario} onLogout={vi.fn()} />);

    expect(await screen.findByRole("alert")).toHaveTextContent("Não foi possível conectar");

    expect(screen.queryByText("R$ 0,00")).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));

    expect(await screen.findByText("R$ 1.048,00")).toBeVisible();
  });

  it("distingue período sem movimentos de ausência de registros", async () => {
    vi.stubGlobal(
      "fetch",

      vi

        .fn()

        .mockResolvedValue(
          resposta({ ...dados, resumo: { ...dados.resumo, operacoesNoPeriodo: 0 } }),
        ),
    );

    render(<Home usuario={usuario} onLogout={vi.fn()} />);

    expect(
      await screen.findByText(
        "Nenhuma operação no período. Os saldos incluem os registros anteriores.",
      ),
    ).toBeVisible();

    expect(screen.getByText("R$ 1.048,00")).toBeVisible();
  });

  it("não inventa composição percentual para custo zero e mantém quantidade visível", async () => {
    vi.stubGlobal(
      "fetch",

      vi.fn().mockResolvedValue(
        resposta({
          ...dados,

          resumo: { ...dados.resumo, custoSaldosBrl: "0" },

          posicoes: [
            {
              ...dados.posicoes[0],

              custoTotalBrl: "0",

              custoMedioBrl: "0",

              participacaoPercentual: null,
            },
          ],
        }),
      ),
    );

    render(<Home usuario={usuario} onLogout={vi.fn()} />);

    expect(
      await screen.findByText("Não há custo positivo para distribuir entre os ativos."),
    ).toBeVisible();

    expect(
      within(screen.getByRole("table", { name: "Saldos e custos" })).getByText("BTC"),
    ).toBeVisible();
  });
});
