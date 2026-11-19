package br.com.encomendas.internal.funcionario.dto;

import br.com.encomendas.internal.funcionario.entity.Setor;
import jakarta.validation.constraints.NotNull;

public record TransferirSetorRequest(
        @NotNull Setor novoSetor) {
}
