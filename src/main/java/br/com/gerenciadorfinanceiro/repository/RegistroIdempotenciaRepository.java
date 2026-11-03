package br.com.gerenciadorfinanceiro.repository;

import br.com.gerenciadorfinanceiro.model.RegistroIdempotencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistroIdempotenciaRepository extends JpaRepository<RegistroIdempotencia, Long> {
    Optional<RegistroIdempotencia> findByChave(String chave);
    Optional<RegistroIdempotencia> findByChaveAndUsuarioId(String chave, Long usuarioId);
    boolean existsByChave(String chave);
    List<RegistroIdempotencia> findByDataExpiracaoBefore(LocalDateTime dataLimite);
}
