import { useState } from "react";
import Carteiras from "./Carteiras";
import Operacoes from "./Operacoes";
import VisaoGeral from "./VisaoGeral";
import type { Usuario } from "./services/autenticacao";

type HomeProps = {
  usuario: Usuario;
  onLogout: () => void;
};

export default function Home({ usuario, onLogout }: HomeProps) {
  const [pagina, setPagina] = useState<"visao" | "perfil" | "carteiras" | "operacoes">("visao");
  return (
    <div className={`layout ${pagina === "visao" ? "visao-geral-layout" : ""}`}>
      <aside className="sidebar" aria-label="Lastro">
        <a className="brand" href="/" aria-label="Lastro — início">
          lastro<span>↗</span>
        </a>
        <p className="tagline">CLAREZA PARA SUAS CONTAS</p>
        <nav aria-label="Navegação principal">
          <button
            className={`sidebar-link ${pagina === "visao" ? "sidebar-current" : ""}`}
            type="button"
            aria-current={pagina === "visao" ? "page" : undefined}
            onClick={() => setPagina("visao")}
          >
            Visão geral
          </button>
          <button
            className={`sidebar-link ${pagina === "perfil" ? "sidebar-current" : ""}`}
            type="button"
            aria-current={pagina === "perfil" ? "page" : undefined}
            onClick={() => setPagina("perfil")}
          >
            Meu perfil
          </button>
          <button
            className={`sidebar-link ${pagina === "carteiras" ? "sidebar-current" : ""}`}
            type="button"
            aria-current={pagina === "carteiras" ? "page" : undefined}
            onClick={() => setPagina("carteiras")}
          >
            Carteiras
          </button>
          <button
            className={`sidebar-link ${pagina === "operacoes" ? "sidebar-current" : ""}`}
            type="button"
            aria-current={pagina === "operacoes" ? "page" : undefined}
            onClick={() => setPagina("operacoes")}
          >
            Operações
          </button>
        </nav>
        <div className="sidebar-footer account-footer">
          <span>{usuario.name}</span>
          <button type="button" onClick={onLogout}>
            Sair
          </button>
        </div>
      </aside>
      <main>
        <header className="topbar">
          <span>
            Lastro <span className="separator">/</span>{" "}
            {pagina === "visao"
              ? "Visão geral"
              : pagina === "perfil"
                ? "Perfil"
                : pagina === "carteiras"
                  ? "Carteiras"
                  : "Operações"}
          </span>
        </header>
        {pagina === "visao" ? (
          <VisaoGeral onOperacoes={() => setPagina("operacoes")} />
        ) : pagina === "operacoes" ? (
          <Operacoes />
        ) : pagina === "carteiras" ? (
          <Carteiras />
        ) : (
          <>
            <section className="intro" aria-labelledby="page-title">
              <p className="eyebrow">SUA CONTA NO LASTRO</p>
              <h1 id="page-title">Olá, {usuario.name}.</h1>
              <p className="intro-text">Consulte as informações do seu perfil.</p>
            </section>
            <section className="profile-card" aria-labelledby="space-title">
              <h2 id="space-title">Meu perfil</h2>
              <dl className="profile-details">
                <div>
                  <dt>Nome</dt>
                  <dd>{usuario.name}</dd>
                </div>
                <div>
                  <dt>Usuário</dt>
                  <dd>{usuario.login}</dd>
                </div>
              </dl>
            </section>
          </>
        )}
      </main>
    </div>
  );
}
