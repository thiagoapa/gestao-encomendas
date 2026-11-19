package br.com.encomendas.internal.funcionario.repository;

import br.com.encomendas.internal.funcionario.entity.Funcionario;
import br.com.encomendas.internal.funcionario.entity.Setor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    Page<Funcionario> findBySetor(Setor setor, Pageable pageable);
}
