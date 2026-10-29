package br.com.gerenciadorfinanceiro.repository;

import br.com.gerenciadorfinanceiro.model.Conta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContaRepository extends JpaRepository<Conta, Long> {
    List<Conta> findByUsuarioId(Long usuarioId);
    Optional<Conta> findByIdAndUsuarioId(Long id, Long usuarioId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Conta c SET c.saldo = c.saldo - :valor, c.version = c.version + 1 " +
           "WHERE c.id = :id AND c.saldo >= :valor")
    int debitarSaldoSeSuficiente(@Param("id") Long id, @Param("valor") BigDecimal valor);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Conta c SET c.saldo = c.saldo + :valor, c.version = c.version + 1 " +
           "WHERE c.id = :id")
    int creditarSaldo(@Param("id") Long id, @Param("valor") BigDecimal valor);
}
