package br.com.gerenciadorfinanceiro.repository;

import br.com.gerenciadorfinanceiro.model.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    List<Orcamento> findByCategoriaUsuarioId(Long usuarioId);

    List<Orcamento> findByCategoriaUsuarioIdAndAnoAndMes(Long usuarioId, Integer ano, Integer mes);

    Optional<Orcamento> findByIdAndCategoriaUsuarioId(Long id, Long usuarioId);

    Optional<Orcamento> findByCategoriaIdAndAnoAndMes(Long categoriaId, Integer ano, Integer mes);

    boolean existsByCategoriaIdAndAnoAndMes(Long categoriaId, Integer ano, Integer mes);
}
