package br.com.gerenciadorfinanceiro.controller;

import br.com.gerenciadorfinanceiro.config.TokenService;
import br.com.gerenciadorfinanceiro.config.idempotency.IdempotencyFilter;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.model.enums.TipoConta;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import br.com.gerenciadorfinanceiro.repository.CategoriaRepository;
import br.com.gerenciadorfinanceiro.repository.ContaRepository;
import br.com.gerenciadorfinanceiro.repository.RegistroIdempotenciaRepository;
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
import br.com.gerenciadorfinanceiro.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdempotencyEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private RegistroIdempotenciaRepository idempotenciaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario usuario;
    private String jwtToken;
    private Conta conta;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        transacaoRepository.deleteAll();
        idempotenciaRepository.deleteAll();
        contaRepository.deleteAll();
        categoriaRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuario = new Usuario("Lucas Silva", "lucas.idempotencia@teste.com", passwordEncoder.encode("123456"));
        usuario = usuarioRepository.save(usuario);

        jwtToken = tokenService.generateToken(usuario);

        conta = new Conta(usuario, "Conta Corrente", "Nubank", TipoConta.CORRENTE, new BigDecimal("1000.00"));
        conta = contaRepository.save(conta);

        categoria = new Categoria(usuario, "Alimentacao", TipoTransacao.DESPESA, "Mercado e lanches", "fa-utensils");
        categoria = categoriaRepository.save(categoria);
    }

    @Test
    @DisplayName("Idempotência Sequencial: POST /transacoes com mesma chave devolve a mesma resposta e debita saldo APENAS uma vez")
    void deveReplayRespostaSemDuplicarDebito() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        String payload = String.format("""
                {
                    "descricao": "Supermercado Mensal",
                    "valor": 200.00,
                    "tipo": "DESPESA",
                    "status": "PAGA",
                    "dataTransacao": "%s",
                    "contaId": %d,
                    "categoriaId": %d,
                    "observacao": "Compras do mes",
                    "tags": []
                }
                """, LocalDate.now(), conta.getId(), categoria.getId());

        // 1ª Chamada: cria transação e debita conta
        var primeiraChamada = mockMvc.perform(post("/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtToken)
                        .header(IdempotencyFilter.IDEMPOTENCY_HEADER, idempotencyKey)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(IdempotencyFilter.REPLAYED_HEADER))
                .andExpect(jsonPath("$.descricao").value("Supermercado Mensal"))
                .andReturn();

        String respostaOriginal = primeiraChamada.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        // Saldo deve ser 1000.00 - 200.00 = 800.00
        Conta contaApos1 = contaRepository.findById(conta.getId()).orElseThrow();
        assertEquals(new BigDecimal("800.00"), contaApos1.getSaldo());
        assertEquals(1, transacaoRepository.count(), "Deve existir exatamente UMA transação salva no banco.");

        // 2ª Chamada: repete o mesmo POST com a MESMA Idempotency-Key
        mockMvc.perform(post("/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtToken)
                        .header(IdempotencyFilter.IDEMPOTENCY_HEADER, idempotencyKey)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(header().string(IdempotencyFilter.REPLAYED_HEADER, "true"))
                .andExpect(content().json(respostaOriginal));

        // Verificação Crítica: saldo NÃO pode ter sido debitado novamente! Permanece 800.00!
        Conta contaApos2 = contaRepository.findById(conta.getId()).orElseThrow();
        assertEquals(new BigDecimal("800.00"), contaApos2.getSaldo(), "O saldo da conta não pode ser debitado duas vezes!");
        assertEquals(1, transacaoRepository.count(), "Nenhuma transação duplicada deve ser criada no banco.");
    }

    @Test
    @DisplayName("Idempotência: Rejeita com HTTP 422 se o cliente reenviar a mesma chave com corpo diferente (Payload Mismatch)")
    void deveRejeitarMesmaChaveComCorpoDiferente() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        String payload1 = String.format("""
                {
                    "descricao": "Compra Original",
                    "valor": 50.00,
                    "tipo": "DESPESA",
                    "status": "PAGA",
                    "dataTransacao": "%s",
                    "contaId": %d,
                    "categoriaId": %d,
                    "observacao": "Obs 1",
                    "tags": []
                }
                """, LocalDate.now(), conta.getId(), categoria.getId());

        String payload2 = String.format("""
                {
                    "descricao": "Compra Alterada com mesmo UUID",
                    "valor": 999.00,
                    "tipo": "DESPESA",
                    "status": "PAGA",
                    "dataTransacao": "%s",
                    "contaId": %d,
                    "categoriaId": %d,
                    "observacao": "Obs 2",
                    "tags": []
                }
                """, LocalDate.now(), conta.getId(), categoria.getId());

        // 1ª Chamada: Sucesso
        mockMvc.perform(post("/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtToken)
                        .header(IdempotencyFilter.IDEMPOTENCY_HEADER, idempotencyKey)
                        .content(payload1))
                .andExpect(status().isOk());

        // 2ª Chamada com corpo divergente: Rejeição com 422
        mockMvc.perform(post("/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtToken)
                        .header(IdempotencyFilter.IDEMPOTENCY_HEADER, idempotencyKey)
                        .content(payload2))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Idempotency-Key já utilizada para uma requisição com corpo/parâmetros diferentes."));
    }

    @Test
    @DisplayName("Idempotência: Rejeita com HTTP 400 se a chave contiver dado pessoal (e-mail ou CPF)")
    void deveRejeitarChaveComDadosPessoais() throws Exception {
        String payload = String.format("""
                {
                    "descricao": "Teste PII",
                    "valor": 10.00,
                    "tipo": "DESPESA",
                    "status": "PENDENTE",
                    "dataTransacao": "%s",
                    "contaId": %d,
                    "categoriaId": %d,
                    "observacao": "",
                    "tags": []
                }
                """, LocalDate.now(), conta.getId(), categoria.getId());

        mockMvc.perform(post("/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtToken)
                        .header(IdempotencyFilter.IDEMPOTENCY_HEADER, "lucas.silva@email.com")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtToken)
                        .header(IdempotencyFilter.IDEMPOTENCY_HEADER, "12345678901")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Idempotência Concorrente: 2 requisições simultâneas com a MESMA Idempotency-Key criam APENAS 1 transação física")
    void deveGarantirConsistenciaEmRequisicoesConcorrentesSimultaneas() throws InterruptedException {
        String idempotencyKey = UUID.randomUUID().toString();
        String payload = String.format("""
                {
                    "descricao": "Compra Concorrente",
                    "valor": 100.00,
                    "tipo": "DESPESA",
                    "status": "PAGA",
                    "dataTransacao": "%s",
                    "contaId": %d,
                    "categoriaId": %d,
                    "observacao": "Concorrência",
                    "tags": []
                }
                """, LocalDate.now(), conta.getId(), categoria.getId());

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latchInicio = new CountDownLatch(1);
        CountDownLatch latchFim = new CountDownLatch(threads);

        AtomicInteger sucessos = new AtomicInteger(0);
        AtomicInteger conflitosOuReplays = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latchInicio.await();
                    var result = mockMvc.perform(post("/transacoes")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Authorization", "Bearer " + jwtToken)
                                    .header(IdempotencyFilter.IDEMPOTENCY_HEADER, idempotencyKey)
                                    .content(payload))
                            .andReturn();

                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        sucessos.incrementAndGet();
                    } else if (status == 409) {
                        conflitosOuReplays.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Ignora
                } finally {
                    latchFim.countDown();
                }
            });
        }

        latchInicio.countDown();
        latchFim.await();
        executor.shutdown();

        // Verificação no banco: APENAS 1 transação deve existir fisicamente!
        long totalTransacoes = transacaoRepository.count();
        assertEquals(1, totalTransacoes, "Deve existir exatamente UMA transação persistida no banco de dados.");

        // Saldo deve ter sido debitado exatamente UMA vez (1000 - 100 = 900)
        Conta contaFinal = contaRepository.findById(conta.getId()).orElseThrow();
        assertEquals(new BigDecimal("900.00"), contaFinal.getSaldo(), "O débito deve ocorrer estritamente uma única vez.");
    }
}
