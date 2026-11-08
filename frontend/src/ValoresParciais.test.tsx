import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import CalculoOperacao from "./CalculoOperacao";
import PosicoesAtuais from "./PosicoesAtuais";

describe("Informações financeiras incompletas", () => {
  it("preserva a receita conhecida e informa custo e resultado pendentes", () => {
    render(
      <CalculoOperacao
        calculo={{
          valorBrutoBrl: "400",
          taxaBrl: "0",
          custoBaixadoBrl: null,
          resultadoNegociacaoBrl: null,
          resultadoMoedaBrl: "0",
          resultadoTaxaBrl: "0",
          resultadoTotalBrl: null,
        }}
      />,
    );
    expect(screen.getByText("R$ 400,00")).toBeVisible();
    expect(screen.getAllByText("Indisponível")).toHaveLength(3);
  });

  it("qualifica a quantidade deficitária e não mostra custo desconhecido como zero", () => {
    render(
      <PosicoesAtuais
        posicoes={[
          {
            ativoId: "2",
            ativoTicker: "BTC",
            quantidade: "-0.03",
            custoTotalBrl: null,
            custoMedioBrl: null,
            quantidadeDisponivel: false,
            comAjustes: false,
            causas: ["1:2:r"],
            instituicoes: [],
          },
        ]}
      />,
    );
    expect(screen.getByText("Precisa de correção")).toBeVisible();
    expect(screen.getAllByText("Indisponível")).toHaveLength(2);
    expect(screen.queryByText("R$ 0,00")).not.toBeInTheDocument();
  });
});

it("posições incompletas não são apresentadas como carteira vazia", () => {
  render(<PosicoesAtuais posicoes={[]} completo={false} />);
  expect(
    screen.getByText("As posições ainda não podem ser apresentadas como completas."),
  ).toBeVisible();
  expect(screen.queryByText("Nenhuma posição encontrada.")).not.toBeInTheDocument();
});
