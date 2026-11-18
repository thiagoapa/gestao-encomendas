package br.com.encomendas.internal.encomenda.entity;

import br.com.encomendas.internal.cliente.entity.Cliente;
import br.com.encomendas.internal.cliente.entity.TipoCliente;
import br.com.encomendas.internal.funcionario.entity.Funcionario;
import br.com.encomendas.internal.funcionario.entity.Setor;
import br.com.encomendas.internal.produto.entity.Produto;
import br.com.encomendas.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regras de negócio do domínio, sem banco e sem Spring. */
class EncomendaTest {

    private Funcionario atendente;
    private Funcionario producao;
    private Funcionario administracao;
    private Cliente cliente;
    private Produto caneca;
    private CapacidadeDiaria dia;
    private Encomenda encomenda;

    @BeforeEach
    void preparar() {
        atendente = new Funcionario("Ana", Setor.ATENDIMENTO);
        producao = new Funcionario("Bruno", Setor.PRODUCAO);
        administracao = new Funcionario("Carla", Setor.ADMINISTRACAO);
        cliente = new Cliente("Mariana Souza", "83999990000", null, null, TipoCliente.PESSOA_FISICA);
        caneca = new Produto("Caneca personalizada", "Caneca de porcelana 325 ml", new BigDecimal("40.00"), 2);
        dia = new CapacidadeDiaria(LocalDate.now().plusDays(7), 50);

        encomenda = new Encomenda(cliente, atendente, CanalAtendimento.WHATSAPP, Ocasiao.CASAMENTO,
                "Caneca branca, nomes 'Ana & Leo' em dourado, fonte script");
        encomenda.adicionarItem(caneca, 10, "Nomes dos noivos", atendente);
        encomenda.definirDataEntrega(dia, atendente);
    }

    @Test
    @DisplayName("Fluxo completo: rascunho -> pendente -> produção -> pronta -> entregue")
    void fluxoCompleto() {
        encomenda.confirmar(atendente);
        assertThat(dia.getCapacidadeReservada()).isEqualTo(20);

        encomenda.registrarPagamento(new BigDecimal("200.00"), FormaPagamento.PIX, atendente);
        encomenda.iniciarProducao(producao);
        encomenda.marcarComoPronta(producao);
        encomenda.registrarPagamento(new BigDecimal("200.00"), FormaPagamento.DINHEIRO, atendente);
        encomenda.registrarEntrega(atendente);

        assertThat(encomenda.getStatus()).isEqualTo(StatusEncomenda.ENTREGUE);
        assertThat(encomenda.getPagamentos()).extracting(Pagamento::getTipo)
                .containsExactly(TipoPagamento.SINAL, TipoPagamento.QUITACAO);
        assertThat(encomenda.calcularSaldoPendente()).isEqualByComparingTo("0");
        assertThat(encomenda.getHistorico()).isNotEmpty();
    }

    @Test
    @DisplayName("Não inicia a produção sem o sinal mínimo")
    void naoIniciaSemSinal() {
        encomenda.confirmar(atendente);
        encomenda.registrarPagamento(new BigDecimal("100.00"), FormaPagamento.PIX, atendente);

        assertThatThrownBy(() -> encomenda.iniciarProducao(producao))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Sinal mínimo");
    }

    @Test
    @DisplayName("Administração pode dispensar o sinal (cliente corporativo faturado)")
    void administracaoDispensaSinal() {
        assertThatThrownBy(() -> encomenda.ajustarPercentualSinal(BigDecimal.ZERO, atendente))
                .isInstanceOf(RegraNegocioException.class);

        encomenda.ajustarPercentualSinal(BigDecimal.ZERO, administracao);
        encomenda.confirmar(atendente);
        encomenda.iniciarProducao(producao);

        assertThat(encomenda.getStatus()).isEqualTo(StatusEncomenda.EM_PRODUCAO);
    }

