package io.github.jaimenafer.lastro.contas;

import io.github.jaimenafer.lastro.instituicoes.Instituicao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "contas")
public class Conta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "perfil_ref", nullable = false, length = 120)
    private String perfilRef;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instituicao_id", nullable = false)
    private Instituicao instituicao;

    protected Conta() {}

    public Conta(String perfilRef, Instituicao instituicao) {
        this.perfilRef = perfilRef;
        this.instituicao = instituicao;
    }

    public Long getId() { return id; }
    public Instituicao getInstituicao() { return instituicao; }
}
