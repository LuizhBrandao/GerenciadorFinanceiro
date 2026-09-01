package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.controller.dto.ResumoSaldosDto;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.model.enums.TipoConta;
import br.com.gerenciadorfinanceiro.repository.ContaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

    @Mock
    private ContaRepository contaRepository;

    @InjectMocks
    private ContaService contaService;

    private Usuario usuario;
    private Conta contaCorrente1;
    private Conta contaCorrente2;
    private Conta contaInvestimento;
    private Conta contaPoupanca;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Luiz", "luiz@email.com", "senha123");
        usuario.setId(1L);

        contaCorrente1 = new Conta(usuario, "Principal - Bradesco", "Bradesco", TipoConta.CORRENTE, new BigDecimal("3000.00"));
        contaCorrente1.setId(1L);

        contaCorrente2 = new Conta(usuario, "Secundaria/Casal - Itaú", "Itaú", TipoConta.CORRENTE, new BigDecimal("2000.00"));
        contaCorrente2.setId(2L);

        contaInvestimento = new Conta(usuario, "Investimentos - Inter", "Inter", TipoConta.INVESTIMENTO, new BigDecimal("1000.00"));
        contaInvestimento.setId(3L);

        contaPoupanca = new Conta(usuario, "Reserva - NuPoupança", "Nubank", TipoConta.POUPANCA, new BigDecimal("500.00"));
        contaPoupanca.setId(4L);
    }

    @Test
    @DisplayName("Deve calcular saldo consolidado apenas para contas correntes e carteiras operacionais")
    void deveCalcularSaldoConsolidadoApenasContasCorrentes() {
        when(contaRepository.findByUsuarioId(1L)).thenReturn(List.of(contaCorrente1, contaCorrente2, contaInvestimento, contaPoupanca));

        BigDecimal saldoConsolidado = contaService.calcularSaldoConsolidado(1L);

        // 3000.00 + 2000.00 = 5000.00 (exclui 1000 de investimento e 500 de poupanca)
        assertEquals(new BigDecimal("5000.00"), saldoConsolidado);
    }

    @Test
    @DisplayName("Deve calcular total investido e em poupança em contexto separado")
    void deveCalcularSaldoInvestimentosEPoupancaSeparado() {
        when(contaRepository.findByUsuarioId(1L)).thenReturn(List.of(contaCorrente1, contaCorrente2, contaInvestimento, contaPoupanca));

        BigDecimal saldoInvestimentos = contaService.calcularSaldoInvestimentos(1L);

        // 1000.00 (investimento) + 500.00 (poupanca) = 1500.00
        assertEquals(new BigDecimal("1500.00"), saldoInvestimentos);
    }

    @Test
    @DisplayName("Deve calcular patrimônio total somando contas operacionais e investimentos")
    void deveCalcularPatrimonioTotal() {
        when(contaRepository.findByUsuarioId(1L)).thenReturn(List.of(contaCorrente1, contaCorrente2, contaInvestimento, contaPoupanca));

        BigDecimal patrimonioTotal = contaService.calcularPatrimonioTotal(1L);

        // 3000 + 2000 + 1000 + 500 = 6500.00
        assertEquals(new BigDecimal("6500.00"), patrimonioTotal);
    }

    @Test
    @DisplayName("Deve retornar ResumoSaldosDto com a segregação completa")
    void deveRetornarResumoSaldosDtoCompleto() {
        when(contaRepository.findByUsuarioId(1L)).thenReturn(List.of(contaCorrente1, contaCorrente2, contaInvestimento, contaPoupanca));

        ResumoSaldosDto resumo = contaService.obterResumoSaldos(1L);

        assertEquals(new BigDecimal("5000.00"), resumo.saldoContasCorrentes());
        assertEquals(new BigDecimal("1500.00"), resumo.saldoInvestimentos());
        assertEquals(new BigDecimal("6500.00"), resumo.patrimonioTotal());
        assertEquals(2, resumo.totalContasCorrentes());
        assertEquals(2, resumo.totalContasInvestimento());
    }
}
