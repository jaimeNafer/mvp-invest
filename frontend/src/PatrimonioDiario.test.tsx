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

it("desenha patrimônio e aportes acumulados sem ligar datas incompletas", () => {
  const pontos = [
    {
      data: "2026-10-01",
      valorTotalBrl: "100",
      valorConhecidoBrl: "100",
      ativosComPreco: 1,
      ativosSemPreco: 0,
      quantidadesCompletas: true,
      aportesLiquidosBrl: "80",
      aportesLiquidosConhecidosBrl: "80",
    },
    {
      data: "2026-10-02",
      valorTotalBrl: null,
      valorConhecidoBrl: "110",
      ativosComPreco: 1,
      ativosSemPreco: 1,
      quantidadesCompletas: true,
      aportesLiquidosBrl: null,
      aportesLiquidosConhecidosBrl: "80",
    },
    {
      data: "2026-10-03",
      valorTotalBrl: "130",
      valorConhecidoBrl: "130",
      ativosComPreco: 1,
      ativosSemPreco: 0,
      quantidadesCompletas: true,
      aportesLiquidosBrl: "90",
      aportesLiquidosConhecidosBrl: "90",
    },
  ];
  render(<PatrimonioDiario pontos={pontos} />);
  fireEvent.click(screen.getByText("Evolução diária do patrimônio"));
  expect(
    screen.getByRole("img", { name: "Evolução diária do patrimônio e aportes líquidos" }),
  ).toBeVisible();
  const grafico = screen.getByRole("img", {
    name: "Evolução diária do patrimônio e aportes líquidos",
  });
  expect(grafico.querySelector(".chart-patrimony")?.getAttribute("d")?.match(/M/g)).toHaveLength(2);
  expect(
    grafico.querySelector(".chart-contributions")?.getAttribute("d")?.match(/M/g),
  ).toHaveLength(2);
  expect(screen.getByText("Os trechos sem valor completo aparecem como lacunas.")).toBeVisible();
});

it("não exibe subtotal zero quando nenhum ativo tem cotação", () => {
  render(
    <PatrimonioDiario
      pontos={[
        {
          data: "2026-10-04",
          valorTotalBrl: null,
          valorConhecidoBrl: "0",
          ativosComPreco: 0,
          ativosSemPreco: 1,
          quantidadesCompletas: true,
        },
      ]}
    />,
  );
  fireEvent.click(screen.getByText("Evolução diária do patrimônio"));
  expect(screen.getByText("Indisponível")).toBeVisible();
  expect(screen.queryByText("R$ 0,00")).not.toBeInTheDocument();
});
