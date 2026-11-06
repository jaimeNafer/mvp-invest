import { obterToken } from "./autenticacao";

export class ErroHttp extends Error {
  constructor(
    public readonly status: number,
    mensagem: string,
  ) {
    super(mensagem);
  }
}

export async function requisitarJson<T>(caminho: string, opcoes: RequestInit = {}): Promise<T> {
  const headers = new Headers(opcoes.headers);
  headers.set("Accept", "application/json");
  if (opcoes.body && !(opcoes.body instanceof FormData))
    headers.set("Content-Type", "application/json");
  const token = obterToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);

  let resposta: Response;
  try {
    resposta = await fetch(`/api/v1${caminho}`, {
      ...opcoes,
      headers,
      cache: "no-store",
      signal: opcoes.signal ?? AbortSignal.timeout(10000),
    });
  } catch {
    throw new Error("Não foi possível conectar ao Lastro. Verifique a conexão e tente novamente.");
  }

  if (!resposta.ok) {
    const problema = await resposta.json().catch(() => null);
    throw new ErroHttp(
      resposta.status,
      typeof problema?.detail === "string"
        ? problema.detail
        : "Não foi possível concluir a solicitação. Tente novamente.",
    );
  }
  return resposta.json();
}
