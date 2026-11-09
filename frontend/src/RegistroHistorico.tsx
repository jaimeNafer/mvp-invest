import { useEffect, useState } from "react";
import ConferenciaRegistroForm from "./ConferenciaRegistroForm";
import ValoracaoHistorico from "./ValoracaoHistorico";
import { classesEstados, nomesEstados } from "./services/conciliacao";
import type {
  ContaCustodia,
  DadosConferencia,
  FinalidadeCripto,
  RegistroTimeline,
} from "./services/conciliacao";
import { decimalPtBr } from "./services/decimais";
import type { Ativo } from "./services/saldosIniciais";

const origens = {
  IMPORTADA: "Importada",
  MANUAL: "Cadastrada",
  SINTETICA: "Ajuste provisório",
};
const nomesFinalidadeCripto: Record<FinalidadeCripto, string> = {
  DESCONHECIDA: "Informar depois",
  CARTEIRA_PROPRIA: "Outra carteira minha",
  TERCEIROS: "Terceiros/pagamento",
  OUTRA: "Outra finalidade",
};
export default function RegistroHistorico({
  registro,
  aberto,
  disabled,
  contas,
  ativos,
  onAbrir,
  onSalvar,
  onSalvarValoracao,
  onRevisar,
  onExcluirSaldoAnterior,
  podeExcluirSaldoAnterior = false,
  onAssociar,
  emGrupo = false,
  associacao,
}: {
  registro: RegistroTimeline;
  aberto: boolean;
  disabled: boolean;
  contas: ContaCustodia[];
  ativos: Ativo[];
  onAbrir: (aberto: boolean) => void;
  onSalvar: (dados: DadosConferencia, saldoAnterior: boolean) => void;
  onSalvarValoracao?: (valorTotalBrl: string) => void | Promise<void>;
  onRevisar: () => void;
  onExcluirSaldoAnterior?: () => void;
  podeExcluirSaldoAnterior?: boolean;
  onAssociar: () => void;
  emGrupo?: boolean;
  associacao?: React.ReactNode;
}) {
  const [edicao, setEdicao] = useState<"corrigir" | "saldo" | null>(null);
  const [finalidadeCripto, setFinalidadeCripto] = useState<FinalidadeCripto>(
    registro.finalidadeCripto ?? "DESCONHECIDA",
  );
  useEffect(() => {
    setFinalidadeCripto(registro.finalidadeCripto ?? "DESCONHECIDA");
  }, [registro.finalidadeCripto]);
  function salvarFinalidadeCripto() {
    const destino = nomesFinalidadeCripto[finalidadeCripto];
    onSalvar(
      {
        data: registro.data,
        contaId: registro.contaId,
        ativoId: registro.ativoId,
        quantidade: registro.quantidade,
        custoTotalBrl: registro.custoTotalBrl,
        natureza: "SAIDA_CRIPTO",
        finalidadeCripto,
        taxaInformada: registro.taxaInformada,
        taxaAtivoId: registro.taxaAtivoId,
        taxaQuantidade: registro.taxaQuantidade,
        motivo: `Finalidade da saída cripto: ${destino}.`,
      },
      false,
    );
  }
  const automatizado =
    registro.natureza === "CREDITO_AUTOMATICO" || registro.natureza === "MOVIMENTACAO_INTERNA";
  return (
    <article
      className={`registro-historico${automatizado ? " registro-credito" : ""}`}
      id={emGrupo ? undefined : `timeline-${registro.id}`}
      tabIndex={-1}
      aria-label={registro.descricao}
    >
      <div className="registro-linha">
        <div>
          <button
            type="button"
            disabled={disabled}
            className="registro-abrir"
            aria-expanded={aberto}
            aria-label={`Detalhar ${registro.descricao}`}
            onClick={() => {
              setEdicao(null);
              onAbrir(!aberto);
            }}
          >
            <span aria-hidden="true">{aberto ? "▾" : "▸"}</span> {registro.descricao}
          </button>
          <small>
            {registro.instituicaoNome} · {registro.contaNome} · {origens[registro.origem]}
          </small>
          {(registro.aguardaConfirmacao ?? registro.aplicado === false) && (
            <small>Aguardando confirmação global</small>
          )}
          {registro.natureza === "CREDITO_AUTOMATICO" && registro.subtipoCredito && (
            <small>
              {
                { BONIFICACAO: "Bonificação", AIRDROP: "Airdrop", RENDIMENTO: "Rendimento" }[
                  registro.subtipoCredito
                ]
              }
            </small>
          )}
          {registro.grupoNumero && <small>Operação {registro.grupoNumero}</small>}
          {registro.origem === "SINTETICA" && <small>Ajuste aceito para uma lacuna de saldo</small>}
        </div>
        <div className="registro-quantidade">
          {registro.quantidade === null
            ? "Quantidade desconhecida"
            : `${decimalPtBr(registro.quantidade)} ${registro.ativoTicker}`}
          <small>
            {registro.data
              ? new Date(registro.data).toLocaleTimeString("pt-BR", {
                  hour: "2-digit",
                  minute: "2-digit",
                })
              : registro.origem === "SINTETICA"
                ? "Posição pela referência do registro"
                : "Data não resolvida"}
          </small>
        </div>
        <div className="registro-estados">
          {(!automatizado || registro.estado !== "COERENTE") && (
            <span className={`estado-conferencia ${classesEstados[registro.estado]}`}>
              {nomesEstados[registro.estado]}
            </span>
          )}
          {registro.revisadoEm && (
            <small
              className="revisao-manual"
              title={`Revisada por ${registro.revisadoPor} em ${new Date(registro.revisadoEm).toLocaleString("pt-BR")}`}
            >
              Revisada por você
            </small>
          )}
        </div>
      </div>
      {aberto && (
        <div className="registro-detalhes">
          {!emGrupo && registro.valoracao && (
            <ValoracaoHistorico
              valoracao={registro.valoracao}
              disabled={disabled}
              onSalvar={(valorTotalBrl) => onSalvarValoracao?.(valorTotalBrl)}
            />
          )}
          {registro.natureza === "CREDITO_AUTOMATICO" &&
            registro.subtipoCredito === "RENDIMENTO" && (
              <p>A disponibilidade para negociação depende das condições da instituição.</p>
            )}
          {registro.natureza === "SAIDA_CRIPTO" && (
            <section aria-label="Finalidade da saída cripto">
              <p>
                Destino:{" "}
                {registro.finalidadeCripto && registro.finalidadeCripto !== "DESCONHECIDA"
                  ? nomesFinalidadeCripto[registro.finalidadeCripto]
                  : "Não identificado"}
              </p>
              {registro.saldoAnteriorPendente && (
                <p>Informe o saldo anterior para conferir a custódia.</p>
              )}
              {registro.taxaDetalhamentoPendente && <p>Taxa incluída, valor não informado.</p>}
              <label>
                Finalidade
                <select
                  value={finalidadeCripto}
                  disabled={disabled}
                  onChange={(evento) =>
                    setFinalidadeCripto(evento.target.value as FinalidadeCripto)
                  }
                >
                  {Object.entries(nomesFinalidadeCripto).map(([valor, nome]) => (
                    <option key={valor} value={valor}>
                      {nome}
                    </option>
                  ))}
                </select>
              </label>
              <button
                className="secondary-button"
                type="button"
                disabled={
                  disabled || finalidadeCripto === (registro.finalidadeCripto ?? "DESCONHECIDA")
                }
                onClick={salvarFinalidadeCripto}
              >
                Salvar finalidade
              </button>
            </section>
          )}
          {registro.principalConhecidoBrl != null && (
            <p>
              Principal conhecido da operação agrupada: R${" "}
              {decimalPtBr(registro.principalConhecidoBrl)}. Consulte a associação para conferir
              taxas e custo final.
            </p>
          )}
          <dl className="registro-metadados">
            <div>
              <dt>Identificador</dt>
              <dd>{registro.id}</dd>
            </div>
            {registro.natureza !== "APORTE" &&
              registro.natureza !== "RETIRADA" &&
              registro.natureza !== "SAIDA_CRIPTO" && (
                <div>
                  <dt>Custo total em reais</dt>
                  <dd>
                    {registro.custoTotalBrl === null
                      ? "Desconhecido"
                      : decimalPtBr(registro.custoTotalBrl)}
                  </dd>
                </div>
              )}
            <div>
              <dt>Versão</dt>
              <dd>{registro.versao}</dd>
            </div>
            {registro.ancoraId && (
              <div>
                <dt>Registro de referência</dt>
                <dd>{registro.ancoraId}</dd>
              </div>
            )}
          </dl>
          {registro.pendencias.length > 0 && (
            <ul className="registro-pendencias">
              {registro.pendencias.map((pendencia) => (
                <li key={pendencia.id}>
                  <strong>{pendencia.mensagem}</strong>
                  {pendencia.impacto && <span>{pendencia.impacto}</span>}
                </li>
              ))}
            </ul>
          )}
          {registro.aceitoEm && (
            <p>
              Ajuste aceito por {registro.aceitoPor} em{" "}
              {new Date(registro.aceitoEm).toLocaleString("pt-BR")} · versão {registro.versao}
            </p>
          )}
          {edicao ? (
            <ConferenciaRegistroForm
              key={`${registro.id}-${edicao}`}
              registro={registro}
              contas={contas}
              ativos={ativos}
              saldoAnterior={edicao === "saldo"}
              disabled={disabled}
              onCancelar={() => setEdicao(null)}
              onSalvar={(dados) => onSalvar(dados, edicao === "saldo")}
            />
          ) : (
            <div className="conciliacao-actions">
              {registro.id.startsWith("registro:") && (
                <>
                  <button
                    className="secondary-button"
                    disabled={disabled}
                    type="button"
                    onClick={() => setEdicao("saldo")}
                  >
                    Informar saldo anterior
                  </button>
                  {!registro.grupoAutomatico && (
                    <button
                      className="secondary-button"
                      disabled={disabled}
                      type="button"
                      onClick={onAssociar}
                    >
                      Associar a um registro
                    </button>
                  )}
                </>
              )}
              {registro.id.startsWith("registro:") && (
                <button
                  className="secondary-button"
                  disabled={disabled}
                  type="button"
                  onClick={() => setEdicao("corrigir")}
                >
                  Corrigir dados
                </button>
              )}
              <button
                className="secondary-button"
                disabled={disabled}
                type="button"
                onClick={() => onAbrir(false)}
              >
                Resolver depois
              </button>
              {registro.id.startsWith("registro:") && !registro.revisadoEm && (
                <button
                  className="secondary-button"
                  disabled={disabled}
                  type="button"
                  onClick={onRevisar}
                >
                  Marcar como revisada por você
                </button>
              )}
              {podeExcluirSaldoAnterior && (
                <button
                  className="secondary-button"
                  disabled={disabled}
                  type="button"
                  onClick={onExcluirSaldoAnterior}
                >
                  Excluir saldo anterior
                </button>
              )}
            </div>
          )}
          {associacao}
          {Object.keys(registro.camposOriginais).length > 0 && (
            <details className="campos-originais" open>
              <summary>Informações originais</summary>
              <dl>
                {Object.entries(registro.camposOriginais).map(([campo, valor]) => (
                  <div key={campo}>
                    <dt>{campo}</dt>
                    <dd>{valor}</dd>
                  </div>
                ))}
              </dl>
            </details>
          )}
        </div>
      )}
    </article>
  );
}
