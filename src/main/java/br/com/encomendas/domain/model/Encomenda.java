package br.com.encomendas.domain.model;

import br.com.encomendas.domain.enums.*;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static br.com.encomendas.domain.model.Validacao.*;

/**
 * Raiz do agregado: a "fonte única da verdade" de cada encomenda.
 *
 * <p>Não há setters. Cada mudança de estado é um método com nome do negócio
 * (confirmar, iniciarProducao, registrarEntrega...) que valida a regra e grava
 * a alteração na trilha de auditoria. Itens, pagamentos, anexos, histórico e a
 * ordem de produção só são alterados por aqui.</p>
 */
@Entity
@Table(name = "encomenda")
public class Encomenda {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final BigDecimal SINAL_PADRAO = new BigDecimal("50.00");
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "atendente_id", nullable = false)
    private Funcionario atendente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalAtendimento canalOrigem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Ocasiao ocasiao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEncomenda status;

    /** Dia de entrega prometido (e a agenda de produção desse dia). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capacidade_diaria_id")
    private CapacidadeDiaria diaEntrega;

    /** Pontos de capacidade reservados na agenda ao confirmar (0 enquanto rascunho). */
    @Column(nullable = false)
    private int pontosReservados;

    /** Percentual mínimo de sinal para liberar a produção. */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualSinal;

    /** Quantas vezes voltou de PRONTO para produção por alteração do cliente. */
    @Column(nullable = false)
    private int quantidadeRetrabalhos;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    private LocalDateTime dataEntregaRealizada;

    @Column(length = 500)
    private String motivoCancelamento;

    @OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ordem_producao_id", nullable = false, unique = true)
    private OrdemProducao ordemProducao;

    @OneToMany(mappedBy = "encomenda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemEncomenda> itens = new ArrayList<>();

    @OneToMany(mappedBy = "encomenda", cascade = CascadeType.ALL)
    @OrderBy("dataPagamento ASC")
    private List<Pagamento> pagamentos = new ArrayList<>();

    @OneToMany(mappedBy = "encomenda", cascade = CascadeType.ALL)
    @OrderBy("dataHora ASC")
    private List<RegistroAlteracao> historico = new ArrayList<>();

    @OneToMany(mappedBy = "encomenda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Anexo> anexos = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "encomenda_equipe_producao",
            joinColumns = @JoinColumn(name = "encomenda_id"),
            inverseJoinColumns = @JoinColumn(name = "funcionario_id"))
    private Set<Funcionario> equipeProducao = new HashSet<>();

    protected Encomenda() {
        // exigido pela JPA
    }

    public Encomenda(Cliente cliente, Funcionario atendente, CanalAtendimento canalOrigem,
                     Ocasiao ocasiao, String especificacaoInicial) {
        this.cliente = exigirNaoNulo(cliente, "cliente");
        this.atendente = exigirAtivo(atendente, "atendente");
        this.canalOrigem = exigirNaoNulo(canalOrigem, "canal de origem");
        this.ocasiao = exigirNaoNulo(ocasiao, "ocasião");
        this.status = StatusEncomenda.RASCUNHO;
        this.percentualSinal = SINAL_PADRAO;
        this.dataCriacao = LocalDateTime.now();
        this.ordemProducao = new OrdemProducao(this, especificacaoInicial);
        cliente.vincularEncomenda(this);
        registrar(atendente, TipoAlteracao.CRIACAO, "Encomenda criada via " + canalOrigem);
    }

    // =====================================================================
    // Itens (só enquanto RASCUNHO: depois de confirmada, valor e capacidade ficam fixos)
    // =====================================================================

    public ItemEncomenda adicionarItem(Produto produto, int quantidade, String personalizacao, Funcionario responsavel) {
        exigirStatus(StatusEncomenda.RASCUNHO, "Itens só podem ser alterados enquanto a encomenda é rascunho");
        exigir(produto != null && produto.isAtivo(), "Produto inválido ou inativo");
        ItemEncomenda item = new ItemEncomenda(this, produto, quantidade, personalizacao);
        itens.add(item);
        registrar(responsavel, TipoAlteracao.ITENS, "Item adicionado: " + quantidade + "x " + produto.getNome());
        return item;
    }

    public void alterarQuantidadeItem(ItemEncomenda item, int novaQuantidade, Funcionario responsavel) {
        exigirStatus(StatusEncomenda.RASCUNHO, "Itens só podem ser alterados enquanto a encomenda é rascunho");
        exigirItemDestaEncomenda(item);
        int anterior = item.getQuantidade();
        item.alterarQuantidade(novaQuantidade);
        registrar(responsavel, TipoAlteracao.ITENS,
                "Quantidade de " + item.getProduto().getNome() + ": " + anterior + " -> " + novaQuantidade);
    }

    public void removerItem(ItemEncomenda item, Funcionario responsavel) {
        exigirStatus(StatusEncomenda.RASCUNHO, "Itens só podem ser alterados enquanto a encomenda é rascunho");
        exigirItemDestaEncomenda(item);
        itens.remove(item);
        registrar(responsavel, TipoAlteracao.ITENS, "Item removido: " + item.getProduto().getNome());
    }

    // =====================================================================
    // Prazo e capacidade
    // =====================================================================

    /**
     * Define ou muda o dia de entrega. Se a encomenda já foi confirmada, move a reserva
     * de capacidade: reserva no dia novo ANTES de liberar o antigo (se o novo estiver lotado,
     * nada muda).
     */
    public void definirDataEntrega(CapacidadeDiaria novoDia, Funcionario responsavel) {
        exigirNaoNulo(novoDia, "dia de entrega");
        exigir(status == StatusEncomenda.RASCUNHO || status == StatusEncomenda.PENDENTE
                        || status == StatusEncomenda.EM_PRODUCAO,
                "Não é possível mudar o prazo de uma encomenda " + status);
        exigir(!novoDia.getData().isBefore(LocalDate.now()), "A data de entrega não pode estar no passado");
        exigir(novoDia != diaEntrega, "A encomenda já está agendada para esse dia");

        if (status != StatusEncomenda.RASCUNHO) {
            novoDia.reservar(pontosReservados);
            diaEntrega.liberar(pontosReservados);
        }
        String anterior = diaEntrega == null ? "sem data" : diaEntrega.getData().format(FORMATO);
        this.diaEntrega = novoDia;
        registrar(responsavel, TipoAlteracao.PRAZO,
                "Entrega: " + anterior + " -> " + novoDia.getData().format(FORMATO));
    }

    // =====================================================================
    // Ciclo de vida
    // =====================================================================

    /** RASCUNHO -> PENDENTE. Trava itens e valor e reserva a capacidade do dia. */
    public void confirmar(Funcionario responsavel) {
        exigir(!itens.isEmpty(), "A encomenda precisa de ao menos um item");
        exigir(diaEntrega != null, "Defina a data de entrega antes de confirmar");
        exigirTransicao(StatusEncomenda.PENDENTE);
        int pontos = calcularPontosCapacidade();
        diaEntrega.reservar(pontos);
        this.pontosReservados = pontos;
        mudarStatus(StatusEncomenda.PENDENTE, responsavel);
    }

    /** Coloca alguém da produção na equipe desta encomenda. */
    public void designarParaProducao(Funcionario operador, Funcionario responsavel) {
        exigirProducao(operador);
        exigir(status == StatusEncomenda.PENDENTE || status == StatusEncomenda.EM_PRODUCAO,
                "Só é possível montar a equipe de encomendas pendentes ou em produção");
        exigir(equipeProducao.add(operador), operador.getNome() + " já está na equipe");
        registrar(responsavel, TipoAlteracao.EQUIPE, operador.getNome() + " designado(a) para a produção");
    }

    /** PENDENTE -> EM_PRODUCAO. Só com o sinal pago; quem inicia confirma a leitura da ficha. */
    public void iniciarProducao(Funcionario operador) {
        exigirProducao(operador);
        exigirTransicao(StatusEncomenda.EM_PRODUCAO);
        exigir(sinalQuitado(), "Sinal mínimo de " + percentualSinal.stripTrailingZeros().toPlainString()
                + "% não pago: exigido R$ " + calcularValorSinal() + ", pago R$ " + calcularTotalPago());
        equipeProducao.add(operador);
        ordemProducao.confirmarLeitura(operador);
        mudarStatus(StatusEncomenda.EM_PRODUCAO, operador);
    }

    /**
     * Alteração pedida pelo cliente. Gera nova versão da ficha e obriga a produção a
     * confirmar a leitura. Se a peça já estava PRONTA, volta para produção (retrabalho).
     */
    public void alterarEspecificacao(String novaEspecificacao, Funcionario responsavel) {
        exigir(status != StatusEncomenda.ENTREGUE && status != StatusEncomenda.CANCELADA,
                "Não é possível alterar uma encomenda " + status);
        ordemProducao.revisar(novaEspecificacao);
        registrar(responsavel, TipoAlteracao.ESPECIFICACAO,
                "Especificação v" + ordemProducao.getVersao() + ": " + ordemProducao.getEspecificacaoTecnica());
        if (status == StatusEncomenda.PRONTO) {
            this.quantidadeRetrabalhos++;
            mudarStatus(StatusEncomenda.EM_PRODUCAO, responsavel);
        }
    }

    /** A produção declara que leu a versão mais recente da ficha. */
    public void confirmarRevisaoNaProducao(Funcionario operador) {
        exigirProducao(operador);
        exigir(!ordemProducao.isConfirmadaPelaProducao(), "Não há revisão pendente de confirmação");
        ordemProducao.confirmarLeitura(operador);
        registrar(operador, TipoAlteracao.ESPECIFICACAO,
                "Produção confirmou a especificação v" + ordemProducao.getVersao());
    }

    /** EM_PRODUCAO -> PRONTO. Bloqueado se houver revisão que a produção ainda não leu. */
    public void marcarComoPronta(Funcionario operador) {
        exigirProducao(operador);
        exigirTransicao(StatusEncomenda.PRONTO);
        exigir(ordemProducao.isConfirmadaPelaProducao(), "A produção ainda não confirmou a especificação v"
                + ordemProducao.getVersao() + " — a peça pode ter sido feita com informação antiga");
        mudarStatus(StatusEncomenda.PRONTO, operador);
    }

    /** PRONTO -> ENTREGUE. Bloqueado enquanto houver saldo a receber. */
    public void registrarEntrega(Funcionario responsavel) {
        exigirTransicao(StatusEncomenda.ENTREGUE);
        exigir(calcularSaldoPendente().signum() == 0,
                "Saldo pendente de R$ " + calcularSaldoPendente() + ": registre o pagamento antes de entregar");
        this.dataEntregaRealizada = LocalDateTime.now();
        mudarStatus(StatusEncomenda.ENTREGUE, responsavel);
    }

    /** Qualquer estado (exceto ENTREGUE) -> CANCELADA. Devolve a capacidade reservada. */
    public void cancelar(String motivo, Funcionario responsavel) {
        exigirTransicao(StatusEncomenda.CANCELADA);
        this.motivoCancelamento = exigirTexto(motivo, "motivo do cancelamento");
        if (pontosReservados > 0) {
            diaEntrega.liberar(pontosReservados);
            this.pontosReservados = 0;
        }
        mudarStatus(StatusEncomenda.CANCELADA, responsavel);
    }

    // =====================================================================
    // Financeiro
    // =====================================================================

    /**
     * Registra um valor recebido. O tipo é decidido aqui: o primeiro é SINAL,
     * o que zera o saldo é QUITACAO, os demais são PARCIAL.
     */
    public Pagamento registrarPagamento(BigDecimal valor, FormaPagamento forma, Funcionario recebidoPor) {
        exigir(status != StatusEncomenda.RASCUNHO, "Confirme a encomenda antes de receber pagamentos");
        exigir(status != StatusEncomenda.ENTREGUE && status != StatusEncomenda.CANCELADA,
                "Não é possível receber pagamento de uma encomenda " + status);
        exigir(valor != null && valor.signum() > 0, "O valor deve ser maior que zero");
        exigirNaoNulo(forma, "forma de pagamento");
        exigirAtivo(recebidoPor, "recebido por");
        BigDecimal saldo = calcularSaldoPendente();
        exigir(valor.compareTo(saldo) <= 0, "O valor excede o saldo pendente de R$ " + saldo);

        TipoPagamento tipo;
        if (valor.compareTo(saldo) == 0) {
            tipo = TipoPagamento.QUITACAO;
        } else if (pagamentos.isEmpty()) {
            tipo = TipoPagamento.SINAL;
        } else {
            tipo = TipoPagamento.PARCIAL;
        }
        Pagamento pagamento = new Pagamento(this, valor, tipo, forma, recebidoPor);
        pagamentos.add(pagamento);
        registrar(recebidoPor, TipoAlteracao.PAGAMENTO, tipo + " de R$ " + valor + " via " + forma);
        return pagamento;
    }

    /** Exceção combinada (ex.: cliente corporativo faturado). Só a administração pode mudar. */
    public void ajustarPercentualSinal(BigDecimal novoPercentual, Funcionario responsavel) {
        exigirAtivo(responsavel, "responsável");
        exigir(responsavel.pertenceAo(Setor.ADMINISTRACAO), "Somente a administração pode alterar o sinal exigido");
        exigir(status == StatusEncomenda.RASCUNHO || status == StatusEncomenda.PENDENTE,
                "O sinal só pode ser alterado antes de iniciar a produção");
        exigir(novoPercentual != null && novoPercentual.signum() >= 0 && novoPercentual.compareTo(CEM) <= 0,
                "O percentual deve estar entre 0 e 100");
        BigDecimal anterior = percentualSinal;
        this.percentualSinal = novoPercentual;
        registrar(responsavel, TipoAlteracao.CONDICAO_FINANCEIRA,
                "Sinal exigido: " + anterior.stripTrailingZeros().toPlainString() + "% -> "
                        + novoPercentual.stripTrailingZeros().toPlainString() + "%");
    }

    // =====================================================================
    // Anexos
    // =====================================================================

    public Anexo anexarArquivo(String nomeArquivo, String url, Funcionario responsavel) {
        exigir(status != StatusEncomenda.ENTREGUE && status != StatusEncomenda.CANCELADA,
                "Não é possível anexar arquivos a uma encomenda " + status);
        Anexo anexo = new Anexo(this, exigirTexto(nomeArquivo, "nome do arquivo"), exigirTexto(url, "url"),
                exigirAtivo(responsavel, "responsável"));
        anexos.add(anexo);
        registrar(responsavel, TipoAlteracao.ANEXO, "Arquivo anexado: " + anexo.getNomeArquivo());
        return anexo;
    }

    // =====================================================================
    // Consultas (o atendente responde o cliente na hora)
    // =====================================================================

    public BigDecimal calcularValorTotal() {
        return itens.stream().map(ItemEncomenda::calcularSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calcularTotalPago() {
        return pagamentos.stream().map(Pagamento::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calcularSaldoPendente() {
        return calcularValorTotal().subtract(calcularTotalPago());
    }

    public BigDecimal calcularValorSinal() {
        return calcularValorTotal().multiply(percentualSinal).divide(CEM, 2, RoundingMode.HALF_UP);
    }

    public boolean sinalQuitado() {
        return calcularTotalPago().compareTo(calcularValorSinal()) >= 0;
    }

    public int calcularPontosCapacidade() {
        return itens.stream().mapToInt(ItemEncomenda::calcularPontosCapacidade).sum();
    }

    public LocalDate getDataEntregaPrevista() {
        return diaEntrega == null ? null : diaEntrega.getData();
    }

    // =====================================================================
    // Internos
    // =====================================================================

    private void mudarStatus(StatusEncomenda novo, Funcionario responsavel) {
        exigirTransicao(novo);
        StatusEncomenda anterior = this.status;
        this.status = novo;
        registrar(responsavel, TipoAlteracao.STATUS, anterior + " -> " + novo);
    }

    private void registrar(Funcionario responsavel, TipoAlteracao tipo, String descricao) {
        exigirAtivo(responsavel, "responsável");
        historico.add(new RegistroAlteracao(this, responsavel, tipo, descricao));
    }

    private void exigirTransicao(StatusEncomenda destino) {
        exigir(status.podeTransitarPara(destino), "Transição inválida: " + status + " -> " + destino);
    }

    private void exigirStatus(StatusEncomenda esperado, String mensagem) {
        exigir(status == esperado, mensagem);
    }

    private void exigirItemDestaEncomenda(ItemEncomenda item) {
        exigir(item != null && itens.contains(item), "O item não pertence a esta encomenda");
    }

    private static Funcionario exigirAtivo(Funcionario funcionario, String campo) {
        exigirNaoNulo(funcionario, campo);
        exigir(funcionario.isAtivo(), funcionario.getNome() + " está inativo(a)");
        return funcionario;
    }

    private static void exigirProducao(Funcionario funcionario) {
        exigirAtivo(funcionario, "operador");
        exigir(funcionario.pertenceAo(Setor.PRODUCAO), funcionario.getNome() + " não é da produção");
    }

    // =====================================================================
    // Leitura
    // =====================================================================

    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public Funcionario getAtendente() { return atendente; }
    public CanalAtendimento getCanalOrigem() { return canalOrigem; }
    public Ocasiao getOcasiao() { return ocasiao; }
    public StatusEncomenda getStatus() { return status; }
    public CapacidadeDiaria getDiaEntrega() { return diaEntrega; }
    public int getPontosReservados() { return pontosReservados; }
    public BigDecimal getPercentualSinal() { return percentualSinal; }
    public int getQuantidadeRetrabalhos() { return quantidadeRetrabalhos; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public LocalDateTime getDataEntregaRealizada() { return dataEntregaRealizada; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public OrdemProducao getOrdemProducao() { return ordemProducao; }
    public List<ItemEncomenda> getItens() { return Collections.unmodifiableList(itens); }
    public List<Pagamento> getPagamentos() { return Collections.unmodifiableList(pagamentos); }
    public List<RegistroAlteracao> getHistorico() { return Collections.unmodifiableList(historico); }
    public List<Anexo> getAnexos() { return Collections.unmodifiableList(anexos); }
    public Set<Funcionario> getEquipeProducao() { return Collections.unmodifiableSet(equipeProducao); }
}
