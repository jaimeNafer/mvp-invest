import { useCallback, useState } from "react";
import Historico from "./Historico";
import Negociacoes from "./Negociacoes";
import SaldosIniciais from "./SaldosIniciais";

export default function Operacoes({ onBloqueio }: { onBloqueio?: (b: boolean) => void }) {
  const [area, setArea] = useState<"importacoes" | "negociacoes" | "saldos">("importacoes");
  const [bloqueado, setBloqueado] = useState(false);
  const bloquear = useCallback(
    (valor: boolean) => {
      setBloqueado(valor);
      onBloqueio?.(valor);
    },
    [onBloqueio],
  );
  return (
    <>
      <nav className="operations-navigation" aria-label="Áreas de operações">
        <button
          type="button"
          className={area === "importacoes" ? "primary-button" : "secondary-button"}
          aria-pressed={area === "importacoes"}
          disabled={bloqueado}
          onClick={() => setArea("importacoes")}
        >
          Importações
        </button>
        <button
          type="button"
          className={area === "negociacoes" ? "primary-button" : "secondary-button"}
          aria-pressed={area === "negociacoes"}
          disabled={bloqueado}
          onClick={() => setArea("negociacoes")}
        >
          Negociações
        </button>
        <button
          type="button"
          className={area === "saldos" ? "primary-button" : "secondary-button"}
          aria-pressed={area === "saldos"}
          disabled={bloqueado}
          onClick={() => setArea("saldos")}
        >
          Saldos iniciais
        </button>
      </nav>
      {area === "importacoes" ? (
        <Historico onBloqueio={bloquear} />
      ) : area === "negociacoes" ? (
        <Negociacoes onSaldos={() => setArea("saldos")} onBloqueio={bloquear} />
      ) : (
        <SaldosIniciais />
      )}
    </>
  );
}
