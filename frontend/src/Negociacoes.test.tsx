import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import Operacoes from "./Operacoes";

const ativos = [
  {
    id: "1",
    codigo: "fiat:BRL",
    ticker: "BRL",
    nome: "Real",
    tipo: "FIAT",
    casasDecimais: 2,
    ativo: true,
  },
  {
    id: "2",
    codigo: "cripto:BTC",
    ticker: "BTC",
    nome: "Bitcoin",
    tipo: "CRIPTO",
    casasDecimais: 18,
    ativo: true,
  },
  {
    id: "3",
    codigo: "cripto:USDT",
    ticker: "USDT",
    nome: "Tether",
    tipo: "CRIPTO",
    casasDecimais: 18,
    ativo: true,
  },
];
const calculo = {
  valorBrutoBrl: "200",
  taxaBrl: "2",
  custoBaixadoBrl: "100",
  resultadoNegociacaoBrl: "98",
  resultadoMoedaBrl: "-3",
  resultadoTaxaBrl: "-1",
  resultadoTotalBrl: "94",
};
const operacao = {
  id: "9007199254740993",
  tipo: "COMPRA",
  instituicaoId: "1",
  instituicaoNome: "Binance",
  ativoId: "2",
  ativoTicker: "BTC",
  ocorridaEm: "2026-10-02T12:30:00Z",
  quantidade: "0.01",
  custoTotalBrl: null,
  moedaCotacaoId: "1",
  moedaCotacaoTicker: "BRL",
  totalBruto: "200",
  precoUnitario: "20000.123456789012345678901234567890123456",
  cotacaoBrl: "1",
  taxaAtivoId: null,
  taxaAtivoTicker: null,
  taxaQuantidade: "0",
  referencia: "Extrato",
  situacao: "CONFIRMADA",
  substituiId: null,
  motivoAlteracao: null,
  criadoEm: "2026-10-02T12:30:00Z",
  alteradoEm: "2026-10-02T12:30:00Z",
  calculo,
};
const posicao = {
  ativoId: "2",
  ativoTicker: "BTC",
  quantidade: "0.01",
  custoTotalBrl: "200",
  custoMedioBrl: "20000",
  instituicoes: [{ instituicaoId: "1", instituicaoNome: "Binance", quantidade: "0.01" }],
};
const json = (data: unknown, status = 200) => new Response(JSON.stringify(data), { status });
type Requisicao = { url: string; opcoes?: RequestInit; body: Record<string, unknown> };
function servidor(
  config: {
    falha?: "rede" | "revisao" | "validacao";
    previa?: (r: Requisicao) => Promise<Response>;
    confirmar?: (r: Requisicao) => Promise<Response>;
    itens?: unknown[];
    detalhe?: unknown;
    revisoes?: { operacoes: string; posicoes: string };
  } = {},
) {
  const requisicoes: Requisicao[] = [];
  let primeira = true;
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: string, opcoes?: RequestInit) => {
      const r = { url, opcoes, body: opcoes?.body ? JSON.parse(String(opcoes.body)) : {} };
      requisicoes.push(r);
      if (url.endsWith("/ativos")) return json(ativos);
      if (url.endsWith("/instituicoes")) return json([{ id: "1", nome: "Binance", ativo: true }]);
      if (url.endsWith("/carteiras"))
        return json([{ id: "50", nome: "Principal", ativo: true, contas: [] }]);
      if (url.endsWith("/posicoes"))
        return json({ revisao: config.revisoes?.posicoes ?? "7", itens: [posicao] });
      if (url.endsWith("/previa"))
        return config.previa
          ? config.previa(r)
          : json({ revisao: "7", operacao, calculo, posicoes: [posicao] });
      if (opcoes?.method === "POST") {
        if (config.confirmar) return config.confirmar(r);
        if (primeira && config.falha) {
          primeira = false;
          if (config.falha === "rede") throw new TypeError("timeout");
          return json(
            {
              detail:
                config.falha === "revisao"
                  ? "A posição mudou. Confira a prévia atualizada."
                  : "Saldo insuficiente na instituição.",
            },
            config.falha === "revisao" ? 409 : 400,
          );
        }
        return json(operacao, 201);
      }
      if (url.includes("/operacoes?"))
        return json({
          itens: config.itens ?? [],
          total: config.itens?.length ?? 0,
          pagina: 0,
          tamanho: 50,
          revisao: config.revisoes?.operacoes ?? "7",
          dataReferencia: "2026-10-01",
        });
      if (url.endsWith(`/operacoes/${operacao.id}`)) return json(config.detalhe ?? operacao);
      if (url.includes("/saldos-iniciais?"))
        return json({
          itens: [],
          total: 0,
          pagina: 0,
          tamanho: 50,
          dataReferencia: null,
          possuiSaldosConfirmados: false,
        });
      if (url.endsWith("/posicoes-iniciais")) return json([]);
      throw new Error(`Rota não prevista: ${url}`);
    }),
  );
  return requisicoes;
}
afterEach(() => vi.unstubAllGlobals());
async function preencher(tipo = "COMPRA") {
  fireEvent.click(await screen.findByRole("button", { name: "Adicionar operação" }));
  fireEvent.change(screen.getByLabelText("Tipo de operação"), { target: { value: tipo } });
  fireEvent.change(screen.getByRole("combobox", { name: "Instituição" }), {
    target: { value: "1" },
  });
  if (tipo !== "APORTE")
    fireEvent.change(screen.getByRole("combobox", { name: "Ativo" }), {
      target: { value: "2" },
    });
  fireEvent.change(screen.getByLabelText("Data e hora (Fortaleza, UTC−03:00)"), {
    target: { value: "2026-10-02T09:30" },
  });
  fireEvent.change(screen.getByLabelText("Quantidade"), { target: { value: "0,01" } });
  if (tipo !== "APORTE") {
    fireEvent.change(screen.getByLabelText("Moeda de cotação"), { target: { value: "1" } });
    fireEvent.change(screen.getByLabelText("Total bruto"), { target: { value: "200" } });
  }
}
const escritas = (r: Requisicao[]) =>
  r.filter((r) => r.opcoes?.method === "POST" && !r.url.endsWith("/previa"));

