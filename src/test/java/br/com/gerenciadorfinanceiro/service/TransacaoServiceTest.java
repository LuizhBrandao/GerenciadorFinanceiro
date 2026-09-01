package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.controller.dto.TransacaoRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.TransacaoResponseDto;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.model.enums.StatusTransacao;
import br.com.gerenciadorfinanceiro.model.enums.TipoConta;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
import br.com.gerenciadorfinanceiro.service.impl.TransacaoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private ContaService contaService;

    @Mock
    private CategoriaService categoriaService;

    @InjectMocks
    private TransacaoServiceImpl transacaoService;

    private Usuario usuario;
    private Conta conta;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Teste", "teste@email.com", "senha123");
        usuario.setId(1L);

        conta = new Conta(usuario, "Conta Corrente", "Banco", TipoConta.CORRENTE, new BigDecimal("1000.00"));
        conta.setId(10L);

        categoria = new Categoria(usuario, "Alimentação", TipoTransacao.DESPESA, "fa-utensils", "Mercado");
        categoria.setId(20L);
    }

    @Test
    @DisplayName("Deve salvar despesa e debitar da conta")
    void deveSalvarDespesaEDebitarConta() {
        TransacaoRequestDto request = new TransacaoRequestDto(
                "Supermercado",
                new BigDecimal("200.00"),
                TipoTransacao.DESPESA,
                StatusTransacao.PAGA,
                LocalDate.now(),
                10L,
                20L,
                "Compras do mês",
                null
        );

        when(contaService.buscarEntidadePorId(10L, 1L)).thenReturn(conta);
        when(categoriaService.buscarEntidadePorId(20L, 1L)).thenReturn(categoria);
        when(transacaoRepository.save(any(Transacao.class))).thenAnswer(invocation -> {
            Transacao t = invocation.getArgument(0);
            t.setId(100L);
            return t;
        });

        TransacaoResponseDto response = transacaoService.salvar(request, usuario);

        assertNotNull(response);
        assertEquals(new BigDecimal("200.00"), response.valor());
        assertEquals(new BigDecimal("800.00"), conta.getSaldo());

        verify(contaService).salvarEntidade(conta);
        verify(transacaoRepository).save(any(Transacao.class));
    }

    @Test
    @DisplayName("Deve criar parcelamento em N vezes com valores divididos")
    void deveCriarParcelamentoCorretamente() {
        TransacaoRequestDto request = new TransacaoRequestDto(
                "Notebook",
                new BigDecimal("3000.00"),
                TipoTransacao.DESPESA,
                StatusTransacao.PAGA,
                LocalDate.of(2026, 1, 15),
                10L,
                20L,
                "Trabalho",
                null
        );

        when(contaService.buscarEntidadePorId(10L, 1L)).thenReturn(conta);
        when(categoriaService.buscarEntidadePorId(20L, 1L)).thenReturn(categoria);
        when(transacaoRepository.save(any(Transacao.class))).thenAnswer(invocation -> {
            Transacao t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        List<TransacaoResponseDto> parcelas = transacaoService.criarParcelamento(request, 3, usuario);

        assertEquals(3, parcelas.size());
        assertEquals(new BigDecimal("1000.00"), parcelas.get(0).valor());
        verify(transacaoRepository, times(3)).save(any(Transacao.class));
    }
}
