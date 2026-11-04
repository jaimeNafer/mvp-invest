type Nome = "adicionar" | "editar" | "arquivar" | "reativar" | "fechar" | "informacao";

const desenhos: Record<Nome, string> = {
  adicionar: "M12 5v14M5 12h14",
  editar: "m16 3 5 5M3 21l5-1L21 7a2 2 0 0 0-5-5L3 16v5Z",
  arquivar: "M3 4h18v4H3zM5 8v12h14V8M9 12h6",
  reativar: "M3 10a9 9 0 1 1 1 8M3 4v6h6M12 8v8M8 12l4-4 4 4",
  fechar: "m6 6 12 12M6 18 18 6",
  informacao: "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20ZM12 11v6M12 7h.01",
};

export default function Icone({ nome }: { nome: Nome }) {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d={desenhos[nome]} />
    </svg>
  );
}
