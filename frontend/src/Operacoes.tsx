import { useCallback, useState } from "react";
import Importacoes from "./Importacoes";
import Negociacoes from "./Negociacoes";
import SaldosIniciais from "./SaldosIniciais";

export default function Operacoes({ onBloqueio }: { onBloqueio?: (b: boolean) => void }) {
  const [area, setArea] = useState<"negociacoes" | "saldos" | "importacoes">("negociacoes");
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
        <button
          type="button"
          className={area === "importacoes" ? "primary-button" : "secondary-button"}
          aria-pressed={area === "importacoes"}
          disabled={bloqueado}
          onClick={() => setArea("importacoes")}
        >
          Importar arquivo
        </button>
      </nav>
      {area === "negociacoes" ? (
        <Negociacoes onSaldos={() => setArea("saldos")} onBloqueio={bloquear} />
      ) : area === "importacoes" ? (
        <Importacoes onBloqueio={bloquear} />
      ) : (
        <SaldosIniciais />
      )}
    </>
  );
}
