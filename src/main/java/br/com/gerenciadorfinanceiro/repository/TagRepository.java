package br.com.gerenciadorfinanceiro.repository;

import br.com.gerenciadorfinanceiro.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByUsuarioId(Long usuarioId);
    Optional<Tag> findByIdAndUsuarioId(Long id, Long usuarioId);
}
