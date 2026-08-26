package br.com.gerenciadorfinanceiro.repository;

import br.com.gerenciadorfinanceiro.model.TransacaoRecorrente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransacaoRecorrenteRepository extends JpaRepository<TransacaoRecorrente, Long> {
    List<TransacaoRecorrente> findByUsuarioId(Long usuarioId);
    List<TransacaoRecorrente> findByUsuarioIdAndAtivoTrue(Long usuarioId);
    Optional<TransacaoRecorrente> findByIdAndUsuarioId(Long id, Long usuarioId);
    List<TransacaoRecorrente> findByAtivoTrue();
}
