package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.exception.SaldoInsuficienteException;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.model.enums.TipoConta;
import br.com.gerenciadorfinanceiro.service.impl.TransferenciaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferenciaServiceTest {

    @Mock
    private ContaService contaService;

    @InjectMocks
    private TransferenciaServiceImpl transferenciaService;

    private Usuario usuario;
    private Conta contaOrigem;
    private Conta contaDestino;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Usuario", "usuario@teste.com", "123456");
        usuario.setId(1L);

        contaOrigem = new Conta(usuario, "Conta Origem", "Banco A", TipoConta.CORRENTE, new BigDecimal("1000.00"));
        contaOrigem.setId(10L);

        contaDestino = new Conta(usuario, "Conta Destino", "Banco B", TipoConta.POUPANCA, new BigDecimal("200.00"));
        contaDestino.setId(20L);
    }

    @Test
    @DisplayName("Deve debitar da conta origem e creditar na conta destino com sucesso")
    void deveTransferirComSucesso() {
        when(contaService.buscarEntidadePorId(10L, 1L)).thenReturn(contaOrigem);
        when(contaService.buscarEntidadePorId(20L, 1L)).thenReturn(contaDestino);

        transferenciaService.transferir(1L, 10L, 20L, new BigDecimal("300.00"));

        assertEquals(new BigDecimal("700.00"), contaOrigem.getSaldo());
        assertEquals(new BigDecimal("500.00"), contaDestino.getSaldo());

        verify(contaService).salvarEntidade(contaOrigem);
        verify(contaService).salvarEntidade(contaDestino);
    }

    @Test
    @DisplayName("Deve lancar excecao quando saldo for insuficiente na conta origem")
    void deveLancarExcecaoSaldoInsuficiente() {
        when(contaService.buscarEntidadePorId(10L, 1L)).thenReturn(contaOrigem);
        when(contaService.buscarEntidadePorId(20L, 1L)).thenReturn(contaDestino);

        assertThrows(SaldoInsuficienteException.class, () ->
                transferenciaService.transferir(1L, 10L, 20L, new BigDecimal("1500.00")));

        verify(contaService, never()).salvarEntidade(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando contas de origem e destino forem iguais")
    void deveLancarExcecaoContasIguais() {
        assertThrows(IllegalArgumentException.class, () ->
                transferenciaService.transferir(1L, 10L, 10L, new BigDecimal("100.00")));
    }
}
