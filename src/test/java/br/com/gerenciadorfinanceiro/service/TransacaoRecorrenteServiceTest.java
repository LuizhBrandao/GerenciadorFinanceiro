package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.TransacaoRecorrente;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.model.enums.FrequenciaRecorrencia;
import br.com.gerenciadorfinanceiro.model.enums.StatusTransacao;
import br.com.gerenciadorfinanceiro.model.enums.TipoConta;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import br.com.gerenciadorfinanceiro.repository.CategoriaRepository;
import br.com.gerenciadorfinanceiro.repository.TransacaoRecorrenteRepository;
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private TransacaoRecorrenteService recorrenteService;

    private Usuario usuario;
    private Conta conta;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Teste", "teste@email.com", "123");
        usuario.setId(1L);

        conta = new Conta(usuario, "Principal", BigDecimal.valueOf(5000), TipoConta.CORRENTE, "Banco");
        conta.setId(10L);

        categoria = new Categoria(usuario, "Contas Básicas", TipoTransacao.DESPESA, "fa-bolt", "Luz e Net");
        categoria.setId(20L);
    }

    @Test
    @DisplayName("Deve salvar recorrência mensal e gerar ocorrências retroativas e futuras para o ano")
    void deveSalvarRecorrenciaEGerarInstanciasDoAno() {
        TransacaoRecorrente rec = new TransacaoRecorrente(
                usuario, "VIVO", BigDecimal.valueOf(150), TipoTransacao.DESPESA,
                FrequenciaRecorrencia.MENSAL, 10, conta, categoria,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "Internet"
        );
        rec.setId(100L);

        when(contaService.buscarPorId(eq(10L), eq(1L))).thenReturn(conta);
        when(categoriaRepository.findByIdAndUsuarioId(eq(20L), eq(1L))).thenReturn(Optional.of(categoria));
        when(transacaoRecorrenteRepository.save(any(TransacaoRecorrente.class))).thenReturn(rec);
        when(transacaoRepository.findByUsuarioId(eq(1L))).thenReturn(new ArrayList<>());
        when(transacaoService.salvar(any(Transacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransacaoRecorrente resultado = recorrenteService.salvar(rec);

        assertNotNull(resultado);
        // Deve ter gerado 12 transações (Janeiro a Dezembro de 2026)
        verify(transacaoService, times(12)).salvar(any(Transacao.class));
    }
}
