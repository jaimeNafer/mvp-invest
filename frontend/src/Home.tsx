import { useCallback, useEffect, useState } from "react";
import Carteiras from "./Carteiras";
import Historico from "./Historico";
import ImportacoesProgressivas from "./ImportacoesProgressivas";
import Operacoes from "./Operacoes";
import VisaoGeral from "./VisaoGeral";
import type { Usuario } from "./services/autenticacao";

const areas = {
  "visao-geral": "Visão geral",
  carteiras: "Carteiras",
  operacoes: "Operações",
  importacoes: "Importações",
  conferencia: "Conferência",
  perfil: "Meu perfil",
} as const;

type Area = keyof typeof areas;
type Destino = {
  area: Area;
  carteira?: string;
  arquivo?: string;
  registro?: string;
  criar?: string;
};

function lerDestino(): Destino {
  const [rota, consulta] = window.location.hash.slice(2).split("?");
  const parametros = new URLSearchParams(consulta);
  return {
    area: Object.hasOwn(areas, rota) ? (rota as Area) : "visao-geral",
    carteira: parametros.get("carteira") ?? undefined,
    arquivo: parametros.get("arquivo") ?? undefined,
    registro: parametros.get("registro") ?? undefined,
    criar: parametros.get("criar") ?? undefined,
  };
}

function endereco({ area, ...contexto }: Destino) {
  const parametros = new URLSearchParams();
  for (const [chave, valor] of Object.entries(contexto)) {
    if (valor) parametros.set(chave, valor);
  }
  return `#/${area}${parametros.size ? `?${parametros}` : ""}`;
}

export default function Home({ usuario, onLogout }: { usuario: Usuario; onLogout: () => void }) {
  const [destino, setDestino] = useState(lerDestino);
  const [bloqueado, setBloqueado] = useState(false);
  const { area, carteira, arquivo, registro } = destino;
  const navegar = useCallback(
    (proximo: Destino) => {
      if (bloqueado) return;
      const url = endereco(proximo);
      if (window.location.hash !== url) window.history.pushState(null, "", url);
      setDestino(proximo);
    },
    [bloqueado],
  );
  useEffect(() => {
    function sincronizar() {
      if (bloqueado) {
        window.history.pushState(null, "", endereco(destino));
      } else {
        setDestino(lerDestino());
      }
    }
    window.addEventListener("popstate", sincronizar);
    return () => window.removeEventListener("popstate", sincronizar);
  }, [bloqueado, destino]);
  function itemMenu(item: Area) {
    return (
      <button
        key={item}
        className={`sidebar-link ${area === item ? "sidebar-current" : ""}`}
        type="button"
        disabled={bloqueado}
        aria-current={area === item ? "page" : undefined}
        onClick={() => navegar({ area: item })}
      >
        {areas[item]}
      </button>
    );
  }
  return (
    <div className={`layout ${area === "visao-geral" ? "visao-geral-layout" : ""}`}>
      <aside className="sidebar" aria-label="Lastro">
        <button
          className="brand"
          type="button"
          aria-label="Lastro — início"
          disabled={bloqueado}
          onClick={() => navegar({ area: "visao-geral" })}
        >
          lastro<span>↗</span>
        </button>
        <p className="tagline">CLAREZA PARA SUAS CONTAS</p>
        <nav aria-label="Navegação principal">
          {(["visao-geral", "carteiras", "operacoes", "importacoes", "conferencia"] as const).map(
            itemMenu,
          )}
        </nav>
        <div className="sidebar-footer">
          {itemMenu("perfil")}
          <div className="account-footer">
            <span>{usuario.name}</span>
            <button
              type="button"
              disabled={bloqueado}
              onClick={() => {
                window.history.replaceState(
                  null,
                  "",
                  window.location.pathname + window.location.search,
                );
                onLogout();
              }}
            >
              Sair
            </button>
          </div>
        </div>
      </aside>
      <main>
        <header className="topbar">
          <span>
            Lastro <span className="separator">/</span> {areas[area]}
          </span>
        </header>
        {area === "visao-geral" && (
          <VisaoGeral
            carteiraId={carteira}
            onCarteira={(id) => navegar({ area, carteira: id })}
            onCriarCarteira={() => navegar({ area: "carteiras", criar: "1" })}
            onImportacoes={() => navegar({ area: "importacoes", carteira })}
            onConferencia={() => navegar({ area: "conferencia" })}
            onOperacoes={() => navegar({ area: "operacoes", carteira })}
          />
        )}
        {area === "carteiras" && (
          <Carteiras
            iniciarCadastro={destino.criar === "1"}
            onVisao={(id) => navegar({ area: "visao-geral", carteira: id })}
            onOperacoes={(id) => navegar({ area: "operacoes", carteira: id })}
            onImportar={(id) => navegar({ area: "importacoes", carteira: id })}
          />
        )}
        {area === "operacoes" && (
          <Operacoes
            key={carteira ?? ""}
            carteiraId={carteira}
            arquivoId={arquivo}
            onBloqueio={setBloqueado}
            onConferencia={(id) => navegar({ area: "conferencia", registro: id, carteira })}
            onSelecionarArquivo={(id) => navegar({ area: "operacoes", carteira, arquivo: id })}
          />
        )}
        {area === "importacoes" && (
          <ImportacoesProgressivas
            key={carteira ?? ""}
            carteiraId={carteira}
            onBloqueio={setBloqueado}
            onOperacoes={(id, escopo) =>
              navegar({ area: "operacoes", arquivo: id, carteira: escopo })
            }
            onConferencia={() => navegar({ area: "conferencia", carteira })}
            onEditarCarteiras={() => navegar({ area: "carteiras" })}
            onCriarCarteira={() => navegar({ area: "carteiras", criar: "1" })}
          />
        )}
        {area === "conferencia" && (
          <Historico
            key={registro ?? ""}
            modoConferencia
            registroId={registro}
            onBloqueio={setBloqueado}
            onOperacoes={() => navegar({ area: "operacoes", carteira })}
          />
        )}
        {area === "perfil" && (
          <section className="intro" aria-labelledby="page-title">
            <h1 id="page-title">Meu perfil</h1>
            <div className="profile-card">
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
            </div>
          </section>
        )}
      </main>
    </div>
  );
}
