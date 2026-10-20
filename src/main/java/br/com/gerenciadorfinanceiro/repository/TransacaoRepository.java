package br.com.gerenciadorfinanceiro.repository;

import br.com.gerenciadorfinanceiro.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long>, JpaSpecificationExecutor<Transacao> {
    List<Transacao> findByUsuarioId(Long usuarioId);
    Optional<Transacao> findByIdAndUsuarioId(Long id, Long usuarioId);
    List<Transacao> findByContaIdAndUsuarioId(Long contaId, Long usuarioId);
    List<Transacao> findByRecorrenciaId(Long recorrenciaId);
    List<Transacao> findByUsuarioIdAndRecorrenciaIsNullAndDescricaoIgnoreCase(Long usuarioId, String descricao);

    @Query("SELECT COALESCE(SUM(t.valor), 0) FROM Transacao t " +
           "WHERE t.usuario.id = :usuarioId " +
           "AND t.categoria.id = :categoriaId " +
           "AND t.tipo = 'DESPESA' " +
           "AND t.status = 'PAGA' " +
           "AND YEAR(t.dataTransacao) = :ano " +
           "AND MONTH(t.dataTransacao) = :mes " +
           "AND (t.transferencia IS NULL OR t.transferencia = false)")
    BigDecimal somarDespesasPagasPorCategoriaECompetencia(
            @Param("usuarioId") Long usuarioId,
            @Param("categoriaId") Long categoriaId,
            @Param("ano") Integer ano,
            @Param("mes") Integer mes);

    @Query("SELECT COALESCE(SUM(t.valor), 0) FROM Transacao t " +
           "WHERE t.usuario.id = :usuarioId " +
           "AND t.tipo = :tipo " +
           "AND t.status = :status " +
           "AND YEAR(t.dataTransacao) = :ano " +
           "AND MONTH(t.dataTransacao) = :mes " +
           "AND (t.transferencia IS NULL OR t.transferencia = false)")
    BigDecimal somarPorTipoEStatusECompetencia(
            @Param("usuarioId") Long usuarioId,
            @Param("tipo") br.com.gerenciadorfinanceiro.model.enums.TipoTransacao tipo,
            @Param("status") br.com.gerenciadorfinanceiro.model.enums.StatusTransacao status,
            @Param("ano") Integer ano,
            @Param("mes") Integer mes);

    @Query("SELECT t.categoria.id, t.categoria.nome, t.categoria.icone, SUM(t.valor) " +
           "FROM Transacao t " +
           "WHERE t.usuario.id = :usuarioId " +
           "AND t.tipo = 'DESPESA' " +
           "AND t.status = 'PAGA' " +
           "AND (t.transferencia IS NULL OR t.transferencia = false) " +
           "GROUP BY t.categoria.id, t.categoria.nome, t.categoria.icone " +
           "ORDER BY SUM(t.valor) DESC")
    List<Object[]> agruparDespesasPagasPorCategoria(@Param("usuarioId") Long usuarioId);

    @Query("SELECT YEAR(t.dataTransacao), MONTH(t.dataTransacao), t.tipo, SUM(t.valor) " +
           "FROM Transacao t " +
           "WHERE t.usuario.id = :usuarioId " +
           "AND t.status = 'PAGA' " +
           "AND (t.transferencia IS NULL OR t.transferencia = false) " +
           "AND t.dataTransacao >= :dataInicio " +
           "AND t.dataTransacao <= :dataFim " +
           "GROUP BY YEAR(t.dataTransacao), MONTH(t.dataTransacao), t.tipo")
    List<Object[]> somarEvolucaoMensalPorPeriodo(
            @Param("usuarioId") Long usuarioId,
            @Param("dataInicio") java.time.LocalDate dataInicio,
            @Param("dataFim") java.time.LocalDate dataFim);

    List<Transacao> findTop5ByUsuarioIdAndDataTransacaoLessThanEqualOrderByDataTransacaoDescIdDesc(
            Long usuarioId, java.time.LocalDate dataLimite);
}
