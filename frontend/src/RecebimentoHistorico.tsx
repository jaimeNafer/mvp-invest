import { useState } from "react";
import { ErroHttp } from "./services/http";
import { mensagemConferencia, receberArquivos } from "./services/conciliacao";
import type { Recebimento } from "./services/conciliacao";

export default function RecebimentoHistorico({
  disabled,
  onRecebido,
  onArquivoRecebido,
  onBloqueio,
}: {
  disabled: boolean;
  onRecebido: () => Promise<void>;
  onArquivoRecebido?: (arquivos: { id: string; nomeArquivo: string }[]) => void;
  onBloqueio: (valor: boolean) => void;
}) {
  const [arquivos, setArquivos] = useState<File[]>([]);
  const [recibo, setRecibo] = useState<
    | (Omit<Recebimento, "arquivos"> & {
        arquivos: (Recebimento["arquivos"][number] & { chave: string })[];
      })
    | null
  >(null);
  const [erro, setErro] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [incerto, setIncerto] = useState(false);
  async function receber() {
    if (!arquivos.length || enviando) return;
    setEnviando(true);
    setErro("");
    onBloqueio(true);
    try {
      const resposta = await receberArquivos(arquivos);
      setRecibo({
        ...resposta,
        arquivos: resposta.arquivos.map((arquivo) => ({ ...arquivo, chave: crypto.randomUUID() })),
      });
      setIncerto(false);
      onBloqueio(false);
      onArquivoRecebido?.(
        resposta.arquivos.flatMap((arquivo) =>
          arquivo.id && /^\d+$/.test(arquivo.id)
            ? [{ id: arquivo.id, nomeArquivo: arquivo.nomeArquivo }]
            : [],
        ),
      );
      await onRecebido();
    } catch (falha) {
      const incerta = !(falha instanceof ErroHttp) || falha.status >= 500;
      setIncerto(incerta);
      onBloqueio(incerta);
      setErro(mensagemConferencia(falha));
    } finally {
      setEnviando(false);
    }
  }
  function selecionar(lista: FileList | null) {
    const selecionados = Array.from(lista ?? []);
    setRecibo(null);
    if (
      selecionados.length > 20 ||
      selecionados.reduce((total, arquivo) => total + arquivo.size, 0) > 10 * 1024 * 1024
    ) {
      setErro("Selecione até 20 arquivos, com tamanho total de até 10 MiB.");
      setArquivos([]);
      return;
    }
    if (selecionados.some((arquivo) => !/\.(csv|xlsx)$/i.test(arquivo.name))) {
      setErro("Selecione arquivos CSV ou XLSX.");
      setArquivos([]);
      return;
    }
    setArquivos(selecionados);
    setErro("");
  }
  return (
    <section className="recebimento-historico" aria-label="Importar histórico">
      <div className="recebimento-conteudo">
        <div className="recebimento-icone" aria-hidden="true">
          <svg
            aria-hidden="true"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.7"
          >
            <path d="M12 16V4m-4 4 4-4 4 4M4 15v3a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-3" />
          </svg>
        </div>
        <div className="recebimento-texto">
          <h2>Importar arquivos</h2>
          <p>Selecione os arquivos de operações da Binance.</p>
          <small id="recebimento-formatos">
            CSV ou XLSX · Até 20 arquivos, 10 MiB no total e 10.000 registros.
          </small>
        </div>
        <div className="recebimento-acoes">
          <label
            className={`secondary-button recebimento-seletor ${disabled || enviando || incerto ? "seletor-desabilitado" : ""}`}
          >
            <span>Selecionar arquivos</span>
            <input
              type="file"
              aria-label="Arquivos do histórico"
              aria-describedby="recebimento-formatos"
              multiple
              accept=".csv,.xlsx"
              disabled={disabled || enviando || incerto}
              onChange={(evento) => selecionar(evento.target.files)}
            />
          </label>
          <button
            className="primary-button"
            type="button"
            disabled={enviando || (!incerto && disabled) || !arquivos.length}
            onClick={receber}
          >
            {enviando ? "Recebendo…" : incerto ? "Repetir recebimento" : "Receber arquivos"}
          </button>
        </div>
      </div>
      {arquivos.length > 0 && !recibo && (
        <div className="recebimento-selecao" aria-live="polite">
          <strong>
            {arquivos.length}{" "}
            {arquivos.length === 1 ? "arquivo selecionado" : "arquivos selecionados"}
          </strong>
          <span>{arquivos.map((arquivo) => arquivo.name).join(" · ")}</span>
        </div>
      )}
      {erro && (
        <p role="alert">
          {erro}
          {incerto &&
            " O recebimento pode ter sido concluído. Repita com os mesmos arquivos para conferir."}
        </p>
      )}
      {recibo && (
        <div className="recibo-recebimento" role="status">
          <strong>
            {recibo.recebidos}{" "}
            {recibo.recebidos === 1 ? "registro recebido" : "registros recebidos"}
          </strong>
          <span>
            {" "}
            · {recibo.reconhecidos} reconhecidos · {recibo.problemas} arquivos com erro
          </span>
          <ul>
            {recibo.arquivos.map((arquivo) => (
              <li key={arquivo.chave}>
                <strong>{arquivo.nomeArquivo}</strong>:{" "}
                {arquivo.erro ? (
                  <span>{arquivo.erro}</span>
                ) : arquivo.reconhecido ? (
                  "Arquivo já recebido"
                ) : (
                  `${arquivo.registros} ${arquivo.registros === 1 ? "registro preservado" : "registros preservados"}`
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
