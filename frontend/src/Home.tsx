import type { Usuario } from "./services/autenticacao";

type HomeProps = {
  usuario: Usuario;
  onLogout: () => void;
};

export default function Home({ usuario, onLogout }: HomeProps) {
  return (
    <div className="layout">
      <aside className="sidebar" aria-label="Lastro">
        <a className="brand" href="/" aria-label="Lastro — início">
          lastro<span>↗</span>
        </a>
        <p className="tagline">CLAREZA PARA SUAS CONTAS</p>
        <nav aria-label="Navegação principal">
          <a className="sidebar-current" href="/" aria-current="page">
            Meu espaço
          </a>
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
            Meu espaço <span className="separator">/</span> Perfil
          </span>
        </header>
        <section className="intro" aria-labelledby="page-title">
          <p className="eyebrow">SEU ESPAÇO NO LASTRO</p>
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
      </main>
    </div>
  );
}
