package br.com.encomendas.internal.produto.controller;

import br.com.encomendas.internal.produto.dto.CriarProdutoRequest;
import br.com.encomendas.internal.produto.dto.ProdutoResumoResponse;
import br.com.encomendas.internal.produto.dto.ReajustarPrecoRequest;
import br.com.encomendas.internal.produto.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @PostMapping
    public ResponseEntity<ProdutoResumoResponse> criar(@Valid @RequestBody CriarProdutoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.criarProduto(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResumoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(produtoService.buscarProdutoPorId(id));
    }

    @GetMapping
    public ResponseEntity<Page<ProdutoResumoResponse>> listarTodos(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(produtoService.listarTodos(pageable));
    }

    @PatchMapping("/{id}/preco")
    public ResponseEntity<ProdutoResumoResponse> reajustarPreco(@PathVariable Long id,
                                                                @Valid @RequestBody ReajustarPrecoRequest request) {
        return ResponseEntity.ok(produtoService.reajustarPreco(id, request));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        produtoService.desativarProduto(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reativar")
    public ResponseEntity<Void> reativar(@PathVariable Long id) {
        produtoService.reativarProduto(id);
        return ResponseEntity.noContent().build();
    }
}
