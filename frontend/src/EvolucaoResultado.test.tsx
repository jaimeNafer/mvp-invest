import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import EvolucaoResultado from "./EvolucaoResultado";

describe("Evolução do resultado realizado", () => {
  it("preserva prejuízo, mês sem operações e valores completos na alternativa tabular", () => {
    render(
      <EvolucaoResultado
        evolucao={{
          acumuladoInicialBrl: "0",
          meses: [
            {
              dataInicio: "2026-07-03",
              dataFim: "2026-07-31",
              resultadoBrl: "-12.12345678",
              acumuladoBrl: "-12.12345678",
              operacoes: 1,
            },
            {
              dataInicio: "2026-08-01",
              dataFim: "2026-08-02",
              resultadoBrl: "0",
              acumuladoBrl: "-12.12345678",
              operacoes: 0,
            },
          ],
        }}
      />,
    );
    fireEvent.click(screen.getByText("Valores por mês"));
    expect(screen.getByRole("table", { name: "Resultado mensal" })).toBeVisible();
    expect(screen.getByText("03/07/2026 a 31/07/2026")).toBeVisible();
    expect(screen.getAllByTitle("Valor completo: R$ -12,12345678").length).toBe(3);
    expect(screen.getByRole("img").querySelector("text")).toBeNull();
    expect(screen.getByText("07/2026")).toHaveClass("chart-month-label");
    const linha = screen.getByRole("img").querySelector("polyline");
    expect(linha?.getAttribute("points")).toBe("10,24 320,136 630,136");
  });

  it("mantém escala visível para período com resultado zero", () => {
    render(
      <EvolucaoResultado
        evolucao={{
          acumuladoInicialBrl: "0",
          meses: [
            {
              dataInicio: "2026-07-03",
              dataFim: "2026-07-03",
              resultadoBrl: "0",
              acumuladoBrl: "0",
              operacoes: 0,
            },
          ],
        }}
      />,
    );
    expect(screen.getByRole("img").querySelector("polyline")).toHaveAttribute(
      "points",
      "10,80 630,80",
    );
    expect(screen.getByRole("img").querySelector("line")).toHaveAttribute("y1", "80");
  });
});
