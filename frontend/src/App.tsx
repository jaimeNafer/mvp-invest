import { useState } from "react";
import Home from "./Home";
import Login from "./Login";
import { sair } from "./services/autenticacao";
import type { Usuario } from "./services/autenticacao";

export default function App() {
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  return usuario ? (
    <Home
      usuario={usuario}
      onLogout={() => {
        sair();
        setUsuario(null);
      }}
    />
  ) : (
    <Login onLogin={setUsuario} />
  );
}
