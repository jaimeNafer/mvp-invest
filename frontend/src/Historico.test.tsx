import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import Historico from "./Historico";
import Operacoes from "./Operacoes";
import PreviaConciliacao from "./PreviaConciliacao";
import RegistroHistorico from "./RegistroHistorico";

const registro = {
  id: "registro:42",
  data: "2026-01-02T12:00:00Z",
  criadoEm: "2026-01-03T12:00:00Z",
  tipo: "OBSERVACAO",
  descricao: "Saída observada",
  instituicaoId: "1",
  instituicaoNome: "Binance",
  contaId: "c1",
  contaNome: "Spot",
  ativoId: "2",
  ativoTicker: "BTC",
  quantidade: "-0.030000000000000001",
  custoTotalBrl: null,
  estado: "PENDENTE",
  origem: "IMPORTADA",
  revisadoPor: "Jaime",
  revisadoEm: "2026-01-04T12:00:00Z",
  versao: "1",
  pendencias: [
    { id: "p1", mensagem: "Custo ainda desconhecido", impacto: "Resultado indisponível" },
  ],
  ancoraId: null,
  camposOriginais: { Change: "-0.030000000000000001", Remark: "registro sintético" },
};
const resumo = {
  total: 3,
  coerentes: 1,
  pendentes: 1,
  inconsistentes: 1,
  ajustesAtivos: 0,
  percentualCoerencia: "33.33",
  registrosAfetados: 2,
  vendasAfetadas: 1,
  ativosAfetados: ["BTC"],
  mensagem: "2 pendências afetam o custo de BTC e o resultado de 1 venda",
};
const conta = { id: "c1", nome: "Spot", instituicaoId: "1", instituicaoNome: "Binance" };
const ativo = {
  id: "2",
  codigo: "cripto:BTC",
  ticker: "BTC",
  nome: "Bitcoin",
  tipo: "CRIPTO",
  casasDecimais: 18,
  ativo: true,
};
const json = (dados: unknown, status = 200) => new Response(JSON.stringify(dados), { status });
afterEach(() => vi.unstubAllGlobals());
function servidor(
  opcoes: {
    incerta?: boolean;
    total?: number;
    revisao?: () => string;
    consulta?: () => Promise<void>;
    agrupar?: boolean;
    grupoAutomatico?: boolean;
    conversao?: boolean;
    mutacao?: (url: string, init: RequestInit) => Response | Promise<Response>;
  } = {},
) {
  const escritas: { url: string; init: RequestInit }[] = [];
  let tentativas = 0;
  const fetchMock = vi.fn(async (url: string, init: RequestInit = {}) => {
    if (url.endsWith("/ativos")) return json([ativo]);
    if (url.endsWith("/conciliacao/contas")) return json([conta]);
    if (url.includes("/timeline?")) {
      await opcoes.consulta?.();
      if (opcoes.agrupar || opcoes.grupoAutomatico) {
        const chave = opcoes.grupoAutomatico ? "grupo:registro:42" : "OP-42";
        const primeira = {
          ...registro,
          ...(opcoes.conversao
            ? {
                tipo: "Binance Convert",
                pagamentosGrupo: [{ ativoTicker: "BNB", quantidade: "0.14536437" }],
                recebimentosGrupo: [{ ativoTicker: "SOL", quantidade: "0.57296862" }],
              }
            : {}),
          grupoNumero: opcoes.grupoAutomatico ? null : chave,
          grupoAutomatico: opcoes.grupoAutomatico ? chave : null,
          ...(opcoes.grupoAutomatico
            ? { estado: "COERENTE", pendencias: [], aplicado: false }
            : {}),
        };
        const segunda = { ...primeira, id: "registro:43", descricao: "Pagamento observado" };
        return json({
          itens: [primeira],
          linhasPorGrupo: { [chave]: [primeira, segunda] },
          total: 1,
          pagina: 0,
          tamanho: 50,
          revisao: "7",
          resumo,
          proximaPendenciaId: primeira.id,
        });
      }
      return json({
        itens: [
          registro,
          {
            ...registro,
            id: "operacao:1",
            descricao: "Compra confirmada",
            estado: "COERENTE",
            revisadoPor: null,
            revisadoEm: null,
          },
          {
            ...registro,
            id: "registro:43",
            descricao: "Data inválida",
            data: null,
            estado: "CORRECAO_NECESSARIA",
            revisadoPor: null,
            revisadoEm: null,
          },
        ],
        total: opcoes.total ?? 3,
        pagina: Number(new URL(url, "http://lastro").searchParams.get("pagina")),
        tamanho: 50,
        revisao: opcoes.revisao?.() ?? "7",
        resumo,
        proximaPendenciaId: "registro:42",
      });
    }
    if (url.endsWith("/previa"))
      return json({
        revisao: "7",
        previaId: "pv1",
        ajustes: [
          {
            lacunaId: "l1",
            ancoraId: "registro:42",
            contaId: "c1",
            contaNome: "Spot",
            instituicaoNome: "Binance",
            ativoId: "2",
            ativoTicker: "BTC",
            quantidadeSugerida: "0.03",
            custoTotalBrl: null,
            acao: "CRIAR",
            quantidadeAnterior: null,
          },
          {
            lacunaId: "l2",
            ancoraId: "registro:43",
            contaId: "c1",
            contaNome: "Spot",
            instituicaoNome: "Binance",
            ativoId: "2",
            ativoTicker: "BTC",
            quantidadeSugerida: "0",
            custoTotalBrl: null,
            acao: "ENCERRAR",
            quantidadeAnterior: "0.01",
          },
        ],
        correspondencias: [
          {
            id: "t1",
            registroIds: ["registro:42", "registro:43"],
            descricao: "Transferência interna identificada",
            contaOrigem: "Funding",
            contaDestino: "Spot",
            quantidade: "0.01",
            ativoTicker: "BTC",
          },
        ],
        resumo,
      });
    if (init.method === "POST" || init.method === "PUT") {
      escritas.push({ url, init });
      if (opcoes.mutacao) return opcoes.mutacao(url, init);
      if (url.endsWith("/recebimentos"))
        return json({
          arquivos: [
            {
              id: "a1",
              nomeArquivo: "dados.csv",
              hash: "abc",
              registros: 3,
              reconhecido: false,
              erro: null,
            },
            {
              id: null,
              nomeArquivo: "ilegivel.xlsx",
              hash: "def",
              registros: 0,
              reconhecido: false,
              erro: "Formato não reconhecido",
            },
          ],
          recebidos: 3,
          reconhecidos: 0,
          problemas: 1,
          revisao: "8",
        });
      if (url.endsWith("/confirmacoes") && opcoes.incerta && tentativas++ === 0)
        throw new TypeError("rede");
      if (url.endsWith("/revisao")) return json(registro);
      if (init.method === "PUT") return json({ ...registro, ...JSON.parse(String(init.body)) });
      return json({ revisao: "8" });
    }
    throw new Error(`Rota inesperada ${url}`);
  });
  vi.stubGlobal("fetch", fetchMock);
  return { escritas, fetchMock };
}
it("mostra a operação agrupada uma vez e mantém as linhas originais nos detalhes", async () => {
  servidor({ agrupar: true });
  render(<Historico />);
  const operacao = await screen.findByRole("button", { name: "Operação OP-42" });
  expect(screen.queryByRole("button", { name: "Detalhar Pagamento observado" })).toBeNull();
  fireEvent.click(operacao);
  fireEvent.click(screen.getByText("Ver 2 registros originais"));
  expect(screen.getByRole("button", { name: "Detalhar Saída observada" })).toBeVisible();
  expect(screen.getByRole("button", { name: "Detalhar Pagamento observado" })).toBeVisible();
});

