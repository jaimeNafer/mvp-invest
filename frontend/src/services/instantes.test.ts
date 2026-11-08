import { expect, it } from "vitest";
import { diferencaSegundos } from "./instantes";
it("preserva a diferenca de nanossegundos e o limite exato de dois segundos", () => {
  expect(diferencaSegundos("2024-05-26T22:05:32.000000001Z", "2024-05-26T22:05:32Z")).toBe(
    "+0.000000001",
  );
  expect(diferencaSegundos("2024-05-26T22:05:34Z", "2024-05-26T22:05:32Z")).toBe("+2");
  expect(diferencaSegundos("2024-05-26T22:05:31.9Z", "2024-05-26T22:05:32Z")).toBe("-0.1");
  expect(diferencaSegundos("invalido", "2024-05-26T22:05:32Z")).toBeNull();
});