    @Test
    @DisplayName("Não entrega com saldo pendente")
    void naoEntregaComSaldo() {
        encomenda.confirmar(atendente);
        encomenda.registrarPagamento(new BigDecimal("200.00"), FormaPagamento.PIX, atendente);
        encomenda.iniciarProducao(producao);
        encomenda.marcarComoPronta(producao);

        assertThatThrownBy(() -> encomenda.registrarEntrega(atendente))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Saldo pendente");
    }

    @Test
    @DisplayName("Alteração do cliente não lida pela produção bloqueia 'pronta'")
    void alteracaoNaoConfirmadaBloqueiaPronta() {
        encomenda.confirmar(atendente);
        encomenda.registrarPagamento(new BigDecimal("200.00"), FormaPagamento.PIX, atendente);
        encomenda.iniciarProducao(producao);

        encomenda.alterarEspecificacao("Caneca branca, nomes 'Ana & Leo' em PRATEADO, fonte script", atendente);

        assertThat(encomenda.getOrdemProducao().getVersao()).isEqualTo(2);
        assertThatThrownBy(() -> encomenda.marcarComoPronta(producao))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("v2");

        encomenda.confirmarRevisaoNaProducao(producao);
        encomenda.marcarComoPronta(producao);
        assertThat(encomenda.getStatus()).isEqualTo(StatusEncomenda.PRONTO);
    }

    @Test
    @DisplayName("Alteração depois de pronta volta para produção e conta retrabalho")
    void alteracaoAposProntaGeraRetrabalho() {
        encomenda.confirmar(atendente);
        encomenda.registrarPagamento(new BigDecimal("200.00"), FormaPagamento.PIX, atendente);
        encomenda.iniciarProducao(producao);
        encomenda.marcarComoPronta(producao);

        encomenda.alterarEspecificacao("Trocar a fonte para bastão", atendente);

        assertThat(encomenda.getStatus()).isEqualTo(StatusEncomenda.EM_PRODUCAO);
        assertThat(encomenda.getQuantidadeRetrabalhos()).isEqualTo(1);
    }

    @Test
    @DisplayName("Dia lotado bloqueia a confirmação")
    void diaLotadoBloqueiaConfirmacao() {
        CapacidadeDiaria diaCheio = new CapacidadeDiaria(LocalDate.now().plusDays(3), 10);
        encomenda.definirDataEntrega(diaCheio, atendente);

        assertThatThrownBy(() -> encomenda.confirmar(atendente))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Capacidade de produção esgotada");
        assertThat(encomenda.getStatus()).isEqualTo(StatusEncomenda.RASCUNHO);
    }

    @Test
    @DisplayName("Reagendar move a reserva; cancelar devolve a capacidade")
    void reagendarECancelar() {
        encomenda.confirmar(atendente);
        CapacidadeDiaria outroDia = new CapacidadeDiaria(LocalDate.now().plusDays(10), 50);

        encomenda.definirDataEntrega(outroDia, atendente);
        assertThat(dia.getCapacidadeReservada()).isZero();
        assertThat(outroDia.getCapacidadeReservada()).isEqualTo(20);

        encomenda.cancelar("Cliente desistiu", atendente);
        assertThat(outroDia.getCapacidadeReservada()).isZero();
        assertThat(encomenda.getStatus()).isEqualTo(StatusEncomenda.CANCELADA);
    }

    @Test
    @DisplayName("Itens ficam travados depois de confirmar")
    void itensTravadosAposConfirmar() {
        encomenda.confirmar(atendente);

        assertThatThrownBy(() -> encomenda.adicionarItem(caneca, 1, null, atendente))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("Pagamento acima do saldo é recusado")
    void pagamentoAcimaDoSaldo() {
        encomenda.confirmar(atendente);

        assertThatThrownBy(() -> encomenda.registrarPagamento(new BigDecimal("500.00"), FormaPagamento.PIX, atendente))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("excede");
    }
}
