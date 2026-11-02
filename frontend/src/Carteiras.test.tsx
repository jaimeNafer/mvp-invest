import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";
import Carteiras from "./Carteiras";
import { armazenamentoLocal } from "./services/armazenamentoLocal";
import type { Carteira } from "./services/carteiras";

const catalogo = [
  { id: "1", nome: "Binance", ativo: true },
  { id: "2", nome: "BitGet", ativo: true },
];
const conta = { id: "10", instituicaoId: "1", instituicaoNome: "Binance" };
const existente: Carteira = { id: "1", nome: "Principal", ativo: true, contas: [conta] };
const json = (dados: unknown, status = 200) => new Response(JSON.stringify(dados), { status });
const endpointInstituicoes = (url: string) => url.endsWith("/instituicoes");

afterEach(() => {
  armazenamentoLocal.remover("lastro.token");
  vi.unstubAllGlobals();
});

describe("cadastro de carteiras", () => {
  it("cria e edita instituições pela modal, arquiva e reativa, mantendo perfis separados pelo Bearer", async () => {
    const registros: (Carteira & { perfil: string })[] = [];
    const fetchMock = vi.fn(async (url: string, opcoes?: RequestInit) => {
      if (endpointInstituicoes(url)) return json(catalogo);
      const token = new Headers(opcoes?.headers).get("Authorization")?.slice(7) ?? "";
      const perfil = JSON.parse(atob(token.split(".")[1])).sub as string;
      const id = url.split("/")[4];
      const carteiraId = id ?? String(registros.length + 1);
      const entrada = opcoes?.body ? JSON.parse(String(opcoes.body)) : null;
      const contas = entrada?.instituicaoIds?.map((instituicaoId: string) => ({
        id: `${perfil}-${carteiraId}-${instituicaoId}`,
        instituicaoId,
        instituicaoNome: catalogo.find((item) => item.id === instituicaoId)?.nome,
      }));
      if (opcoes?.method === "POST") {
        const criado = {
          id: String(registros.length + 1),
          perfil,
          nome: entrada.nome,
          ativo: true,
          contas,
        };
        registros.push(criado);
        return json(criado, 201);
      }
      if (opcoes?.method === "PUT" || opcoes?.method === "PATCH") {
        const registro = registros.find((item) => item.id === id && item.perfil === perfil);
        if (!registro) return json({ detail: "Carteira não encontrada." }, 404);
        Object.assign(registro, entrada, contas ? { contas } : {});
        return json(registro);
      }
      return json(registros.filter((item) => item.perfil === perfil));
    });
    vi.stubGlobal("fetch", fetchMock);
    render(<App />);
    fireEvent.change(screen.getByLabelText("Usuário"), { target: { value: "jaime" } });
    fireEvent.click(screen.getByRole("button", { name: /Entrar/ }));
    await screen.findByRole("heading", { name: "Olá, Jaime." });
    fireEvent.click(screen.getByRole("button", { name: "Carteiras" }));
    await screen.findByText("Nenhuma carteira cadastrada.");
    fireEvent.click(screen.getByRole("button", { name: "Adicionar carteira" }));
    fireEvent.change(screen.getByLabelText("Nome da carteira"), {
      target: { value: "  Principal  " },
    });
    fireEvent.click(await screen.findByRole("checkbox", { name: "Binance" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "BitGet" }));
    fireEvent.click(screen.getByRole("button", { name: "Salvar carteira" }));
    await screen.findByText("Binance, BitGet", { selector: "td" });
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(
        ([, opcoes]) =>
          opcoes?.method === "POST" &&
          opcoes.body === JSON.stringify({ nome: "Principal", instituicaoIds: ["1", "2"] }),
      ),
    ).toBe(true);
    fireEvent.click(screen.getByRole("button", { name: "Editar Principal" }));
    expect(screen.getByLabelText("Nome da carteira")).toHaveValue("Principal");
    fireEvent.change(screen.getByLabelText("Nome da carteira"), {
      target: { value: "Longo prazo" },
    });
    const bitget = await screen.findByRole("checkbox", { name: "BitGet" });
    expect(bitget).toBeChecked();
    fireEvent.click(bitget);
    fireEvent.click(screen.getByRole("button", { name: "Salvar alterações" }));
    await screen.findByText("Longo prazo", { selector: "td" });
    expect(screen.getByText("Binance", { selector: "td" })).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Arquivar Longo prazo" }));
    await screen.findByRole("button", { name: "Reativar Longo prazo" });
    await waitFor(() =>
      expect(screen.getByRole("button", { name: "Reativar Longo prazo" })).toBeEnabled(),
    );
    expect(screen.getByText("Longo prazo", { selector: "td" })).toBeVisible();
    expect(screen.getByText("Arquivada", { exact: true })).toBeVisible();
    expect(screen.getByRole("button", { name: "Editar Longo prazo" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Reativar Longo prazo" }));
    await screen.findByRole("button", { name: "Arquivar Longo prazo" });
    fireEvent.click(screen.getByRole("button", { name: "Sair" }));
    fireEvent.change(screen.getByLabelText("Usuário"), { target: { value: "eliane" } });
    fireEvent.click(screen.getByRole("button", { name: /Entrar/ }));
    await screen.findByRole("heading", { name: "Olá, Eliane." });
    fireEvent.click(screen.getByRole("button", { name: "Carteiras" }));
    await screen.findByText("Nenhuma carteira cadastrada.");
    expect(screen.queryByText("Longo prazo")).not.toBeInTheDocument();
    expect(
      fetchMock.mock.calls.every(([, opcoes]) => !new Headers(opcoes?.headers).has("X-Perfil-Id")),
    ).toBe(true);
  });

  it("falha de listagem oferece nova tentativa sem inventar estado vazio", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockRejectedValueOnce(new Error("offline")).mockResolvedValueOnce(json([])),
    );
    render(<Carteiras />);
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Não foi possível conectar ao Lastro.",
    );
    expect(screen.queryByText("Nenhuma carteira cadastrada.")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));
    expect(await screen.findByText("Nenhuma carteira cadastrada.")).toBeVisible();
  });

  it("exige seleção e permite recuperar o catálogo sem perder o nome digitado", async () => {
    let consultas = 0;
    const fetchMock = vi.fn(async (url: string, _opcoes?: RequestInit) => {
      if (!endpointInstituicoes(url)) return json([]);
      if (++consultas === 1) throw new Error("offline");
      return json(catalogo);
    });
    vi.stubGlobal("fetch", fetchMock);
    render(<Carteiras />);
    await screen.findByText("Nenhuma carteira cadastrada.");
    fireEvent.click(screen.getByRole("button", { name: "Adicionar carteira" }));
    fireEvent.change(screen.getByLabelText("Nome da carteira"), {
      target: { value: "Meu recorte" },
    });
    await screen.findByRole("alert");
    expect(screen.getByRole("button", { name: "Salvar carteira" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));
    await screen.findByRole("checkbox", { name: "Binance" });
    expect(screen.getByLabelText("Nome da carteira")).toHaveValue("Meu recorte");
    fireEvent.click(screen.getByRole("button", { name: "Salvar carteira" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Selecione pelo menos uma instituição.",
    );
    expect(
      fetchMock.mock.calls.every(([, opcoes]) => !opcoes?.method || opcoes.method === "GET"),
    ).toBe(true);
  });

  it("mantém seleção e nome quando a gravação é recusada", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (url: string, opcoes?: RequestInit) => {
        if (endpointInstituicoes(url)) return json(catalogo);
        return opcoes?.method === "POST" ? json({ detail: "Nome já utilizado." }, 409) : json([]);
      }),
    );
    render(<Carteiras />);
    await screen.findByText("Nenhuma carteira cadastrada.");
    fireEvent.click(screen.getByRole("button", { name: "Adicionar carteira" }));
    fireEvent.change(screen.getByLabelText("Nome da carteira"), { target: { value: "Principal" } });
    fireEvent.click(await screen.findByRole("checkbox", { name: "Binance" }));
    fireEvent.click(screen.getByRole("button", { name: "Salvar carteira" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Nome já utilizado.");
    expect(screen.getByRole("dialog")).toBeVisible();
    expect(screen.getByLabelText("Nome da carteira")).toHaveValue("Principal");
    expect(screen.getByRole("checkbox", { name: "Binance" })).toBeChecked();
    expect(screen.queryByText("Carteira cadastrada.")).not.toBeInTheDocument();
  });

  it("consulta antiga não substitui a lista após um cadastro", async () => {
    let concluirPrimeira: ((resposta: Response) => void) | undefined;
    let consultas = 0;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (url: string, opcoes?: RequestInit) => {
        if (endpointInstituicoes(url)) return json(catalogo);
        if (opcoes?.method === "POST") return json(existente, 201);
        if (++consultas === 1)
          return new Promise<Response>((resolve) => {
            concluirPrimeira = resolve;
          });
        return json([existente]);
      }),
    );
    render(<Carteiras />);
    fireEvent.click(screen.getByRole("button", { name: "Adicionar carteira" }));
    fireEvent.change(screen.getByLabelText("Nome da carteira"), { target: { value: "Principal" } });
    fireEvent.click(await screen.findByRole("checkbox", { name: "Binance" }));
    fireEvent.click(screen.getByRole("button", { name: "Salvar carteira" }));
    await screen.findByText("Principal", { selector: "td" });
    await act(async () => {
      concluirPrimeira?.(json([]));
    });
    expect(screen.getByText("Principal", { selector: "td" })).toBeVisible();
  });

  it("cancela sem gravar e permite manter instituição inativa já vinculada", async () => {
    const fetchMock = vi.fn(async (url: string, _opcoes?: RequestInit) =>
      endpointInstituicoes(url)
        ? json(catalogo.map((item) => ({ ...item, ativo: false })))
        : json([existente]),
    );
    vi.stubGlobal("fetch", fetchMock);
    render(<Carteiras />);
    await screen.findByText("Principal", { selector: "td" });
    const adicionar = screen.getByRole("button", { name: "Adicionar carteira" });
    adicionar.focus();
    fireEvent.click(adicionar);
    expect(screen.getByLabelText("Nome da carteira")).toHaveFocus();
    expect(await screen.findByRole("checkbox", { name: /Binance/ })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Cancelar" }));
    expect(adicionar).toHaveFocus();
    fireEvent.click(screen.getByRole("button", { name: "Editar Principal" }));
    const binance = await screen.findByRole("checkbox", { name: /Binance/ });
    expect(binance).toBeChecked();
    expect(binance).toBeEnabled();
    expect(screen.getByRole("checkbox", { name: /BitGet/ })).toBeDisabled();
    fireEvent(
      screen.getByRole("dialog"),
      new Event("cancel", { bubbles: false, cancelable: true }),
    );
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(fetchMock.mock.calls.every(([, opcoes]) => !opcoes?.method)).toBe(true);
  });

  it("bloqueia fechamento e campos enquanto salva", async () => {
    let concluir: ((resposta: Response) => void) | undefined;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (url: string, opcoes?: RequestInit) => {
        if (endpointInstituicoes(url)) return json(catalogo);
        if (opcoes?.method === "POST")
          return new Promise<Response>((resolve) => {
            concluir = resolve;
          });
        return json([]);
      }),
    );
    render(<Carteiras />);
    await screen.findByText("Nenhuma carteira cadastrada.");
    fireEvent.click(screen.getByRole("button", { name: "Adicionar carteira" }));
    fireEvent.change(screen.getByLabelText("Nome da carteira"), { target: { value: "Principal" } });
    fireEvent.click(await screen.findByRole("checkbox", { name: "Binance" }));
    fireEvent.click(screen.getByRole("button", { name: "Salvar carteira" }));
    expect(screen.getByRole("button", { name: "Cancelar" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Fechar modal" })).toBeDisabled();
    expect(screen.getByRole("checkbox", { name: "Binance" })).toBeDisabled();
    fireEvent(
      screen.getByRole("dialog"),
      new Event("cancel", { bubbles: false, cancelable: true }),
    );
    expect(screen.getByRole("dialog")).toBeVisible();
    await act(async () => {
      concluir?.(json(existente, 201));
    });
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  });
});
