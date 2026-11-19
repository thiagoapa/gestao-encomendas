package br.com.encomendas.internal.cliente.repository;

import br.com.encomendas.internal.cliente.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}