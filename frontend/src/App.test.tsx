import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";
import { armazenamentoLocal } from "./services/armazenamentoLocal";

afterEach(() => {
  armazenamentoLocal.remover("lastro.token");
  vi.unstubAllGlobals();
});

describe("login pelo serviço local do frontend", () => {
  it("entra com os dois usuários e sai sem depender do backend", async () => {
    const fetchMock = vi.fn().mockRejectedValue(new Error("backend indisponível"));
    vi.stubGlobal("fetch", fetchMock);
    render(<App />);
    expect(screen.getByLabelText("Senha")).toHaveValue("");
    fireEvent.change(screen.getByLabelText("Usuário"), { target: { value: " Jaime " } });
    fireEvent.click(screen.getByRole("button", { name: /Entrar no meu espaço/ }));
    expect(await screen.findByRole("heading", { name: "Olá, Jaime." })).toBeVisible();
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
    fireEvent.click(screen.getByRole("button", { name: /Entrar no meu espaço/ }));
    expect(await screen.findByRole("heading", { name: "Olá, Eliane." })).toBeVisible();
    const payloadEliane = (armazenamentoLocal.obter("lastro.token") ?? "").split(".")[1];
    expect(JSON.parse(atob(payloadEliane))).toMatchObject({
      sub: "2",
      name: "Eliane",
      role: "acesso_total",
    });
    expect(screen.queryByRole("heading", { name: "Olá, Jaime." })).not.toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it.each([
    ["desconhecido", ""],
    ["jaime", "incorreta"],
  ])("recusa as credenciais inválidas de %s", async (login, senha) => {
    render(<App />);
    fireEvent.change(screen.getByLabelText("Usuário"), { target: { value: login } });
    fireEvent.change(screen.getByLabelText("Senha"), { target: { value: senha } });
    fireEvent.click(screen.getByRole("button", { name: /Entrar no meu espaço/ }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Usuário ou senha inválidos.");
    expect(screen.getByLabelText("Senha")).toHaveValue("");
    expect(screen.queryByRole("heading", { name: /Olá,/ })).not.toBeInTheDocument();
    expect(armazenamentoLocal.obter("lastro.token")).toBeNull();
  });

  it("preserva o token na recarga sem restaurar o perfil automaticamente", async () => {
    const { unmount } = render(<App />);
    fireEvent.change(screen.getByLabelText("Usuário"), { target: { value: "jaime" } });
    fireEvent.click(screen.getByRole("button", { name: /Entrar no meu espaço/ }));
    await screen.findByRole("heading", { name: "Olá, Jaime." });
    const token = armazenamentoLocal.obter("lastro.token");
    unmount();
    render(<App />);
    expect(screen.getByRole("heading", { name: "Bem-vindo de volta." })).toBeVisible();
    expect(screen.getByLabelText("Senha")).toHaveValue("");
    expect(armazenamentoLocal.obter("lastro.token")).toBe(token);
  });
});
