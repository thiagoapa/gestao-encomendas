package br.com.encomendas.domain.model;

import br.com.encomendas.domain.enums.TipoCliente;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static br.com.encomendas.domain.model.Validacao.*;

@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 20)
    private String telefone;

    @Column(length = 120)
    private String email;

    /** CPF ou CNPJ (opcional para pessoa física que compra pelo Instagram, por exemplo). */
    @Column(length = 18, unique = true)
    private String documento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCliente tipo;

    @Column(nullable = false)
    private LocalDateTime dataCadastro;

    @OneToMany(mappedBy = "cliente")
    private List<Encomenda> encomendas = new ArrayList<>();

    protected Cliente() {
        // exigido pela JPA
    }

    public Cliente(String nome, String telefone, String email, String documento, TipoCliente tipo) {
        this.nome = exigirTexto(nome, "nome");
        this.tipo = exigirNaoNulo(tipo, "tipo");
        this.documento = textoOpcional(documento);
        definirContato(telefone, email);
        this.dataCadastro = LocalDateTime.now();
    }

    // ---------- métodos modificadores ----------

    public void atualizarContato(String telefone, String email) {
        definirContato(telefone, email);
    }

    public void corrigirNome(String nome) {
        this.nome = exigirTexto(nome, "nome");
    }

    /** Chamado pela própria Encomenda ao ser criada, para manter os dois lados da relação coerentes. */
    void vincularEncomenda(Encomenda encomenda) {
        encomendas.add(encomenda);
    }

    private void definirContato(String telefone, String email) {
        String tel = textoOpcional(telefone);
        String mail = textoOpcional(email);
        exigir(tel != null || mail != null, "Informe ao menos um contato (telefone ou e-mail)");
        this.telefone = tel;
        this.email = mail;
    }

    // ---------- leitura ----------

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
    public String getDocumento() { return documento; }
    public TipoCliente getTipo() { return tipo; }
    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public List<Encomenda> getEncomendas() { return Collections.unmodifiableList(encomendas); }
}
