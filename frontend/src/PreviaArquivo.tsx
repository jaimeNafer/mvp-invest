import { useState } from "react";
import { decimalPtBr } from "./services/decimais";
import { dataHoraFortaleza } from "./services/operacoes";
import type { PreviaImportacao } from "./services/importacoes";
const situacoes = { NOVO: "Novo", RECONHECIDO: "Já registrado", CONFLITO: "Conflito" };
export default function PreviaArquivo({ previa }: { previa: PreviaImportacao }) {
  const [pagina, setPagina] = useState(0);
  const tamanho = 20;
  return (
    <section className="operation-preview" aria-label="Prévia da importação">
      <h2>Confira a importação</h2>
      <p>
        {previa.novos} novo(s) · {previa.reconhecidos} já registrado(s)
      </p>
      {previa.erros.length > 0 && (
        <div role="alert" className="message-error">
          <p>Nenhuma operação será gravada. Corrija os erros do arquivo e analise novamente.</p>
          <ul>
            {previa.erros.map((erro) => (
              <li key={`${erro.linha}-${erro.mensagem}`}>
                Linha {erro.linha}: {erro.mensagem}
              </li>
            ))}
          </ul>
        </div>
      )}
      <div className="table-scroll">
        <table className="data-table">
          <caption>Eventos do arquivo</caption>
          <thead>
            <tr>
              {[
                "Linha",
                "Data e hora (Fortaleza)",
                "Referência",
                "Tipo",
                "Quantidade",
                "Principal (BRL)",
                "Taxa (BRL)",
                "Situação",
              ].map((t) => (
                <th key={t}>{t}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {previa.eventos.slice(pagina * tamanho, (pagina + 1) * tamanho).map((evento) => (
              <tr key={`${evento.linha}-${evento.referencia}`}>
                <td>{evento.linha}</td>
                <td>{dataHoraFortaleza(evento.ocorridaEm)}</td>
                <td>{evento.referencia}</td>
                <td>
                  {evento.tipo === "APORTE"
                    ? "Aporte"
                    : evento.tipo === "COMPRA"
                      ? "Compra"
                      : evento.tipo}
                </td>
                <td>
                  {decimalPtBr(evento.quantidade)} {evento.ativo}
                </td>
                <td>{evento.principal === null ? "—" : decimalPtBr(evento.principal)}</td>
                <td>{decimalPtBr(evento.taxa)}</td>
                <td>{situacoes[evento.situacao]}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="pagination">
        <span>{previa.eventos.length} evento(s)</span>
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
          disabled={(pagina + 1) * tamanho >= previa.eventos.length}
          onClick={() => setPagina(pagina + 1)}
        >
          Próxima
        </button>
      </div>
    </section>
  );
}
