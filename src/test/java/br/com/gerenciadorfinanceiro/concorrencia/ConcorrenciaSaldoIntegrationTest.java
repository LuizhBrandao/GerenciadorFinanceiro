package br.com.gerenciadorfinanceiro.concorrencia;

import br.com.gerenciadorfinanceiro.exception.SaldoInsuficienteException;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.model.enums.StatusTransacao;
import br.com.gerenciadorfinanceiro.model.enums.TipoConta;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import br.com.gerenciadorfinanceiro.repository.CategoriaRepository;
import br.com.gerenciadorfinanceiro.repository.ContaRepository;
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
import br.com.gerenciadorfinanceiro.repository.UsuarioRepository;
import br.com.gerenciadorfinanceiro.service.ContaService;
import br.com.gerenciadorfinanceiro.service.TransacaoService;
import br.com.gerenciadorfinanceiro.service.TransferenciaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class ConcorrenciaSaldoIntegrationTest {

    @Autowired
    private ContaService contaService;

    @Autowired
    private TransacaoService transacaoService;

    @Autowired
    private TransferenciaService transferenciaService;

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    private Usuario usuario;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        transacaoRepository.deleteAll();
        contaRepository.deleteAll();
        categoriaRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuario = usuarioRepository.save(new Usuario("Usuario Concorrencia", "concorrencia@teste.com", "senha123"));
        categoria = categoriaRepository.save(new Categoria(usuario, "Geral", TipoTransacao.DESPESA, "Descricao", "fa-tag"));
    }

    @Test
    @DisplayName("Concorrência: Dois débitos simultâneos de R$ 100 em conta com saldo R$ 100 devem permitir apenas UM débito")
    void devePrevenirSaldoNegativoEmDebitosSimultaneos() throws InterruptedException {
        Conta conta = contaRepository.save(new Conta(usuario, "Conta Teste Concorrencia", "Banco", TipoConta.CORRENTE, new BigDecimal("100.00")));
        Long contaId = conta.getId();

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latchInicio = new CountDownLatch(1);
        CountDownLatch latchFim = new CountDownLatch(threads);

        AtomicInteger sucessos = new AtomicInteger(0);
        AtomicInteger falhasSaldo = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latchInicio.await(); // Aguarda todas as threads estarem prontas
                    contaService.debitarSaldo(contaId, new BigDecimal("100.00"));
                    sucessos.incrementAndGet();
                } catch (SaldoInsuficienteException e) {
                    falhasSaldo.incrementAndGet();
                } catch (Exception e) {
                    // Outras exceções
                } finally {
                    latchFim.countDown();
                }
            });
        }

        latchInicio.countDown(); // Dispara as 2 threads no mesmo instante
        latchFim.await();
        executor.shutdown();

        Conta contaAtualizada = contaRepository.findById(contaId).orElseThrow();

        assertEquals(1, sucessos.get(), "Exatamente 1 débito deve ter sucesso.");
        assertEquals(1, falhasSaldo.get(), "Exatamente 1 débito deve ser rejeitado por saldo insuficiente.");
        assertEquals(new BigDecimal("0.00"), contaAtualizada.getSaldo(), "Saldo final deve ser exatamente 0.00 e nunca negativo.");
    }

    @Test
    @DisplayName("Concorrência: Efetivar a mesma transação pendente 2 vezes em paralelo deve debitar a conta apenas UMA vez")
    void deveEfetivarTransacaoApenasUmaVez() throws InterruptedException {
        Conta conta = contaRepository.save(new Conta(usuario, "Conta Efetivar", "Banco", TipoConta.CORRENTE, new BigDecimal("500.00")));
        Transacao transacao = transacaoRepository.save(new Transacao(
                usuario,
                "Despesa Pendente",
                new BigDecimal("100.00"),
                TipoTransacao.DESPESA,
                StatusTransacao.PENDENTE,
                LocalDate.now(),
                conta,
                categoria,
                "Observação"
        ));

        Long transacaoId = transacao.getId();
        Long usuarioId = usuario.getId();

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latchInicio = new CountDownLatch(1);
        CountDownLatch latchFim = new CountDownLatch(threads);

        AtomicInteger chamadasConcluidas = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latchInicio.await();
                    transacaoService.efetivar(transacaoId, usuarioId);
                    chamadasConcluidas.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    latchFim.countDown();
                }
            });
        }

        latchInicio.countDown();
        latchFim.await();
        executor.shutdown();

        Conta contaAtualizada = contaRepository.findById(conta.getId()).orElseThrow();
        Transacao transacaoAtualizada = transacaoRepository.findById(transacaoId).orElseThrow();

        assertEquals(threads, chamadasConcluidas.get(), "Ambas as chamadas devem ser concluídas com segurança.");
        assertEquals(StatusTransacao.PAGA, transacaoAtualizada.getStatus(), "Transação deve estar PAGA.");
        assertEquals(new BigDecimal("400.00"), contaAtualizada.getSaldo(), "Saldo deve ter sido debitado apenas uma vez (500 - 100 = 400).");
    }

    @Test
    @DisplayName("Concorrência: Duas transferências simultâneas de R$ 100 quando há apenas R$ 150 devem permitir apenas UMA")
    void devePrevenirTransferenciaDuplicadaSemSaldo() throws InterruptedException {
        Conta origem = contaRepository.save(new Conta(usuario, "Origem", "Banco A", TipoConta.CORRENTE, new BigDecimal("150.00")));
        Conta destino = contaRepository.save(new Conta(usuario, "Destino", "Banco B", TipoConta.POUPANCA, new BigDecimal("0.00")));

        Long origemId = origem.getId();
        Long destinoId = destino.getId();
        Long usuarioId = usuario.getId();

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latchInicio = new CountDownLatch(1);
        CountDownLatch latchFim = new CountDownLatch(threads);

        AtomicInteger transferenciasSucesso = new AtomicInteger(0);
        AtomicInteger transferenciasFalha = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latchInicio.await();
                    transferenciaService.transferir(usuarioId, origemId, destinoId, new BigDecimal("100.00"));
                    transferenciasSucesso.incrementAndGet();
                } catch (SaldoInsuficienteException e) {
                    transferenciasFalha.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    latchFim.countDown();
                }
            });
        }

        latchInicio.countDown();
        latchFim.await();
        executor.shutdown();

        Conta origemAtualizada = contaRepository.findById(origemId).orElseThrow();
        Conta destinoAtualizada = contaRepository.findById(destinoId).orElseThrow();

        assertEquals(1, transferenciasSucesso.get(), "Apenas 1 transferência de R$ 100 deve suceder.");
        assertEquals(1, transferenciasFalha.get(), "A segunda transferência deve falhar por saldo insuficiente.");
        assertEquals(new BigDecimal("50.00"), origemAtualizada.getSaldo(), "Saldo da origem deve ser 150 - 100 = 50.00.");
        assertEquals(new BigDecimal("100.00"), destinoAtualizada.getSaldo(), "Saldo do destino deve ser 0 + 100 = 100.00.");
    }
}
