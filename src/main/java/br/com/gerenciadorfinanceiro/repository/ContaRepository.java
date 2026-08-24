package br.com.gerenciadorfinanceiro.repository;

import br.com.gerenciadorfinanceiro.model.Conta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContaRepository extends JpaRepository<Conta, Long> {
    List<Conta> findByUsuarioId(Long usuarioId);
    Optional<Conta> findByIdAndUsuarioId(Long id, Long usuarioId);
}
