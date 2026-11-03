package io.github.jaimenafer.lastro.acesso;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class UsuarioAtualContexto implements IUsuarioAtual {
  private DadosUsuario dados;

  public void carregar(DadosUsuario dados) {
    this.dados = dados;
  }

  public String id() {
    return obterDadosUsuario().id();
  }

  public String nome() {
    return obterDadosUsuario().nome();
  }

  public String role() {
    return obterDadosUsuario().role();
  }

  private DadosUsuario obterDadosUsuario() {
    if (dados == null) throw new IllegalStateException("Usuário não carregado nesta requisição.");
    return dados;
  }
}
