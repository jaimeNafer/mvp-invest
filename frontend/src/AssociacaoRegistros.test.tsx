import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import AssociacaoRegistros from "./AssociacaoRegistros";
import type { GrupoAssociacaoResponse, LinhaAssociacaoResponse } from "./services/associacoes";

const linha: LinhaAssociacaoResponse = {
  id: "registro:1",
  versao: "1",
  data: "2026-11-06T12:30:45.123456Z",
  instituicaoId: "binance",
  contaId: "conta:1",
  ativoTicker: "BTC",
  quantidade: "0.1",
  operacao: "TRADE",
  referencia: "ordem-1",
  papel: "NAO_DEFINIDO",
  grupoNumero: null,
  arquivo: "trades.csv",
  linha: 2,
  aba: "Trades",
};

const pagamento: LinhaAssociacaoResponse = {
  ...linha,
  id: "registro:2",
  ativoTicker: "BRL",
  quantidade: "-100",
  papel: "PAGAMENTO",
};

const grupo: GrupoAssociacaoResponse = {
  revisao: "9",
  previaId: "previa-1",
  numero: "OP-42",
  versao: "3",
  linhas: [linha, pagamento],
  natureza: "COMPRA",
  recebimentos: [{ ativoTicker: "BTC", quantidade: "0.1" }],
  pagamentos: [],
  taxas: [],
  principalBrl: "12000.00",
  custoFinalBrl: null,
  pendencias: ["Custo em reais desconhecido"],
  resolvidas: [],
  completo: false,
  desfazer: false,
};

const candidatos = { revisao: "9", linhas: [linha, pagamento], grupo: null };
const props = {
  registroId: linha.id,
  revisao: "9",
  disabled: false,
  onCancelar: vi.fn(),
  onSalvar: vi.fn().mockResolvedValue(undefined),
};
const resposta = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

afterEach(() => vi.unstubAllGlobals());

