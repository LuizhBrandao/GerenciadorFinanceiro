package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.controller.dto.OrcamentoRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.OrcamentoResponseDto;
import br.com.gerenciadorfinanceiro.exception.EntidadeNaoEncontradaException;
import br.com.gerenciadorfinanceiro.exception.RegraNegocioException;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Orcamento;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import br.com.gerenciadorfinanceiro.repository.OrcamentoRepository;
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
import br.com.gerenciadorfinanceiro.service.impl.OrcamentoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcamentoServiceTest {

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private CategoriaService categoriaService;

    @Mock
    private TransacaoRepository transacaoRepository;

    @InjectMocks
    private OrcamentoServiceImpl orcamentoService;

    private Usuario usuario;
    private Categoria categoriaDespesa;
    private Categoria categoriaReceita;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Maria", "maria@email.com", "senha123");
        usuario.setId(1L);

        categoriaDespesa = new Categoria(usuario, "Alimentação & Supermercado", TipoTransacao.DESPESA, "Mercado", "fa-utensils");
        categoriaDespesa.setId(10L);

        categoriaReceita = new Categoria(usuario, "Salário", TipoTransacao.RECEITA, "Trabalho", "fa-money-bill");
        categoriaReceita.setId(20L);
    }

    @Test
    @DisplayName("Deve salvar orçamento com sucesso para categoria de despesa")
    void deveSalvarOrcamentoComSucesso() {
        OrcamentoRequestDto request = new OrcamentoRequestDto(
                10L, 2026, 10, new BigDecimal("1200.00"), "Meta de mercado outubro"
        );

        Orcamento orcamentoSalvo = new Orcamento(categoriaDespesa, 2026, 10, new BigDecimal("1200.00"), "Meta de mercado outubro");
        orcamentoSalvo.setId(100L);

        when(categoriaService.buscarEntidadePorId(eq(10L), eq(1L))).thenReturn(categoriaDespesa);
        when(orcamentoRepository.existsByCategoriaIdAndAnoAndMes(eq(10L), eq(2026), eq(10))).thenReturn(false);
        when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(orcamentoSalvo);
        when(transacaoRepository.somarDespesasPagasPorCategoriaECompetencia(eq(1L), eq(10L), eq(2026), eq(10)))
                .thenReturn(new BigDecimal("300.00"));

        OrcamentoResponseDto response = orcamentoService.salvar(request, 1L);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(new BigDecimal("1200.00"), response.valorLimite());
        assertEquals(new BigDecimal("300.00"), response.valorGasto());
        assertEquals(new BigDecimal("25.00"), response.percentualConsumido());
        assertFalse(response.ultrapassado());
        verify(orcamentoRepository).save(any(Orcamento.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar criar orçamento para categoria de RECEITA")
    void deveLancarExcecaoAoSalvarParaCategoriaReceita() {
        OrcamentoRequestDto request = new OrcamentoRequestDto(
                20L, 2026, 10, new BigDecimal("5000.00"), "Meta"
        );

        when(categoriaService.buscarEntidadePorId(eq(20L), eq(1L))).thenReturn(categoriaReceita);

        assertThrows(RegraNegocioException.class, () -> orcamentoService.salvar(request, 1L));
        verify(orcamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar criar orçamento duplicado para mesma categoria e competência")
    void deveLancarExcecaoAoSalvarDuplicado() {
        OrcamentoRequestDto request = new OrcamentoRequestDto(
                10L, 2026, 10, new BigDecimal("1000.00"), "Meta"
        );

        when(categoriaService.buscarEntidadePorId(eq(10L), eq(1L))).thenReturn(categoriaDespesa);
        when(orcamentoRepository.existsByCategoriaIdAndAnoAndMes(eq(10L), eq(2026), eq(10))).thenReturn(true);

        assertThrows(RegraNegocioException.class, () -> orcamentoService.salvar(request, 1L));
        verify(orcamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve apurar orçamento ultrapassado quando gasto for maior que o limite")
    void deveApurarOrcamentoUltrapassado() {
        Orcamento orcamento = new Orcamento(categoriaDespesa, 2026, 10, new BigDecimal("500.00"), "Meta apertada");
        orcamento.setId(200L);

        when(orcamentoRepository.findByIdAndCategoriaUsuarioId(eq(200L), eq(1L))).thenReturn(Optional.of(orcamento));
        when(transacaoRepository.somarDespesasPagasPorCategoriaECompetencia(eq(1L), eq(10L), eq(2026), eq(10)))
                .thenReturn(new BigDecimal("650.00"));

        OrcamentoResponseDto response = orcamentoService.buscarPorId(200L, 1L);

        assertNotNull(response);
        assertEquals(new BigDecimal("500.00"), response.valorLimite());
        assertEquals(new BigDecimal("650.00"), response.valorGasto());
        assertEquals(new BigDecimal("130.00"), response.percentualConsumido());
        assertTrue(response.ultrapassado());
    }

    @Test
    @DisplayName("Deve atualizar limite e recalcular consumo")
    void deveAtualizarLimiteComSucesso() {
        Orcamento existente = new Orcamento(categoriaDespesa, 2026, 10, new BigDecimal("500.00"), "Meta inicial");
        existente.setId(200L);

        OrcamentoRequestDto request = new OrcamentoRequestDto(
                10L, 2026, 10, new BigDecimal("800.00"), "Meta expandida"
        );

        when(orcamentoRepository.findByIdAndCategoriaUsuarioId(eq(200L), eq(1L))).thenReturn(Optional.of(existente));
        when(orcamentoRepository.save(any(Orcamento.class))).thenReturn(existente);
        when(transacaoRepository.somarDespesasPagasPorCategoriaECompetencia(eq(1L), eq(10L), eq(2026), eq(10)))
                .thenReturn(new BigDecimal("400.00"));

        OrcamentoResponseDto response = orcamentoService.atualizar(200L, request, 1L);

        assertNotNull(response);
        assertEquals(new BigDecimal("800.00"), response.valorLimite());
        assertEquals(new BigDecimal("50.00"), response.percentualConsumido());
        assertFalse(response.ultrapassado());
    }

    @Test
    @DisplayName("Deve excluir orçamento com sucesso")
    void deveExcluirOrcamento() {
        Orcamento existente = new Orcamento(categoriaDespesa, 2026, 10, new BigDecimal("500.00"), "Meta");
        existente.setId(200L);

        when(orcamentoRepository.findByIdAndCategoriaUsuarioId(eq(200L), eq(1L))).thenReturn(Optional.of(existente));

        orcamentoService.excluir(200L, 1L);

        verify(orcamentoRepository).delete(existente);
    }
}
