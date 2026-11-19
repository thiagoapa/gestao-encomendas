package br.com.encomendas.internal.funcionario.mapper;

import br.com.encomendas.internal.funcionario.dto.CriarFuncionarioRequest;
import br.com.encomendas.internal.funcionario.dto.FuncionarioResumoResponse;
import br.com.encomendas.internal.funcionario.entity.Funcionario;

public final class FuncionarioMapper {

    private FuncionarioMapper() {
    }

    public static Funcionario converteParaEntidade(CriarFuncionarioRequest request) {
        return new Funcionario(request.nome(), request.setor());
    }

    public static FuncionarioResumoResponse paraResumoResposta(Funcionario funcionario) {
        return new FuncionarioResumoResponse(
                funcionario.getId(),
                funcionario.getNome(),
                funcionario.getSetor(),
                funcionario.isAtivo()
        );
    }
}
