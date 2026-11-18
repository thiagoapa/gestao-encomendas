package br.com.encomendas.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

import static br.com.encomendas.domain.model.Validacao.*;

/**
 * Linha da encomenda. Guarda o preço e o esforço do produto NO MOMENTO da venda,
 * para que reajustes futuros do catálogo não alterem encomendas já fechadas.
 */
@Entity
@Table(name = "item_encomenda")
public class ItemEncomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "encomenda_id", nullable = false)
    private Encomenda encomenda;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private int pontosPorUnidade;

    /** Nomes, frases, cores, fontes... o que for específico deste item. */
    @Column(length = 1000)
    private String personalizacao;

    protected ItemEncomenda() {
        // exigido pela JPA
    }

    ItemEncomenda(Encomenda encomenda, Produto produto, int quantidade, String personalizacao) {
        this.encomenda = encomenda;
        this.produto = produto;
        this.quantidade = validarQuantidade(quantidade);
        this.precoUnitario = produto.getPrecoBase();
        this.pontosPorUnidade = produto.getPontosCapacidade();
        this.personalizacao = textoOpcional(personalizacao);
    }

    // ---------- métodos modificadores (chamados só pela Encomenda) ----------

    void alterarQuantidade(int novaQuantidade) {
        this.quantidade = validarQuantidade(novaQuantidade);
    }

    private static int validarQuantidade(int quantidade) {
        exigir(quantidade > 0, "A quantidade deve ser maior que zero");
        return quantidade;
    }

    // ---------- leitura ----------

    public BigDecimal calcularSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public int calcularPontosCapacidade() {
        return pontosPorUnidade * quantidade;
    }

    public Long getId() { return id; }
    public Encomenda getEncomenda() { return encomenda; }
    public Produto getProduto() { return produto; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public int getPontosPorUnidade() { return pontosPorUnidade; }
    public String getPersonalizacao() { return personalizacao; }
}
