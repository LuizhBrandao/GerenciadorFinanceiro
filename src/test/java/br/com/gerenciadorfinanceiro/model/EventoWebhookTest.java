package br.com.gerenciadorfinanceiro.model;

import br.com.gerenciadorfinanceiro.model.enums.StatusProcessamentoWebhook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventoWebhookTest {

    @Test
    @DisplayName("Deve instanciar EventoWebhook com status RECEBIDO e dados válidos")
    void deveCriarEventoWebhookComSucesso() {
        EventoWebhook evento = new EventoWebhook("evt_123456", "STRIPE", "payment_intent.succeeded", "{\"amount\":1000}");

        assertNotNull(evento);
        assertEquals("evt_123456", evento.getEventId());
        assertEquals("STRIPE", evento.getGateway());
        assertEquals("payment_intent.succeeded", evento.getTipoEvento());
        assertEquals(StatusProcessamentoWebhook.RECEBIDO, evento.getStatus());
        assertNotNull(evento.getDataRecebimento());
        assertNull(evento.getDataProcessamento());
    }

    @Test
    @DisplayName("Deve rejeitar criação de EventoWebhook com eventId nulo ou vazio")
    void deveLancarExcecaoAoCriarSemEventId() {
        assertThrows(IllegalArgumentException.class, () ->
                new EventoWebhook(null, "STRIPE", "payment_intent.succeeded", null));
        assertThrows(IllegalArgumentException.class, () ->
                new EventoWebhook("   ", "STRIPE", "payment_intent.succeeded", null));
    }

    @Test
    @DisplayName("Deve alternar ciclo de vida do evento: RECEBIDO -> PROCESSANDO -> PROCESSADO")
    void deveGerenciarCicloDeVidaDeSucesso() {
        EventoWebhook evento = new EventoWebhook("evt_123456", "STRIPE", "payment_intent.succeeded", null);
        
        evento.marcarComoProcessando();
        assertEquals(StatusProcessamentoWebhook.PROCESSANDO, evento.getStatus());

        evento.marcarComoProcessado();
        assertEquals(StatusProcessamentoWebhook.PROCESSADO, evento.getStatus());
        assertNotNull(evento.getDataProcessamento());
        assertNull(evento.getMensagemErro());
    }

    @Test
    @DisplayName("Deve registrar falha no evento com mensagem de erro")
    void deveRegistrarFalha() {
        EventoWebhook evento = new EventoWebhook("evt_123456", "STRIPE", "payment_intent.succeeded", null);
        evento.marcarComoFalha("Timeout ao conectar com serviço de notificação");

        assertEquals(StatusProcessamentoWebhook.FALHA, evento.getStatus());
        assertNotNull(evento.getDataProcessamento());
        assertEquals("Timeout ao conectar com serviço de notificação", evento.getMensagemErro());
    }

    @Test
    @DisplayName("Dois eventos com mesmo eventId devem ser considerados iguais (equals e hashCode)")
    void deveGarantirIgualdadePorEventId() {
        EventoWebhook ev1 = new EventoWebhook("evt_999", "STRIPE", "charge.succeeded", null);
        EventoWebhook ev2 = new EventoWebhook("evt_999", "OUTRO", "outro.evento", null);

        assertEquals(ev1, ev2);
        assertEquals(ev1.hashCode(), ev2.hashCode());
    }
}
