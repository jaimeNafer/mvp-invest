import { useId, useState } from "react";
import Icone from "./Icone";

export default function AjudaCampo({ rotulo, descricao }: { rotulo: string; descricao: string }) {
  const id = useId();
  const [aberta, setAberta] = useState(false);
  return (
    <span className="field-help">
      <span>{rotulo}</span>
      <button
        type="button"
        className="field-help-button"
        aria-label={`Sobre ${rotulo.toLowerCase()}`}
        aria-describedby={id}
        onMouseEnter={() => setAberta(true)}
        onMouseLeave={(event) => {
          if (!event.currentTarget.contains(document.activeElement)) setAberta(false);
        }}
        onFocus={() => setAberta(true)}
        onBlur={() => setAberta(false)}
        onClick={() => setAberta(true)}
        onKeyDown={(event) => {
          if (event.key === "Escape") {
            event.preventDefault();
            event.stopPropagation();
            setAberta(false);
          }
        }}
      >
        <Icone nome="informacao" />
        <span id={id} role="tooltip" className="field-help-tooltip" hidden={!aberta}>
          {descricao}
        </span>
      </button>
    </span>
  );
}
