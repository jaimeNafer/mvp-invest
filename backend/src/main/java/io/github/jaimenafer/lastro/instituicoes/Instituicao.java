package io.github.jaimenafer.lastro.instituicoes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "instituicoes")
public class Instituicao {
    @Id
    private Long id;
    @Column(nullable = false, length = 160)
    private String nome;
    @Column(nullable = false)
    private boolean ativo;

    protected Instituicao() {}
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public boolean isAtivo() { return ativo; }
}
