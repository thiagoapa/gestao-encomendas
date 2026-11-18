package br.com.encomendas.domain.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static br.com.encomendas.domain.model.Validacao.*;

/**
 * Agenda de produção de um dia: quanto a equipe aguenta produzir e quanto já foi prometido.
 * Resolve o problema de prometer entregas em dias já lotados.
 */
@Entity
@Table(name = "capacidade_diaria")
public class CapacidadeDiaria {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dia", nullable = false, unique = true)
    private LocalDate data;

    @Column(nullable = false)
    private int capacidadeMaxima;

    @Column(nullable = false)
    private int capacidadeReservada;

    /** Trava otimista: dois atendentes não conseguem reservar a última vaga ao mesmo tempo. */
    @Version
    private Long versao;

    protected CapacidadeDiaria() {
        // exigido pela JPA
    }

    public CapacidadeDiaria(LocalDate data, int capacidadeMaxima) {
        this.data = exigirNaoNulo(data, "data");
        exigir(capacidadeMaxima > 0, "A capacidade máxima deve ser maior que zero");
        this.capacidadeMaxima = capacidadeMaxima;
        this.capacidadeReservada = 0;
    }

    // ---------- métodos modificadores ----------

    public void ajustarCapacidadeMaxima(int novaCapacidade) {
        exigir(novaCapacidade > 0, "A capacidade máxima deve ser maior que zero");
        exigir(novaCapacidade >= capacidadeReservada,
                "Já existem " + capacidadeReservada + " pontos reservados em " + data.format(FORMATO)
                        + "; a capacidade não pode ficar abaixo disso");
        this.capacidadeMaxima = novaCapacidade;
    }

    /** Usado apenas pela Encomenda ao ser confirmada ou reagendada. */
    void reservar(int pontos) {
        exigir(pontos > 0, "A reserva deve ter ao menos 1 ponto");
        exigir(pontos <= capacidadeDisponivel(),
                "Capacidade de produção esgotada para " + data.format(FORMATO)
                        + ": disponível " + capacidadeDisponivel() + ", necessário " + pontos);
        this.capacidadeReservada += pontos;
    }

    /** Usado apenas pela Encomenda ao ser cancelada ou reagendada. */
    void liberar(int pontos) {
        this.capacidadeReservada = Math.max(0, capacidadeReservada - pontos);
    }

    // ---------- leitura ----------

    public int capacidadeDisponivel() {
        return capacidadeMaxima - capacidadeReservada;
    }

    public boolean estaLotada() {
        return capacidadeDisponivel() <= 0;
    }

    public Long getId() { return id; }
    public LocalDate getData() { return data; }
    public int getCapacidadeMaxima() { return capacidadeMaxima; }
    public int getCapacidadeReservada() { return capacidadeReservada; }
}
