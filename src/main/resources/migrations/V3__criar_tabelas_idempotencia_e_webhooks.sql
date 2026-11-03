-- ====================================================================================
-- MIGRATION V3: Criação de tabelas e índices para Idempotência e Webhooks de Pagamento
-- ====================================================================================

-- 1. Tabela de eventos recebidos de Webhooks (ex: Stripe, gateways)
CREATE TABLE IF NOT EXISTS eventos_webhook (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(120) NOT NULL,
    gateway VARCHAR(50) NOT NULL,
    tipo_evento VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    payload_resumo VARCHAR(500),
    mensagem_erro VARCHAR(1000),
    data_recebimento TIMESTAMP NOT NULL,
    data_processamento TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_eventos_webhook_event_id'
    ) THEN
        ALTER TABLE eventos_webhook ADD CONSTRAINT uk_eventos_webhook_event_id UNIQUE (event_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_eventos_webhook_status ON eventos_webhook (status);
CREATE INDEX IF NOT EXISTS idx_eventos_webhook_gateway ON eventos_webhook (gateway);

-- 2. Tabela de registros de idempotência para endpoints do cliente (Idempotency-Key)
CREATE TABLE IF NOT EXISTS registros_idempotencia (
    id BIGSERIAL PRIMARY KEY,
    chave VARCHAR(100) NOT NULL,
    usuario_id BIGINT NOT NULL,
    metodo_http VARCHAR(10) NOT NULL,
    uri_endpoint VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    response_status_code INT,
    response_body TEXT,
    data_criacao TIMESTAMP NOT NULL,
    data_expiracao TIMESTAMP NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_registros_idempotencia_chave'
    ) THEN
        ALTER TABLE registros_idempotencia ADD CONSTRAINT uk_registros_idempotencia_chave UNIQUE (chave);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_registros_idempotencia_usuario ON registros_idempotencia (usuario_id);
CREATE INDEX IF NOT EXISTS idx_registros_idempotencia_expiracao ON registros_idempotencia (data_expiracao);
