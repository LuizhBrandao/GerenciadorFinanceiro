package br.com.gerenciadorfinanceiro.repository;

import br.com.gerenciadorfinanceiro.model.PreferenciasUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PreferenciasUsuarioRepository extends JpaRepository<PreferenciasUsuario, Long> {
    Optional<PreferenciasUsuario> findByUsuarioId(Long usuarioId);
}
