import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import type { CadastroCarteira, Carteira } from "./services/carteiras";
import { listarInstituicoes } from "./services/instituicoes";
import type { Instituicao } from "./services/instituicoes";

type Props = {
  carteira: Carteira | null;
  salvando: boolean;
  erroServidor: string;
  onSalvar: (cadastro: CadastroCarteira) => Promise<boolean>;
  onCancelar: () => void;
};

export default function CarteiraForm({
  carteira,
  salvando,
  erroServidor,
  onSalvar,
  onCancelar,
}: Props) {
  const [nome, setNome] = useState(carteira?.nome ?? "");
  const [selecionadas, setSelecionadas] = useState<string[]>(
    carteira?.contas.map((conta) => conta.instituicaoId) ?? [],
  );
  const [instituicoes, setInstituicoes] = useState<Instituicao[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erroCatalogo, setErroCatalogo] = useState("");
  const [erro, setErro] = useState("");

  const carregar = useCallback(async (signal?: AbortSignal) => {
    setCarregando(true);
    setErroCatalogo("");
    try {
      const dados = await listarInstituicoes(signal);
      if (!signal?.aborted) setInstituicoes(dados);
    } catch (falha) {
      if (!signal?.aborted)
        setErroCatalogo(
          falha instanceof Error ? falha.message : "Não foi possível carregar as instituições.",
        );
    } finally {
      if (!signal?.aborted) setCarregando(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    void carregar(controller.signal);
    return () => controller.abort();
  }, [carregar]);

  async function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (salvando || carregando || erroCatalogo) return;
    if (!nome.trim() || selecionadas.length === 0) {
      setErro(
        !nome.trim() ? "Informe o nome da carteira." : "Selecione pelo menos uma instituição.",
      );
      return;
    }
    setErro("");
    await onSalvar({ nome: nome.trim(), instituicaoIds: selecionadas });
  }

  return (
    <form
      className="carteira-form"
      onSubmit={enviar}
      aria-label={carteira ? "Editar carteira" : "Adicionar carteira"}
    >
      <label htmlFor="nome-carteira">Nome da carteira</label>
      <input
        id="nome-carteira"
        type="text"
        value={nome}
        onChange={(event) => setNome(event.target.value)}
        maxLength={160}
        required
        disabled={salvando}
        autoComplete="off"
      />
      <fieldset className="instituicoes-selecao" disabled={salvando}>
        <legend>Instituições</legend>
        <p className="field-hint">Selecione as contas que deseja reunir nesta carteira.</p>
        {carregando ? (
          <p role="status">Carregando instituições…</p>
        ) : erroCatalogo ? (
          <div>
            <p className="message-error" role="alert">
              {erroCatalogo}
            </p>
            <button className="secondary-button" type="button" onClick={() => void carregar()}>
              Tentar novamente
            </button>
          </div>
        ) : instituicoes.length === 0 ? (
          <p className="field-hint">Nenhuma instituição disponível.</p>
        ) : (
          instituicoes.map((instituicao) => {
            const existente =
              carteira?.contas.some((conta) => conta.instituicaoId === instituicao.id) ?? false;
            return (
              <label
                key={instituicao.id}
                className={`instituicao-opcao ${selecionadas.includes(instituicao.id) ? "selecionada" : ""}`}
              >
                <input
                  type="checkbox"
                  checked={selecionadas.includes(instituicao.id)}
                  disabled={!instituicao.ativo && !existente}
                  onChange={(event) =>
                    setSelecionadas((atuais) =>
                      event.target.checked
                        ? [...atuais, instituicao.id]
                        : atuais.filter((id) => id !== instituicao.id),
                    )
                  }
                />
                <span>
                  {instituicao.nome}
                  {!instituicao.ativo && <small> · Indisponível para novos vínculos</small>}
                </span>
              </label>
            );
          })
        )}
      </fieldset>
      {(erro || erroServidor) && (
        <p className="message-error" role="alert">
          {erro || erroServidor}
        </p>
      )}
      <div className="form-actions">
        <button
          className="primary-button"
          type="submit"
          disabled={salvando || carregando || !!erroCatalogo || instituicoes.length === 0}
        >
          {salvando ? "Salvando…" : carteira ? "Salvar alterações" : "Salvar carteira"}
        </button>
        <button className="secondary-button" type="button" onClick={onCancelar} disabled={salvando}>
          Cancelar
        </button>
      </div>
    </form>
  );
}
