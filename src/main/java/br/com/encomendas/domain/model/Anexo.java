package br.com.encomendas.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Arquivo de arte ou referência enviado pelo cliente (o arquivo fica num storage; aqui só o link). */
@Entity
@Table(name = "anexo")
public class Anexo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "encomenda_id", nullable = false)
    private Encomenda encomenda;

    @Column(nullable = false, length = 255)
    private String nomeArquivo;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(nullable = false)
    private LocalDateTime dataEnvio;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "enviado_por_id", nullable = false)
    private Funcionario enviadoPor;

    protected Anexo() {
        // exigido pela JPA
    }

    Anexo(Encomenda encomenda, String nomeArquivo, String url, Funcionario enviadoPor) {
        this.encomenda = encomenda;
        this.nomeArquivo = nomeArquivo;
        this.url = url;
        this.enviadoPor = enviadoPor;
        this.dataEnvio = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Encomenda getEncomenda() { return encomenda; }
    public String getNomeArquivo() { return nomeArquivo; }
    public String getUrl() { return url; }
    public LocalDateTime getDataEnvio() { return dataEnvio; }
    public Funcionario getEnviadoPor() { return enviadoPor; }
}
