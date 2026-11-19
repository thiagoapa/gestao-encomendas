package br.com.encomendas.internal.funcionario.controller;

import br.com.encomendas.internal.funcionario.dto.CriarFuncionarioRequest;
import br.com.encomendas.internal.funcionario.dto.FuncionarioResumoResponse;
import br.com.encomendas.internal.funcionario.dto.TransferirSetorRequest;
import br.com.encomendas.internal.funcionario.entity.Setor;
import br.com.encomendas.internal.funcionario.service.FuncionarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("funcionarios")
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    public FuncionarioController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @PostMapping
    public ResponseEntity<FuncionarioResumoResponse> criar(@Valid @RequestBody CriarFuncionarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(funcionarioService.criarFuncionario(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FuncionarioResumoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(funcionarioService.buscarFuncionarioPorId(id));
    }

    @GetMapping
    public ResponseEntity<Page<FuncionarioResumoResponse>> listar(@RequestParam(required = false) Setor setor,
                                                                  @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(funcionarioService.listar(setor, pageable));
    }

    @PatchMapping("/{id}/setor")
    public ResponseEntity<FuncionarioResumoResponse> transferirSetor(@PathVariable Long id,
                                                                     @Valid @RequestBody TransferirSetorRequest request) {
        return ResponseEntity.ok(funcionarioService.transferirSetor(id, request));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        funcionarioService.desativarFuncionario(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reativar")
    public ResponseEntity<Void> reativar(@PathVariable Long id) {
        funcionarioService.reativarFuncionario(id);
        return ResponseEntity.noContent().build();
    }
}
