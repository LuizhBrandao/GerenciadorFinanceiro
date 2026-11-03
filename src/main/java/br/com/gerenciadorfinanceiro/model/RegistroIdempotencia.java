package br.com.gerenciadorfinanceiro.model;

import br.com.gerenciadorfinanceiro.model.enums.StatusIdempotencia;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "registros_idempotencia", uniqueConstraints = {
        @UniqueConstraint(name = "uk_registros_idempotencia_chave", columnNames = {"chave"})
}, indexes = {
        @Index(name = "idx_registros_idempotencia_usuario", columnList = "usuario_id"),
        @Index(name = "idx_registros_idempotencia_expiracao", columnList = "data_expiracao")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegistroIdempotencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String chave;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "metodo_http", nullable = false, length = 10)
    private String metodoHttp;

    @Column(name = "uri_endpoint", nullable = false, length = 255)
    private String uriEndpoint;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusIdempotencia status;

    @Column(name = "response_status_code")
    private Integer responseStatusCode;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_expiracao", nullable = false)
    private LocalDateTime dataExpiracao;

    public RegistroIdempotencia(String chave, Long usuarioId, String metodoHttp, String uriEndpoint, String requestHash, Integer ttlHoras) {
        if (chave == null || chave.trim().isEmpty()) {
            throw new IllegalArgumentException("A chave de idempotência não pode ser nula ou vazia.");
        }
        if (usuarioId == null) {
            throw new IllegalArgumentException("O ID do usuário autenticado deve ser fornecido.");
        }
        if (metodoHttp == null || metodoHttp.trim().isEmpty()) {
            throw new IllegalArgumentException("O método HTTP deve ser informado.");
        }
        if (uriEndpoint == null || uriEndpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("A URI do endpoint deve ser informada.");
        }
        if (requestHash == null || requestHash.trim().isEmpty()) {
            throw new IllegalArgumentException("O hash da requisição deve ser informado.");
        }

        this.chave = chave.trim();
        this.usuarioId = usuarioId;
        this.metodoHttp = metodoHttp.trim().toUpperCase();
        this.uriEndpoint = uriEndpoint.trim();
        this.requestHash = requestHash.trim();
        this.status = StatusIdempotencia.PROCESSANDO;
        this.dataCriacao = LocalDateTime.now();
        int horas = (ttlHoras != null && ttlHoras > 0) ? ttlHoras : 24;
        this.dataExpiracao = this.dataCriacao.plusHours(horas);
    }

    public void concluir(int statusCode, String responseBody) {
        this.status = StatusIdempotencia.CONCLUIDO;
        this.responseStatusCode = statusCode;
        this.responseBody = responseBody;
    }

    public void falhar(int statusCode, String responseBody) {
        this.status = StatusIdempotencia.FALHA;
        this.responseStatusCode = statusCode;
        this.responseBody = responseBody;
    }

    public boolean verificarCorrespondenciaPayload(String hashAtual) {
        if (hashAtual == null) return false;
        return Objects.equals(this.requestHash, hashAtual.trim());
    }

    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(this.dataExpiracao);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegistroIdempotencia that)) return false;
        return Objects.equals(chave, that.chave);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chave);
    }
}
