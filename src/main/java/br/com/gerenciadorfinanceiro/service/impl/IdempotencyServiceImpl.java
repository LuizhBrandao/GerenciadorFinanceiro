package br.com.gerenciadorfinanceiro.service.impl;

import br.com.gerenciadorfinanceiro.config.idempotency.IdempotencyCheckResult;
import br.com.gerenciadorfinanceiro.model.RegistroIdempotencia;
import br.com.gerenciadorfinanceiro.model.enums.StatusIdempotencia;
import br.com.gerenciadorfinanceiro.repository.RegistroIdempotenciaRepository;
import br.com.gerenciadorfinanceiro.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyServiceImpl.class);

    private static final Pattern UUID_V4_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
    );
    private static final Pattern CPF_PATTERN = Pattern.compile("\\d{11}");

    private final RegistroIdempotenciaRepository idempotenciaRepository;
    private final PlatformTransactionManager transactionManager;

    @Override
    public String calcularHash(String payload) {
        if (payload == null) {
            payload = "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 não disponível: " + e.getMessage(), e);
        }
    }

    @Override
    public void validarChave(String chave) {
        if (chave == null || chave.trim().isEmpty()) {
            throw new IllegalArgumentException("A chave de idempotência (Idempotency-Key) não pode ser vazia.");
        }
        String chaveLimpa = chave.trim();

        if (chaveLimpa.contains("@") || CPF_PATTERN.matcher(chaveLimpa).find()) {
            throw new IllegalArgumentException("Violação de segurança: Não é permitido utilizar dados pessoais (e-mail, CPF) como chave de idempotência.");
        }

        if (!UUID_V4_PATTERN.matcher(chaveLimpa).matches()) {
            throw new IllegalArgumentException("A chave de idempotência deve ser um UUID v4 válido (ex: 550e8400-e29b-41d4-a716-446655440000).");
        }
    }

    @Override
    public IdempotencyCheckResult verificarOuIniciar(String chave, Long usuarioId, String metodoHttp, String uriEndpoint, String requestHash) {
        validarChave(chave);

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        try {
            return tx.execute(status -> {
                Optional<RegistroIdempotencia> existenteOpt = idempotenciaRepository.findByChave(chave.trim());
                if (existenteOpt.isPresent()) {
                    RegistroIdempotencia reg = existenteOpt.get();
                    if (reg.getStatus() == StatusIdempotencia.PROCESSANDO) {
                        log.warn("[Idempotência] Chave '{}' já se encontra em processamento simultâneo.", chave);
                        return IdempotencyCheckResult.criarEmProcessamento();
                    }
                    if (reg.getStatus() == StatusIdempotencia.CONCLUIDO) {
                        if (!reg.verificarCorrespondenciaPayload(requestHash)) {
                            log.warn("[Idempotência] Chave '{}' reenviada com corpo diferente do original.", chave);
                            return IdempotencyCheckResult.criarPayloadDivergente();
                        }
                        log.info("[Idempotência] Chave '{}' reutilizada com sucesso. Retornando resposta em cache.", chave);
                        return IdempotencyCheckResult.criarConcluido(reg.getResponseStatusCode(), reg.getResponseBody());
                    }
                }

                RegistroIdempotencia novo = new RegistroIdempotencia(
                        chave.trim(),
                        usuarioId != null ? usuarioId : 0L,
                        metodoHttp,
                        uriEndpoint,
                        requestHash,
                        24
                );
                idempotenciaRepository.saveAndFlush(novo);
                log.info("[Idempotência] Nova chave '{}' registrada com status PROCESSANDO.", chave);
                return IdempotencyCheckResult.criarNovo();
            });
        } catch (DataIntegrityViolationException ex) {
            log.warn("[Idempotência] Colisão concorrente na inserção da chave '{}'. Retornando em processamento.", chave);
            return IdempotencyCheckResult.criarEmProcessamento();
        }
    }

    @Override
    public void registrarConclusao(String chave, int statusCode, String responseBody) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        tx.executeWithoutResult(status -> {
            idempotenciaRepository.findByChave(chave.trim()).ifPresent(reg -> {
                reg.concluir(statusCode, responseBody);
                idempotenciaRepository.saveAndFlush(reg);
                log.info("[Idempotência] Chave '{}' marcada como CONCLUIDO com HTTP status {}.", chave, statusCode);
            });
        });
    }

    @Override
    public void registrarFalha(String chave, int statusCode, String responseBody) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        tx.executeWithoutResult(status -> {
            idempotenciaRepository.findByChave(chave.trim()).ifPresent(reg -> {
                reg.falhar(statusCode, responseBody);
                idempotenciaRepository.saveAndFlush(reg);
                log.warn("[Idempotência] Chave '{}' marcada como FALHA com HTTP status {}.", chave, statusCode);
            });
        });
    }
}
