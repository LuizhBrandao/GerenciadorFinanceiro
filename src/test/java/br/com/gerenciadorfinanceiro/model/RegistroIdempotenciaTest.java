package br.com.gerenciadorfinanceiro.model;

import br.com.gerenciadorfinanceiro.model.enums.StatusIdempotencia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RegistroIdempotenciaTest {

    @Test
    @DisplayName("Deve instanciar RegistroIdempotencia com status PROCESSANDO e expiração padrão de 24h")
    void deveCriarRegistroComSucesso() {
        String chave = "550e8400-e29b-41d4-a716-446655440000";
        String hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        RegistroIdempotencia reg = new RegistroIdempotencia(chave, 1L, "POST", "/transacoes", hash, 24);

        assertNotNull(reg);
        assertEquals(chave, reg.getChave());
        assertEquals(1L, reg.getUsuarioId());
        assertEquals("POST", reg.getMetodoHttp());
        assertEquals("/transacoes", reg.getUriEndpoint());
        assertEquals(hash, reg.getRequestHash());
        assertEquals(StatusIdempotencia.PROCESSANDO, reg.getStatus());
        assertNotNull(reg.getDataCriacao());
        assertNotNull(reg.getDataExpiracao());
        assertTrue(reg.getDataExpiracao().isAfter(LocalDateTime.now()));
        assertFalse(reg.isExpirado());
    }

    @Test
    @DisplayName("Deve rejeitar criação de RegistroIdempotencia com campos obrigatórios ausentes")
    void deveRejeitarCamposInvalidos() {
        assertThrows(IllegalArgumentException.class, () ->
                new RegistroIdempotencia(null, 1L, "POST", "/transacoes", "hash", 24));
        assertThrows(IllegalArgumentException.class, () ->
                new RegistroIdempotencia("chave", null, "POST", "/transacoes", "hash", 24));
        assertThrows(IllegalArgumentException.class, () ->
                new RegistroIdempotencia("chave", 1L, "", "/transacoes", "hash", 24));
        assertThrows(IllegalArgumentException.class, () ->
                new RegistroIdempotencia("chave", 1L, "POST", null, "hash", 24));
        assertThrows(IllegalArgumentException.class, () ->
                new RegistroIdempotencia("chave", 1L, "POST", "/transacoes", null, 24));
    }

    @Test
    @DisplayName("Deve concluir registro de idempotência salvando status e payload da resposta")
    void deveConcluirRegistroComSucesso() {
        RegistroIdempotencia reg = new RegistroIdempotencia("chave-123", 1L, "POST", "/transacoes", "hash1", 24);

        reg.concluir(201, "{\"id\":10,\"descricao\":\"Transação criada\"}");

        assertEquals(StatusIdempotencia.CONCLUIDO, reg.getStatus());
        assertEquals(201, reg.getResponseStatusCode());
        assertEquals("{\"id\":10,\"descricao\":\"Transação criada\"}", reg.getResponseBody());
    }

    @Test
    @DisplayName("Deve verificar correspondência do hash do payload da requisição")
    void deveVerificarCorrespondenciaPayload() {
        RegistroIdempotencia reg = new RegistroIdempotencia("chave-123", 1L, "POST", "/transacoes", "hash-esperado", 24);

        assertTrue(reg.verificarCorrespondenciaPayload("hash-esperado"));
        assertFalse(reg.verificarCorrespondenciaPayload("outro-hash-diferente"));
        assertFalse(reg.verificarCorrespondenciaPayload(null));
    }

    @Test
    @DisplayName("Dois registros com mesma chave devem ser considerados iguais")
    void deveGarantirIgualdadePorChave() {
        RegistroIdempotencia r1 = new RegistroIdempotencia("chave-uuid", 1L, "POST", "/transacoes", "hash", 24);
        RegistroIdempotencia r2 = new RegistroIdempotencia("chave-uuid", 2L, "PATCH", "/contas", "outro-hash", 24);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }
}
