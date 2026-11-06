import { useState } from "react";
import type { ReciboImportacao } from "./services/importacoes";
import { dataHoraFortaleza } from "./services/operacoes";
export default function ReciboArquivo({
  recibo,
  onOperacao,
}: {
  recibo: ReciboImportacao;
  onOperacao?: (id: string) => void;
}) {
  const [pagina, setPagina] = useState(0);
  const tamanho = 20;
  return (
    <section aria-label="Recibo da importação">
      <h2>Recibo da importação</h2>
      <dl className="saldo-resumo">
        <dt>Arquivo</dt>
        <dd>{recibo.nomeArquivo}</dd>
        <dt>Recebido em</dt>
        <dd>{dataHoraFortaleza(recibo.criadoEm)}</dd>
        <dt>Identificador</dt>
        <dd>{recibo.id}</dd>
        <dt>SHA-256</dt>
        <dd>{recibo.hash}</dd>
        <dt>Resultado</dt>
        <dd>
          {recibo.novos} novo(s) · {recibo.reconhecidos} já registrado(s)
        </dd>
      </dl>
      <div className="table-scroll">
        <table>
          <caption>Operações vinculadas</caption>
          <thead>
            <tr>
              <th>Linha</th>
              <th>Referência</th>
              <th>Operação</th>
            </tr>
          </thead>
          <tbody>
            {recibo.eventos.slice(pagina * tamanho, (pagina + 1) * tamanho).map((evento) => (
              <tr key={`${evento.linha}-${evento.operacaoId}`}>
                <td>{evento.linha}</td>
                <td>{evento.referencia}</td>
                <td>
                  {onOperacao ? (
                    <button
                      type="button"
                      className="secondary-button"
                      aria-label={`Consultar operação ${evento.operacaoId}`}
                      onClick={() => onOperacao(evento.operacaoId)}
                    >
                      {evento.operacaoId}
                    </button>
                  ) : (
                    evento.operacaoId
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="pagination">
        <span>{recibo.eventos.length} operação(ões)</span>
        <button
          type="button"
          className="secondary-button"
          disabled={pagina === 0}
          onClick={() => setPagina(pagina - 1)}
        >
          Anterior
        </button>
        <span>Página {pagina + 1}</span>
        <button
          type="button"
          className="secondary-button"
          disabled={(pagina + 1) * tamanho >= recibo.eventos.length}
          onClick={() => setPagina(pagina + 1)}
        >
          Próxima
        </button>
      </div>
    </section>
  );
}