it("mostra conversão agrupada com origem, destino e linhas originais", async () => {
  servidor({ grupoAutomatico: true, conversao: true });
  render(<Historico />);
  const conversao = await screen.findByRole("button", { name: "Binance Convert" });
  expect(screen.getByText("0,14536437 BNB → 0,57296862 SOL")).toBeVisible();
  expect(screen.getByText("Aguardando confirmação global")).toBeVisible();
  fireEvent.click(conversao);
  fireEvent.click(screen.getByText("Ver 2 registros originais"));
  expect(screen.getByRole("button", { name: "Detalhar Saída observada" })).toBeVisible();
  expect(screen.getByRole("button", { name: "Detalhar Pagamento observado" })).toBeVisible();
});

it("mostra o vínculo automático antes da confirmação sem oferecer associação manual", async () => {
  servidor({ grupoAutomatico: true });
  render(<Historico />);
  expect(await screen.findByText("Aguardando confirmação global")).toBeVisible();
  expect(screen.getByText("Coerente")).toBeVisible();
  fireEvent.click(await screen.findByRole("button", { name: "Operação identificada" }));
  fireEvent.click(screen.getByText("Ver 2 registros originais"));
  fireEvent.click(screen.getByRole("button", { name: "Detalhar Saída observada" }));
  expect(screen.queryByRole("button", { name: "Associar a um registro" })).toBeNull();
});
it("abre o histórico por padrão com estados textuais e revisão manual independente", async () => {
  servidor();
  render(<Operacoes />);
  expect(screen.getByRole("button", { name: "Importações" })).toHaveAttribute(
    "aria-pressed",
    "true",
  );
  expect(screen.getByLabelText("Arquivos do histórico")).toHaveAttribute("accept", ".csv,.xlsx");
  expect(screen.queryByRole("button", { name: "Importar arquivo" })).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Histórico" })).not.toBeInTheDocument();
  expect(await screen.findByText("Com pendência")).toHaveClass("estado-pendente");
  expect(screen.getByText("Coerente")).toHaveClass("estado-coerente");
  expect(screen.getByText("Precisa de correção")).toHaveClass("estado-correcao");
  expect(screen.getByText("Revisada por você")).toBeVisible();
  expect(screen.getByText(resumo.mensagem)).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Detalhar Saída observada" }));
  expect(screen.getByText("registro sintético")).toBeVisible();
  expect(screen.getByText("Resultado indisponível")).toBeVisible();
});

