package io.github.jaimenafer.lastro.acesso;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.*;

class LeitorJwtMockTests {
    private final JsonMapper json = JsonMapper.builder().build();
    private final LeitorJwtMock leitor = new LeitorJwtMock(json);

    @Test void extraiIdentificadorNomeUnicodeERoleSemGuardarToken() {
        var usuario = leitor.ler(token(Map.of("sub", "pessoa-1", "name", "João 日本", "role", "acesso_total")));
        assertThat(usuario).isEqualTo(new UsuarioAtual.Dados("pessoa-1", "João 日本", "acesso_total"));
    }

    @Test void recusaEstruturaBase64JsonEAlgoritmoIncompativeisComMock() {
        var valido = token(Map.of("sub", "1", "name", "Jaime", "role", "acesso_total"));
        for (String invalido : new String[]{"", "abc", "a.b.c.d", "a.b.", "@@.@@.",
                segmento("{\"alg\":\"none\"}") + "." + segmento("[]") + ".",
                segmento("{\"alg\":\"none\"}") + "." + segmento("{invalido") + ".",
                segmento("{\"alg\":\"HS256\"}") + "." + valido.split("\\.")[1] + ".",
                valido + "assinatura", "a".repeat(8193)}) {
            assertThatThrownBy(() -> leitor.ler(invalido)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> leitor.ler(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void exigeClaimsTextuaisNaoVaziasDentroDosLimites() {
        for (String campo : new String[]{"sub", "name", "role"}) {
            var claims = new HashMap<String, Object>(Map.of("sub", "1", "name", "Jaime", "role", "acesso_total"));
            claims.remove(campo);
            assertThatThrownBy(() -> leitor.ler(token(claims))).isInstanceOf(IllegalArgumentException.class);
            for (Object invalido : new Object[]{null, 1, true, "   ", "a".repeat(campo.equals("name") ? 161 : 121)}) {
                claims.put(campo, invalido);
                assertThatThrownBy(() -> leitor.ler(token(claims))).isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    private String token(Map<String, ?> claims) {
        return segmento("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "."
            + Base64.getUrlEncoder().withoutPadding().encodeToString(json.writeValueAsBytes(claims)) + ".";
    }

    private String segmento(String valor) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(valor.getBytes(StandardCharsets.UTF_8));
    }
}
