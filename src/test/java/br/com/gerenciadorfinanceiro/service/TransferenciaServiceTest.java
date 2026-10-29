package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.exception.SaldoInsuficienteException;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.model.enums.TipoConta;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import br.com.gerenciadorfinanceiro.repository.CategoriaRepository;
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
import br.com.gerenciadorfinanceiro.service.impl.TransferenciaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferenciaServiceTest {

    @Mock
    private ContaService contaService;

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

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
    @DisplayName("Deve debitar da conta origem, creditar na conta destino e registrar lançamentos espelhados no extrato")
    void deveTransferirComSucesso() {
        when(contaService.buscarEntidadePorId(10L, 1L)).thenReturn(contaOrigem);
        when(contaService.buscarEntidadePorId(20L, 1L)).thenReturn(contaDestino);
        when(categoriaRepository.findByUsuarioIdAndNomeIgnoreCaseAndTipo(eq(1L), anyString(), any(TipoTransacao.class)))
                .thenReturn(Optional.empty());
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        transferenciaService.transferir(1L, 10L, 20L, new BigDecimal("300.00"));

        assertEquals(new BigDecimal("700.00"), contaOrigem.getSaldo());
        assertEquals(new BigDecimal("500.00"), contaDestino.getSaldo());

        verify(contaService).debitarSaldo(10L, new BigDecimal("300.00"));
        verify(contaService).creditarSaldo(20L, new BigDecimal("300.00"));

        ArgumentCaptor<Transacao> captor = ArgumentCaptor.forClass(Transacao.class);
        verify(transacaoRepository, times(2)).save(captor.capture());

        List<Transacao> lancamentos = captor.getAllValues();
        Transacao saida = lancamentos.get(0);
        Transacao entrada = lancamentos.get(1);

        assertTrue(saida.isTransferenciaPatrimonial());
        assertEquals(TipoTransacao.DESPESA, saida.getTipo());
        assertEquals(contaOrigem, saida.getConta());
        assertEquals(new BigDecimal("300.00"), saida.getValor());

        assertTrue(entrada.isTransferenciaPatrimonial());
        assertEquals(TipoTransacao.RECEITA, entrada.getTipo());
        assertEquals(contaDestino, entrada.getConta());
        assertEquals(new BigDecimal("300.00"), entrada.getValor());
    }

    @Test
    @DisplayName("Deve lancar excecao quando saldo for insuficiente na conta origem")
    void deveLancarExcecaoSaldoInsuficiente() {
        when(contaService.buscarEntidadePorId(10L, 1L)).thenReturn(contaOrigem);
        when(contaService.buscarEntidadePorId(20L, 1L)).thenReturn(contaDestino);
        doThrow(new SaldoInsuficienteException("Saldo insuficiente"))
                .when(contaService).debitarSaldo(10L, new BigDecimal("1500.00"));

        assertThrows(SaldoInsuficienteException.class, () ->
                transferenciaService.transferir(1L, 10L, 20L, new BigDecimal("1500.00")));

        verify(contaService, never()).creditarSaldo(any(), any());
        verify(transacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando contas de origem e destino forem iguais")
    void deveLancarExcecaoContasIguais() {
        assertThrows(IllegalArgumentException.class, () ->
                transferenciaService.transferir(1L, 10L, 10L, new BigDecimal("100.00")));
    }
}
