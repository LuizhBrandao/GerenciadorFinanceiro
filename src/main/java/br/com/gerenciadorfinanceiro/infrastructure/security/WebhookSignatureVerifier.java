package br.com.gerenciadorfinanceiro.infrastructure.security;

import br.com.gerenciadorfinanceiro.exception.AssinaturaInvalidaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class WebhookSignatureVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";

    @Value("${webhook.stripe.secret:whsec_test_secret_key_default}")
    private String defaultSecret;

    @Value("${webhook.stripe.tolerance-seconds:300}")
    private long defaultToleranceSeconds;

    public void verificarAssinatura(byte[] rawBody, String signatureHeader) {
        verificarAssinatura(rawBody != null ? new String(rawBody, StandardCharsets.UTF_8) : "", signatureHeader, defaultSecret, defaultToleranceSeconds);
    }

    public void verificarAssinatura(String rawBody, String signatureHeader) {
        verificarAssinatura(rawBody, signatureHeader, defaultSecret, defaultToleranceSeconds);
    }

    public void verificarAssinatura(String rawBody, String signatureHeader, String secret, long toleranceSeconds) {
        if (signatureHeader == null || signatureHeader.trim().isEmpty()) {
            throw new AssinaturaInvalidaException("Cabeçalho de assinatura do webhook ausente.");
        }
        if (secret == null || secret.trim().isEmpty()) {
            throw new AssinaturaInvalidaException("Chave secreta do webhook não configurada.");
        }
        if (rawBody == null) {
            rawBody = "";
        }

        long timestamp = -1;
        List<String> assinaturasV1 = new ArrayList<>();

        String[] partes = signatureHeader.split(",");
        for (String parte : partes) {
            String[] chaveValor = parte.trim().split("=", 2);
            if (chaveValor.length == 2) {
                String chave = chaveValor[0].trim();
                String valor = chaveValor[1].trim();
                if ("t".equals(chave)) {
                    try {
                        timestamp = Long.parseLong(valor);
                    } catch (NumberFormatException e) {
                        throw new AssinaturaInvalidaException("Timestamp inválido no cabeçalho de assinatura.");
                    }
                } else if ("v1".equals(chave)) {
                    assinaturasV1.add(valor);
                }
            }
        }

        if (timestamp <= 0) {
            throw new AssinaturaInvalidaException("Timestamp ausente ou inválido no cabeçalho de assinatura.");
        }
        if (assinaturasV1.isEmpty()) {
            throw new AssinaturaInvalidaException("Nenhuma assinatura v1 encontrada no cabeçalho.");
        }

        if (toleranceSeconds > 0) {
            long agora = Instant.now().getEpochSecond();
            if (Math.abs(agora - timestamp) > toleranceSeconds) {
                throw new AssinaturaInvalidaException("Assinatura do webhook expirada ou timestamp fora da tolerância.");
            }
        }

        String payloadAssinado = timestamp + "." + rawBody;
        String assinaturaEsperada = calcularHmacSha256(payloadAssinado, secret);

        boolean valido = false;
        byte[] expectedBytes = assinaturaEsperada.getBytes(StandardCharsets.UTF_8);

        for (String sig : assinaturasV1) {
            byte[] providedBytes = sig.getBytes(StandardCharsets.UTF_8);
            if (MessageDigest.isEqual(expectedBytes, providedBytes)) {
                valido = true;
                break;
            }
        }

        if (!valido) {
            throw new AssinaturaInvalidaException("Assinatura do webhook inválida. O payload foi adulterado ou o secret diverge.");
        }
    }

    public String gerarAssinaturaHeader(String rawBody, long timestamp, String secret) {
        String payloadAssinado = timestamp + "." + (rawBody != null ? rawBody : "");
        String hash = calcularHmacSha256(payloadAssinado, secret);
        return "t=" + timestamp + ",v1=" + hash;
    }

    public String calcularHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);
            byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesParaHex(hashBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Erro criptográfico ao calcular HMAC-SHA256: " + e.getMessage(), e);
        }
    }

    private static String bytesParaHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
