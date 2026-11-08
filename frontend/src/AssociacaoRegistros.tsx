import { diferencaSegundos } from "./services/instantes";
import { useEffect, useId, useMemo, useRef, useState } from "react";
import {
  listarCandidatosAssociacao,
  obterGrupoAssociacao,
  obterPreviaAssociacao,
} from "./services/associacoes";
import type {
  GrupoAssociacaoResponse,
  LinhaAssociacaoResponse,
  PapelAssociacao,
  RequestAssociacao,
} from "./services/associacoes";

const papeis: { valor: PapelAssociacao; nome: string }[] = [
  { valor: "NAO_DEFINIDO", nome: "A definir" },
  { valor: "RECEBIMENTO", nome: "Recebimento" },
  { valor: "PAGAMENTO", nome: "Pagamento" },
  { valor: "TAXA", nome: "Taxa" },
];

function mostrarDelta(data: string, dataClicada: string) {
  const diferenca = diferencaSegundos(data, dataClicada);
  return diferenca === null ? "" : `${diferenca} s do registro aberto`;
}

function textoErro(erro: unknown) {
  return erro instanceof Error ? erro.message : "Não foi possível concluir a solicitação.";
}

export default function AssociacaoRegistros({
  registroId,
  revisao,
  disabled,
  onCancelar,
  onSalvar,
}: {
  registroId: string;
  revisao: string;
  disabled: boolean;
  onCancelar: () => void;
  onSalvar: (dados: RequestAssociacao & { previaId: string }) => Promise<void>;
}) {
  const prefixo = useId();
  const [linhas, setLinhas] = useState<LinhaAssociacaoResponse[]>([]);
  const [selecionadas, setSelecionadas] = useState<string[]>([]);
  const [candidataSelecionada, setCandidataSelecionada] = useState("");
  const [papeisSelecionados, setPapeisSelecionados] = useState<Record<string, PapelAssociacao>>({});
  const [numero, setNumero] = useState("");
  const [versao, setVersao] = useState<string | null>(null);
  const [taxaZeroConfirmada, setTaxaZeroConfirmada] = useState(false);
  const [cotacaoBrl, setCotacaoBrl] = useState("");
  const [desfazer, setDesfazer] = useState(false);
  const [previa, setPrevia] = useState<GrupoAssociacaoResponse | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [revisaoCandidatos, setRevisaoCandidatos] = useState<string | null>(null);
  const [consultandoGrupo, setConsultandoGrupo] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");
  const [avisos, setAvisos] = useState<string[]>([]);
  const [janelaMilissegundos, setJanelaMilissegundos] = useState<number>();
  const previaAbort = useRef<AbortController | null>(null);
  const grupoAbort = useRef<AbortController | null>(null);
  const linhaClicada = useRef<LinhaAssociacaoResponse | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    let ativa = true;
    setCarregando(true);
    setErro("");
    listarCandidatosAssociacao(registroId, controller.signal)
      .then((resposta) => {
        if (!ativa) return;
        setRevisaoCandidatos(resposta.revisao);
        setAvisos(resposta.avisos ?? []);
        setJanelaMilissegundos(resposta.janelaMilissegundos);
        setTaxaZeroConfirmada(resposta.grupo?.taxaZeroConfirmada ?? false);
        setCotacaoBrl(resposta.grupo?.cotacaoBrl ?? "");
        const porId = new Map(resposta.linhas.map((linha) => [linha.id, linha]));
        resposta.grupo?.linhas.forEach((linha) => {
          const atual = porId.get(linha.id);
          porId.set(linha.id, atual ? { ...atual, papel: linha.papel } : linha);
        });
        const registro = porId.get(registroId) ?? null;
        linhaClicada.current = registro;
        const idsGrupo = resposta.grupo?.linhas.map((linha) => linha.id) ?? [];
        const idsSelecionados = [
          ...new Set([...idsGrupo, ...(idsGrupo.includes(registroId) ? [] : [registroId])]),
        ];
        setLinhas([...porId.values()]);
        setSelecionadas(idsSelecionados);
        setCandidataSelecionada("");
        setPapeisSelecionados(
          Object.fromEntries([...porId.values()].map((linha) => [linha.id, linha.papel])),
        );
        setNumero(resposta.grupo?.numero ?? "");
        setVersao(resposta.grupo?.versao ?? null);
        setDesfazer(false);
        setPrevia(null);
      })
      .catch((falha: unknown) => {
        if (ativa && !controller.signal.aborted) setErro(textoErro(falha));
      })
      .finally(() => {
        if (ativa) setCarregando(false);
      });
    return () => {
      ativa = false;
      controller.abort();
      grupoAbort.current?.abort();
      previaAbort.current?.abort();
    };
  }, [registroId]);

  useEffect(() => {
    if (revisaoCandidatos === null || revisaoCandidatos === revisao) return;
    previaAbort.current?.abort();
    setPrevia(null);
    setErro(
      "A revisão dos registros mudou. Seu rascunho foi preservado; feche e abra novamente para carregar candidatos atualizados.",
    );
  }, [revisao, revisaoCandidatos]);

  const invalidaPrevia = () => {
    previaAbort.current?.abort();
    setPrevia(null);
  };
  const linhasDoPedido = useMemo(
    () =>
      selecionadas.flatMap((id) => {
        const linha = linhas.find((item) => item.id === id);
        return linha
          ? [{ id, versao: linha.versao, papel: papeisSelecionados[id] ?? "NAO_DEFINIDO" }]
          : [];
      }),
    [linhas, papeisSelecionados, selecionadas],
  );
  const dadosPedido: RequestAssociacao = {
    revisao: revisaoCandidatos ?? revisao,
    numero: numero.trim() || null,
    versao,
    linhas: linhasDoPedido,
    taxaZeroConfirmada,
    cotacaoBrl: cotacaoBrl.trim() || null,
    desfazer,
  };

  const buscarGrupo = async (destino = numero.trim(), candidataId = "") => {
    if (!destino) return;
    setErro("");
    setConsultandoGrupo(true);
    invalidaPrevia();
    grupoAbort.current?.abort();
    const controller = new AbortController();
    grupoAbort.current = controller;
    try {
      const grupo = await obterGrupoAssociacao(destino, controller.signal);
      if (controller.signal.aborted) return;
      const porId = new Map(linhas.map((linha) => [linha.id, linha]));
      grupo.linhas.forEach((linha) => {
        const atual = porId.get(linha.id);
        porId.set(linha.id, atual ? { ...atual, papel: linha.papel } : linha);
      });
      const idsGrupo = grupo.linhas.map((linha) => linha.id);
      const ids = [
        ...new Set([...idsGrupo, ...(idsGrupo.includes(registroId) ? [] : [registroId])]),
      ];
      setLinhas([...porId.values()]);
      setSelecionadas(ids);
      setPapeisSelecionados((atuais) => ({
        ...atuais,
        ...Object.fromEntries(grupo.linhas.map((linha) => [linha.id, linha.papel])),
      }));
      setNumero(grupo.numero ?? destino);
      setVersao(grupo.versao);
      setCandidataSelecionada(candidataId);
      setTaxaZeroConfirmada(grupo.taxaZeroConfirmada ?? false);
      setCotacaoBrl(grupo.cotacaoBrl ?? "");
      setDesfazer(false);
    } catch (falha) {
      if (!controller.signal.aborted) setErro(textoErro(falha));
    } finally {
      if (grupoAbort.current === controller) {
        grupoAbort.current = null;
        if (!controller.signal.aborted) setConsultandoGrupo(false);
      }
    }
  };

  const solicitarPrevia = async (evento: React.FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    if (
      consultandoGrupo ||
      revisaoCandidatos !== revisao ||
      (!desfazer && linhasDoPedido.length < 2)
    ) {
      return;
    }
    previaAbort.current?.abort();
    const controller = new AbortController();
    previaAbort.current = controller;
    setErro("");
    setPrevia(null);
    try {
      const resposta = await obterPreviaAssociacao(dadosPedido, controller.signal);
      if (!controller.signal.aborted) setPrevia(resposta);
    } catch (falha) {
      if (!controller.signal.aborted) setErro(textoErro(falha));
    }
  };

  const salvar = async () => {
    if (!previa || disabled || salvando) return;
    setErro("");
    setSalvando(true);
    try {
      await onSalvar({ ...dadosPedido, previaId: previa.previaId });
    } catch (falha) {
      setErro(textoErro(falha));
    } finally {
      setSalvando(false);
    }
  };

  const alternarLinha = (id: string, marcada: boolean) => {
    invalidaPrevia();
    setSelecionadas((atuais) =>
      marcada ? [...atuais, id] : atuais.filter((selecionada) => selecionada !== id),
    );
  };

  const podeAcrescentar = (id: string) => {
    if (janelaMilissegundos === undefined || selecionadas.includes(id)) return true;
    const datas = linhas
      .filter((linha) => linha.id === id || selecionadas.includes(linha.id))
      .map((linha) => Date.parse(linha.data ?? ""));
    return Math.max(...datas) - Math.min(...datas) <= janelaMilissegundos;
  };

  const escolherCandidata = (id: string) => {
    invalidaPrevia();
    const candidata = linhas.find((linha) => linha.id === id);
    if (candidata?.grupoNumero) {
      void buscarGrupo(candidata.grupoNumero, id);
      return;
    }
    setCandidataSelecionada(id);
    if (versao !== null) {
      setTaxaZeroConfirmada(false);
      setCotacaoBrl("");
      setDesfazer(false);
    }
    setNumero("");
    setVersao(null);
    setSelecionadas([registroId, id]);
  };

  const candidatas = [
    ...new Map(
      linhas
        .filter(
          (linha) =>
            linha.id !== registroId &&
            (!linha.grupoNumero || linha.grupoNumero !== linhaClicada.current?.grupoNumero) &&
            linha.papel !== "NAO_DEFINIDO" &&
            (linha.grupoNumero || linha.papel !== linhaClicada.current?.papel),
        )
        .map((linha) => [linha.grupoNumero ?? linha.id, linha]),
    ).values(),
  ];

  if (carregando) return <p role="status">Carregando linhas relacionadas…</p>;

  return (
    <section className="previa-conciliacao" aria-label="Associar registros">
      <h3>Vincular registros</h3>
      {erro && <p role="alert">{erro}</p>}
      {avisos.map((aviso) => (
        <p key={aviso}>{aviso}</p>
      ))}
      {candidatas.length > 0 && (
        <fieldset
          className="associacao-destinos"
          disabled={disabled || salvando || consultandoGrupo}
        >
          <legend>Destino do vínculo</legend>
          {candidatas.map((linha) => {
            const rotulo = linha.grupoNumero ?? `${linha.ativoTicker} ${linha.quantidade}`;
            const delta = mostrarDelta(linha.data ?? "", linhaClicada.current?.data ?? "");
            return candidatas.length > 1 ? (
              <label className="associacao-destino" key={linha.id}>
                <input
                  type="radio"
                  name={`${prefixo}-candidata`}
                  checked={candidataSelecionada === linha.id}
                  onChange={() => escolherCandidata(linha.id)}
                />
                <span>
                  <strong>{rotulo}</strong> <small>{delta}</small>
                  {linha.referencia && <small>{linha.referencia}</small>}
                </span>
              </label>
            ) : (
              <button
                type="button"
                className="secondary-button"
                aria-pressed={candidataSelecionada === linha.id}
                key={linha.id}
                onClick={() => escolherCandidata(linha.id)}
              >
                Usar {rotulo}
              </button>
            );
          })}
        </fieldset>
      )}
      <form onSubmit={(evento) => void solicitarPrevia(evento)}>
        <details className="associacao-opcoes">
          <summary>Opções de conferência</summary>
          <div className="registro-detalhes">
            <label htmlFor={`${prefixo}-numero`}>Número do grupo</label>{" "}
            <input
              id={`${prefixo}-numero`}
              value={numero}
              disabled={disabled || salvando || consultandoGrupo}
              onChange={(evento) => {
                setNumero(evento.target.value);
                setVersao(null);
                setDesfazer(false);
                invalidaPrevia();
              }}
            />{" "}
            <button
              className="secondary-button"
              type="button"
              disabled={disabled || salvando || consultandoGrupo || !numero.trim()}
              onClick={() => void buscarGrupo()}
            >
              Buscar grupo
            </button>
          </div>
          <div className="registro-detalhes associacao-tabela">
            <table>
              <thead>
                <tr>
                  <th scope="col">Selecionar</th>
                  <th scope="col">Data e hora</th>
                  <th scope="col">Ativo e quantidade</th>
                  <th scope="col">Origem</th>
                  <th scope="col">Papel</th>
                </tr>
              </thead>
              <tbody>
                {linhas.map((linha) => (
                  <tr key={linha.id}>
                    <td>
                      <input
                        type="checkbox"
                        aria-label={`${linha.ativoTicker} ${linha.quantidade} ${linha.id}`}
                        checked={selecionadas.includes(linha.id)}
                        disabled={
                          disabled || salvando || consultandoGrupo || !podeAcrescentar(linha.id)
                        }
                        onChange={(evento) => alternarLinha(linha.id, evento.target.checked)}
                      />
                    </td>
                    <td>
                      <time dateTime={linha.data ?? undefined}>{linha.data}</time>
                      <small>
                        {mostrarDelta(
                          linha.data ?? "",
                          linhaClicada.current?.data ?? linha.data ?? "",
                        )}
                      </small>
                    </td>
                    <td>
                      {linha.ativoTicker} · {linha.quantidade}
                    </td>
                    <td>
                      {linha.arquivo} · linha {linha.linha} · aba {linha.aba}
                      <small>
                        {linha.operacao} · {linha.referencia}
                      </small>
                    </td>
                    <td>
                      <select
                        aria-label={`Papel de ${linha.id}`}
                        value={papeisSelecionados[linha.id] ?? "NAO_DEFINIDO"}
                        disabled={
                          disabled ||
                          salvando ||
                          consultandoGrupo ||
                          !selecionadas.includes(linha.id)
                        }
                        onChange={(evento) => {
                          invalidaPrevia();
                          setPapeisSelecionados((atuais) => ({
                            ...atuais,
                            [linha.id]: evento.target.value as PapelAssociacao,
                          }));
                        }}
                      >
                        {papeis.map((papel) => (
                          <option key={papel.valor} value={papel.valor}>
                            {papel.nome}
                          </option>
                        ))}
                      </select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {linhas.length === 0 && <p>Nenhuma linha candidata encontrada para este registro.</p>}
          </div>
          <fieldset disabled={disabled || salvando || consultandoGrupo}>
            <label htmlFor={`${prefixo}-cotacao`}>Cotação em reais (se conhecida)</label>{" "}
            <input
              id={`${prefixo}-cotacao`}
              inputMode="decimal"
              value={cotacaoBrl}
              onChange={(evento) => {
                setCotacaoBrl(evento.target.value);
                invalidaPrevia();
              }}
            />
            {versao !== null && (
              <label>
                <input
                  type="checkbox"
                  checked={desfazer}
                  onChange={(evento) => {
                    setDesfazer(evento.target.checked);
                    invalidaPrevia();
                  }}
                />
                Desfazer associação do grupo
              </label>
            )}
          </fieldset>
        </details>
        <div className="conciliacao-actions">
          <button
            className="primary-button"
            type="submit"
            disabled={
              disabled ||
              salvando ||
              consultandoGrupo ||
              revisaoCandidatos !== revisao ||
              (!desfazer && linhasDoPedido.length < 2)
            }
          >
            Ver prévia
          </button>
          {previa && (
            <button
              className="primary-button"
              type="button"
              disabled={disabled || salvando}
              onClick={() => void salvar()}
            >
              {salvando ? "Salvando…" : "Salvar associação"}
            </button>
          )}
          <button
            className="secondary-button"
            type="button"
            disabled={disabled || salvando}
            onClick={onCancelar}
          >
            Cancelar
          </button>
        </div>
      </form>
      {previa && (
        <section aria-label="Prévia da associação" className="registro-detalhes">
          <h3>Prévia da associação</h3>
          <p>
            {previa.natureza
              ? { COMPRA: "Compra", VENDA: "Venda", CONVERSAO: "Conversão" }[previa.natureza]
              : "Natureza pendente"}{" "}
            · {previa.numero ?? "Grupo sem número"}
          </p>
          {previa.natureza === "CONVERSAO" && (
            <p>A cotação informada corresponde a uma unidade do ativo enviado.</p>
          )}
          <p>
            Principal conhecido: {previa.principalBrl ?? "Desconhecido"} · Custo final:{" "}
            {previa.custoFinalBrl ?? "Custo final desconhecido"}
          </p>
          <p>
            Taxas:{" "}
            {previa.taxas.length
              ? previa.taxas.map((taxa) => `${taxa.ativoTicker} ${taxa.quantidade}`).join(" · ")
              : "Sem taxas identificadas"}
          </p>
          <p>
            Recebimentos:{" "}
            {previa.recebimentos.length
              ? previa.recebimentos
                  .map((parcela) => `${parcela.ativoTicker} ${parcela.quantidade}`)
                  .join(" · ")
              : "Nenhum identificado"}
          </p>
          <p>
            Pagamentos:{" "}
            {previa.pagamentos.length
              ? previa.pagamentos
                  .map((parcela) => `${parcela.ativoTicker} ${parcela.quantidade}`)
                  .join(" · ")
              : "Nenhum identificado"}
          </p>
          {previa.pendencias.length > 0 && (
            <>
              <h4>Pendências</h4>
              <ul>
                {previa.pendencias.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </>
          )}
          {previa.resolvidas.length > 0 && (
            <>
              <h4>Resoluções propostas</h4>
              <ul>
                {previa.resolvidas.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </>
          )}
        </section>
      )}
    </section>
  );
}
