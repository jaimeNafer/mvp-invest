import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";
import { armazenamentoLocal } from "./services/armazenamentoLocal";

afterEach(() => {
  armazenamentoLocal.remover("lastro.token");
  window.history.replaceState(null, "", "/");
  vi.unstubAllGlobals();
});

describe("login pelo serviço local do frontend", () => {
  it("separa operações, importações e conferência no menu principal", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("backend indisponível")));
    render(<App />);
    fireEvent.change(screen.getByLabelText("Usuário"), { target: { value: "jaime" } });
    fireEvent.click(screen.getByRole("button", { name: /Entrar/ }));
    await screen.findByRole("heading", { name: "Visão geral" });
    const menu = screen.getByRole("navigation", { name: "Navegação principal" });
    expect(Array.from(menu.querySelectorAll("button")).map((item) => item.textContent)).toEqual([
      "Visão geral",
      "Carteiras",
      "Operações",
      "Importações",
      "Conferência",
    ]);
    fireEvent.click(screen.getByRole("button", { name: "Conferência" }));
    expect(await screen.findByRole("heading", { name: "Conferência" })).toBeVisible();
  });

  it("entra com os dois usuários e sai sem depender do backend", async () => {
    const fetchMock = vi.fn().mockRejectedValue(new Error("backend indisponível"));
    vi.stubGlobal("fetch", fetchMock);
    render(<App />);
    expect(screen.getByLabelText("Senha")).toHaveValue("");
    fireEvent.change(screen.getByLabelText("Usuário"), { target: { value: " Jaime " } });
    fireEvent.click(screen.getByRole("button", { name: /Entrar/ }));
    expect(await screen.findByRole("heading", { name: "Visão geral" })).toBeVisible();
    const tokenJaime = armazenamentoLocal.obter("lastro.token");
    expect(tokenJaime).not.toBeNull();
    const [header, payload, assinatura] = (tokenJaime ?? "").split(".");
    expect(JSON.parse(atob(header))).toEqual({ alg: "none", typ: "JWT" });
    expect(JSON.parse(atob(payload))).toEqual({
      sub: "1",
      name: "Jaime",
      role: "acesso_total",
      iat: expect.any(Number),
    });
    expect(assinatura).toBe("");
    fireEvent.click(screen.getByRole("button", { name: "Sair" }));
    expect(armazenamentoLocal.obter("lastro.token")).toBeNull();
    expect(screen.getByLabelText("Usuário")).toHaveValue("");
    expect(screen.getByLabelText("Senha")).toHaveValue("");
    fireEvent.change(screen.getByLabelText("Usuário"), { target: { value: "eliane" } });
    fireEvent.click(screen.getByRole("button", { name: /Entrar/ }));
    expect(await screen.findByRole("heading", { name: "Visão geral" })).toBeVisible();
    const payloadEliane = (armazenamentoLocal.obter("lastro.token") ?? "").split(".")[1];
    expect(JSON.parse(atob(payloadEliane))).toMatchObject({
      sub: "2",
      name: "Eliane",
      role: "acesso_total",
    });
    expect(screen.queryByRole("heading", { name: "Olá, Jaime." })).not.toBeInTheDocument();
    expect(new Set(fetchMock.mock.calls.map(([url]) => url))).toEqual(
      new Set([
        "/api/v1/visao-geral",
        "/api/v1/conciliacao/cotacoes/atualizacao",
        "/api/v1/carteiras",
      ]),
    );
  });

  it.each([
    ["desconhecido", ""],
    ["jaime", "incorreta"],
  ])("recusa as credenciais inválidas de %s", async (login, senha) => {
    render(<App />);
    fireEvent.change(screen.getByLabelText("Usuário"), { target: { value: login } });
    fireEvent.change(screen.getByLabelText("Senha"), { target: { value: senha } });
    fireEvent.click(screen.getByRole("button", { name: /Entrar/ }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Usuário ou senha inválidos.");
    expect(screen.getByLabelText("Senha")).toHaveValue("");
    expect(screen.queryByRole("heading", { name: /Olá,/ })).not.toBeInTheDocument();
    expect(armazenamentoLocal.obter("lastro.token")).toBeNull();
  });

  it("preserva o token na recarga sem restaurar o perfil automaticamente", async () => {
    const { unmount } = render(<App />);
    fireEvent.change(screen.getByLabelText("Usuário"), { target: { value: "jaime" } });
    fireEvent.click(screen.getByRole("button", { name: /Entrar/ }));
    await screen.findByRole("heading", { name: "Visão geral" });
    const token = armazenamentoLocal.obter("lastro.token");
    unmount();
    render(<App />);
    expect(screen.getByRole("heading", { name: "Bem-vindo de volta." })).toBeVisible();
    expect(screen.getByLabelText("Senha")).toHaveValue("");
    expect(armazenamentoLocal.obter("lastro.token")).toBe(token);
  });
});
