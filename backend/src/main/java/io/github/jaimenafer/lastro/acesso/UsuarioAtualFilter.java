package io.github.jaimenafer.lastro.acesso;

import io.github.jaimenafer.lastro.acesso.domain.exception.AcessoInvalidoException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(0)
@RequiredArgsConstructor
public class UsuarioAtualFilter extends OncePerRequestFilter {
  private final ILeitorToken tokens;
  private final UsuarioAtualContexto usuarioAtual;

  @Qualifier("handlerExceptionResolver")
  private final org.springframework.web.servlet.HandlerExceptionResolver erros;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    var caminho = request.getRequestURI().substring(request.getContextPath().length());
    return !caminho.equals("/api") && !caminho.startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    DadosUsuario usuario;
    try {
      var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
      if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
        throw new IllegalArgumentException("Bearer ausente.");
      }
      usuario = tokens.ler(authorization.substring(7));
    } catch (IllegalArgumentException erro) {
      erros.resolveException(request, response, null, new AcessoInvalidoException());
      return;
    }
    usuarioAtual.carregar(usuario);
    chain.doFilter(request, response);
  }
}
