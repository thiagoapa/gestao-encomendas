package br.com.encomendas.internal.produto.mapper;

import br.com.encomendas.internal.produto.dto.CriarProdutoRequest;
import br.com.encomendas.internal.produto.dto.ProdutoResumoResponse;
import br.com.encomendas.internal.produto.entity.Produto;

public final class ProdutoMapper {

    private ProdutoMapper() {
    }

    public static Produto converteParaEntidade(CriarProdutoRequest request) {
        return new Produto(
                request.nome(),
                request.descricao(),
                request.precoBase(),
                request.pontosCapacidade()
        );
    }

    public static ProdutoResumoResponse paraResumoResposta(Produto produto) {
        return new ProdutoResumoResponse(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getPrecoBase(),
                produto.getPontosCapacidade(),
                produto.isAtivo()
        );
    }
}
