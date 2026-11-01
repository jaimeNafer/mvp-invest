import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import Home from "./Home";

const session = {
  id: "1",
  login: "jaime",
  name: "Jaime",
};

describe("perfil selecionado", () => {
  it("apresenta a identidade recebida do serviço", () => {
    render(<Home usuario={session} onLogout={vi.fn()} />);
    expect(screen.getByRole("heading", { name: "Olá, Jaime." })).toBeVisible();
    expect(screen.getByRole("heading", { name: "Meu perfil" })).toBeVisible();
    expect(screen.getByText("jaime", { exact: true })).toBeVisible();
  });

  it("disponibiliza a ação de encerrar a sessão", () => {
    const onLogout = vi.fn();
    render(<Home usuario={session} onLogout={onLogout} />);
    fireEvent.click(screen.getByRole("button", { name: "Sair" }));
    expect(onLogout).toHaveBeenCalledOnce();
  });
});
