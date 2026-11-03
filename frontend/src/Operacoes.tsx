import { useState } from "react";
import Negociacoes from "./Negociacoes";
import SaldosIniciais from "./SaldosIniciais";

export default function Operacoes() {
  const [area, setArea] = useState<"negociacoes" | "saldos">("negociacoes");
  const [bloqueado, setBloqueado] = useState(false);
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
      </nav>
      {area === "negociacoes" ? (
        <Negociacoes onSaldos={() => setArea("saldos")} onBloqueio={setBloqueado} />
      ) : (
        <SaldosIniciais />
      )}
    </>
  );
}
