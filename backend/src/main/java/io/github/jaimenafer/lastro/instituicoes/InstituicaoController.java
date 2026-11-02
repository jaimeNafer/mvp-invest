package io.github.jaimenafer.lastro.instituicoes;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InstituicaoController {
    private final InstituicaoRepository instituicoes;

    public InstituicaoController(InstituicaoRepository instituicoes) {
        this.instituicoes = instituicoes;
    }

    @GetMapping("/api/v1/instituicoes")
    public List<Resposta> listar() {
        return instituicoes.findAllByOrderByNomeAscIdAsc().stream()
            .map(instituicao -> new Resposta(instituicao.getId().toString(), instituicao.getNome(), instituicao.isAtivo()))
            .toList();
    }

    public record Resposta(String id, String nome, boolean ativo) {}
}