it("mostra confirmação pendente mesmo quando existe uma versão anterior aplicada", () => {
  render(
    <RegistroHistorico
      registro={{
        ...registro,
        estado: "PENDENTE",
        origem: "IMPORTADA",
        aplicado: true,
        aguardaConfirmacao: true,
      }}
      aberto={false}
      disabled={false}
      contas={[]}
      ativos={[]}
      onAbrir={() => {}}
      onSalvar={() => {}}
      onRevisar={() => {}}
      onAssociar={() => {}}
    />,
  );
  expect(screen.getByText("Aguardando confirmação global")).toBeVisible();
});

it("apresenta crédito automático discreto e permite consultar os dados originais", () => {
  render(
    <RegistroHistorico
      registro={
        {
          ...registro,
          descricao: "Megadrop Rewards",
          natureza: "CREDITO_AUTOMATICO",
          subtipoCredito: "BONIFICACAO",
          estado: "COERENTE",
          pendencias: [],
          aplicado: false,
        } as never
      }
      aberto={true}
      disabled={false}
      contas={[]}
      ativos={[]}
      onAbrir={() => {}}
      onSalvar={() => {}}
      onRevisar={() => {}}
      onAssociar={() => {}}
    />,
  );
  expect(screen.getByRole("article")).toHaveClass("registro-credito");
  expect(screen.getByText("Bonificação")).toBeInTheDocument();
  expect(screen.queryByText("Coerente")).not.toBeInTheDocument();
  expect(screen.queryByText("Com pendência")).not.toBeInTheDocument();
  expect(screen.getByText("registro sintético")).toBeInTheDocument();
});

it("apresenta movimentação interna Earn de forma discreta e com dados originais", () => {
  render(
    <RegistroHistorico
      registro={
        {
          ...registro,
          descricao: "Simple Earn Locked Subscription",
          natureza: "MOVIMENTACAO_INTERNA",
          estado: "COERENTE",
          pendencias: [],
          aplicado: false,
        } as never
      }
      aberto={true}
      disabled={false}
      contas={[]}
      ativos={[]}
      onAbrir={() => {}}
      onSalvar={() => {}}
      onRevisar={() => {}}
      onAssociar={() => {}}
    />,
  );
  expect(screen.getByRole("article")).toHaveClass("registro-credito");
  expect(screen.queryByText("Coerente")).not.toBeInTheDocument();
  expect(screen.queryByText("Com pendência")).not.toBeInTheDocument();
  expect(screen.getByText("registro sintético")).toBeInTheDocument();
});

