import { act, fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import CalculoOperacao from "./CalculoOperacao";

const calculo = {
  valorBrutoBrl: "2000",
  taxaBrl: "10",
  custoBaixadoBrl: "125.125",
  resultadoNegociacaoBrl: "1874.875",
  resultadoMoedaBrl: "0",
  resultadoTaxaBrl: "-10",
  resultadoTotalBrl: "1864.875",
};

describe("explicação do custo utilizado", () => {
  it("usa descrição clara e mostra a fórmula ao passar o cursor", () => {
    render(<CalculoOperacao calculo={calculo} />);
    expect(screen.getByText("Custo de aquisição utilizado")).toBeVisible();
    expect(screen.queryByText("Custo baixado")).not.toBeInTheDocument();
    expect(screen.getByText("R$ 125,12")).toBeVisible();
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
    fireEvent.mouseEnter(
      screen.getByRole("button", { name: "Sobre custo de aquisição utilizado" }),
    );
    expect(screen.getByRole("tooltip")).toHaveTextContent(
      "quantidade vendida × custo médio anterior",
    );
    expect(screen.getByRole("tooltip")).toHaveTextContent("pagar taxas");
    fireEvent.mouseLeave(
      screen.getByRole("button", { name: "Sobre custo de aquisição utilizado" }),
    );
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
  });

  it("oferece ajuda por teclado e Escape fecha somente a tooltip", () => {
    const teclaNaModal = vi.fn();
    render(
      <dialog open aria-label="Detalhe da operação" onKeyDown={teclaNaModal}>
        <CalculoOperacao calculo={calculo} />
      </dialog>,
    );
    const ajuda = screen.getByRole("button", { name: "Sobre custo de aquisição utilizado" });
    act(() => ajuda.focus());
    expect(screen.getByRole("tooltip")).toBeVisible();
    expect(ajuda).toHaveAccessibleDescription(/Custo histórico/);
    fireEvent.keyDown(ajuda, { key: "Escape" });
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
    expect(teclaNaModal).not.toHaveBeenCalled();
    expect(ajuda).toHaveFocus();
  });

  it("abre ao clicar sem submeter o formulário", () => {
    const salvar = vi.fn();
    render(
      <form onSubmit={salvar}>
        <CalculoOperacao calculo={calculo} />
      </form>,
    );
    fireEvent.click(screen.getByRole("button", { name: "Sobre custo de aquisição utilizado" }));
    expect(screen.getByRole("tooltip")).toBeVisible();
    expect(salvar).not.toHaveBeenCalled();
  });
});
