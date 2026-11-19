package br.com.encomendas.internal.funcionario.dto;

import br.com.encomendas.internal.funcionario.entity.Setor;

public record FuncionarioResumoResponse(
        Long id,
        String nome,
        Setor setor,
        boolean ativo) {
}
