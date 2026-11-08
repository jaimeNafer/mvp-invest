import { fireEvent, render, screen } from "@testing-library/react";
import { expect, it } from "vitest";
import PatrimonioDiario from "./PatrimonioDiario";

it("mantém o histórico recolhido e limita as datas exibidas por página", () => {
  const pontos = Array.from({ length: 40 }, (_, indice) => ({
    data: new Date(Date.UTC(2024, 0, indice + 1)).toISOString().slice(0, 10),
    valorTotalBrl: "100",
    valorConhecidoBrl: "100",
    ativosComPreco: 1,
    ativosSemPreco: 0,
    quantidadesCompletas: true,
  }));
  render(<PatrimonioDiario pontos={pontos} />);
  expect(screen.getByText("09/02/2024")).not.toBeVisible();
  fireEvent.click(screen.getByText("Evolução diária do patrimônio"));
  expect(screen.getByRole("table").querySelectorAll("tbody tr")).toHaveLength(15);
  expect(screen.getByText("Página 1 de 3")).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Mais antigas" }));
  expect(screen.getByText("Página 2 de 3")).toBeVisible();
});
