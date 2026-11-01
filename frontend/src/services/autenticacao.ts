import { armazenamentoLocal } from "./armazenamentoLocal";
import { gerarTokenMock } from "./jwt";

export type Usuario = { id: string; login: string; name: string };

const chaveToken = "lastro.token";

const usuarios = [
  { id: "1", login: "jaime", name: "Jaime", senha: "" },
  { id: "2", login: "eliane", name: "Eliane", senha: "" },
];

export async function entrar(login: string, senha: string): Promise<Usuario> {
  const usuario = usuarios.find(
    (candidato) => candidato.login === login.trim().toLowerCase() && candidato.senha === senha,
  );
  if (!usuario) throw new Error("Usuário ou senha inválidos.");
  const token = gerarTokenMock({
    sub: usuario.id,
    name: usuario.name,
    role: "acesso_total",
  });
  armazenamentoLocal.salvar(chaveToken, token);
  return { id: usuario.id, login: usuario.login, name: usuario.name };
}

export function sair(): void {
  armazenamentoLocal.remover(chaveToken);
}
