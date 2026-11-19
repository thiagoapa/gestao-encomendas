package br.com.encomendas.internal.produto.service;

import br.com.encomendas.internal.produto.dto.CriarProdutoRequest;
import br.com.encomendas.internal.produto.dto.ProdutoResumoResponse;
import br.com.encomendas.internal.produto.dto.ReajustarPrecoRequest;
import br.com.encomendas.internal.produto.entity.Produto;
import br.com.encomendas.internal.produto.mapper.ProdutoMapper;
import br.com.encomendas.internal.produto.repository.ProdutoRepository;
import br.com.encomendas.shared.exception.RecursoNaoEncontradoException;
import br.com.encomendas.shared.exception.RegraNegocioException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public ProdutoResumoResponse criarProduto(CriarProdutoRequest request) {
        if (produtoRepository.existsByNomeIgnoreCase(request.nome().trim())) {
            throw new RegraNegocioException("Já existe um produto com o nome '" + request.nome().trim() + "'");
        }
        Produto produto = ProdutoMapper.converteParaEntidade(request);
        produtoRepository.save(produto);
        return ProdutoMapper.paraResumoResposta(produto);
    }

    @Transactional(readOnly = true)
    public ProdutoResumoResponse buscarProdutoPorId(Long id) {
        return ProdutoMapper.paraResumoResposta(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResumoResponse> listarTodos(Pageable pageable) {
        return produtoRepository.findAll(pageable).map(ProdutoMapper::paraResumoResposta);
    }

    @Transactional
    public ProdutoResumoResponse reajustarPreco(Long id, ReajustarPrecoRequest request) {
        Produto produto = buscarEntidadePorId(id);
        produto.reajustarPreco(request.novoPreco());
        return ProdutoMapper.paraResumoResposta(produto);
    }

    @Transactional
    public void desativarProduto(Long id) {
        buscarEntidadePorId(id).desativar();
    }

    @Transactional
    public void reativarProduto(Long id) {
        buscarEntidadePorId(id).reativar();
    }

    /** Usado por outros módulos (ex.: encomenda) para obter o produto. */
    @Transactional(readOnly = true)
    public Produto buscarEntidadePorId(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
    }
}
