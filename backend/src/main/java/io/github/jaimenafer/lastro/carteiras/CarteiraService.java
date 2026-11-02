package io.github.jaimenafer.lastro.carteiras;

import io.github.jaimenafer.lastro.acesso.UsuarioAtual;
import io.github.jaimenafer.lastro.contas.Conta;
import io.github.jaimenafer.lastro.contas.ContaRepository;
import io.github.jaimenafer.lastro.instituicoes.InstituicaoRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CarteiraService {
    private final CarteiraRepository carteiras;
    private final ContaRepository contas;
    private final InstituicaoRepository instituicoes;
    private final UsuarioAtual usuarioAtual;

    public CarteiraService(CarteiraRepository carteiras, ContaRepository contas,
                          InstituicaoRepository instituicoes, UsuarioAtual usuarioAtual) {
        this.carteiras = carteiras;
        this.contas = contas;
        this.instituicoes = instituicoes;
        this.usuarioAtual = usuarioAtual;
    }

    public List<Carteira> listar(Boolean ativo) {
        return ativo == null ? carteiras.findByPerfilRefOrderByNomeAscIdAsc(usuarioAtual.id())
            : carteiras.findByPerfilRefAndAtivoOrderByNomeAscIdAsc(usuarioAtual.id(), ativo);
    }

    @Transactional
    public Carteira cadastrar(String nome, List<Long> instituicaoIds) {
        var carteira = new Carteira(usuarioAtual.id(), nome);
        carteira.alterar(nome, selecionarContas(instituicaoIds, Set.of()));
        return carteiras.saveAndFlush(carteira);
    }

    @Transactional
    public Carteira alterar(long id, String nome, List<Long> instituicaoIds) {
        var carteira = buscar(id);
        if (!carteira.isAtivo()) throw new DadosInvalidos("Reative a carteira para editá-la.");
        var anteriores = carteira.getContas().stream().map(conta -> conta.getInstituicao().getId())
            .collect(java.util.stream.Collectors.toSet());
        carteira.alterar(nome, selecionarContas(instituicaoIds, anteriores));
        return carteiras.saveAndFlush(carteira);
    }

    @Transactional
    public Carteira alterarSituacao(long id, boolean ativo) {
        var carteira = buscar(id);
        carteira.alterarSituacao(ativo);
        return carteira;
    }

    private List<Conta> selecionarContas(List<Long> ids, Set<Long> anteriores) {
        if (ids.isEmpty() || new HashSet<>(ids).size() != ids.size()) {
            throw new DadosInvalidos("Selecione pelo menos uma instituição, sem repetições.");
        }
        var selecionadas = instituicoes.findAllByIdIn(ids);
        if (selecionadas.size() != ids.size()
            || selecionadas.stream().anyMatch(instituicao -> !instituicao.isAtivo() && !anteriores.contains(instituicao.getId()))) {
            throw new DadosInvalidos("Selecione instituições disponíveis no catálogo.");
        }
        var existentes = contas.findAllByPerfilRefAndInstituicaoIdIn(usuarioAtual.id(), ids);
        return selecionadas.stream().map(instituicao -> existentes.stream()
            .filter(conta -> conta.getInstituicao().getId().equals(instituicao.getId())).findFirst()
            .orElseGet(() -> contas.save(new Conta(usuarioAtual.id(), instituicao)))).toList();
    }

    private Carteira buscar(long id) {
        return carteiras.findByIdAndPerfilRef(id, usuarioAtual.id())
            .orElseThrow(() -> new NaoEncontrada("Carteira não encontrada."));
    }

    public static class NaoEncontrada extends RuntimeException {
        NaoEncontrada(String mensagem) { super(mensagem); }
    }

    public static class DadosInvalidos extends RuntimeException {
        DadosInvalidos(String mensagem) { super(mensagem); }
    }
}
