package br.com.encomendas.internal.produto.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ReajustarPrecoRequest(
        @NotNull @Positive BigDecimal novoPreco) {
}