describe("Associação de registros", () => {
  it("considera o intervalo entre os extremos de toda a seleção", async () => {
    const anterior = { ...pagamento, data: "2026-11-06T12:30:43.123456Z" };
    const posterior = {
      ...pagamento,
      id: "registro:3",
      quantidade: "-200",
      data: "2026-11-06T12:30:47.123456Z",
    };
    const dados = {
      ...candidatos,
      linhas: [linha, anterior, posterior],
      janelaMilissegundos: 2000,
    };
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(resposta(dados)));
    render(<AssociacaoRegistros {...props} />);
    fireEvent.click(await screen.findByText("Opções de conferência"));
    const primeira = screen.getByRole("checkbox", { name: /BRL -100/ });
    const ultima = screen.getByRole("checkbox", { name: /BRL -200/ });
    expect(ultima).toBeEnabled();
    fireEvent.click(primeira);
    expect(ultima).toBeDisabled();
    fireEvent.click(primeira);
    expect(ultima).toBeEnabled();
  });

  it("abre com a linha clicada selecionada e exige prévia antes de salvar", async () => {
    const fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes("/candidatos")) return Promise.resolve(resposta(candidatos));
      return Promise.resolve(resposta(grupo));
    });
    vi.stubGlobal("fetch", fetch);
    render(<AssociacaoRegistros {...props} />);
    await screen.findByText("Opções de conferência");
    fireEvent.click(screen.getByText("Opções de conferência"));

    const checkbox = await screen.findByRole("checkbox", { name: /BTC/ });
    expect(checkbox).toBeChecked();
    const destino = screen.queryByRole("button", { name: /Usar BRL/ });
    if (destino) fireEvent.click(destino);
    fireEvent.click(screen.getByRole("button", { name: "Ver prévia" }));
    await screen.findByText("Custo em reais desconhecido");
    expect(props.onSalvar).not.toHaveBeenCalled();
    expect(
      fetch.mock.calls.some(
        ([url, options]) =>
          String(url).includes("/associacoes/previa") && options?.method === "POST",
      ),
    ).toBe(true);
    fireEvent.click(screen.getByRole("button", { name: "Salvar associação" }));
    await waitFor(() =>
      expect(props.onSalvar).toHaveBeenCalledWith(
        expect.objectContaining({ previaId: "previa-1" }),
      ),
    );
  });

  it("invalida a prévia quando a seleção é editada", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockImplementation((url: string) =>
          Promise.resolve(resposta(String(url).includes("/previa") ? grupo : candidatos)),
        ),
    );
    render(<AssociacaoRegistros {...props} />);
    await screen.findByText("Opções de conferência");
    fireEvent.click(screen.getByText("Opções de conferência"));
    await screen.findByRole("checkbox", { name: /BTC/ });
    const destino = screen.queryByRole("button", { name: /Usar BRL/ });
    if (destino) fireEvent.click(destino);
    fireEvent.click(screen.getByRole("button", { name: "Ver prévia" }));
    await screen.findByText("Prévia da associação");
    fireEvent.click(screen.getByRole("checkbox", { name: /BTC/ }));
    expect(screen.queryByText("Prévia da associação")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Salvar associação" })).not.toBeInTheDocument();
  });

  it("mostra candidatas por rádio sem escolher uma automaticamente", async () => {
    const candidata: LinhaAssociacaoResponse = {
      ...linha,
      id: "registro:2",
      ativoTicker: "BRL",
      quantidade: "-100",
      operacao: "Transaction Spend",
      papel: "PAGAMENTO",
    };
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        resposta({
          ...candidatos,
          linhas: [linha, candidata, { ...candidata, id: "registro:3", quantidade: "-200" }],
        }),
      ),
    );
    render(<AssociacaoRegistros {...props} />);
    await screen.findByText("Opções de conferência");
    fireEvent.click(screen.getByText("Opções de conferência"));

    const radio = await screen.findByRole("radio", { name: /BRL -100/ });
    expect(radio).not.toBeChecked();
    fireEvent.click(radio);
    expect(radio).toBeChecked();
    expect(screen.getByRole("checkbox", { name: /BRL -100/ })).toBeChecked();
    fireEvent.click(screen.getByRole("radio", { name: /BRL -200/ }));
    expect(radio).not.toBeChecked();
    expect(screen.getByRole("checkbox", { name: /BRL -100/ })).not.toBeChecked();
    expect(screen.getByRole("checkbox", { name: /BRL -200/ })).toBeChecked();
  });

  it("busca um grupo pelo número e preserva a entrada ao falhar", async () => {
    const fetch = vi
      .fn()
      .mockImplementation((url: string) =>
        String(url).includes("/OP-42")
          ? Promise.resolve(resposta({ detail: "Grupo não encontrado" }, 404))
          : Promise.resolve(resposta(candidatos)),
      );
    vi.stubGlobal("fetch", fetch);
    render(<AssociacaoRegistros {...props} />);
    await screen.findByText("Opções de conferência");
    fireEvent.click(screen.getByText("Opções de conferência"));
    const numero = await screen.findByRole("textbox", { name: "Número do grupo" });
    fireEvent.change(numero, { target: { value: "OP-42" } });
    fireEvent.click(screen.getByRole("button", { name: "Buscar grupo" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Grupo não encontrado");
    expect(numero).toHaveValue("OP-42");
  });

  it("carrega todas as linhas de um grupo existente", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockImplementation((url: string) =>
          String(url).includes("/candidatos")
            ? Promise.resolve(resposta({ ...candidatos, grupo }))
            : Promise.resolve(resposta(grupo)),
        ),
    );
    render(<AssociacaoRegistros {...props} />);
    await screen.findByText("Opções de conferência");
    fireEvent.click(screen.getByText("Opções de conferência"));
    expect(await screen.findByRole("checkbox", { name: /BTC/ })).toBeChecked();
    expect(screen.getByRole("textbox", { name: "Número do grupo" })).toHaveValue("OP-42");
  });

  it("mantém os dados atuais da linha e aplica o papel do snapshot ao carregar o grupo inicial", async () => {
    const atual = { ...linha, versao: "2", quantidade: "0.2", papel: "NAO_DEFINIDO" as const };
    const snapshot = { ...linha, versao: "1", quantidade: "0.1", papel: "PAGAMENTO" as const };
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          resposta({ ...candidatos, linhas: [atual], grupo: { ...grupo, linhas: [snapshot] } }),
        ),
    );
    render(<AssociacaoRegistros {...props} />);
    await screen.findByText("Opções de conferência");
    fireEvent.click(screen.getByText("Opções de conferência"));

    await screen.findByRole("checkbox", { name: /BTC/ });
    expect(screen.getByRole("checkbox", { name: /BTC 0.2/ })).toBeChecked();
    expect(screen.getByRole("combobox", { name: `Papel de ${linha.id}` })).toHaveValue("PAGAMENTO");
  });

  it("mantém as linhas atuais ao buscar um grupo por número", async () => {
    const atual = { ...linha, versao: "2", quantidade: "0.2" };
    const snapshot = { ...linha, versao: "1", quantidade: "0.1", papel: "PAGAMENTO" as const };
    const fetch = vi.fn().mockImplementation((url: string) => {
      if (String(url).includes("/candidatos")) {
        return Promise.resolve(resposta({ ...candidatos, linhas: [atual] }));
      }
      if (String(url).includes("/OP-42")) {
        return Promise.resolve(resposta({ ...grupo, linhas: [snapshot, pagamento] }));
      }
      return Promise.resolve(resposta(grupo));
    });
    vi.stubGlobal("fetch", fetch);
    render(<AssociacaoRegistros {...props} />);
    await screen.findByText("Opções de conferência");
    fireEvent.click(screen.getByText("Opções de conferência"));

    const numero = await screen.findByRole("textbox", { name: "Número do grupo" });
    fireEvent.change(numero, { target: { value: "OP-42" } });
    fireEvent.click(screen.getByRole("button", { name: "Buscar grupo" }));
    await waitFor(() => expect(screen.getByRole("checkbox", { name: /BTC 0.2/ })).toBeChecked());
    fireEvent.click(screen.getByRole("button", { name: "Ver prévia" }));
    await screen.findByText("Prévia da associação");

    const chamadaPrevia = fetch.mock.calls.find(
      ([url, options]) => String(url).includes("/associacoes/previa") && options?.method === "POST",
    );
    expect(JSON.parse(String(chamadaPrevia?.[1]?.body)).linhas).toEqual([
      { id: linha.id, versao: "2", papel: "PAGAMENTO" },
      { id: pagamento.id, versao: "1", papel: "PAGAMENTO" },
    ]);
  });

  it("preserva o rascunho e invalida a prévia quando a revisão da tela muda", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockImplementation((url: string) =>
          Promise.resolve(resposta(String(url).includes("/previa") ? grupo : candidatos)),
        ),
    );
    const { rerender } = render(<AssociacaoRegistros {...props} />);
    await screen.findByText("Opções de conferência");
    fireEvent.click(screen.getByText("Opções de conferência"));

    await screen.findByRole("checkbox", { name: /BTC/ });
    fireEvent.click(screen.getByRole("button", { name: /Usar BRL/ }));
    fireEvent.change(screen.getByRole("textbox", { name: "Número do grupo" }), {
      target: { value: "OP-rascunho" },
    });
    fireEvent.change(screen.getByRole("textbox", { name: "Cotação em reais (se conhecida)" }), {
      target: { value: "123.45" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Ver prévia" }));
    await screen.findByText("Prévia da associação");

    rerender(<AssociacaoRegistros {...props} revisao="10" />);

    expect(await screen.findByRole("alert")).toHaveTextContent(/atualizados/);
    expect(screen.queryByText("Prévia da associação")).not.toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: /BTC/ })).toBeChecked();
    expect(screen.getByRole("textbox", { name: "Número do grupo" })).toHaveValue("OP-rascunho");
    expect(screen.getByRole("textbox", { name: "Cotação em reais (se conhecida)" })).toHaveValue(
      "123.45",
    );
    expect(screen.getByRole("button", { name: "Ver prévia" })).toBeDisabled();
  });

  it("bloqueia edição durante a busca de grupo e aborta a resposta tardia ao cancelar", async () => {
    let resolverGrupo: (resposta: Response) => void = () => {};
    const respostaGrupo = new Promise<Response>((resolve) => {
      resolverGrupo = resolve;
    });
    const fetch = vi
      .fn()
      .mockImplementation((url: string) =>
        String(url).includes("/candidatos") ? Promise.resolve(resposta(candidatos)) : respostaGrupo,
      );
    vi.stubGlobal("fetch", fetch);
    const { rerender } = render(
      <AssociacaoRegistros {...props} onCancelar={() => rerender(<p>Associação fechada</p>)} />,
    );

    await screen.findByRole("checkbox", { name: /BTC/ });
    fireEvent.change(screen.getByRole("textbox", { name: "Número do grupo" }), {
      target: { value: "OP-42" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Buscar grupo" }));

    expect(screen.getByRole("checkbox", { name: /BTC/ })).toBeDisabled();
    expect(screen.getByRole("combobox", { name: `Papel de ${linha.id}` })).toBeDisabled();
    expect(screen.getByRole("textbox", { name: "Cotação em reais (se conhecida)" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Ver prévia" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Cancelar" })).toBeEnabled();

    const chamadaGrupo = fetch.mock.calls.find(([url]) => String(url).includes("/OP-42"));
    fireEvent.click(screen.getByRole("button", { name: "Cancelar" }));
    expect(await screen.findByText("Associação fechada")).toBeVisible();
    expect(chamadaGrupo?.[1]?.signal.aborted).toBe(true);

    resolverGrupo(resposta(grupo));
    expect(await screen.findByText("Associação fechada")).toBeVisible();
  });
});
