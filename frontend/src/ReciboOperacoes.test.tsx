import { fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import Importacoes from "./Importacoes";
import type { Operacao } from "./services/operacoes";
const operacao: Operacao = {
  id: "op-20",
  tipo: "APORTE",
  instituicaoId: "1",
  instituicaoNome: "Instituição sintética",
  ativoId: "1",
  ativoTicker: "BRL",
  ocorridaEm: "2026-01-01T12:00:00Z",
  quantidade: "10.23",
  custoTotalBrl: "10.23",
  moedaCotacaoId: null,
  moedaCotacaoTicker: null,
  totalBruto: null,
  cotacaoBrl: null,
  taxaAtivoId: null,
  taxaAtivoTicker: null,
  taxaQuantidade: "0",
  referencia: "ref-sintetica",
  situacao: "CONFIRMADA",
  substituiId: "op-anterior",
  motivoAlteracao: null,
  criadoEm: "2026-01-02T12:00:00Z",
  alteradoEm: "2026-01-02T12:00:00Z",
  calculo: null,
};
const recibo = {
  id: "i1",
  hash: "abc",
  nomeArquivo: "sintetico.csv",
  criadoEm: "2026-01-02T12:00:00Z",
  novos: 21,
  reconhecidos: 0,
  eventos: Array.from({ length: 21 }, (_, indice) => ({
    linha: indice + 2,
    referencia: `ref-${indice}`,
    operacaoId: `op-${indice}`,
  })),
};
const json = (dados: unknown, status = 200) => new Response(JSON.stringify(dados), { status });
afterEach(() => vi.unstubAllGlobals());
function servidor(falha = false) {
  const requisicoes: string[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: string, opcoes?: RequestInit) => {
      requisicoes.push(url);
      if (url.endsWith("/importacoes/previa"))
        return json({
          hash: "abc",
          revisao: "1",
          previaId: "p1",
          novos: 21,
          reconhecidos: 0,
          erros: [],
          eventos: [],
        });
      if (url.endsWith("/importacoes") && opcoes?.method === "POST") return json(recibo);
      if (url.endsWith("/importacoes/i1")) return json(recibo);
      if (url.endsWith("/operacoes/op-20"))
        return falha
          ? json({ detail: "Registro indisponível para consulta." }, 400)
          : json(operacao);
      if (url.endsWith("/operacoes/op-anterior"))
        return json({ ...operacao, id: "op-anterior", situacao: "SUBSTITUIDA", substituiId: null });
      return json({ itens: [recibo], pagina: 0, tamanho: 20, total: 1 });
    }),
  );
  return requisicoes;
}
async function abrirRecibo() {
  render(<Importacoes onBloqueio={() => {}} />);
  fireEvent.click(await screen.findByRole("button", { name: "Detalhes de sintetico.csv" }));
  const dialogo = await screen.findByRole("dialog");
  fireEvent.click(within(dialogo).getByRole("button", { name: "Próxima" }));
  fireEvent.click(within(dialogo).getByRole("button", { name: "Consultar operação op-20" }));
}
it("consulta operação e versão anterior em uma única modal e preserva página do recibo", async () => {
  const requisicoes = servidor();
  await abrirRecibo();
  const dialogo = await screen.findByRole("dialog", { name: "Detalhes da operação" });
  expect(await within(dialogo).findByText("Instituição sintética")).toBeVisible();
  expect(within(dialogo).getByText("10,23")).toBeVisible();
  expect(screen.getAllByRole("dialog")).toHaveLength(1);
  fireEvent.click(within(dialogo).getByRole("button", { name: "Consultar versão anterior" }));
  expect(await within(dialogo).findByText("#op-anterior")).toBeVisible();
  expect(requisicoes).toContain("/api/v1/operacoes/op-anterior");
  fireEvent.click(within(dialogo).getByRole("button", { name: "Voltar ao recibo" }));
  expect(screen.getByRole("dialog", { name: "Detalhes da importação" })).toBeVisible();
  expect(within(dialogo).getByText("Página 2")).toBeVisible();
  expect(within(dialogo).getByRole("button", { name: "Consultar operação op-20" })).toBeVisible();
});
it("mostra erro de consulta sem perder recibo e limpa erro ao voltar", async () => {
  servidor(true);
  await abrirRecibo();
  const dialogo = screen.getByRole("dialog");
  expect(await within(dialogo).findByRole("alert")).toHaveTextContent(
    "Registro indisponível para consulta.",
  );
  fireEvent.click(within(dialogo).getByRole("button", { name: "Voltar ao recibo" }));
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(within(dialogo).getByText("Página 2")).toBeVisible();
});
it("abre operação pelo recibo recém confirmado e volta à mesma página", async () => {
  servidor();
  render(<Importacoes onBloqueio={() => {}} />);
  fireEvent.change(screen.getByLabelText("Arquivo de operações"), {
    target: { files: [new File(["sintetico"], "sintetico.csv")] },
  });
  fireEvent.click(screen.getByLabelText(/Os depósitos são aportes próprios/));
  fireEvent.click(screen.getByLabelText(/O total das compras OCBS inclui as taxas/));
  fireEvent.click(screen.getByRole("button", { name: "Analisar arquivo" }));
  fireEvent.click(await screen.findByRole("button", { name: "Confirmar importação" }));
  const reciboInline = await screen.findByRole("region", { name: "Recibo da importação" });
  fireEvent.click(within(reciboInline).getByRole("button", { name: "Próxima" }));
  fireEvent.click(within(reciboInline).getByRole("button", { name: "Consultar operação op-20" }));
  const dialogo = await screen.findByRole("dialog", { name: "Detalhes da operação" });
  expect(await within(dialogo).findByText("#op-20")).toBeVisible();
  fireEvent.click(within(dialogo).getByRole("button", { name: "Fechar modal" }));
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  expect(within(reciboInline).getByText("Página 2")).toBeVisible();
});
