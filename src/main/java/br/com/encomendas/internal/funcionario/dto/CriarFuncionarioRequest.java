package br.com.encomendas.internal.funcionario.dto;

import br.com.encomendas.internal.funcionario.entity.Setor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarFuncionarioRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotNull Setor setor) {
}
