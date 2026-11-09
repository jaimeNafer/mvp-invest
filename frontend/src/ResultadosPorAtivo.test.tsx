import { render, screen } from "@testing-library/react";
import { expect, it } from "vitest";
import ResultadosPorAtivo from "./ResultadosPorAtivo";

it("preserva subtotal explicitamente indisponível em um ativo sem parcela calculável", () => {
  render(
    <ResultadosPorAtivo
      completo={false}
      resultados={[
        {
          ativoId: "1",
          ativoTicker: "BTC",
          ativoNome: "Bitcoin",
          negociacaoBrl: null,
          moedaBrl: null,
          taxaBrl: null,
          resultadoTotalBrl: "42",
          subtotalResultadoBrl: null,
        },
      ]}
    />,
  );
  expect(screen.getAllByText("Indisponível").length).toBeGreaterThan(0);
  expect(screen.queryByText("R$ 42,00")).not.toBeInTheDocument();
});