it("permite informar finalidade da saída cripto sem abrir formulário extenso", () => {
  const salvar = vi.fn();
  render(
    <RegistroHistorico
      registro={{
        ...registro,
        estado: "COERENTE",
        origem: "IMPORTADA",
        natureza: "SAIDA_CRIPTO",
        finalidadeCripto: "DESCONHECIDA",
        taxaDetalhamentoPendente: true,
      }}
      aberto
      disabled={false}
      contas={[]}
      ativos={[]}
      onAbrir={() => {}}
      onSalvar={salvar}
      onRevisar={() => {}}
      onAssociar={() => {}}
    />,
  );
  expect(screen.getByText("Destino: Não identificado")).toBeVisible();
  expect(screen.queryByText("Informações fiscais pendentes.")).not.toBeInTheDocument();
  expect(screen.getByText("Taxa incluída, valor não informado.")).toBeVisible();
  fireEvent.change(screen.getByLabelText("Finalidade"), { target: { value: "TERCEIROS" } });
  fireEvent.click(screen.getByRole("button", { name: "Salvar finalidade" }));
  expect(salvar).toHaveBeenCalledWith(
    expect.objectContaining({ finalidadeCripto: "TERCEIROS" }),
    false,
  );
});
it("recebe vários arquivos e preserva registros legíveis e erros por arquivo", async () => {
  const { escritas } = servidor();
  render(<Historico />);
  await screen.findByText("Saída observada");
  fireEvent.change(screen.getByLabelText("Arquivos do histórico"), {
    target: {
      files: [new File(["sintético"], "dados.csv"), new File(["sintético"], "ilegivel.xlsx")],
    },
  });
  expect(screen.getByText("2 arquivos selecionados")).toBeVisible();
  expect(screen.getByText("dados.csv · ilegivel.xlsx")).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Receber arquivos" }));
  expect(await screen.findByText("Formato não reconhecido")).toBeVisible();
  expect(screen.getByText("3 registros recebidos")).toBeVisible();
  expect((escritas[0].init.body as FormData).getAll("arquivos")).toHaveLength(2);
  expect(new Headers(escritas[0].init.headers).has("Content-Type")).toBe(false);
});
it("apresenta contagens e progresso de conferência com acessibilidade", async () => {
  servidor();
  render(<Historico />);
  const resumoElement = await screen.findByRole("region", { name: "Conferência do histórico" });
  expect(within(resumoElement).getByText("Registros analisados")).toBeVisible();
  expect(within(resumoElement).getByText("Precisam de atenção")).toBeVisible();
  expect(within(resumoElement).getByText("33,33%")).toBeVisible();
  expect(within(resumoElement).getByRole("progressbar")).toHaveAttribute(
    "aria-valuetext",
    "1 de 3 registros coerentes",
  );
});
it("corrige na própria linha sem transformar custo desconhecido em zero", async () => {
  const { escritas } = servidor();
  render(<Historico />);
  fireEvent.click(await screen.findByRole("button", { name: "Detalhar Saída observada" }));
  fireEvent.click(screen.getByRole("button", { name: "Corrigir dados" }));
  expect(screen.getByLabelText("Custo total em reais (se conhecido)")).toHaveValue("");
  fireEvent.change(screen.getByLabelText("Quantidade"), {
    target: { value: "-0,030000000000000002" },
  });
  fireEvent.change(screen.getByLabelText("Motivo"), {
    target: { value: "Correção da quantidade observada" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Salvar dados" }));
  await waitFor(() => expect(escritas).toHaveLength(1));
  expect(escritas[0].url).toBe("/api/v1/conciliacao/registros/registro%3A42");
  expect(JSON.parse(String(escritas[0].init.body))).toMatchObject({
    revisao: "7",
    versao: "1",
    quantidade: "-0.030000000000000002",
    custoTotalBrl: null,
    natureza: "OBSERVADA",
  });
});
it("prévia exige aceite explícito e repete confirmação incerta com corpo e chave preservados", async () => {
  const { escritas } = servidor({ incerta: true });
  const bloqueio = vi.fn();
  render(<Historico onBloqueio={bloqueio} />);
  await screen.findByText("Saída observada");
  fireEvent.click(screen.getByRole("button", { name: "Conferir histórico" }));
  const previa = await screen.findByRole("region", { name: "Prévia da conciliação" });
  expect(within(previa).getByText("Encerrar ajuste")).toBeVisible();
  expect(screen.getByRole("button", { name: "Aplicar registros e conciliação" })).toBeDisabled();
  fireEvent.click(screen.getByRole("checkbox", { name: /Criar ajuste/ }));
  fireEvent.click(screen.getByRole("button", { name: "Aplicar registros e conciliação" }));
  expect(bloqueio).toHaveBeenLastCalledWith(true);
  fireEvent.click(await screen.findByRole("button", { name: "Repetir confirmação" }));
  expect(screen.getByLabelText("Estado")).toBeDisabled();
  await screen.findByText("Conciliação confirmada.");
  expect(escritas).toHaveLength(2);
  expect(escritas[0].init.body).toBe(escritas[1].init.body);
  expect(new Headers(escritas[0].init.headers).get("Idempotency-Key")).toBe(
    new Headers(escritas[1].init.headers).get("Idempotency-Key"),
  );
  expect(JSON.parse(String(escritas[0].init.body))).toEqual({
    revisao: "7",
    previaId: "pv1",
    aceitarLacunaIds: ["l1"],
    aceitarCorrespondenciaIds: [],
  });
  expect(bloqueio).toHaveBeenLastCalledWith(false);
});
it("envia filtros e paginação ao servidor sem misturar páginas", async () => {
  const { fetchMock } = servidor({ total: 80 });
  render(<Historico />);
  await screen.findByText("Saída observada");
  fireEvent.click(screen.getByRole("button", { name: "Próxima página" }));
  await waitFor(() =>
    expect(fetchMock.mock.calls.some(([url]) => url.includes("pagina=1"))).toBe(true),
  );
  fireEvent.change(screen.getByLabelText("Estado"), { target: { value: "PENDENTE" } });
  await waitFor(() =>
    expect(
      fetchMock.mock.calls.some(
        ([url]) => url.includes("pagina=0") && url.includes("estado=PENDENTE"),
      ),
    ).toBe(true),
  );
});
it("aplica registros recebidos sem exigir ajuste provisório", () => {
  const confirmar = vi.fn();
  render(
    <PreviaConciliacao
      previa={{
        revisao: "7",
        previaId: "p2",
        registrosParaAplicar: 2,
        ajustes: [],
        correspondencias: [],
        resumo,
      }}
      disabled={false}
      onConfirmar={confirmar}
      onFechar={() => {}}
    />,
  );
  expect(screen.getByText("2 registros aguardam aplicação nesta conferência.")).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Aplicar registros e conciliação" }));
  expect(confirmar).toHaveBeenCalledWith([], []);
});
it("mantém taxa desconhecida ao classificar observação como negociação", async () => {
  const { escritas } = servidor();
  render(<Historico />);
  fireEvent.click(await screen.findByRole("button", { name: "Detalhar Saída observada" }));
  fireEvent.click(screen.getByRole("button", { name: "Corrigir dados" }));
  fireEvent.change(screen.getByLabelText("Natureza do registro"), { target: { value: "VENDA" } });
  expect(screen.getByLabelText("Taxa conhecida")).not.toBeChecked();
  expect(screen.getByLabelText("Quantidade da taxa")).toBeDisabled();
  fireEvent.change(screen.getByLabelText("Total bruto"), { target: { value: "2.500,00" } });
  fireEvent.change(screen.getByLabelText("Motivo"), {
    target: { value: "Classificação pela nota de negociação" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Salvar dados" }));
  await waitFor(() => expect(escritas).toHaveLength(1));
  expect(JSON.parse(String(escritas[0].init.body))).toMatchObject({
    natureza: "VENDA",
    taxaInformada: false,
    taxaQuantidade: null,
    totalBruto: "2500.00",
    custoTotalBrl: null,
  });
});
it("bloqueia saída enquanto o primeiro comando ainda aguarda resposta", async () => {
  let responder: (resposta: Response) => void = () => {};
  const pendente = new Promise<Response>((resolve) => {
    responder = resolve;
  });
  servidor({ mutacao: () => pendente });
  render(<Operacoes />);
  await screen.findByText("Saída observada");
  fireEvent.click(screen.getByRole("button", { name: "Conferir histórico" }));
  await screen.findByRole("region", { name: "Prévia da conciliação" });
  fireEvent.click(screen.getByRole("checkbox", { name: /Criar ajuste/ }));
  fireEvent.click(screen.getByRole("button", { name: "Aplicar registros e conciliação" }));
  expect(screen.getByRole("button", { name: "Negociações" })).toBeDisabled();
  expect(screen.getByRole("button", { name: "Repetir confirmação" })).toBeDisabled();
  responder(json({ revisao: "8" }));
  await screen.findByText("Conciliação confirmada.");
  expect(screen.getByRole("button", { name: "Negociações" })).toBeEnabled();
});
it("atualiza revisão após conflito antes de pedir uma nova prévia", async () => {
  let revisao = "7";
  let consultas = 0;
  let liberarConsulta: () => void = () => {};
  const consultaPendente = new Promise<void>((resolve) => {
    liberarConsulta = resolve;
  });
  const { fetchMock } = servidor({
    consulta: async () => {
      if (++consultas > 1) await consultaPendente;
    },
    revisao: () => revisao,
    mutacao: () => {
      revisao = "8";
      return json({ detail: "O histórico mudou. Confira novamente." }, 409);
    },
  });
  render(<Historico />);
  fireEvent.click(await screen.findByRole("button", { name: "Detalhar Saída observada" }));
  fireEvent.click(screen.getByRole("button", { name: "Corrigir dados" }));
  fireEvent.change(screen.getByLabelText("Motivo"), { target: { value: "Correção explícita" } });
  fireEvent.click(screen.getByRole("button", { name: "Salvar dados" }));
  await screen.findByText("Carregando histórico…");
  expect(screen.getByLabelText("Motivo")).toHaveValue("Correção explícita");
  liberarConsulta();
  await screen.findByText("O histórico mudou. Confira novamente.");
  await waitFor(() =>
    expect(screen.getByRole("button", { name: "Conferir histórico" })).toBeEnabled(),
  );
  expect(screen.getByLabelText("Motivo")).toHaveValue("Correção explícita");
  fireEvent.click(screen.getByRole("button", { name: "Conferir histórico" }));
  await screen.findByRole("region", { name: "Prévia da conciliação" });
  const envio = fetchMock.mock.calls.find(([url]) => url.endsWith("/previa"));
  expect(JSON.parse(String(envio?.[1]?.body))).toEqual({ revisao: "8" });
});

it("editar o custo preserva segundos e precisão do instante original", async () => {
  const original = registro.data;
  registro.data = "2026-01-02T12:34:58.123456Z";
  try {
    const { escritas } = servidor();
    render(<Historico />);
    fireEvent.click(await screen.findByRole("button", { name: "Detalhar Saída observada" }));
    fireEvent.click(screen.getByRole("button", { name: "Corrigir dados" }));
    fireEvent.change(screen.getByLabelText("Custo total em reais (se conhecido)"), {
      target: { value: "10" },
    });
    fireEvent.change(screen.getByLabelText("Motivo"), {
      target: { value: "Corrigir custo sem alterar instante" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Salvar dados" }));
    await waitFor(() => expect(escritas).toHaveLength(1));
    expect(JSON.parse(String(escritas[0].init.body)).data).toBe(registro.data);
  } finally {
    registro.data = original;
  }
});

it("associar abre candidatos do registro selecionado sem disparar a previa global", async () => {
  const { fetchMock } = servidor();
  const original = globalThis.fetch;
  const consulta = vi.fn(async (url: string, init?: RequestInit) => {
    if (url.includes("/associacoes/candidatos?"))
      return json({
        revisao: "7",
        grupo: null,
        linhas: [
          {
            ...registro,
            operacao: "Buy",
            referencia: "Wallet/teste",
            papel: "PAGAMENTO",
            grupoNumero: null,
            arquivo: "teste.xlsx",
            linha: 3,
            aba: "Spot",
          },
        ],
      });
    return original(url, init);
  });
  vi.stubGlobal("fetch", consulta);
  render(<Historico />);
  fireEvent.click(await screen.findByRole("button", { name: "Detalhar Saída observada" }));
  fireEvent.click(screen.getByRole("button", { name: "Associar a um registro" }));
  const card = screen.getByRole("article", { name: "Saída observada" });
  expect(await within(card).findByRole("region", { name: "Associar registros" })).toBeVisible();
  expect(consulta.mock.calls.some(([url]) => url.includes("registroId=registro%3A42"))).toBe(true);
  expect(fetchMock.mock.calls.some(([url]) => url.endsWith("/conciliacao/previa"))).toBe(false);
});

it("preserva formulario apos conflito seguido de falha ao atualizar o historico", async () => {
  let falhar = false;
  servidor({
    consulta: async () => {
      if (falhar) throw new Error("Sem conexão");
    },
    mutacao: () => {
      falhar = true;
      return json({ detail: "Atualize a revisão." }, 409);
    },
  });
  render(<Historico />);
  fireEvent.click(await screen.findByRole("button", { name: "Detalhar Saída observada" }));
  fireEvent.click(screen.getByRole("button", { name: "Corrigir dados" }));
  fireEvent.change(screen.getByLabelText("Motivo"), { target: { value: "Preservar rascunho" } });
  fireEvent.click(screen.getByRole("button", { name: "Salvar dados" }));
  await screen.findByText("Atualize a revisão.");
  expect(screen.getByLabelText("Motivo")).toHaveValue("Preservar rascunho");
  expect(screen.getByRole("button", { name: "Conferir histórico" })).toBeDisabled();
  expect(screen.getByRole("button", { name: "Tentar novamente" })).toBeEnabled();
});
