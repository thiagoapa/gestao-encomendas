package br.com.encomendas.domain.model;

import br.com.encomendas.domain.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova que os relacionamentos JPA funcionam: grava uma encomenda completa no H2,
 * limpa o contexto de persistência e lê tudo de volta do banco.
 */
@DataJpaTest
class MapeamentoJpaTest {

    @Autowired
    private TestEntityManager em;

    @Test
    void gravaELeEncomendaComTodosOsRelacionamentos() {
        Funcionario atendente = em.persist(new Funcionario("Ana", Setor.ATENDIMENTO));
        Funcionario producao = em.persist(new Funcionario("Bruno", Setor.PRODUCAO));
        Funcionario producao2 = em.persist(new Funcionario("Diego", Setor.PRODUCAO));
        Cliente cliente = em.persist(new Cliente("Empresa X Ltda", null, "compras@empresax.com",
                "12.345.678/0001-90", TipoCliente.PESSOA_JURIDICA));
        Produto camiseta = em.persist(new Produto("Camiseta estampada", null, new BigDecimal("35.00"), 3));
        CapacidadeDiaria dia = em.persist(new CapacidadeDiaria(LocalDate.now().plusDays(5), 100));

        Encomenda encomenda = new Encomenda(cliente, atendente, CanalAtendimento.INSTAGRAM, Ocasiao.CORPORATIVO,
                "Camiseta preta, logo branco 20 cm no peito");
        encomenda.adicionarItem(camiseta, 20, "Tamanhos: 10 M, 10 G", atendente);
        encomenda.definirDataEntrega(dia, atendente);
        encomenda.anexarArquivo("logo.svg", "https://storage.exemplo/logo.svg", atendente);
        encomenda.confirmar(atendente);
        encomenda.registrarPagamento(new BigDecimal("350.00"), FormaPagamento.PIX, atendente);
        encomenda.iniciarProducao(producao);
        encomenda.designarParaProducao(producao2, producao);
        Long id = em.persistAndGetId(encomenda, Long.class);
        em.flush();
        em.clear();

        Encomenda lida = em.find(Encomenda.class, id);

        assertThat(lida.getStatus()).isEqualTo(StatusEncomenda.EM_PRODUCAO);
        assertThat(lida.getCliente().getNome()).isEqualTo("Empresa X Ltda");                 // @ManyToOne
        assertThat(lida.getItens()).hasSize(1);                                               // @OneToMany
        assertThat(lida.getItens().get(0).getProduto().getNome()).isEqualTo("Camiseta estampada");
        assertThat(lida.getPagamentos()).hasSize(1);
        assertThat(lida.getAnexos()).hasSize(1);
        assertThat(lida.getHistorico()).hasSizeGreaterThanOrEqualTo(6);
        assertThat(lida.getOrdemProducao().isConfirmadaPelaProducao()).isTrue();             // @OneToOne
        assertThat(lida.getOrdemProducao().getEncomenda()).isSameAs(lida);
        assertThat(lida.getEquipeProducao()).extracting(Funcionario::getNome)               // @ManyToMany
                .containsExactlyInAnyOrder("Bruno", "Diego");
        assertThat(lida.getDiaEntrega().getCapacidadeReservada()).isEqualTo(60);
        assertThat(lida.calcularSaldoPendente()).isEqualByComparingTo("350.00");
        assertThat(lida.getCliente().getEncomendas()).hasSize(1);                             // lado mappedBy
    }
}
