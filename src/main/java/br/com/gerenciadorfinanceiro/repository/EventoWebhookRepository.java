package br.com.gerenciadorfinanceiro.repository;

import br.com.gerenciadorfinanceiro.model.EventoWebhook;
import br.com.gerenciadorfinanceiro.model.enums.StatusProcessamentoWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventoWebhookRepository extends JpaRepository<EventoWebhook, Long> {
    Optional<EventoWebhook> findByEventId(String eventId);
    boolean existsByEventId(String eventId);
    List<EventoWebhook> findByStatus(StatusProcessamentoWebhook status);
}
