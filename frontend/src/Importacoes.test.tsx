import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import Importacoes from "./Importacoes";
import PreviaArquivo from "./PreviaArquivo";
import ReciboArquivo from "./ReciboArquivo";
import type { PreviaImportacao } from "./services/importacoes";
vi.mock("./VisaoGeral", () => ({ default: () => null }));
vi.mock("./Negociacoes", () => ({ default: () => null }));
const evento = {
  linha: 2,
  referencia: "teste-1",
  tipo: "APORTE",
  ocorridaEm: "2026-01-01T12:00:00Z",
  ativo: "BRL",
  quantidade: "100.00",
  principal: null,
  taxa: "0",
  situacao: "NOVO",
};
const previa = {
  hash: "abc",
  revisao: "1",
  previaId: "p1",
  novos: 1,
  reconhecidos: 0,
  erros: [],
  eventos: [evento],
};
const recibo = {
  id: "i1",
  hash: "abc",
  nomeArquivo: "teste.csv",
  criadoEm: "2026-01-02T12:00:00Z",
  novos: 1,
  reconhecidos: 0,
  eventos: [{ linha: 2, referencia: "teste-1", operacaoId: "o1" }],
};
const json = (dados: unknown) => new Response(JSON.stringify(dados));
afterEach(() => vi.unstubAllGlobals());
function servidor(erros = false, incerta = false, confirmar?: () => Promise<Response>) {
  let primeiro = true;
  const escritas: RequestInit[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: string, opcoes?: RequestInit) => {
      if (url.endsWith("/previa"))
        return json({
          ...previa,
          erros: erros ? [{ linha: 3, mensagem: "Ativo não reconhecido." }] : [],
        });
      if (opcoes?.method === "POST") {
        escritas.push(opcoes);
        if (confirmar) return confirmar();
        if (incerta && primeiro) {
          primeiro = false;
          throw new TypeError("rede");
        }
        return json(recibo);
      }
      if (url.endsWith("/i1")) return json(recibo);
      return json({ itens: [recibo], total: 1, pagina: 0, tamanho: 20 });
    }),
  );
  return escritas;
}
async function preparar() {
  fireEvent.change(screen.getByLabelText("Arquivo de operações"), {
    target: { files: [new File(["fixture sintética"], "teste.csv")] },
  });
  fireEvent.click(screen.getByLabelText(/Os depósitos são aportes próprios/));
  fireEvent.click(screen.getByLabelText(/O total das compras OCBS inclui as taxas/));
  fireEvent.click(screen.getByRole("button", { name: "Analisar arquivo" }));
  await screen.findByRole("region", { name: "Prévia da importação" });
}
it("envia multipart sem forçar content-type e confirma somente após prévia", async () => {
  const escritas = servidor();
  render(<Importacoes onBloqueio={() => {}} />);
  expect(screen.getByRole("button", { name: "Analisar arquivo" })).toBeDisabled();
  await preparar();
  expect(escritas).toHaveLength(0);
  fireEvent.click(screen.getByRole("button", { name: "Confirmar importação" }));
  await screen.findByText("Importação concluída.");
  expect(escritas[0].body).toBeInstanceOf(FormData);
  expect((escritas[0].body as FormData).get("hash")).toBe("abc");
  expect((escritas[0].body as FormData).get("aportesProprios")).toBe("true");
  expect(new Headers(escritas[0].headers).has("Content-Type")).toBe(false);
});
it("bloqueia o lote inteiro com erro de linha e preserva o arquivo", async () => {
  servidor(true);
  render(<Importacoes onBloqueio={() => {}} />);
  await preparar();
  expect(screen.getByText(/Ativo não reconhecido/)).toBeVisible();
  expect(screen.getByRole("button", { name: "Confirmar importação" })).toBeDisabled();
  expect(screen.getByLabelText("Arquivo de operações")).toHaveProperty(
    "files",
    expect.arrayContaining([expect.objectContaining({ name: "teste.csv" })]),
  );
});
it("preserva corpo e chave após resultado incerto e impede edição e saída", async () => {
  const escritas = servidor(false, true);
  const bloquear = vi.fn();
  render(<Importacoes onBloqueio={bloquear} />);
  await preparar();
  fireEvent.click(screen.getByRole("button", { name: "Confirmar importação" }));
  await screen.findByRole("button", { name: "Verificar solicitação" });
  expect(screen.getByLabelText("Arquivo de operações")).toBeDisabled();
  expect(bloquear).toHaveBeenLastCalledWith(true);
  const saida = new Event("beforeunload", { cancelable: true });
  window.dispatchEvent(saida);
  expect(saida.defaultPrevented).toBe(true);
  fireEvent.click(screen.getByRole("button", { name: "Verificar solicitação" }));
  await screen.findByText("Importação concluída.");
  expect(escritas[0].body).toBe(escritas[1].body);
  expect(new Headers(escritas[0].headers).get("Idempotency-Key")).toBe(
    new Headers(escritas[1].headers).get("Idempotency-Key"),
  );
});
it("consulta detalhes de importação anterior", async () => {
  servidor();
  render(<Importacoes onBloqueio={() => {}} />);
  fireEvent.click(await screen.findByRole("button", { name: "Detalhes de teste.csv" }));
  expect(await screen.findByRole("dialog")).toHaveTextContent("o1");
});

