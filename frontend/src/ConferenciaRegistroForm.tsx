import { useId, useState } from "react";
import type {
  ContaCustodia,
  DadosConferencia,
  NaturezaRegistro,
  RegistroTimeline,
} from "./services/conciliacao";
import { decimalCanonico, decimalPtBr } from "./services/decimais";
import type { Ativo } from "./services/saldosIniciais";

function dataLocal(data: string | null) {
  if (!data) return "";
  const instante = new Date(data);
  const local = new Date(instante.getTime() - instante.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
}
function quantidadeCanonica(valor: string) {
  if (!valor.trim()) return null;
  const negativo = valor.trim().startsWith("-");
  return `${negativo ? "-" : ""}${decimalCanonico(negativo ? valor.trim().slice(1) : valor)}`;
}
export default function ConferenciaRegistroForm({
  registro,
  contas,
  ativos,
  saldoAnterior,
  disabled,
  onSalvar,
  onCancelar,
}: {
  registro: RegistroTimeline;
  contas: ContaCustodia[];
  ativos: Ativo[];
  saldoAnterior: boolean;
  disabled: boolean;
  onSalvar: (dados: DadosConferencia) => void;
  onCancelar: () => void;
}) {
  const prefixo = useId();
  const [data, setData] = useState(saldoAnterior ? "" : dataLocal(registro.data));
  const [contaId, setContaId] = useState(registro.contaId);
  const [ativoId, setAtivoId] = useState(registro.ativoId ?? "");
  const [quantidade, setQuantidade] = useState(
    saldoAnterior ? "" : registro.quantidade === null ? "" : decimalPtBr(registro.quantidade),
  );
  const [custo, setCusto] = useState(
    saldoAnterior || registro.custoTotalBrl === null ? "" : decimalPtBr(registro.custoTotalBrl),
  );
  const [motivo, setMotivo] = useState("");
  const [natureza, setNatureza] = useState<NaturezaRegistro>(
    saldoAnterior ? "SALDO_ANTERIOR" : (registro.natureza ?? "OBSERVADA"),
  );
  const [moedaCotacaoId, setMoedaCotacaoId] = useState(registro.moedaCotacaoId ?? "");
  const [totalBruto, setTotalBruto] = useState(
    registro.totalBruto == null ? "" : decimalPtBr(registro.totalBruto),
  );
  const [cotacaoBrl, setCotacaoBrl] = useState(
    registro.cotacaoBrl == null ? "" : decimalPtBr(registro.cotacaoBrl),
  );
  const [taxaInformada, setTaxaInformada] = useState(registro.taxaInformada ?? false);
  const [taxaAtivoId, setTaxaAtivoId] = useState(registro.taxaAtivoId ?? "");
  const [taxaQuantidade, setTaxaQuantidade] = useState(
    registro.taxaQuantidade == null ? "" : decimalPtBr(registro.taxaQuantidade),
  );
  const [erro, setErro] = useState("");
  const negociacao = natureza === "COMPRA" || natureza === "VENDA";
  const saidaCripto = natureza === "SAIDA_CRIPTO";
  const cotacoes = ativos.filter(
    (ativo) =>
      ["fiat:BRL", "cripto:USDT", "cripto:USDC"].includes(ativo.codigo) && ativo.id !== ativoId,
  );
  const moedasTaxa = ativos.filter((ativo) => ativo.id === ativoId || ativo.id === moedaCotacaoId);
  return (
    <form
      className="conferencia-form"
      onSubmit={(evento) => {
        evento.preventDefault();
        setErro("");
        try {
          onSalvar({
            data:
              !saldoAnterior && data === dataLocal(registro.data)
                ? registro.data
                : data
                  ? new Date(data).toISOString()
                  : null,
            contaId: contaId || null,
            ativoId: ativoId || null,
            quantidade: quantidadeCanonica(quantidade),
            custoTotalBrl: custo.trim() ? decimalCanonico(custo) : null,
            natureza,
            motivo: motivo.trim(),
            ...(saidaCripto
              ? {
                  finalidadeCripto: registro.finalidadeCripto ?? "DESCONHECIDA",
                  taxaInformada: Boolean(taxaQuantidade.trim()),
                  taxaAtivoId: taxaQuantidade.trim() ? ativoId || null : null,
                  taxaQuantidade: taxaQuantidade.trim() ? decimalCanonico(taxaQuantidade) : null,
                }
              : {}),
            ...(negociacao
              ? {
                  moedaCotacaoId: moedaCotacaoId || null,
                  totalBruto: totalBruto.trim() ? decimalCanonico(totalBruto) : null,
                  cotacaoBrl: cotacaoBrl.trim() ? decimalCanonico(cotacaoBrl) : null,
                  taxaInformada,
                  taxaAtivoId: taxaInformada ? taxaAtivoId || null : null,
                  taxaQuantidade:
                    taxaInformada && taxaQuantidade.trim() ? decimalCanonico(taxaQuantidade) : null,
                }
              : {}),
          });
        } catch (falha) {
          setErro(falha instanceof Error ? falha.message : "Confira os dados informados.");
        }
      }}
    >
      <h3>{saldoAnterior ? "Informar saldo anterior" : "Corrigir dados"}</h3>
      {saldoAnterior && (
        <p>
          Informe um saldo que você conhece e sua data real. O custo pode permanecer desconhecido.
        </p>
      )}
      <fieldset disabled={disabled}>
        {!saldoAnterior && (
          <>
            <label htmlFor={`${prefixo}-natureza`}>Natureza do registro</label>
            <select
              id={`${prefixo}-natureza`}
              value={natureza}
              onChange={(evento) => setNatureza(evento.target.value as NaturezaRegistro)}
            >
              <option value="OBSERVADA">Registro observado</option>
              <option value="SALDO_ANTERIOR">Saldo anterior informado</option>
              <option value="APORTE">Aporte em reais</option>
              <option value="RETIRADA">Retirada em reais</option>
              <option value="SAIDA_CRIPTO">Saída de criptomoeda</option>
              {natureza === "MOVIMENTACAO_INTERNA" && (
                <option value="MOVIMENTACAO_INTERNA">Movimentação interna</option>
              )}
              {natureza === "CREDITO_AUTOMATICO" && (
                <option value="CREDITO_AUTOMATICO">Crédito automático</option>
              )}
              <option value="COMPRA">Compra</option>
              <option value="VENDA">Venda</option>
            </select>
          </>
        )}
        <label htmlFor={`${prefixo}-data`}>Data e hora{!saldoAnterior && " (se conhecida)"}</label>
        <input
          id={`${prefixo}-data`}
          type="datetime-local"
          value={data}
          required={saldoAnterior}
          onChange={(evento) => setData(evento.target.value)}
        />
        <label htmlFor={`${prefixo}-conta`}>Conta de custódia</label>
        <select
          id={`${prefixo}-conta`}
          value={contaId}
          required={saldoAnterior}
          onChange={(evento) => setContaId(evento.target.value)}
        >
          <option value="">Não identificada</option>
          {contas.map((conta) => (
            <option key={conta.id} value={conta.id}>
              {conta.instituicaoNome} · {conta.nome}
            </option>
          ))}
        </select>
        <label htmlFor={`${prefixo}-ativo`}>Ativo</label>
        <select
          id={`${prefixo}-ativo`}
          value={ativoId}
          required={saldoAnterior}
          onChange={(evento) => setAtivoId(evento.target.value)}
        >
          <option value="">Não identificado</option>
          {ativos.map((ativo) => (
            <option key={ativo.id} value={ativo.id} disabled={!ativo.ativo}>
              {ativo.ticker}
              {!ativo.ativo ? " (inativo)" : ""}
            </option>
          ))}
        </select>
        <label htmlFor={`${prefixo}-quantidade`}>Quantidade</label>
        <input
          id={`${prefixo}-quantidade`}
          inputMode="decimal"
          required={saldoAnterior}
          value={quantidade}
          onChange={(evento) => setQuantidade(evento.target.value)}
        />
        {negociacao && (
          <>
            <p className="form-orientacao">
              Em compra e venda, informe a quantidade negociada positiva. Informações ausentes
              continuam pendentes.
            </p>
            <label htmlFor={`${prefixo}-cotacao`}>Moeda de cotação</label>
            <select
              id={`${prefixo}-cotacao`}
              value={moedaCotacaoId}
              onChange={(evento) => {
                setMoedaCotacaoId(evento.target.value);
                setTaxaAtivoId("");
              }}
            >
              <option value="">Não identificada</option>
              {cotacoes.map((ativo) => (
                <option key={ativo.id} value={ativo.id} disabled={!ativo.ativo}>
                  {ativo.ticker}
                </option>
              ))}
            </select>
            <label htmlFor={`${prefixo}-bruto`}>Total bruto</label>
            <input
              id={`${prefixo}-bruto`}
              inputMode="decimal"
              value={totalBruto}
              onChange={(evento) => setTotalBruto(evento.target.value)}
            />
            <label htmlFor={`${prefixo}-conversao`}>Cotação em reais (se conhecida)</label>
            <input
              id={`${prefixo}-conversao`}
              inputMode="decimal"
              value={cotacaoBrl}
              onChange={(evento) => setCotacaoBrl(evento.target.value)}
            />
            <label className="taxa-conhecida">
              <input
                type="checkbox"
                checked={taxaInformada}
                onChange={(evento) => setTaxaInformada(evento.target.checked)}
              />
              Taxa conhecida
            </label>
            <p className="form-orientacao">
              Marque somente se conhece a taxa. Informe zero explicitamente quando não houve taxa.
            </p>
            <label htmlFor={`${prefixo}-taxa-ativo`}>Moeda da taxa</label>
            <select
              id={`${prefixo}-taxa-ativo`}
              value={taxaAtivoId}
              disabled={!taxaInformada}
              onChange={(evento) => setTaxaAtivoId(evento.target.value)}
            >
              <option value="">Não identificada</option>
              {moedasTaxa.map((ativo) => (
                <option key={ativo.id} value={ativo.id}>
                  {ativo.ticker}
                </option>
              ))}
            </select>
            <label htmlFor={`${prefixo}-taxa`}>Quantidade da taxa</label>
            <input
              id={`${prefixo}-taxa`}
              inputMode="decimal"
              value={taxaQuantidade}
              disabled={!taxaInformada}
              onChange={(evento) => setTaxaQuantidade(evento.target.value)}
            />
          </>
        )}
        {saidaCripto && (
          <>
            <label htmlFor={`${prefixo}-taxa-saida`}>Taxa incluída na saída (se conhecida)</label>
            <input
              id={`${prefixo}-taxa-saida`}
              inputMode="decimal"
              value={taxaQuantidade}
              onChange={(evento) => setTaxaQuantidade(evento.target.value)}
            />
          </>
        )}
        {!saidaCripto && (
          <>
            <label htmlFor={`${prefixo}-custo`}>Custo total em reais (se conhecido)</label>
            <input
              id={`${prefixo}-custo`}
              inputMode="decimal"
              value={custo}
              onChange={(evento) => setCusto(evento.target.value)}
            />
            <p className="form-orientacao">
              Deixe o custo vazio se desconhecido. Zero declara custo conhecido igual a zero.
            </p>
          </>
        )}
        <label htmlFor={`${prefixo}-motivo`}>Motivo</label>
        <input
          id={`${prefixo}-motivo`}
          required
          maxLength={500}
          value={motivo}
          onChange={(evento) => setMotivo(evento.target.value)}
        />
      </fieldset>
      {erro && <p role="alert">{erro}</p>}
      <div className="conciliacao-actions">
        <button className="primary-button" disabled={disabled} type="submit">
          {saldoAnterior ? "Salvar saldo anterior" : "Salvar dados"}
        </button>
        <button className="secondary-button" type="button" disabled={disabled} onClick={onCancelar}>
          Cancelar edição
        </button>
      </div>
    </form>
  );
}
