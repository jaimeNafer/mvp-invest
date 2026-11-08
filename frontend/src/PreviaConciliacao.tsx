import { useState } from "react";
import type { PreviaConciliacao as Previa } from "./services/conciliacao";
import { decimalPtBr } from "./services/decimais";

const acoes = {
  CRIAR: "Criar ajuste",
  MANTER: "Manter ajuste",
  REDUZIR: "Reduzir ajuste",
  ENCERRAR: "Encerrar ajuste",
  ALTERAR: "Alterar ajuste",
};
export default function PreviaConciliacao({
  previa,
  disabled,
  onConfirmar,
  onFechar,
}: {
  previa: Previa;
  disabled: boolean;
  onConfirmar: (lacunas: string[], correspondencias: string[]) => void;
  onFechar: () => void;
}) {
  const [lacunas, setLacunas] = useState<string[]>([]);
  const [correspondencias, setCorrespondencias] = useState<string[]>([]);
  const escolher = (atual: string[], id: string, marcado: boolean) =>
    marcado ? [...atual, id] : atual.filter((valor) => valor !== id);
  return (
    <section className="previa-conciliacao" aria-label="Prévia da conciliação">
      <h2>Revise as mudanças propostas</h2>
      <p>
        Ajustes explicam lacunas de quantidade. Não comprovam compra, aporte ou custo. Escolha
        somente o que deseja aceitar.
      </p>
      {(previa.registrosParaAplicar ?? 0) > 0 && (
        <p>
          {previa.registrosParaAplicar}{" "}
          {previa.registrosParaAplicar === 1 ? "registro aguarda" : "registros aguardam"} aplicação
          nesta conferência.
        </p>
      )}
      {(previa.grupos?.length ?? 0) > 0 && (
        <section aria-label="Operações agrupadas">
          <h3>Operações agrupadas nesta confirmação</h3>
          {previa.grupos?.map((grupo) => (
            <div key={grupo.previaId}>
              <strong>
                {grupo.numero ?? "Novo grupo"} ·{" "}
                {grupo.desfazer
                  ? "Desfazer associação"
                  : (grupo.natureza ?? "Classificação pendente")}
              </strong>
              <p>
                Recebido:{" "}
                {grupo.recebimentos.length
                  ? grupo.recebimentos
                      .map((item) => `${decimalPtBr(item.quantidade)} ${item.ativoTicker}`)
                      .join(" · ")
                  : "não identificado"}
                {" · "}
                Pago:{" "}
                {grupo.pagamentos.length
                  ? grupo.pagamentos
                      .map((item) => `${decimalPtBr(item.quantidade)} ${item.ativoTicker}`)
                      .join(" · ")
                  : "não identificado"}
                {grupo.taxas.length > 0 && (
                  <>
                    {" · "}Taxas:{" "}
                    {grupo.taxas
                      .map((item) => `${decimalPtBr(item.quantidade)} ${item.ativoTicker}`)
                      .join(" · ")}
                  </>
                )}
              </p>
              {grupo.principalBrl !== null && (
                <p>Principal conhecido: R$ {decimalPtBr(grupo.principalBrl)}</p>
              )}
              <details>
                <summary>Ver {grupo.linhas.length} linhas originais</summary>
                <ul>
                  {grupo.linhas.map((linha) => (
                    <li key={linha.id}>
                      <time dateTime={linha.data ?? undefined}>
                        {linha.data ?? "Data não informada"}
                      </time>
                      {" · "}
                      {linha.ativoTicker} {linha.quantidade ?? "Quantidade não informada"}
                      {" · "}
                      {linha.arquivo ?? "Arquivo não informado"}
                      {linha.linha !== null && ` · linha ${linha.linha}`}
                      {linha.aba && ` · aba ${linha.aba}`}
                    </li>
                  ))}
                </ul>
              </details>
              {grupo.pendencias.map((mensagem) => (
                <p key={mensagem}>{mensagem}</p>
              ))}
            </div>
          ))}
        </section>
      )}
      {previa.correspondencias.length > 0 && (
        <fieldset disabled={disabled}>
          <legend>Possíveis transferências internas</legend>
          {previa.correspondencias.map((item) => (
            <label className="escolha-conciliacao" key={item.id}>
              <input
                type="checkbox"
                checked={correspondencias.includes(item.id)}
                onChange={(evento) =>
                  setCorrespondencias(escolher(correspondencias, item.id, evento.target.checked))
                }
              />
              <span>
                <strong>{item.descricao}</strong>
                <span>
                  {item.contaOrigem} → {item.contaDestino} · {decimalPtBr(item.quantidade)}{" "}
                  {item.ativoTicker}
                </span>
                <small>{item.registroIds.join(" · ")}</small>
              </span>
            </label>
          ))}
        </fieldset>
      )}
      {previa.ajustes.length > 0 && (
        <fieldset disabled={disabled}>
          <legend>Ajustes provisórios e suas mudanças</legend>
          {previa.ajustes.map((item) => (
            <label className="escolha-conciliacao" key={item.lacunaId}>
              <input
                type="checkbox"
                checked={lacunas.includes(item.lacunaId)}
                onChange={(evento) =>
                  setLacunas(escolher(lacunas, item.lacunaId, evento.target.checked))
                }
              />
              <span>
                <strong>{acoes[item.acao]}</strong>
                <span>
                  {item.instituicaoNome} · {item.contaNome} · {decimalPtBr(item.quantidadeSugerida)}{" "}
                  {item.ativoTicker}
                </span>
                {item.quantidadeAnterior !== null && (
                  <small>
                    Quantidade anterior: {decimalPtBr(item.quantidadeAnterior)} {item.ativoTicker}
                  </small>
                )}
                <small>
                  {item.custoTotalBrl === null
                    ? "Custo desconhecido"
                    : `Custo em reais: ${decimalPtBr(item.custoTotalBrl)}`}{" "}
                  · Referência: {item.ancoraId}
                </small>
              </span>
            </label>
          ))}
        </fieldset>
      )}
      {previa.ajustes.length === 0 &&
        previa.correspondencias.length === 0 &&
        !(previa.grupos?.length ?? 0) && (
          <p>
            Nenhuma associação ou ajuste disponível nesta revisão. As informações pendentes
            permanecem identificadas no histórico.
          </p>
        )}
      <div className="conciliacao-actions">
        <button
          type="button"
          className="primary-button"
          disabled={
            disabled ||
            (lacunas.length + correspondencias.length === 0 &&
              !(previa.registrosParaAplicar ?? 0) &&
              !(previa.grupos?.length ?? 0))
          }
          onClick={() => onConfirmar(lacunas, correspondencias)}
        >
          Aplicar registros e conciliação
        </button>
        <button type="button" className="secondary-button" disabled={disabled} onClick={onFechar}>
          Resolver depois
        </button>
      </div>
    </section>
  );
}
