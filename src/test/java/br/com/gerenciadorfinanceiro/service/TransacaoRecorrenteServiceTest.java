package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.controller.dto.RecorrenciaRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.RecorrenciaResponseDto;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.TransacaoRecorrente;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.model.enums.FrequenciaRecorrencia;
import br.com.gerenciadorfinanceiro.model.enums.TipoConta;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import br.com.gerenciadorfinanceiro.repository.TransacaoRecorrenteRepository;
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
import br.com.gerenciadorfinanceiro.service.impl.TransacaoRecorrenteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransacaoRecorrenteServiceTest {

    @Mock
    private TransacaoRecorrenteRepository transacaoRecorrenteRepository;

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private TransacaoService transacaoService;

    @Mock
    private ContaService contaService;

    @Mock
    private CategoriaService categoriaService;

    @InjectMocks
    private TransacaoRecorrenteServiceImpl recorrenteService;

    private Usuario usuario;
    private Conta conta;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Teste", "teste@email.com", "123");
        usuario.setId(1L);

        conta = new Conta(usuario, "Principal", "Banco", TipoConta.CORRENTE, BigDecimal.valueOf(5000));
        conta.setId(10L);

        categoria = new Categoria(usuario, "Contas Básicas", TipoTransacao.DESPESA, "Luz e Net", "fa-bolt");
        categoria.setId(20L);
    }

    @Test
    @DisplayName("Deve salvar recorrência mensal e gerar ocorrências retroativas e futuras para o ano")
    void deveSalvarRecorrenciaEGerarInstanciasDoAno() {
        RecorrenciaRequestDto request = new RecorrenciaRequestDto(
                "VIVO",
                BigDecimal.valueOf(150),
                TipoTransacao.DESPESA,
                FrequenciaRecorrencia.MENSAL,
                10,
                10L,
                20L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                "Internet"
        );

        TransacaoRecorrente recSalva = new TransacaoRecorrente(
                usuario, "VIVO", BigDecimal.valueOf(150), TipoTransacao.DESPESA,
                FrequenciaRecorrencia.MENSAL, 10, conta, categoria,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "Internet"
        );
        recSalva.setId(100L);

        when(contaService.buscarEntidadePorId(eq(10L), eq(1L))).thenReturn(conta);
        when(categoriaService.buscarEntidadePorId(eq(20L), eq(1L))).thenReturn(categoria);
        when(transacaoRecorrenteRepository.save(any(TransacaoRecorrente.class))).thenReturn(recSalva);
        when(transacaoRepository.findByRecorrenciaId(any())).thenReturn(new ArrayList<>());
        when(transacaoRepository.findByUsuarioIdAndRecorrenciaIsNullAndDescricaoIgnoreCase(any(), any())).thenReturn(new ArrayList<>());
        when(transacaoService.salvarEntidade(any(Transacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecorrenciaResponseDto resultado = recorrenteService.salvar(request, usuario);

        assertNotNull(resultado);
        // Deve ter gerado 12 transações (Janeiro a Dezembro de 2026)
        verify(transacaoService, times(12)).salvarEntidade(any(Transacao.class));
    }

    @Test
    @DisplayName("Deve sincronizar transações vinculadas à recorrência por recorrencia_id")
    void deveSincronizarTransacoesPorRecorrenciaId() {
        RecorrenciaRequestDto updateRequest = new RecorrenciaRequestDto(
                "VIVO FIBRA NOVO",
                BigDecimal.valueOf(180),
                TipoTransacao.DESPESA,
                FrequenciaRecorrencia.MENSAL,
                15,
                10L,
                20L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                "Upgrade de plano"
        );

        TransacaoRecorrente existente = new TransacaoRecorrente(
                usuario, "VIVO", BigDecimal.valueOf(150), TipoTransacao.DESPESA,
                FrequenciaRecorrencia.MENSAL, 10, conta, categoria,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "Internet"
        );
        existente.setId(100L);

        Transacao tPendente = new Transacao(
                usuario, "VIVO", BigDecimal.valueOf(150), TipoTransacao.DESPESA,
                br.com.gerenciadorfinanceiro.model.enums.StatusTransacao.PENDENTE,
                LocalDate.of(2026, 6, 10), conta, categoria, "Internet"
        );
        tPendente.vincularRecorrencia(existente);

        when(transacaoRecorrenteRepository.findByIdAndUsuarioId(eq(100L), eq(1L))).thenReturn(java.util.Optional.of(existente));
        when(contaService.buscarEntidadePorId(eq(10L), eq(1L))).thenReturn(conta);
        when(categoriaService.buscarEntidadePorId(eq(20L), eq(1L))).thenReturn(categoria);
        when(transacaoRecorrenteRepository.save(any(TransacaoRecorrente.class))).thenReturn(existente);
        when(transacaoRepository.findByRecorrenciaId(eq(100L))).thenReturn(java.util.List.of(tPendente));
        when(transacaoRepository.findByUsuarioIdAndRecorrenciaIsNullAndDescricaoIgnoreCase(any(), any())).thenReturn(new ArrayList<>());
        when(transacaoService.salvarEntidade(any(Transacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecorrenciaResponseDto resultado = recorrenteService.atualizar(100L, updateRequest, 1L);

        assertNotNull(resultado);
        verify(transacaoRepository).save(tPendente);
        org.junit.jupiter.api.Assertions.assertEquals("VIVO FIBRA NOVO", tPendente.getDescricao());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("180.00"), tPendente.getValor());
    }
}
