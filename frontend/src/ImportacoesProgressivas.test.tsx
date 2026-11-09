import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import ImportacoesProgressivas from "./ImportacoesProgressivas";
import { listarCarteiras } from "./services/carteiras";

vi.mock("./services/carteiras", () => ({
  listarCarteiras: vi.fn().mockResolvedValue([
    {
      id: "inativa",
      nome: "Antiga",
      ativo: false,
      contas: [{ id: "1", instituicaoId: "binance", instituicaoNome: "Binance" }],
    },
    {
      id: "ativa",
      nome: "Principal",
      ativo: true,
      contas: [{ id: "2", instituicaoId: "binance", instituicaoNome: "Binance" }],
    },
  ]),
}));
vi.mock("./RecebimentoHistorico", () => ({
  default: ({ disabled }: { disabled: boolean }) => (
    <button type="button" disabled={disabled}>
      Recebimento disponível
    </button>
  ),
}));

afterEach(() => {
  vi.unstubAllGlobals();
  vi.clearAllMocks();
});

it("preserva a Binance compartilhada e bloqueia o envio ao trocar para carteira sem suporte", async () => {
  const binance = { id: "1", instituicaoId: "b", instituicaoNome: "Binance" };
  const bitget = { id: "2", instituicaoId: "g", instituicaoNome: "Bitget" };
  vi.mocked(listarCarteiras).mockResolvedValueOnce([
    { id: "1", nome: "Principal", ativo: true, contas: [binance, bitget] },
    { id: "2", nome: "Cripto", ativo: true, contas: [binance] },
    { id: "3", nome: "Outra", ativo: true, contas: [bitget] },
  ]);
  vi.stubGlobal(
    "fetch",
    vi.fn(
      async () => new Response(JSON.stringify({ itens: [], pagina: 0, tamanho: 20, total: 0 })),
    ),
  );
  render(
    <ImportacoesProgressivas
      onBloqueio={() => {}}
      onOperacoes={() => {}}
      onConferencia={() => {}}
    />,
  );
  const carteira = await screen.findByRole("combobox", { name: "Carteira" });
  const instituicao = screen.getByRole("combobox", { name: "Instituição" });
  const envio = screen.getByRole("button", { name: "Recebimento disponível" });
  expect(envio).toBeDisabled();
  fireEvent.change(carteira, { target: { value: "1" } });
  expect(instituicao).toHaveValue("b");
  expect(envio).toBeEnabled();
  fireEvent.change(carteira, { target: { value: "2" } });
  expect(instituicao).toHaveValue("b");
  fireEvent.change(carteira, { target: { value: "3" } });
  expect(instituicao).toHaveValue("");
  expect(envio).toBeDisabled();
});

it("usa uma carteira ativa e limita o recebimento ao vínculo Binance", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn(
      async () => new Response(JSON.stringify({ itens: [], pagina: 0, tamanho: 20, total: 0 })),
    ),
  );
  render(
    <ImportacoesProgressivas
      onBloqueio={() => {}}
      onOperacoes={() => {}}
      onConferencia={() => {}}
    />,
  );

  expect(await screen.findByRole("combobox", { name: "Carteira" })).toHaveValue("ativa");
  expect(screen.getByRole("combobox", { name: "Instituição" })).toHaveValue("binance");
  expect(screen.getByText("Recebimento disponível")).toBeVisible();
});

it("confirma a exclusão de recebimentos legados e progressivos e atualiza a lista", async () => {
  const excluidos = new Set<string>();
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const caminho = String(input);
    if (init?.method === "DELETE") {
      excluidos.add(caminho);
      return new Response(null, { status: 204 });
    }
    if (caminho.includes("/recebimentos?"))
      return new Response(
        JSON.stringify({
          itens: excluidos.has("/api/v1/importacoes/recebimentos/progresso")
            ? []
            : [
                {
                  id: "progresso",
                  nomeArquivo: "atual.csv",
                  criadoEm: "2026-11-02T12:00:00Z",
                  registros: 1,
                },
              ],
          pagina: 0,
          tamanho: 20,
          total: 1,
        }),
      );
    if (caminho.includes("/importacoes?"))
      return new Response(
        JSON.stringify({
          itens: excluidos.has("/api/v1/importacoes/recebimentos/importacao%3Aversao%2Fantiga")
            ? []
            : [
                {
                  id: "versao/antiga",
                  nomeArquivo: "legado.xlsx",
                  criadoEm: "2026-11-01T12:00:00Z",
                  novos: 2,
                  reconhecidos: 0,
                },
              ],
          pagina: 0,
          tamanho: 20,
          total: 1,
        }),
      );
    return new Response(JSON.stringify([]));
  });
  vi.stubGlobal("fetch", fetchMock);
  render(
    <ImportacoesProgressivas
      onBloqueio={() => {}}
      onOperacoes={() => {}}
      onConferencia={() => {}}
    />,
  );

  const excluirLegado = await screen.findByRole("button", { name: "Excluir legado.xlsx" });
  expect(excluirLegado).toHaveTextContent(/^Excluir$/);
  fireEvent.click(excluirLegado);
  expect(await screen.findByText(/A exclusão é permanente/)).toBeVisible();
  expect(screen.getByRole("dialog")).toHaveTextContent("legado.xlsx");
  fireEvent.click(screen.getByRole("button", { name: "Excluir recebimento" }));

  fireEvent.click(await screen.findByRole("button", { name: "Excluir atual.csv" }));
  fireEvent.click(screen.getByRole("button", { name: "Excluir recebimento" }));
  expect(await screen.findByText("Nenhum recebimento encontrado.")).toBeVisible();
  expect(fetchMock).toHaveBeenCalledWith(
    "/api/v1/importacoes/recebimentos/importacao%3Aversao%2Fantiga",
    expect.objectContaining({ method: "DELETE" }),
  );
  expect(fetchMock).toHaveBeenCalledWith(
    "/api/v1/importacoes/recebimentos/progresso",
    expect.objectContaining({ method: "DELETE" }),
  );
});

it("mantém o recebimento e informa o erro quando a exclusão falha", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      if (init?.method === "DELETE")
        return new Response(JSON.stringify({ detail: "O recebimento possui dados em uso." }), {
          status: 409,
        });
      if (String(input).includes("/recebimentos?"))
        return new Response(
          JSON.stringify({
            itens: [
              {
                id: "atual",
                nomeArquivo: "atual.csv",
                criadoEm: "2026-11-02T12:00:00Z",
                registros: 1,
              },
            ],
            pagina: 0,
            tamanho: 20,
            total: 1,
          }),
        );
      return new Response(JSON.stringify({ itens: [], pagina: 0, tamanho: 20, total: 0 }));
    }),
  );
  render(
    <ImportacoesProgressivas
      onBloqueio={() => {}}
      onOperacoes={() => {}}
      onConferencia={() => {}}
    />,
  );

  fireEvent.click(await screen.findByRole("button", { name: "Excluir atual.csv" }));
  fireEvent.click(screen.getByRole("button", { name: "Excluir recebimento" }));

  expect(await screen.findByRole("alert")).toHaveTextContent("O recebimento possui dados em uso.");
  expect(screen.getByRole("button", { name: "Excluir atual.csv" })).toBeVisible();
});
