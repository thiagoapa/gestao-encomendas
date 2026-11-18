package br.com.encomendas.domain.model;

import br.com.encomendas.domain.enums.Setor;
import jakarta.persistence.*;

import static br.com.encomendas.domain.model.Validacao.*;

/** Colaborador da empresa. Aparece como responsável em toda alteração registrada. */
@Entity
@Table(name = "funcionario")
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Setor setor;

    @Column(nullable = false)
    private boolean ativo;

    protected Funcionario() {
        // exigido pela JPA
    }

    public Funcionario(String nome, Setor setor) {
        this.nome = exigirTexto(nome, "nome");
        this.setor = exigirNaoNulo(setor, "setor");
        this.ativo = true;
    }

    // ---------- métodos modificadores ----------

    public void transferirParaSetor(Setor novoSetor) {
        exigirNaoNulo(novoSetor, "setor");
        exigir(novoSetor != setor, "O funcionário já pertence ao setor " + novoSetor);
        this.setor = novoSetor;
    }

    public void desativar() {
        exigir(ativo, "Funcionário já está inativo");
        this.ativo = false;
    }

    public void reativar() {
        exigir(!ativo, "Funcionário já está ativo");
        this.ativo = true;
    }

    // ---------- leitura ----------

    public boolean pertenceAo(Setor s) {
        return ativo && setor == s;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public Setor getSetor() { return setor; }
    public boolean isAtivo() { return ativo; }
}
