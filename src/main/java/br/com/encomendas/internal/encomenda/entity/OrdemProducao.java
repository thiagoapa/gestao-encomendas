package br.com.encomendas.internal.encomenda.entity;

import br.com.encomendas.internal.funcionario.entity.Funcionario;
import br.com.encomendas.internal.funcionario.entity.Setor;
import jakarta.persistence.*;

import java.time.LocalDateTime;

import static br.com.encomendas.shared.util.Validacao.*;

/**
 * Ficha que a produção usa para fabricar. Toda revisão aumenta a versão e exige que
 * a produção confirme a leitura — assim uma alteração pedida pelo cliente não se perde
 * entre o atendimento e a bancada.
 */
@Entity
@Table(name = "ordem_producao")
public class OrdemProducao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "ordemProducao")
    private Encomenda encomenda;

    @Column(nullable = false, length = 4000)
    private String especificacaoTecnica;

    @Column(nullable = false)
    private int versao;

    @Column(nullable = false)
    private LocalDateTime dataUltimaRevisao;

    @Column(nullable = false)
    private boolean confirmadaPelaProducao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmada_por_id")
    private Funcionario confirmadaPor;

    private LocalDateTime dataConfirmacao;

    protected OrdemProducao() {
        // exigido pela JPA
    }

    OrdemProducao(Encomenda encomenda, String especificacaoTecnica) {
        this.encomenda = encomenda;
        this.especificacaoTecnica = exigirTexto(especificacaoTecnica, "especificação técnica");
        this.versao = 1;
        this.dataUltimaRevisao = LocalDateTime.now();
        this.confirmadaPelaProducao = false;
    }

    // ---------- métodos modificadores (chamados só pela Encomenda) ----------

    void revisar(String novaEspecificacao) {
        String nova = exigirTexto(novaEspecificacao, "especificação técnica");
        exigir(!nova.equals(especificacaoTecnica), "A nova especificação é igual à atual");
        this.especificacaoTecnica = nova;
        this.versao++;
        this.dataUltimaRevisao = LocalDateTime.now();
        this.confirmadaPelaProducao = false;
        this.confirmadaPor = null;
        this.dataConfirmacao = null;
    }

    void confirmarLeitura(Funcionario operador) {
        exigirNaoNulo(operador, "operador");
        exigir(operador.pertenceAo(Setor.PRODUCAO), "Somente a produção pode confirmar a ordem de produção");
        this.confirmadaPelaProducao = true;
        this.confirmadaPor = operador;
        this.dataConfirmacao = LocalDateTime.now();
    }

    // ---------- leitura ----------

    public Long getId() { return id; }
    public Encomenda getEncomenda() { return encomenda; }
    public String getEspecificacaoTecnica() { return especificacaoTecnica; }
    public int getVersao() { return versao; }
    public LocalDateTime getDataUltimaRevisao() { return dataUltimaRevisao; }
    public boolean isConfirmadaPelaProducao() { return confirmadaPelaProducao; }
    public Funcionario getConfirmadaPor() { return confirmadaPor; }
    public LocalDateTime getDataConfirmacao() { return dataConfirmacao; }
}
