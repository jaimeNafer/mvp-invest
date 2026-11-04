import { render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import CamposOperacao from "./CamposOperacao";
import SaldoInicialForm from "./SaldoInicialForm";
import type { CamposOperacao as Campos } from "./services/cadastroOperacao";
import type { Ativo } from "./services/saldosIniciais";

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
    codigo: "fiat:USD",
    ticker: "USD",
    nome: "Dólar",
    tipo: "FIAT",
    casasDecimais: 2,
    ativo: true,
  },
  {
    id: "3",
    codigo: "cripto:TESTE",
    ticker: "TESTE",
    nome: "Cripto de teste",
    tipo: "CRIPTO",
    casasDecimais: 18,
    ativo: true,
  },
  {
    id: "4",
    codigo: "cripto:INATIVO",
    ticker: "INATIVO",
    nome: "Cripto inativa",
    tipo: "CRIPTO",
    casasDecimais: 18,
    ativo: false,
  },
  {
    id: "5",
    codigo: "fiat:JPY",
    ticker: "JPY",
    nome: "Iene",
    tipo: "FIAT",
    casasDecimais: 2,
    ativo: true,
  },
  {
    id: "6",
    codigo: "cripto:USDC",
    ticker: "USDC",
    nome: "USD Coin",
    tipo: "CRIPTO",
    casasDecimais: 18,
    ativo: true,
  },
];
const campos: Campos = {
  tipo: "COMPRA",
  instituicaoId: "1",
  ativoId: "3",
  data: "2026-10-01T10:00",
  quantidade: "1",
  moedaCotacaoId: "1",
  totalBruto: "100",
  cotacaoBrl: "1",
  taxaAtivoId: "",
  taxaQuantidade: "",
  referencia: "",
  motivo: "",
};

describe("ativos disponíveis pelo catálogo", () => {
  it("oferece criptoativo adicional sem lista fixa e mantém inativo desabilitado", () => {
    render(
      <CamposOperacao
        campos={campos}
        ativos={ativos}
        instituicoes={[]}
        cancelar={false}
        corrigindo={false}
        bloqueado={false}
        alterar={vi.fn()}
      />,
    );
    const bases = within(screen.getByLabelText("Ativo"));
    expect(bases.getByRole("option", { name: "TESTE" })).toBeEnabled();
    expect(bases.getByRole("option", { name: "INATIVO (inativo)" })).toBeDisabled();
    expect(bases.queryByRole("option", { name: "USD" })).not.toBeInTheDocument();
    const cotacoes = within(screen.getByLabelText("Moeda de cotação"));
    expect(cotacoes.getByRole("option", { name: "BRL" })).toBeEnabled();
    expect(cotacoes.getByRole("option", { name: "USDC" })).toBeEnabled();
    expect(cotacoes.queryByRole("option", { name: "TESTE" })).not.toBeInTheDocument();
    expect(cotacoes.queryByRole("option", { name: "USD" })).not.toBeInTheDocument();
  });

  it("saldo inicial oferece criptos do catálogo e somente BRL e USD entre moedas fiduciárias", () => {
    render(
      <SaldoInicialForm
        ativos={ativos}
        instituicoes={[]}
        saldo={null}
        dataReferencia={null}
        possuiConfirmados={false}
        salvando={false}
        erro=""
        pendente={null}
        onSalvar={vi.fn()}
        onCancelar={vi.fn()}
      />,
    );
    const options = within(screen.getByLabelText("Ativo"));
    expect(options.getByRole("option", { name: "TESTE — Cripto de teste" })).toBeEnabled();
    expect(options.getByRole("option", { name: "USD — Dólar" })).toBeEnabled();
    expect(options.queryByRole("option", { name: "JPY — Iene" })).not.toBeInTheDocument();
    expect(
      options.getByRole("option", { name: "INATIVO — Cripto inativa — inativo" }),
    ).toBeDisabled();
  });
});
