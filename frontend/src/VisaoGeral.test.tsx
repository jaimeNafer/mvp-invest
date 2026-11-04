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
const resposta = (body: unknown) => new Response(JSON.stringify(body), { status: 200 });
afterEach(() => vi.unstubAllGlobals());

describe("Visão geral", () => {
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
      vi.fn().mockResolvedValue(
        resposta({
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
    );
    render(<Home usuario={usuario} onLogout={vi.fn()} />);
    expect(
      await screen.findByText(
        "Nenhum registro até a data final. Consulte outro período ou registre suas operações.",
      ),
    ).toBeVisible();
    expect(screen.getByRole("button", { name: "Ir para operações" })).toBeEnabled();
    expect(screen.queryByRole("heading", { name: "Composição a custo" })).not.toBeInTheDocument();
    expect(screen.queryByText("R$ 0,00")).not.toBeInTheDocument();
  });

  it("preserva filtros após rejeição e informa cobertura anterior", async () => {
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(resposta({ ...dados, dataInicio: "2026-10-01" }))
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({ detail: "A data inicial não pode ser posterior à data final." }),
          { status: 400 },
        ),
      );
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
    expect(screen.queryByText("R$ 1.048,00")).not.toBeInTheDocument();
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
    expect(screen.getByText("Bitcoin")).toBeVisible();
    expect(screen.getByRole("heading", { name: "Composição a custo" })).toBeVisible();
    const custodia = screen.getByText("2 instituições");
    fireEvent.click(custodia);
    expect(screen.getByText("Binance")).toBeVisible();
    expect(screen.getByText("BitGet")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Meu perfil" }));
    expect(screen.getByRole("heading", { name: "Olá, Teste." })).toBeVisible();
  });

  it("aplica o período somente ao enviar os filtros", async () => {
    const fetch = vi.fn().mockImplementation(() => Promise.resolve(resposta(dados)));
    vi.stubGlobal("fetch", fetch);
    render(<Home usuario={usuario} onLogout={vi.fn()} />);
    await screen.findByText("R$ 1.048,00");
    fireEvent.change(screen.getByLabelText("De"), { target: { value: "2026-10-01" } });
    expect(fetch).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByRole("button", { name: "Aplicar" }));
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(2));
    expect(fetch.mock.calls[1][0]).toBe(
      "/api/v1/visao-geral?dataInicio=2026-10-01&dataFim=2026-10-06",
    );
  });

  it("exibe erro com nova tentativa sem fabricar indicadores", async () => {
    const fetch = vi
      .fn()
      .mockRejectedValueOnce(new Error("offline"))
      .mockImplementation(() => Promise.resolve(resposta(dados)));
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
    expect(within(screen.getByRole("table")).getByText("BTC")).toBeVisible();
  });
});
