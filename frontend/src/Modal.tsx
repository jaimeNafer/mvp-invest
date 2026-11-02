import { useEffect, useId, useRef } from "react";
import type { ReactNode } from "react";
import Icone from "./Icone";

type Props = {
  titulo: string;
  bloqueado: boolean;
  onFechar: () => void;
  children: ReactNode;
};

export default function Modal({ titulo, bloqueado, onFechar, children }: Props) {
  const dialogo = useRef<HTMLDialogElement>(null);
  const tituloId = useId();

  useEffect(() => {
    const elemento = dialogo.current;
    const focoAnterior = document.activeElement;
    elemento?.showModal();
    elemento?.querySelector<HTMLInputElement>("input:not([disabled])")?.focus();
    return () => {
      elemento?.close();
      if (focoAnterior instanceof HTMLElement && focoAnterior.isConnected) focoAnterior.focus();
    };
  }, []);

  return (
    <dialog
      ref={dialogo}
      className="modal"
      aria-labelledby={tituloId}
      aria-modal="true"
      onCancel={(event) => {
        event.preventDefault();
        if (!bloqueado) onFechar();
      }}
    >
      <div className="modal-heading">
        <h2 id={tituloId}>{titulo}</h2>
        <button
          className="icon-button"
          type="button"
          aria-label="Fechar modal"
          title="Fechar"
          disabled={bloqueado}
          onClick={onFechar}
        >
          <Icone nome="fechar" />
        </button>
      </div>
      {children}
    </dialog>
  );
}
