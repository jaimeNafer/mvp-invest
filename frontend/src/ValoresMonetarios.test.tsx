import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import CalculoOperacao from "./CalculoOperacao";
import PosicoesAtuais from "./PosicoesAtuais";

describe("apresentação monetária das operações", () => {
  it("apresenta custos e médias em duas casas e mantém o valor integral acessível", () => {
    render(
      <PosicoesAtuais
        posicoes={[
          {
            ativoId: "1",
            ativoTicker: "BTC",
            quantidade: "0.01",
            custoTotalBrl: "1000.015",
            custoMedioBrl: "180.123456789012345678901234567890123456",
            instituicoes: [],
          },
        ]}
      />,
    );
    expect(screen.getByText("R$ 1.000,02")).toHaveAttribute(
      "title",
      "Valor completo: R$ 1.000,015",
    );
    expect(screen.getByText("R$ 180,12")).toHaveAttribute(
      "title",
      "Valor completo: R$ 180,123456789012345678901234567890123456",
    );
    expect(screen.getByText("R$ 180,12")).toHaveAccessibleDescription(
      "Valor completo: R$ 180,123456789012345678901234567890123456",
    );
  });
  it("arredonda as parcelas negativas por HALF_EVEN preservando os valores exatos", () => {
    render(
      <CalculoOperacao
        calculo={{
          valorBrutoBrl: "1000.015",
          taxaBrl: "0.000001",
          custoBaixadoBrl: "200",
          resultadoNegociacaoBrl: "-1.015",
          resultadoMoedaBrl: "-1.005",
          resultadoTaxaBrl: "-0.00000001",
          resultadoTotalBrl: "-2.02000001",
        }}
      />,
    );
    expect(screen.getByText("R$ -1,02")).toHaveAttribute("title", "Valor completo: R$ -1,015");
    expect(screen.getByText("R$ -1,00")).toHaveAttribute("title", "Valor completo: R$ -1,005");
    expect(screen.getByText("entre R$ -0,01 e R$ 0,00")).toHaveAccessibleDescription(
      "Valor completo: R$ -0,00000001",
    );
    expect(screen.getByText("menor que R$ 0,01")).toHaveAttribute(
      "title",
      "Valor completo: R$ 0,000001",
    );
  });
});
