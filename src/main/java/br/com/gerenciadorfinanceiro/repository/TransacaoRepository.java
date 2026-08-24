package br.com.gerenciadorfinanceiro.repository;

import br.com.gerenciadorfinanceiro.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long>, JpaSpecificationExecutor<Transacao> {
    List<Transacao> findByUsuarioId(Long usuarioId);
    Optional<Transacao> findByIdAndUsuarioId(Long id, Long usuarioId);
    List<Transacao> findByContaIdAndUsuarioId(Long contaId, Long usuarioId);
}
