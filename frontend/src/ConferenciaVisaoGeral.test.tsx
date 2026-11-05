import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import ConferenciaVisaoGeral from "./ConferenciaVisaoGeral";

const resposta = (body: unknown) => new Response(JSON.stringify(body), { status: 200 });
const item = {
  operacaoId: "7",
  ocorridaEm: "2026-10-01T03:00:00Z",
  instituicaoNome: "Binance",
  ativoTicker: "BTC",
  referencia: "ordem",
  justificativa: null,
};
const lista = { revisao: "3", pagina: 0, tamanho: 10, total: 11, itens: [item] };
const props = {
  conferencia: { custoZeroDeclarado: 0, referenciasRepetidas: 11 },
  dataFim: "2026-10-06",
  revisao: "3",
  onAtualizar: vi.fn(),
  onOperacoes: vi.fn(),
};

afterEach(() => vi.unstubAllGlobals());

describe("Conferência da visão geral", () => {
  it("libera a lista ao voltar enquanto o detalhe está pendente e ignora sua resposta tardia", async () => {
    let resolver: (resposta: Response) => void = () => {};
    const pendente = new Promise<Response>((resolve) => {
      resolver = resolve;
    });
    const fetch = vi
      .fn()
      .mockImplementation((url: string) =>
        url.includes("/operacoes/") ? pendente : Promise.resolve(resposta(lista)),
      );
    vi.stubGlobal("fetch", fetch);
    render(<ConferenciaVisaoGeral {...props} />);
    fireEvent.click(screen.getByRole("button", { name: /11 registros com referência repetida/ }));
    fireEvent.click(await screen.findByRole("button", { name: "Ver registro #7" }));
    expect(await screen.findByRole("status")).toHaveTextContent("Consultando registros");
    fireEvent.click(screen.getByRole("button", { name: "Voltar aos avisos" }));
    await waitFor(() => expect(screen.queryByRole("status")).not.toBeInTheDocument());
    expect(screen.getByRole("button", { name: "Próxima" })).toBeEnabled();
    expect(fetch.mock.calls[1][1].signal.aborted).toBe(true);
    resolver(resposta({ id: "7", ativoTicker: "Não deve aparecer" }));
    expect(screen.queryByText("Não deve aparecer")).not.toBeInTheDocument();
  });

  it("preserva paginação na falha e permite tentar novamente", async () => {
    let falhar = true;
    const fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes("pagina=1")) {
        if (falhar) {
          falhar = false;
          return Promise.reject(new Error("offline"));
        }
        return Promise.resolve(
          resposta({ ...lista, pagina: 1, itens: [{ ...item, operacaoId: "8" }] }),
        );
      }
      return Promise.resolve(resposta(lista));
    });
    vi.stubGlobal("fetch", fetch);
    render(<ConferenciaVisaoGeral {...props} />);
    fireEvent.click(screen.getByRole("button", { name: /11 registros com referência repetida/ }));
    await screen.findByRole("button", { name: "Ver registro #7" });
    fireEvent.click(screen.getByRole("button", { name: "Próxima" }));
    expect(await screen.findByRole("alert")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));
    expect(await screen.findByRole("button", { name: "Ver registro #8" })).toBeVisible();
    expect(screen.getByText("Página 2 · 11 registros")).toBeVisible();
    expect(screen.getByRole("button", { name: "Próxima" })).toBeDisabled();
  });
});
