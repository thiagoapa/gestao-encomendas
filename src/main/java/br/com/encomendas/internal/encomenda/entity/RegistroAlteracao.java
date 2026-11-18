package br.com.encomendas.internal.encomenda.entity;

import br.com.encomendas.internal.funcionario.entity.Funcionario;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Trilha de auditoria: quem mudou o quê e quando. Só se acrescenta, nunca se edita
 * (por isso não há métodos modificadores).
 */
@Entity
@Table(name = "registro_alteracao")
public class RegistroAlteracao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "encomenda_id", nullable = false)
    private Encomenda encomenda;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Funcionario responsavel;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoAlteracao tipo;

    @Column(nullable = false, length = 4000)
    private String descricao;

    protected RegistroAlteracao() {
        // exigido pela JPA
    }

    RegistroAlteracao(Encomenda encomenda, Funcionario responsavel, TipoAlteracao tipo, String descricao) {
        this.encomenda = encomenda;
        this.responsavel = responsavel;
        this.tipo = tipo;
        this.descricao = descricao;
        this.dataHora = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Encomenda getEncomenda() { return encomenda; }
    public Funcionario getResponsavel() { return responsavel; }
    public LocalDateTime getDataHora() { return dataHora; }
    public TipoAlteracao getTipo() { return tipo; }
    public String getDescricao() { return descricao; }
}