describe("ciclo de negociações", () => {
  it("não publica histórico e posições de revisões diferentes e permite atualizar explicitamente", async () => {
    const revisoes = { operacoes: "2", posicoes: "1" };
    const r = servidor({ revisoes, itens: [operacao] });
    render(<Operacoes />);
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Seu histórico foi atualizado durante a consulta.",
    );
    expect(screen.queryByRole("region", { name: "Posição atual global" })).not.toBeInTheDocument();
    expect(
      screen.queryByRole("region", { name: "Histórico de operações" }),
    ).not.toBeInTheDocument();
    expect(r.filter((r) => r.url.includes("/operacoes?"))).toHaveLength(1);
    expect(r.filter((r) => r.url.endsWith("/posicoes"))).toHaveLength(1);
    revisoes.posicoes = "2";
    fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));
    expect(await screen.findByRole("region", { name: "Posição atual global" })).toBeVisible();
    expect(
      screen.getByRole("button", { name: `Detalhes da operação ${operacao.id}` }),
    ).toBeEnabled();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(r.filter((r) => r.url.includes("/operacoes?"))).toHaveLength(2);
  });

  it("oferece atualização quando as revisões divergem após uma consulta anterior coerente", async () => {
    const revisoes = { operacoes: "1", posicoes: "1" };
    servidor({ revisoes, itens: [operacao] });
    render(<Operacoes />);
    await screen.findByRole("region", { name: "Posição atual global" });
    revisoes.operacoes = "2";
    fireEvent.change(screen.getByLabelText("Filtrar ativo"), { target: { value: "2" } });
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Seu histórico foi atualizado durante a consulta.",
    );
    expect(screen.queryByRole("region", { name: "Posição atual global" })).not.toBeInTheDocument();
    revisoes.posicoes = "2";
    fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));
    expect(await screen.findByRole("region", { name: "Posição atual global" })).toBeVisible();
    expect(screen.getByLabelText("Filtrar ativo")).toHaveValue("2");
  });

  it("protege a saída enquanto a primeira confirmação está pendente e remove a proteção após sucesso", async () => {
    let resolver: ((resposta: Response) => void) | undefined;
    servidor({
      confirmar: () =>
        new Promise<Response>((resolve) => {
          resolver = resolve;
        }),
    });
    render(<Operacoes />);
    await preencher();
    await screen.findByRole("region", { name: "Prévia da operação" });
    fireEvent.click(screen.getByRole("button", { name: "Confirmar operação" }));
    await waitFor(() => expect(resolver).toBeDefined());
    expect(screen.getByRole("button", { name: "Confirmando…" })).toBeDisabled();
    const saidaPendente = new Event("beforeunload", { cancelable: true });
    window.dispatchEvent(saidaPendente);
    expect(saidaPendente.defaultPrevented).toBe(true);
    await act(async () => resolver?.(json(operacao, 201)));
    await screen.findByText("Operação confirmada.");
    const saidaConcluida = new Event("beforeunload", { cancelable: true });
    window.dispatchEvent(saidaConcluida);
    expect(saidaConcluida.defaultPrevented).toBe(false);
  });

  it("apresenta preço unitário recebido na prévia com todas as casas decimais", async () => {
    servidor();
    render(<Operacoes />);
    await preencher();
    const previa = await screen.findByRole("region", { name: "Prévia da operação" });
    expect(
      within(previa).getByText("20.000,123456789012345678901234567890123456 BRL"),
    ).toBeVisible();
  });

  it("registra aporte como BRL sem enviar campos residuais de negociação", async () => {
    const r = servidor();
    render(<Operacoes />);
    await preencher();
    fireEvent.change(screen.getByLabelText("Tipo de operação"), { target: { value: "APORTE" } });
    fireEvent.change(screen.getByLabelText("Quantidade"), { target: { value: "1.000,15" } });
    expect(screen.queryByLabelText("Total bruto")).not.toBeInTheDocument();
    await screen.findByRole("region", { name: "Prévia da operação" });
    fireEvent.click(screen.getByRole("button", { name: "Confirmar operação" }));
    await screen.findByText("Operação confirmada.");
    expect(escritas(r)[0].body).toEqual({
      revisao: "7",
      operacao: {
        tipo: "APORTE",
        instituicaoId: "1",
        ativoId: "1",
        ocorridaEm: "2026-10-02T09:30:00-03:00",
        quantidade: "1000.15",
      },
    });
  });

  it.each(["2", "3"])(
    "envia venda cotada em stablecoin e taxa na moeda %s sem converter para float",
    async (taxaAtivoId) => {
      const r = servidor();
      render(<Operacoes />);
      await preencher("VENDA");
      fireEvent.change(screen.getByLabelText("Moeda de cotação"), { target: { value: "3" } });
      fireEvent.change(screen.getByLabelText("Cotação histórica em reais por unidade"), {
        target: { value: "5,123456789012345678" },
      });
      fireEvent.change(screen.getByLabelText("Moeda da taxa"), { target: { value: taxaAtivoId } });
      fireEvent.change(screen.getByLabelText("Quantidade da taxa"), {
        target: { value: "0,000000000000000001" },
      });
      await screen.findByRole("region", { name: "Prévia da operação" });
      fireEvent.click(screen.getByRole("button", { name: "Confirmar operação" }));
      await screen.findByText("Operação confirmada.");
      expect(escritas(r)[0].body.operacao).toMatchObject({
        tipo: "VENDA",
        moedaCotacaoId: "3",
        cotacaoBrl: "5.123456789012345678",
        taxaAtivoId,
        taxaQuantidade: "0.000000000000000001",
      });
    },
  );

  it("permite apenas detalhes em registros históricos e encaminha aberturas para saldos iniciais", async () => {
    const abertura = {
      ...operacao,
      tipo: "SALDO_INICIAL",
      moedaCotacaoId: null,
      moedaCotacaoTicker: null,
      totalBruto: null,
      cotacaoBrl: null,
      calculo: null,
    };
    const historica = { ...operacao, id: "31", situacao: "CANCELADA", calculo: null };
    servidor({ itens: [abertura, historica], detalhe: abertura });
    render(<Operacoes />);
    const detalhe = await screen.findByRole("button", {
      name: `Detalhes da operação ${operacao.id}`,
    });
    expect(screen.queryByRole("button", { name: /Corrigir operação/ })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Cancelar operação/ })).not.toBeInTheDocument();
    fireEvent.click(detalhe);
    fireEvent.click(await screen.findByRole("button", { name: "Gerenciar saldos iniciais" }));
    expect(await screen.findByRole("button", { name: "Adicionar saldo inicial" })).toBeEnabled();
  });

  it("consulta a prévia sem gravar e confirma com revisão, UUID, decimais e fuso explícito", async () => {
    const r = servidor();
    render(<Operacoes />);
    await preencher();
    expect(screen.getByRole("button", { name: "Confirmar operação" })).toBeDisabled();
    await screen.findByRole("region", { name: "Prévia da operação" });
    expect(escritas(r)).toHaveLength(0);
    expect(screen.getByText("R$ -3,00")).toBeVisible();
    expect(screen.getByText("R$ -1,00")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Confirmar operação" }));
    await screen.findByText("Operação confirmada.");
    expect(escritas(r)[0].body).toEqual({
      revisao: "7",
      operacao: {
        tipo: "COMPRA",
        instituicaoId: "1",
        ativoId: "2",
        ocorridaEm: "2026-10-02T09:30:00-03:00",
        quantidade: "0.01",
        moedaCotacaoId: "1",
        totalBruto: "200",
        cotacaoBrl: "1",
      },
    });
    expect(new Headers(escritas(r)[0].opcoes?.headers).get("Idempotency-Key")).toMatch(
      /^[a-f0-9-]{36}$/,
    );
  });

  it("invalida imediatamente a prévia ao editar e descarta resposta antiga", async () => {
    let resolver: ((r: Response) => void) | undefined;
    const r = servidor({
      previa: async () =>
        new Promise<Response>((resolve) => {
          resolver = resolve;
        }),
    });
    render(<Operacoes />);
    await preencher();
    await waitFor(() => expect(resolver).toBeDefined());
    fireEvent.change(screen.getByLabelText("Quantidade"), { target: { value: "" } });
    await act(async () => resolver?.(json({ revisao: "7", operacao, calculo, posicoes: [] })));
    expect(screen.getByRole("button", { name: "Confirmar operação" })).toBeDisabled();
    expect(screen.queryByRole("region", { name: "Prévia da operação" })).not.toBeInTheDocument();
    expect(escritas(r)).toHaveLength(0);
  });

  it("repete comando e chave após rede incerta e protege o formulário contra perda", async () => {
    const r = servidor({ falha: "rede" });
    render(<Operacoes />);
    await preencher();
    await screen.findByRole("region", { name: "Prévia da operação" });
    fireEvent.click(screen.getByRole("button", { name: "Confirmar operação" }));
    await screen.findByText(/resultado do envio ainda não foi confirmado/);
    expect(screen.getByLabelText("Quantidade")).toBeDisabled();
    expect(screen.getByRole("button", { name: "Fechar modal" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Verificar solicitação" }));
    await screen.findByText("Operação confirmada.");
    expect(escritas(r)).toHaveLength(2);
    expect(escritas(r)[0].opcoes?.body).toBe(escritas(r)[1].opcoes?.body);
    expect(new Headers(escritas(r)[0].opcoes?.headers).get("Idempotency-Key")).toBe(
      new Headers(escritas(r)[1].opcoes?.headers).get("Idempotency-Key"),
    );
  });

  it("preserva campos e exige nova confirmação quando a revisão muda", async () => {
    const r = servidor({ falha: "revisao" });
    render(<Operacoes />);
    await preencher();
    await screen.findByRole("region", { name: "Prévia da operação" });
    fireEvent.click(screen.getByRole("button", { name: "Confirmar operação" }));
    await screen.findByText("A posição mudou. Confira a prévia atualizada.");
    expect(screen.getByLabelText("Quantidade")).toHaveValue("0,01");
    await waitFor(() => expect(r.filter((r) => r.url.endsWith("/previa"))).toHaveLength(2));
    expect(escritas(r)).toHaveLength(1);
    await waitFor(() =>
      expect(screen.getByRole("button", { name: "Confirmar operação" })).toBeEnabled(),
    );
    fireEvent.click(screen.getByRole("button", { name: "Confirmar operação" }));
    await screen.findByText("Operação confirmada.");
  });

  it("mantém os dados após rejeição e permite corrigir sem reutilizar prévia", async () => {
    servidor({ falha: "validacao" });
    render(<Operacoes />);
    await preencher();
    await screen.findByRole("region", { name: "Prévia da operação" });
    fireEvent.click(screen.getByRole("button", { name: "Confirmar operação" }));
    await screen.findByText("Saldo insuficiente na instituição.");
    expect(screen.getByLabelText("Total bruto")).toHaveValue("200");
    fireEvent.change(screen.getByLabelText("Total bruto"), { target: { value: "100" } });
    expect(screen.getByRole("button", { name: "Confirmar operação" })).toBeDisabled();
    await screen.findByRole("region", { name: "Prévia da operação" });
  });

  it("filtra por ativo e carteira e apresenta posição global com custódia", async () => {
    const r = servidor({ itens: [operacao] });
    render(<Operacoes />);
    const posicoes = await screen.findByRole("region", { name: "Posição atual global" });
    expect(within(posicoes).getByText("R$ 20.000,00")).toBeVisible();
    expect(within(posicoes).getByText(/Binance: 0,01 BTC/)).toBeVisible();
    fireEvent.change(screen.getByLabelText("Filtrar ativo"), { target: { value: "2" } });
    fireEvent.click(await screen.findByLabelText("Principal"));
    await waitFor(() =>
      expect(r.some((r) => r.url.includes("ativoId=2") && r.url.includes("carteiraIds=50"))).toBe(
        true,
      ),
    );
    fireEvent.click(
      await screen.findByRole("button", { name: `Detalhes da operação ${operacao.id}` }),
    );
    await waitFor(() => expect(screen.getByText("R$ 94,00")).toBeVisible());
  });

  it("corrige e cancela negociações com motivo e prévia obrigatória", async () => {
    const r = servidor({ itens: [operacao] });
    render(<Operacoes />);
    fireEvent.click(
      await screen.findByRole("button", { name: `Corrigir operação ${operacao.id}` }),
    );
    expect(screen.getByLabelText("Quantidade")).toHaveValue("0,01");
    fireEvent.change(screen.getByLabelText("Motivo da correção"), {
      target: { value: "Extrato conferido" },
    });
    await screen.findByRole("region", { name: "Prévia da operação" });
    fireEvent.click(screen.getByRole("button", { name: "Confirmar correção" }));
    await screen.findByText("Operação corrigida. A versão anterior foi preservada.");
    expect(escritas(r)[0].url).toContain(`/${operacao.id}/substituicoes`);
    fireEvent.click(
      await screen.findByRole("button", { name: `Cancelar operação ${operacao.id}` }),
    );
    fireEvent.change(screen.getByLabelText("Motivo do cancelamento"), {
      target: { value: "Lançamento duplicado" },
    });
    await screen.findByRole("region", { name: "Prévia da operação" });
    fireEvent.click(screen.getByRole("button", { name: "Confirmar cancelamento" }));
    await screen.findByText("Operação cancelada. O registro foi preservado no histórico.");
    expect(escritas(r)[1].body).toEqual({ revisao: "7", motivo: "Lançamento duplicado" });
  });
});
