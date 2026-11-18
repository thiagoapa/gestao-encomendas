package br.com.encomendas.internal.produto.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

import static br.com.encomendas.shared.util.Validacao.*;

/** Item do catálogo (caneca, camiseta, topo de bolo...). */
@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoBase;

    /**
     * Esforço de produção de UMA unidade, em "pontos de capacidade".
     * É o que a CapacidadeDiaria soma para saber se o dia está lotado.
     */
    @Column(nullable = false)
    private int pontosCapacidade;

    @Column(nullable = false)
    private boolean ativo;

    protected Produto() {
        // exigido pela JPA
    }

    public Produto(String nome, String descricao, BigDecimal precoBase, int pontosCapacidade) {
        this.nome = exigirTexto(nome, "nome");
        this.descricao = textoOpcional(descricao);
        this.precoBase = validarPreco(precoBase);
        this.pontosCapacidade = validarPontos(pontosCapacidade);
        this.ativo = true;
    }

    // ---------- métodos modificadores ----------

    /** Não afeta encomendas já feitas: o item guarda o preço do momento da venda. */
    public void reajustarPreco(BigDecimal novoPreco) {
        this.precoBase = validarPreco(novoPreco);
    }

    public void redefinirEsforco(int pontos) {
        this.pontosCapacidade = validarPontos(pontos);
    }

    public void atualizarDescricao(String descricao) {
        this.descricao = textoOpcional(descricao);
    }

    public void desativar() {
        exigir(ativo, "Produto já está inativo");
        this.ativo = false;
    }

    public void reativar() {
        exigir(!ativo, "Produto já está ativo");
        this.ativo = true;
    }

    private static BigDecimal validarPreco(BigDecimal preco) {
        exigir(preco != null && preco.signum() > 0, "O preço deve ser maior que zero");
        return preco;
    }

    private static int validarPontos(int pontos) {
        exigir(pontos > 0, "O esforço de produção deve ser maior que zero");
        return pontos;
    }

    // ---------- leitura ----------

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public BigDecimal getPrecoBase() { return precoBase; }
    public int getPontosCapacidade() { return pontosCapacidade; }
    public boolean isAtivo() { return ativo; }
}
