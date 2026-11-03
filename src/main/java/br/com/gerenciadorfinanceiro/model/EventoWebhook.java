package br.com.gerenciadorfinanceiro.model;

import br.com.gerenciadorfinanceiro.model.enums.StatusProcessamentoWebhook;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "eventos_webhook", uniqueConstraints = {
        @UniqueConstraint(name = "uk_eventos_webhook_event_id", columnNames = {"event_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 120)
    private String eventId;

    @Column(nullable = false, length = 50)
    private String gateway;

    @Column(name = "tipo_evento", nullable = false, length = 100)
    private String tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusProcessamentoWebhook status;

    @Column(name = "payload_resumo", length = 500)
    private String payloadResumo;

    @Column(name = "mensagem_erro", length = 1000)
    private String mensagemErro;

    @Column(name = "data_recebimento", nullable = false, updatable = false)
    private LocalDateTime dataRecebimento;

    @Column(name = "data_processamento")
    private LocalDateTime dataProcessamento;

    public EventoWebhook(String eventId, String gateway, String tipoEvento, String payloadResumo) {
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("O ID do evento do gateway não pode ser nulo ou vazio.");
        }
        if (gateway == null || gateway.trim().isEmpty()) {
            throw new IllegalArgumentException("O gateway de pagamento deve ser informado.");
        }
        if (tipoEvento == null || tipoEvento.trim().isEmpty()) {
            throw new IllegalArgumentException("O tipo de evento deve ser informado.");
        }

        this.eventId = eventId.trim();
        this.gateway = gateway.trim().toUpperCase();
        this.tipoEvento = tipoEvento.trim();
        this.payloadResumo = (payloadResumo != null && payloadResumo.length() > 500)
                ? payloadResumo.substring(0, 500)
                : payloadResumo;
        this.status = StatusProcessamentoWebhook.RECEBIDO;
        this.dataRecebimento = LocalDateTime.now();
    }

    public void marcarComoProcessando() {
        if (this.status == StatusProcessamentoWebhook.PROCESSADO) {
            throw new IllegalStateException("Evento já concluído não pode voltar para processamento.");
        }
        this.status = StatusProcessamentoWebhook.PROCESSANDO;
    }

    public void marcarComoProcessado() {
        this.status = StatusProcessamentoWebhook.PROCESSADO;
        this.dataProcessamento = LocalDateTime.now();
        this.mensagemErro = null;
    }

    public void marcarComoFalha(String motivo) {
        this.status = StatusProcessamentoWebhook.FALHA;
        this.dataProcessamento = LocalDateTime.now();
        this.mensagemErro = (motivo != null && motivo.length() > 1000)
                ? motivo.substring(0, 1000)
                : motivo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventoWebhook that)) return false;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
}
