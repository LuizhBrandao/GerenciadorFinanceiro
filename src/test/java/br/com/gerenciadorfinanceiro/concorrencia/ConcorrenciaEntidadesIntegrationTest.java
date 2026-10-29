package br.com.gerenciadorfinanceiro.concorrencia;

import br.com.gerenciadorfinanceiro.controller.dto.OrcamentoRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.RecorrenciaRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.TransacaoRequestDto;
import br.com.gerenciadorfinanceiro.exception.RegraNegocioException;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.model.enums.FrequenciaRecorrencia;
import br.com.gerenciadorfinanceiro.model.enums.StatusTransacao;
import br.com.gerenciadorfinanceiro.model.enums.TipoConta;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import br.com.gerenciadorfinanceiro.repository.CategoriaRepository;
import br.com.gerenciadorfinanceiro.repository.ContaRepository;
import br.com.gerenciadorfinanceiro.repository.OrcamentoRepository;
import br.com.gerenciadorfinanceiro.repository.TagRepository;
import br.com.gerenciadorfinanceiro.repository.TransacaoRecorrenteRepository;
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
import br.com.gerenciadorfinanceiro.repository.UsuarioRepository;
import br.com.gerenciadorfinanceiro.service.CategoriaService;
import br.com.gerenciadorfinanceiro.service.OrcamentoService;
import br.com.gerenciadorfinanceiro.service.TransacaoRecorrenteService;
import br.com.gerenciadorfinanceiro.service.TransacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class ConcorrenciaEntidadesIntegrationTest {

    @Autowired
    private OrcamentoService orcamentoService;

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private TransacaoService transacaoService;

    @Autowired
    private TransacaoRecorrenteService transacaoRecorrenteService;

    @Autowired
    private OrcamentoRepository orcamentoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private TransacaoRecorrenteRepository transacaoRecorrenteRepository;

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario usuario;
    private Conta conta;
    private Categoria categoriaDespesa;

    @BeforeEach
    void setUp() {
        transacaoRepository.deleteAll();
        transacaoRecorrenteRepository.deleteAll();
        orcamentoRepository.deleteAll();
        tagRepository.deleteAll();
        contaRepository.deleteAll();
        categoriaRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuario = usuarioRepository.save(new Usuario("Usuario Entidades Test", "entidades@teste.com", "senha123"));
        conta = contaRepository.save(new Conta(usuario, "Conta Teste", "Banco", TipoConta.CORRENTE, new BigDecimal("50000.00")));
        categoriaDespesa = categoriaRepository.save(new Categoria(usuario, "Alimentação", TipoTransacao.DESPESA, "Mercado e refeições", "fa-utensils"));
    }

    @Test
    @DisplayName("Concorrência: Duas criações simultâneas de orçamento para a mesma categoria e mês devem permitir apenas UM")
    void deveImpedirOrcamentoDuplicadoEmInsercoesSimultaneas() throws InterruptedException {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latchInicio = new CountDownLatch(1);
        CountDownLatch latchFim = new CountDownLatch(threads);

        AtomicInteger sucessos = new AtomicInteger(0);
        AtomicInteger falhasRegra = new AtomicInteger(0);

        OrcamentoRequestDto request = new OrcamentoRequestDto(
                categoriaDespesa.getId(),
                2026,
                11,
                new BigDecimal("500.00"),
                "Limite para alimentação"
        );

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latchInicio.await();
                    orcamentoService.salvar(request, usuario.getId());
                    sucessos.incrementAndGet();
                } catch (RegraNegocioException e) {
                    falhasRegra.incrementAndGet();
                } catch (Exception e) {
                    // Outras exceções
                } finally {
                    latchFim.countDown();
                }
            });
        }

        latchInicio.countDown();
        latchFim.await();
        executor.shutdown();

        assertEquals(1, sucessos.get(), "Exatamente um cadastro de orçamento deve ter sucesso.");
        assertEquals(1, falhasRegra.get(), "O segundo cadastro concorrente deve ser rejeitado com RegraNegocioException.");
        assertEquals(1, orcamentoRepository.findByCategoriaUsuarioId(usuario.getId()).size(),
                "A base de dados deve conter apenas um orçamento para a competência.");
    }

    @Test
    @DisplayName("Concorrência: Duas inicializações simultâneas de categorias padrão devem manter total de 13 categorias")
    void deveImpedirDuplicacaoDeCategoriasPadraoEmChamadasSimultaneas() throws InterruptedException {
        Usuario novoUsuario = usuarioRepository.save(new Usuario("Novo User", "novo@teste.com", "senha123"));

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latchInicio = new CountDownLatch(1);
        CountDownLatch latchFim = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latchInicio.await();
                    categoriaService.inicializarCategoriasPadrao(novoUsuario);
                } catch (Exception e) {
                    // Ingestão idempotente de constraint
                } finally {
                    latchFim.countDown();
                }
            });
        }

        latchInicio.countDown();
        latchFim.await();
        executor.shutdown();

        int totalCategorias = categoriaRepository.findByUsuarioId(novoUsuario.getId()).size();
        assertEquals(13, totalCategorias, "O usuário deve possuir exatamente 13 categorias padrão, sem duplicidades.");
    }

    @Test
    @DisplayName("Concorrência: Lançar simultaneamente a mesma instância de recorrência gera apenas UMA transação no banco")
    void deveImpedirGeracaoDuplicadaDeTransacaoRecorrente() throws InterruptedException {
        LocalDate proximoMes = LocalDate.now().plusMonths(1);
        LocalDate dataAlvo = LocalDate.of(proximoMes.getYear(), proximoMes.getMonth(), 15);

        RecorrenciaRequestDto recRequest = new RecorrenciaRequestDto(
                "Internet Fibra",
                new BigDecimal("150.00"),
                TipoTransacao.DESPESA,
                FrequenciaRecorrencia.MENSAL,
                15,
                conta.getId(),
                categoriaDespesa.getId(),
                proximoMes,
                null,
                "Recorrência de teste"
        );
        var recResponse = transacaoRecorrenteService.salvar(recRequest, usuario);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latchInicio = new CountDownLatch(1);
        CountDownLatch latchFim = new CountDownLatch(threads);

        AtomicInteger sucessos = new AtomicInteger(0);
        AtomicInteger falhasRegra = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latchInicio.await();
                    transacaoRecorrenteService.lancarInstancia(recResponse.id(), usuario.getId(), dataAlvo);
                    sucessos.incrementAndGet();
                } catch (RegraNegocioException e) {
                    falhasRegra.incrementAndGet();
                } catch (Exception e) {
                    // Outras falhas
                } finally {
                    latchFim.countDown();
                }
            });
        }

        latchInicio.countDown();
        latchFim.await();
        executor.shutdown();

        assertEquals(1, sucessos.get(), "Exatamente um lançamento de recorrência deve ter sucesso.");
        assertEquals(1, falhasRegra.get(), "A chamada concorrente duplicada deve ser rejeitada com RegraNegocioException.");

        String chave = "REC_" + recResponse.id() + "_" + dataAlvo;
        long totalComChave = transacaoRepository.findAll().stream()
                .filter(t -> chave.equals(t.getChaveRecorrencia()))
                .count();

        assertEquals(1, totalComChave, "Deve existir exatamente UMA transação persistida com a chave única de recorrência.");
    }

    @Test
    @DisplayName("Concorrência: Transações simultâneas criando a mesma Tag resultam em apenas UMA Tag persistida no banco")
    void deveImpedirCriacaoDeTagsDuplicadasConcorrentes() throws InterruptedException {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latchInicio = new CountDownLatch(1);
        CountDownLatch latchFim = new CountDownLatch(threads);

        AtomicInteger concluidas = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    latchInicio.await();
                    TransacaoRequestDto tRequest = new TransacaoRequestDto(
                            "Compra " + index,
                            new BigDecimal("50.00"),
                            TipoTransacao.DESPESA,
                            StatusTransacao.PAGA,
                            LocalDate.now(),
                            conta.getId(),
                            categoriaDespesa.getId(),
                            "Obs " + index,
                            Set.of("TAG_CONCORRENTE")
                    );
                    transacaoService.salvar(tRequest, usuario);
                    concluidas.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Erro na thread " + index + ": " + e.getClass().getName() + " - " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    latchFim.countDown();
                }
            });
        }

        latchInicio.countDown();
        latchFim.await();
        executor.shutdown();

        assertEquals(2, concluidas.get(), "Ambas as transações devem ser criadas com sucesso.");

        long countTags = tagRepository.findByUsuarioId(usuario.getId()).stream()
                .filter(t -> "TAG_CONCORRENTE".equalsIgnoreCase(t.getNome()))
                .count();

        assertEquals(1, countTags, "Deve existir apenas UMA tag 'TAG_CONCORRENTE' salva para o usuário no banco.");
    }
}
