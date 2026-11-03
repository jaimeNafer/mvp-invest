import { describe, expect, it } from "vitest";
import { decimalCanonico, decimalPtBr, moeda } from "./decimais";

describe("representação decimal", () => {
  it("converte pt-BR sem perder dígitos", () => {
    expect(decimalCanonico("1.234,56000001")).toBe("1234.56000001");
    expect(decimalCanonico("0,000000000000000001")).toBe("0.000000000000000001");
    expect(decimalPtBr("999999999999999999999999999999.12345678")).toBe(
      "999.999.999.999.999.999.999.999.999.999,12345678",
    );
    for (const v of ["", "1e3", "NaN", "0.01", "-1", "1,2,3"])
      expect(() => decimalCanonico(v)).toThrow();
  });
  it("apresenta HALF_EVEN e distingue valor pequeno de zero", () => {
    expect(moeda("1.005")).toBe("R$ 1,00");
    expect(moeda("1.015")).toBe("R$ 1,02");
    expect(moeda("0.00000001")).toBe("menor que R$ 0,01");
    expect(moeda("0")).toBe("R$ 0,00");
    expect(moeda("999999999999999999999999999999.99999999")).toBe(
      "R$ 1.000.000.000.000.000.000.000.000.000.000,00",
    );
  });
  it.each([
    ["-1.015", "R$ -1,02"],
    ["-1.005", "R$ -1,00"],
    ["-0.01", "R$ -0,01"],
    ["-0.00000001", "entre R$ -0,01 e R$ 0,00"],
    ["-0.00999999", "entre R$ -0,01 e R$ 0,00"],
    ["-0.00", "R$ 0,00"],
    [
      "-999999999999999999999999999999.99999999",
      "R$ -1.000.000.000.000.000.000.000.000.000.000,00",
    ],
  ])("apresenta o valor negativo %s com sinal e HALF_EVEN", (valor, esperado) => {
    expect(moeda(valor)).toBe(esperado);
  });
});
