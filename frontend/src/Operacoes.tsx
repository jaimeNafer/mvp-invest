import { useCallback, useState } from "react";

import Negociacoes from "./Negociacoes";

import SaldosIniciais from "./SaldosIniciais";

export default function Operacoes({
  carteiraId,

  arquivoId,

  onBloqueio,

  onConferencia,

  onSelecionarArquivo,
}: {
  carteiraId?: string;

  arquivoId?: string;

  onBloqueio: (b: boolean) => void;

  onConferencia: (id: string) => void;

  onSelecionarArquivo: (id?: string) => void;
}) {
  const [saldos, setSaldos] = useState(false);

  const [bloqueado, setBloqueado] = useState(false);

  const bloquear = useCallback(
    (b: boolean) => {
      setBloqueado(b);

      onBloqueio(b);
    },

    [onBloqueio],
  );

  return (
    <>
      {saldos && (
        <div className="operations-navigation">
          <button
            type="button"
            className="secondary-button"
            disabled={bloqueado}
            onClick={() => setSaldos(!saldos)}
          >
            {saldos ? "Voltar às operações" : "Saldos iniciais"}
          </button>
        </div>
      )}
      {saldos ? (
        <SaldosIniciais />
      ) : (
        <Negociacoes
          carteiraId={carteiraId}
          arquivoId={arquivoId}
          onSaldos={() => setSaldos(true)}
          onBloqueio={bloquear}
          onConferencia={onConferencia}
          onSelecionarArquivo={onSelecionarArquivo}
        />
      )}
    </>
  );
}
