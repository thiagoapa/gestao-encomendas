package br.com.encomendas.internal.funcionario.service;

import br.com.encomendas.internal.funcionario.dto.CriarFuncionarioRequest;
import br.com.encomendas.internal.funcionario.dto.FuncionarioResumoResponse;
import br.com.encomendas.internal.funcionario.dto.TransferirSetorRequest;
import br.com.encomendas.internal.funcionario.entity.Funcionario;
import br.com.encomendas.internal.funcionario.entity.Setor;
import br.com.encomendas.internal.funcionario.mapper.FuncionarioMapper;
import br.com.encomendas.internal.funcionario.repository.FuncionarioRepository;
import br.com.encomendas.shared.exception.RecursoNaoEncontradoException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;

    public FuncionarioService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    @Transactional
    public FuncionarioResumoResponse criarFuncionario(CriarFuncionarioRequest request) {
        Funcionario funcionario = FuncionarioMapper.converteParaEntidade(request);
        funcionarioRepository.save(funcionario);
        return FuncionarioMapper.paraResumoResposta(funcionario);
    }

    @Transactional(readOnly = true)
    public FuncionarioResumoResponse buscarFuncionarioPorId(Long id) {
        return FuncionarioMapper.paraResumoResposta(buscarEntidadePorId(id));
    }

    /** Lista todos ou só os de um setor (ex.: ?setor=PRODUCAO). */
    @Transactional(readOnly = true)
    public Page<FuncionarioResumoResponse> listar(Setor setor, Pageable pageable) {
        Page<Funcionario> pagina = (setor == null)
                ? funcionarioRepository.findAll(pageable)
                : funcionarioRepository.findBySetor(setor, pageable);
        return pagina.map(FuncionarioMapper::paraResumoResposta);
    }

    @Transactional
    public FuncionarioResumoResponse transferirSetor(Long id, TransferirSetorRequest request) {
        Funcionario funcionario = buscarEntidadePorId(id);
        funcionario.transferirParaSetor(request.novoSetor());
        return FuncionarioMapper.paraResumoResposta(funcionario);
    }

    @Transactional
    public void desativarFuncionario(Long id) {
        buscarEntidadePorId(id).desativar();
    }

    @Transactional
    public void reativarFuncionario(Long id) {
        buscarEntidadePorId(id).reativar();
    }

    /** Usado por outros módulos (ex.: encomenda) para obter o responsável por uma ação. */
    @Transactional(readOnly = true)
    public Funcionario buscarEntidadePorId(Long id) {
        return funcionarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", id));
    }
}
