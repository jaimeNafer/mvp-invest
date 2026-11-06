import { useEffect, useRef, useState } from "react";
import PreviaArquivo from "./PreviaArquivo";
import ReciboArquivo from "./ReciboArquivo";
import { ErroHttp } from "./services/http";
import {
  analisarImportacao,
  arquivoImportacao,
  confirmarImportacao,
  prepararImportacao,
} from "./services/importacoes";
import type {
  ComandoImportacao,
  DecisoesImportacao,
  PreviaImportacao,
  ReciboImportacao,
} from "./services/importacoes";
import { mensagemOperacao } from "./services/operacoes";
const decisoesIniciais: DecisoesImportacao = {
  aportesProprios: false,
  taxasIncluidas: false,
  posicaoAnteriorZerada: false,
};
export default function ImportacaoForm({
  onBloqueio,
  onConcluido,
  onOperacao,
}: {
  onBloqueio: (b: boolean) => void;
  onConcluido: () => void;
  onOperacao?: (id: string) => void;
}) {
  const [arquivo, setArquivo] = useState<File | null>(null);
  const [decisoes, setDecisoes] = useState(decisoesIniciais);
  const [previa, setPrevia] = useState<PreviaImportacao | null>(null);
  const [recibo, setRecibo] = useState<ReciboImportacao | null>(null);
  const [erro, setErro] = useState("");
  const [analisando, setAnalisando] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [incerta, setIncerta] = useState<ComandoImportacao | null>(null);
  const consulta = useRef<AbortController | null>(null);
  const comando = useRef<ComandoImportacao | null>(null);
  const envio = useRef(false);
  const bloqueado = salvando || !!incerta;
  useEffect(() => {
    onBloqueio(bloqueado);
  }, [bloqueado, onBloqueio]);
  useEffect(() => () => consulta.current?.abort(), []);
  useEffect(() => {
    if (!bloqueado) return;
    const proteger = (evento: BeforeUnloadEvent) => evento.preventDefault();
    window.addEventListener("beforeunload", proteger);
    return () => window.removeEventListener("beforeunload", proteger);
  }, [bloqueado]);
  function invalidar() {
    consulta.current?.abort();
    setAnalisando(false);
    setPrevia(null);
    comando.current = null;
    setErro("");
    setRecibo(null);
  }
  async function analisar() {
    if (!arquivo || !decisoes.aportesProprios || !decisoes.taxasIncluidas || bloqueado) return;
    invalidar();
    const controller = new AbortController();
    consulta.current = controller;
    setAnalisando(true);
    try {
      const resposta = await analisarImportacao(
        arquivoImportacao(arquivo, decisoes),
        AbortSignal.any([controller.signal, AbortSignal.timeout(60000)]),
      );
      if (!controller.signal.aborted) {
        setPrevia(resposta);
        comando.current = prepararImportacao(arquivo, decisoes, resposta);
      }
    } catch (falha) {
      if (!controller.signal.aborted) setErro(mensagemOperacao(falha));
    } finally {
      if (!controller.signal.aborted) setAnalisando(false);
    }
  }
  async function confirmar() {
    const pedido = incerta ?? comando.current;
    if (
      envio.current ||
      !pedido ||
      !previa ||
      previa.erros.length ||
      previa.eventos.some((e) => e.situacao === "CONFLITO")
    )
      return;
    envio.current = true;
    setSalvando(true);
    setErro("");
    onBloqueio(true);
    try {
      const resposta = await confirmarImportacao(pedido);
      setRecibo(resposta);
      setIncerta(null);
      setPrevia(null);
      comando.current = null;
      onConcluido();
    } catch (falha) {
      setErro(mensagemOperacao(falha));
      if (!(falha instanceof ErroHttp) || falha.status >= 500) setIncerta(pedido);
      else {
        setIncerta(null);
        setPrevia(null);
        comando.current = null;
      }
    } finally {
      envio.current = false;
      setSalvando(false);
    }
  }
  return (
    <div className="operations-panel import-panel">
      <h2>Importar arquivo</h2>
      <p>
        Selecione o relatório enriquecido de eventos da Binance em CSV. Confira os eventos antes de
        confirmar.
      </p>
      {recibo ? (
        <>
          <p role="status" className="message-success">
            Importação concluída.
          </p>
          <ReciboArquivo recibo={recibo} onOperacao={onOperacao} />
          <button
            type="button"
            className="secondary-button"
            onClick={() => {
              invalidar();
              setArquivo(null);
              setDecisoes(decisoesIniciais);
            }}
          >
            Importar outro arquivo
          </button>
        </>
      ) : (
        <>
          <form
            className="saldo-form"
            onSubmit={(e) => {
              e.preventDefault();
              void analisar();
            }}
          >
            <fieldset className="saldo-campos" disabled={bloqueado}>
              <label>
                Arquivo de operações
                <input
                  type="file"
                  accept=".csv,text/csv"
                  onChange={(e) => {
                    invalidar();
                    setArquivo(e.target.files?.[0] ?? null);
                  }}
                />
              </label>
              {arquivo && <p className="import-file-name">{arquivo.name}</p>}
              <label className="check-label">
                <input
                  type="checkbox"
                  checked={decisoes.aportesProprios}
                  onChange={(e) => {
                    invalidar();
                    setDecisoes((d) => ({ ...d, aportesProprios: e.target.checked }));
                  }}
                />
                Os depósitos são aportes próprios, sem realização de ganho.
              </label>
              <label className="check-label">
                <input
                  type="checkbox"
                  checked={decisoes.taxasIncluidas}
                  onChange={(e) => {
                    invalidar();
                    setDecisoes((d) => ({ ...d, taxasIncluidas: e.target.checked }));
                  }}
                />
                O total das compras OCBS inclui as taxas informadas no arquivo.
              </label>
              <label className="check-label">
                <input
                  type="checkbox"
                  checked={decisoes.posicaoAnteriorZerada}
                  onChange={(e) => {
                    invalidar();
                    setDecisoes((d) => ({ ...d, posicaoAnteriorZerada: e.target.checked }));
                  }}
                />
                Não havia posição anterior ao histórico deste arquivo.
              </label>
            </fieldset>
            <div className="form-actions">
              <button
                type="submit"
                className="secondary-button"
                disabled={
                  !arquivo ||
                  !decisoes.aportesProprios ||
                  !decisoes.taxasIncluidas ||
                  bloqueado ||
                  analisando
                }
              >
                {analisando ? "Analisando…" : "Analisar arquivo"}
              </button>
            </div>
          </form>
          {previa && <PreviaArquivo previa={previa} />}
          {incerta && (
            <p role="status" className="pending-request">
              O resultado do envio ainda não foi confirmado. Verifique a solicitação antes de
              continuar.
            </p>
          )}
          {erro && (
            <p role="alert" className="message-error">
              {erro}
            </p>
          )}
          {previa && (
            <div className="form-actions">
              <button
                type="button"
                className="primary-button"
                disabled={
                  salvando ||
                  previa.erros.length > 0 ||
                  previa.eventos.some((e) => e.situacao === "CONFLITO")
                }
                onClick={() => void confirmar()}
              >
                {salvando
                  ? "Confirmando…"
                  : incerta
                    ? "Verificar solicitação"
                    : "Confirmar importação"}
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
