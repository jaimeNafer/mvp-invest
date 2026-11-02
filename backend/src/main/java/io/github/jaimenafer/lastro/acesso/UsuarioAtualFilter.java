package io.github.jaimenafer.lastro.acesso;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

@Component
@Order(0)
public class UsuarioAtualFilter extends OncePerRequestFilter {
    private final LeitorJwtMock tokens;
    private final UsuarioAtual usuarioAtual;
    private final JsonMapper json;

    public UsuarioAtualFilter(LeitorJwtMock tokens, UsuarioAtual usuarioAtual, JsonMapper json) {
        this.tokens = tokens;
        this.usuarioAtual = usuarioAtual;
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var caminho = request.getRequestURI().substring(request.getContextPath().length());
        return !caminho.equals("/api") && !caminho.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        UsuarioAtual.Dados usuario;
        try {
            var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
                throw new IllegalArgumentException("Bearer ausente.");
            }
            usuario = tokens.ler(authorization.substring(7));
        } catch (IllegalArgumentException erro) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            json.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Token de acesso ausente ou inválido. Entre novamente."));
            return;
        }
        usuarioAtual.carregar(usuario);
        chain.doFilter(request, response);
    }
}
