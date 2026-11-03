package br.com.gerenciadorfinanceiro.config.idempotency;

import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.service.IdempotencyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(10)
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotent-Replayed";

    private final IdempotencyService idempotencyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER.toLowerCase());
        }

        // Se o header não foi enviado ou a requisição for de leitura, segue fluxo padrão sem idempotência
        if (idempotencyKey == null || idempotencyKey.isBlank() || isMetodoLeitura(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        Long usuarioId = extrairUsuarioId();
        String requestHash = idempotencyService.calcularHash(cachedRequest.getBodyAsString());

        IdempotencyCheckResult checkResult;
        try {
            checkResult = idempotencyService.verificarOuIniciar(
                    idempotencyKey,
                    usuarioId,
                    cachedRequest.getMethod(),
                    cachedRequest.getRequestURI(),
                    requestHash
            );
        } catch (IllegalArgumentException ex) {
            enviarRespostaErro(response, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", ex.getMessage(), request.getRequestURI());
            return;
        }

        if (checkResult.payloadDivergente()) {
            enviarRespostaErro(response, 422, "Unprocessable Entity",
                    "Idempotency-Key já utilizada para uma requisição com corpo/parâmetros diferentes.", request.getRequestURI());
            return;
        }

        if (checkResult.emProcessamento()) {
            enviarRespostaErro(response, HttpServletResponse.SC_CONFLICT, "Conflict",
                    "Operação em processamento para esta Idempotency-Key. Aguarde a finalização.", request.getRequestURI());
            return;
        }

        if (checkResult.responseStatusCode() != null) {
            // Replay da resposta em cache gravada originalmente!
            response.setStatus(checkResult.responseStatusCode());
            response.setHeader(REPLAYED_HEADER, "true");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            if (checkResult.responseBody() != null) {
                byte[] bytes = checkResult.responseBody().getBytes(StandardCharsets.UTF_8);
                response.getOutputStream().write(bytes);
                response.getOutputStream().flush();
            }
            return;
        }

        // Nova requisição: executa os controllers normalmente
        try {
            filterChain.doFilter(cachedRequest, responseWrapper);

            int status = responseWrapper.getStatus();
            byte[] responseBytes = responseWrapper.getContentAsByteArray();
            String responseBody = new String(responseBytes, StandardCharsets.UTF_8);

            if (status < 500) {
                idempotencyService.registrarConclusao(idempotencyKey, status, responseBody);
            } else {
                idempotencyService.registrarFalha(idempotencyKey, status, responseBody);
            }

            responseWrapper.copyBodyToResponse();
        } catch (Exception ex) {
            idempotencyService.registrarFalha(idempotencyKey, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
            throw ex;
        }
    }

    private boolean isMetodoLeitura(String method) {
        return "GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
    }

    private Long extrairUsuarioId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario usuario) {
            return usuario.getId();
        }
        return 0L;
    }

    private void enviarRespostaErro(HttpServletResponse response, int status, String error, String message, String path) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String json = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                java.time.Instant.now(), status, error, message, path
        );
        response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
    }
}
