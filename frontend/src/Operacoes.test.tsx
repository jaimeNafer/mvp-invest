import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import Operacoes from "./SaldosIniciais";
import SaldoInicialForm from "./SaldoInicialForm";
import type { Ativo, Saldo } from "./services/saldosIniciais";

const ativos: Ativo[] = [
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
];
const instituicoes = [{ id: "1", nome: "Binance", ativo: true }];
const base: Saldo = {
  id: "10",
  instituicaoId: "1",
  instituicaoNome: "Binance",
  ativoId: "2",
  ativoTicker: "BTC",
  dataReferencia: "2026-10-01",
  quantidade: "0.01",
  custoTotalBrl: "4000",
  situacao: "CONFIRMADA",
  substituiId: null,
  justificativaCustoZero: null,
  motivoAlteracao: null,
  criadoEm: "2026-10-02T03:00:00Z",
  alteradoEm: "2026-10-02T03:00:00Z",
};
const json = (data: unknown, status = 200) => new Response(JSON.stringify(data), { status });
afterEach(() => vi.unstubAllGlobals());

function servidor(falha?: "http" | "rede", inicial: Saldo[] = []) {
  let registros = [...inicial];
  let primeira = true;
  const fetchMock = vi.fn(async (url: string, opcoes?: RequestInit) => {
    if (url.endsWith("/ativos")) return json(ativos);
    if (url.endsWith("/instituicoes")) return json(instituicoes);
    if (url.endsWith("/carteiras"))
      return json([{ id: "1", nome: "Principal", ativo: true, contas: [] }]);
    if (url.endsWith("/posicoes-iniciais")) return json([]);
    if (opcoes?.method === "POST") {
      const e = JSON.parse(String(opcoes.body));
      if (falha === "http" && primeira) {
        primeira = false;
        return json({ detail: "Revise o custo informado." }, 400);
      }
      if (url.endsWith("/cancelamento")) {
        registros = registros.map((s) => ({
          ...s,
          situacao: "CANCELADA",
          motivoAlteracao: e.motivo,
        }));
        return json(registros[0]);
      }
      const criado = { ...base, ...e };
      registros = [criado];
      if (falha === "rede" && primeira) {
        primeira = false;
        throw new TypeError("Falha de rede após confirmação");
      }
      return json(criado, 201);
    }
    if (url.includes("/saldos-iniciais?"))
      return json({
        itens: registros,
        pagina: 0,
        tamanho: 50,
        total: registros.length,
        dataReferencia: registros.length ? base.dataReferencia : null,
        possuiSaldosConfirmados: registros.some((s) => s.situacao === "CONFIRMADA"),
      });
    return json(registros[0]);
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

async function preencher() {
  await screen.findByText("Nenhum saldo inicial encontrado.");
  fireEvent.click(screen.getByRole("button", { name: "Adicionar saldo inicial" }));
  fireEvent.change(screen.getByRole("combobox", { name: "Instituição" }), {
    target: { value: "1" },
  });
  fireEvent.change(screen.getByRole("combobox", { name: "Ativo" }), {
    target: { value: "2" },
  });
  fireEvent.change(screen.getByLabelText("Data de início do acompanhamento"), {
    target: { value: "2026-10-01" },
  });
  fireEvent.change(screen.getByLabelText("Quantidade"), { target: { value: "0,01" } });
  fireEvent.change(screen.getByLabelText("Custo histórico total em reais"), {
    target: { value: "4.000,00" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Revisar dados" }));
}

describe("operações e saldo inicial", () => {
  it("revisa antes de gravar e envia decimais em strings com chave única", async () => {
    const fetchMock = servidor();
    render(<Operacoes />);
    await preencher();
    expect(fetchMock.mock.calls.filter(([, o]) => o?.method === "POST")).toHaveLength(0);
    fireEvent.click(screen.getByRole("button", { name: "Confirmar saldo inicial" }));
    await screen.findByText("Saldo inicial confirmado.");
    const envio = fetchMock.mock.calls.find(([, o]) => o?.method === "POST");
    expect(JSON.parse(String(envio?.[1]?.body))).toMatchObject({
      quantidade: "0.01",
      custoTotalBrl: "4000.00",
    });
    expect(new Headers(envio?.[1]?.headers).get("Idempotency-Key")).toMatch(/^[a-f0-9-]{36}$/);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(await screen.findByRole("button", { name: "Corrigir saldo 10" })).toBeEnabled();
  });

  it("preserva os campos após rejeição e permite revisão", async () => {
    servidor("http");
    render(<Operacoes />);
    await preencher();
    fireEvent.click(screen.getByRole("button", { name: "Confirmar saldo inicial" }));
    await screen.findByText("Revise o custo informado.");
    fireEvent.click(screen.getByRole("button", { name: "Voltar" }));
    expect(screen.getByLabelText("Quantidade")).toHaveValue("0,01");
    expect(screen.getByLabelText("Custo histórico total em reais")).toHaveValue("4.000,00");
  });

  it("retoma envio incerto com a mesma chave e o mesmo comando após fechar a modal", async () => {
    const fetchMock = servidor("rede");
    render(<Operacoes />);
    await preencher();
    fireEvent.click(screen.getByRole("button", { name: "Confirmar saldo inicial" }));
    await screen.findByText(/resultado do envio ainda não foi confirmado/);
    fireEvent.click(screen.getByRole("button", { name: "Consultar listagem" }));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(await screen.findByRole("button", { name: "Adicionar saldo inicial" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Verificar solicitação" }));
    const dialogo = screen.getByRole("dialog");
    fireEvent.click(within(dialogo).getByRole("button", { name: "Verificar solicitação" }));
    await screen.findByText("Saldo inicial confirmado.");
    const envios = fetchMock.mock.calls.filter(([, o]) => o?.method === "POST");
    expect(envios).toHaveLength(2);
    expect(envios[0][1]?.body).toBe(envios[1][1]?.body);
    expect(new Headers(envios[0][1]?.headers).get("Idempotency-Key")).toBe(
      new Headers(envios[1][1]?.headers).get("Idempotency-Key"),
    );
  });

  it("permite correção com motivo e cancelamento preservado no histórico", async () => {
    const fetchMock = servidor(undefined, [base]);
    render(<Operacoes />);
    fireEvent.click(await screen.findByRole("button", { name: "Corrigir saldo 10" }));
    expect(screen.getByLabelText("Data de início do acompanhamento")).toBeDisabled();
    fireEvent.change(screen.getByLabelText("Custo histórico total em reais"), {
      target: { value: "4.200" },
    });
    fireEvent.change(screen.getByLabelText("Motivo da correção"), {
      target: { value: "Extrato conferido" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Revisar dados" }));
    fireEvent.click(screen.getByRole("button", { name: "Confirmar correção" }));
    await screen.findByText(/Saldo inicial corrigido/);
    expect(
      fetchMock.mock.calls.some(
        ([u, o]) =>
          u.endsWith("/10/substituicoes") &&
          JSON.parse(String(o?.body)).motivo === "Extrato conferido",
      ),
    ).toBe(true);
    fireEvent.click(await screen.findByRole("button", { name: "Cancelar saldo 10" }));
    fireEvent.change(screen.getByLabelText("Motivo do cancelamento"), {
      target: { value: "Revisão" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Confirmar cancelamento" }));
    await screen.findByText(/Saldo inicial cancelado/);
  });

  it("calcula custo BRL pela quantidade e exige confirmação para custo zero", () => {
    const salvar = vi.fn();
    render(
      <SaldoInicialForm
        ativos={ativos}
        instituicoes={instituicoes}
        saldo={null}
        dataReferencia={null}
        possuiConfirmados={false}
        salvando={false}
        erro=""
        pendente={null}
        onSalvar={salvar}
        onCancelar={() => {}}
      />,
    );
    expect(screen.getByLabelText("Data de início do acompanhamento")).toHaveValue("");
    fireEvent.change(screen.getByLabelText("Instituição"), { target: { value: "1" } });
    fireEvent.change(screen.getByLabelText("Ativo"), { target: { value: "1" } });
    fireEvent.change(screen.getByLabelText("Quantidade"), { target: { value: "1.234,56" } });
    expect(screen.getByLabelText("Custo histórico total em reais")).toHaveValue("1.234,56");
    expect(screen.getByLabelText("Custo histórico total em reais")).toHaveAttribute("readonly");
    fireEvent.change(screen.getByLabelText("Ativo"), { target: { value: "2" } });
    fireEvent.change(screen.getByLabelText("Data de início do acompanhamento"), {
      target: { value: "2026-10-01" },
    });
    fireEvent.change(screen.getByLabelText("Custo histórico total em reais"), {
      target: { value: "0" },
    });
    fireEvent.submit(screen.getByRole("form"));
    expect(screen.getByRole("alert")).toHaveTextContent("Confirme o custo zero");
    expect(salvar).not.toHaveBeenCalled();
  });
});
