package io.github.jaimenafer.lastro.acesso;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class UsuarioAtual {
    private Dados dados;

    void carregar(Dados dados) {
        this.dados = dados;
    }

    public String id() { return obterDados().id(); }
    public String nome() { return obterDados().nome(); }
    public String role() { return obterDados().role(); }

    private Dados obterDados() {
        if (dados == null) throw new IllegalStateException("Usuário não carregado nesta requisição.");
        return dados;
    }

    public record Dados(String id, String nome, String role) {}
}
