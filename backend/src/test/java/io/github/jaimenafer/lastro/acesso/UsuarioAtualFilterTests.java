package io.github.jaimenafer.lastro.acesso;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioAtualFilterTests {
    private final JsonMapper json = JsonMapper.builder().build();

    @Test void carregaTodasAsInformacoesAntesDeProsseguirSemEsconderErrosDaAplicacao() {
        var usuario = new UsuarioAtualContexto();
        var filtro = new UsuarioAtualFilter(new LeitorJwtMock(json), usuario, (request, response, handler, erro) -> { throw new AssertionError(erro); });
        var request = new MockHttpServletRequest("GET", "/api/v1/titulares");
        var encoder = Base64.getUrlEncoder().withoutPadding();
        var header = encoder.encodeToString(json.writeValueAsBytes(Map.of("alg", "none")));
        var claims = encoder.encodeToString(json.writeValueAsBytes(Map.of("sub", "externo-1", "name", "Érica", "role", "consulta")));
        request.addHeader("Authorization", "bearer " + header + "." + claims + ".");
        assertThatThrownBy(() -> filtro.doFilter(request, new MockHttpServletResponse(), (entrada, saida) -> {
            assertThat(usuario.id()).isEqualTo("externo-1");
            assertThat(usuario.nome()).isEqualTo("Érica");
            assertThat(usuario.role()).isEqualTo("consulta");
            throw new IllegalArgumentException("Erro de negócio.");
        })).isInstanceOf(IllegalArgumentException.class).hasMessage("Erro de negócio.");
    }

    @Test void naoExigeTokenParaPaginaOuArquivosEstaticos() throws Exception {
        var usuario = new UsuarioAtualContexto();
        var filtro = new UsuarioAtualFilter(new LeitorJwtMock(json), usuario, (request, response, handler, erro) -> { throw new AssertionError(erro); });
        for (String caminho : new String[]{"/", "/index.html", "/assets/app.js", "/error"}) {
            var executou = new AtomicBoolean();
            filtro.doFilter(new MockHttpServletRequest("GET", caminho), new MockHttpServletResponse(),
                (entrada, saida) -> executou.set(true));
            assertThat(executou).isTrue();
        }
        assertThatThrownBy(usuario::id).isInstanceOf(IllegalStateException.class);
    }
}
