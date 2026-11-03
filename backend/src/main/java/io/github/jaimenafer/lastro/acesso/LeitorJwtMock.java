package io.github.jaimenafer.lastro.acesso;

import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class LeitorJwtMock implements ILeitorToken {
  private final JsonMapper json;

  public DadosUsuario ler(String token) {
    if (token == null || token.length() > 8192)
      throw new IllegalArgumentException("Token inválido.");
    var partes = token.split("\\.", -1);
    if (partes.length != 3 || !partes[2].isEmpty())
      throw new IllegalArgumentException("Token inválido.");
    try {
      var header = decodificar(partes[0]);
      if (!"none".equals(texto(header, "alg", 20)))
        throw new IllegalArgumentException("Token inválido.");
      var claims = decodificar(partes[1]);
      return new DadosUsuario(
          texto(claims, "sub", 120), texto(claims, "name", 160), texto(claims, "role", 120));
    } catch (JacksonException erro) {
      throw new IllegalArgumentException("Token inválido.", erro);
    }
  }

  private JsonNode decodificar(String segmento) {
    if (segmento.isEmpty() || !segmento.matches("[A-Za-z0-9_-]+")) {
      throw new IllegalArgumentException("Token inválido.");
    }
    var objeto = json.readTree(Base64.getUrlDecoder().decode(segmento));
    if (objeto == null || !objeto.isObject()) throw new IllegalArgumentException("Token inválido.");
    return objeto;
  }

  private String texto(JsonNode objeto, String campo, int limite) {
    var valor = objeto.get(campo);
    if (valor == null
        || !valor.isString()
        || valor.stringValue().isBlank()
        || valor.stringValue().length() > limite)
      throw new IllegalArgumentException("Token inválido.");
    return valor.stringValue();
  }
}