it("protege edição e saída desde o primeiro envio pendente", async () => {
  let resolver: ((resposta: Response) => void) | undefined;
  servidor(
    false,
    false,
    () =>
      new Promise<Response>((resolve) => {
        resolver = resolve;
      }),
  );
  render(<Importacoes onBloqueio={() => {}} />);
  await preparar();
  fireEvent.click(screen.getByRole("button", { name: "Confirmar importação" }));
  expect(screen.getByRole("button", { name: "Confirmando…" })).toBeDisabled();
  expect(screen.getByLabelText("Arquivo de operações")).toBeDisabled();
  const saida = new Event("beforeunload", { cancelable: true });
  window.dispatchEvent(saida);
  expect(saida.defaultPrevented).toBe(true);
  await act(async () => resolver?.(json(recibo)));
  await screen.findByText("Importação concluída.");
  const concluida = new Event("beforeunload", { cancelable: true });
  window.dispatchEvent(concluida);
  expect(concluida.defaultPrevented).toBe(false);
});
it("exibe cinco mil eventos em páginas compactas com unidades e rótulos de produto", () => {
  const dados: PreviaImportacao = {
    ...previa,
    eventos: Array.from({ length: 5000 }, (_, indice) => ({
      ...evento,
      situacao: "NOVO",
      linha: indice + 2,
      referencia: `sintetica-${indice}`,
    })),
  };
  render(<PreviaArquivo previa={dados} />);
  const tabela = screen.getByRole("table");
  expect(within(tabela).getAllByRole("row")).toHaveLength(21);
  expect(within(tabela).getByRole("columnheader", { name: "Principal (BRL)" })).toBeVisible();
  expect(within(tabela).getAllByText("Aporte")).toHaveLength(20);
  expect(screen.queryByText("sintetica-20")).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Próxima" }));
  expect(screen.getByText("sintetica-20")).toBeVisible();
  expect(screen.queryByText("sintetica-0")).not.toBeInTheDocument();
});
it("invalida a prévia imediatamente ao mudar uma declaração", async () => {
  servidor();
  render(<Importacoes onBloqueio={() => {}} />);
  await preparar();
  fireEvent.click(screen.getByLabelText(/Não havia posição anterior/));
  expect(screen.queryByRole("region", { name: "Prévia da importação" })).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Confirmar importação" })).not.toBeInTheDocument();
});

it("mantém recibos grandes compactos sem ocultar operações", () => {
  render(
    <ReciboArquivo
      recibo={{
        ...recibo,
        eventos: Array.from({ length: 5000 }, (_, indice) => ({
          linha: indice + 2,
          referencia: `ref-${indice}`,
          operacaoId: `operacao-${indice}`,
        })),
      }}
    />,
  );
  expect(within(screen.getByRole("table")).getAllByRole("row")).toHaveLength(21);
  expect(screen.queryByText("operacao-20")).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Próxima" }));
  expect(screen.getByText("operacao-20")).toBeVisible();
});

it("orienta e seleciona o formato CSV aceito pelo backend", async () => {
  servidor();
  render(<Importacoes onBloqueio={() => {}} />);
  expect(screen.getByLabelText("Arquivo de operações")).toHaveAttribute("accept", ".csv,text/csv");
  expect(
    screen.getByText(/relatório enriquecido de eventos da Binance em CSV/i),
  ).toBeInTheDocument();
});
