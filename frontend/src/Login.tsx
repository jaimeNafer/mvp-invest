import { useState } from "react";
import type { FormEvent } from "react";
import { entrar } from "./services/autenticacao";
import type { Usuario } from "./services/autenticacao";

export default function Login({ onLogin }: { onLogin: (usuario: Usuario) => void }) {
  const [login, setLogin] = useState("");
  const [senha, setSenha] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError("");
    try {
      onLogin(await entrar(login, senha));
    } catch {
      setError("Usuário ou senha inválidos.");
    } finally {
      setSenha("");
      setBusy(false);
    }
  }

  return (
    <div className="access-page">
      <section className="access-intro" aria-label="Lastro">
        <a className="brand" href="/" aria-label="Lastro — início">
          lastro<span>↗</span>
        </a>
        <p className="tagline">CLAREZA PARA SUAS CONTAS</p>
        <div className="access-promise">
          <p className="eyebrow">SEU HISTÓRICO, COM CONTEXTO</p>
          <h1>
            Entenda seus números.
            <br />
            Organize o próximo passo.
          </h1>
          <p>
            Operações, custos e resultados em um só lugar, com clareza para conferir cada registro.
          </p>
        </div>
      </section>
      <main className="access-main">
        <form className="login-card" onSubmit={submit} aria-labelledby="login-title">
          <span className="badge">Seu espaço no Lastro</span>
          <h2 id="login-title">Bem-vindo de volta.</h2>
          <p>Entre para acessar seu espaço.</p>
          <label htmlFor="login">Usuário</label>
          <input
            id="login"
            type="text"
            value={login}
            onChange={(event) => setLogin(event.target.value)}
            autoComplete="username"
            autoCapitalize="none"
            spellCheck={false}
            maxLength={120}
            required
            disabled={busy}
          />
          <label htmlFor="senha">Senha</label>
          <input
            id="senha"
            type="password"
            value={senha}
            onChange={(event) => setSenha(event.target.value)}
            autoComplete="current-password"
            disabled={busy}
          />
          {error && (
            <p className="access-error" role="alert">
              {error}
            </p>
          )}
          <button className="primary-button" type="submit" disabled={busy || !login.trim()}>
            {busy ? "Entrando…" : "Entrar no meu espaço"}
            <span aria-hidden="true">↗</span>
          </button>
        </form>
      </main>
    </div>
  );
}
