-- ====================================================================================
-- MIGRATION V2: Adicionar constraints de integridade e unicidade contra Race Conditions
-- ATENÇÃO: Revise este arquivo e execute as consultas de diagnóstico antes de aplicar.
-- ====================================================================================

-- ------------------------------------------------------------------------------------
-- CONSULTAS DIAGNÓSTICAS (Execute estas consultas ANTES de aplicar as constraints para
-- verificar se há registros duplicados ou inconsistentes na base de dados):
-- ------------------------------------------------------------------------------------
-- 1. Contas com saldo negativo:
--    SELECT id, usuario_id, nome, saldo FROM contas WHERE saldo < 0;
--
-- 2. Orçamentos duplicados:
--    SELECT categoria_id, ano, mes, COUNT(*) FROM orcamentos GROUP BY categoria_id, ano, mes HAVING COUNT(*) > 1;
--
-- 3. Categorias duplicadas:
--    SELECT usuario_id, nome, tipo, COUNT(*) FROM categorias GROUP BY usuario_id, nome, tipo HAVING COUNT(*) > 1;
--
-- 4. Tags duplicadas:
--    SELECT usuario_id, nome, COUNT(*) FROM tags GROUP BY usuario_id, nome HAVING COUNT(*) > 1;
--
-- 5. Chaves de recorrência duplicadas:
--    SELECT chave_recorrencia, COUNT(*) FROM transacoes WHERE chave_recorrencia IS NOT NULL GROUP BY chave_recorrencia HAVING COUNT(*) > 1;
-- ------------------------------------------------------------------------------------

-- 1. Constraint CHECK para garantir que o saldo de uma conta nunca fique negativo no banco
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_contas_saldo_nao_negativo'
    ) THEN
        ALTER TABLE contas ADD CONSTRAINT chk_contas_saldo_nao_negativo CHECK (saldo >= 0);
    END IF;
END $$;

-- 2. Constraint UNIQUE para impedir múltiplos orçamentos para a mesma categoria na mesma competência (ano/mês)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_orcamentos_categoria_ano_mes'
    ) THEN
        ALTER TABLE orcamentos ADD CONSTRAINT uk_orcamentos_categoria_ano_mes UNIQUE (categoria_id, ano, mes);
    END IF;
END $$;

-- 3. Constraint UNIQUE para impedir categorias duplicadas com mesmo nome e tipo para o mesmo usuário
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_categorias_usuario_nome_tipo'
    ) THEN
        ALTER TABLE categorias ADD CONSTRAINT uk_categorias_usuario_nome_tipo UNIQUE (usuario_id, nome, tipo);
    END IF;
END $$;

-- 4. Constraint UNIQUE para impedir tags duplicadas com mesmo nome para o mesmo usuário
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_tags_usuario_nome'
    ) THEN
        ALTER TABLE tags ADD CONSTRAINT uk_tags_usuario_nome UNIQUE (usuario_id, nome);
    END IF;
END $$;

-- 5. Coluna e Constraint UNIQUE para chave idempotente de transações geradas por recorrência
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns WHERE table_name = 'transacoes' AND column_name = 'chave_recorrencia'
    ) THEN
        ALTER TABLE transacoes ADD COLUMN chave_recorrencia VARCHAR(100);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_transacoes_chave_recorrencia'
    ) THEN
        ALTER TABLE transacoes ADD CONSTRAINT uk_transacoes_chave_recorrencia UNIQUE (chave_recorrencia);
    END IF;
END $$;
