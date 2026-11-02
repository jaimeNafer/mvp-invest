package io.github.jaimenafer.lastro.carteiras;

import io.github.jaimenafer.lastro.contas.Conta;
import io.github.jaimenafer.lastro.instituicoes.Instituicao;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "carteiras")
public class Carteira {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "perfil_ref", nullable = false, length = 120)
    private String perfilRef;
    @Column(nullable = false, length = 160)
    private String nome;
    @Column(nullable = false)
    private boolean ativo = true;
    @OneToMany(mappedBy = "carteira", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private Set<Conta> contas = new HashSet<>();

    protected Carteira() {}

    Carteira(String perfilRef, String nome) {
        this.perfilRef = perfilRef;
        this.nome = nome;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public boolean isAtivo() { return ativo; }
    public Set<Conta> getContas() { return Set.copyOf(contas); }

    void alterar(String nome, Collection<Instituicao> instituicoes) {
        this.nome = nome;
        var ids = instituicoes.stream().map(Instituicao::getId).collect(java.util.stream.Collectors.toSet());
        contas.removeIf(conta -> !ids.contains(conta.getInstituicao().getId()));
        for (var instituicao : instituicoes) {
            if (contas.stream().noneMatch(conta -> conta.getInstituicao().getId().equals(instituicao.getId()))) {
                contas.add(new Conta(this, instituicao));
            }
        }
    }

    void alterarSituacao(boolean ativo) { this.ativo = ativo; }
}
