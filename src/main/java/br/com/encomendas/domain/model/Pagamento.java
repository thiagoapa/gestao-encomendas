package br.com.encomendas.domain.model;

import br.com.encomendas.domain.enums.FormaPagamento;
import br.com.encomendas.domain.enums.TipoPagamento;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro financeiro. É imutável: não tem métodos modificadores de propósito —
 * um pagamento registrado não se edita (no futuro, um estorno será um novo registro).
 */
@Entity
@Table(name = "pagamento")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "encomenda_id", nullable = false)
    private Encomenda encomenda;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPagamento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormaPagamento forma;

    @Column(nullable = false)
    private LocalDateTime dataPagamento;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recebido_por_id", nullable = false)
    private Funcionario recebidoPor;

    protected Pagamento() {
        // exigido pela JPA
    }

    Pagamento(Encomenda encomenda, BigDecimal valor, TipoPagamento tipo, FormaPagamento forma, Funcionario recebidoPor) {
        this.encomenda = encomenda;
        this.valor = valor;
        this.tipo = tipo;
        this.forma = forma;
        this.recebidoPor = recebidoPor;
        this.dataPagamento = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Encomenda getEncomenda() { return encomenda; }
    public BigDecimal getValor() { return valor; }
    public TipoPagamento getTipo() { return tipo; }
    public FormaPagamento getForma() { return forma; }
    public LocalDateTime getDataPagamento() { return dataPagamento; }
    public Funcionario getRecebidoPor() { return recebidoPor; }
}
