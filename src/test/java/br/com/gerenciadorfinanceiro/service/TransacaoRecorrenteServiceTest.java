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

        categoria = new Categoria(usuario, "Contas Básicas", TipoTransacao.DESPESA, "fa-bolt", "Luz e Net");
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
        when(transacaoRepository.findByUsuarioId(eq(1L))).thenReturn(new ArrayList<>());
        when(transacaoService.salvarEntidade(any(Transacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecorrenciaResponseDto resultado = recorrenteService.salvar(request, usuario);

        assertNotNull(resultado);
        // Deve ter gerado 12 transações (Janeiro a Dezembro de 2026)
        verify(transacaoService, times(12)).salvarEntidade(any(Transacao.class));
    }
}
