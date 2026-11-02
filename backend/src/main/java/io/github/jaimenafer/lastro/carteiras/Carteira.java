package io.github.jaimenafer.lastro.carteiras;

import io.github.jaimenafer.lastro.contas.Conta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
    @ManyToMany
    @JoinTable(name = "carteira_contas", joinColumns = @JoinColumn(name = "carteira_id"),
        inverseJoinColumns = @JoinColumn(name = "conta_id"))
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

    void alterar(String nome, Collection<Conta> contas) {
        this.nome = nome;
        this.contas.clear();
        this.contas.addAll(contas);
    }

    void alterarSituacao(boolean ativo) { this.ativo = ativo; }
}
