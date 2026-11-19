package br.com.encomendas.internal.produto.dto;

import java.math.BigDecimal;

public record ProdutoResumoResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal precoBase,
        int pontosCapacidade,
        boolean ativo) {
}
