-- ==============================================================================
-- CARGA INICIAL (SEED) - 10 CATEGORIAS ESSENCIAIS E DADOS DE TESTE
-- ==============================================================================

-- 1. Usuário de Demonstração (Senha padrão: 123456 criptografada com BCrypt)
INSERT INTO usuarios (id, nome, email, senha, data_criacao)
VALUES (1, 'Usuário de Teste', 'teste@financeiro.com', '$2a$10$wN1yD0eY7UqLlhfFzYxUDeW7xG8L6GqA5wA2.C9B7iO5E0U8f7n8.', CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- 2. Preferências do Usuário
INSERT INTO preferencias_usuario (id, usuario_id, moeda_padrao, notificacoes_email, tema_escuro, limite_alerta_orcamento)
VALUES (1, 1, 'BRL', true, false, 80.00)
ON CONFLICT (id) DO NOTHING;

-- 3. As 10 Categorias Essenciais
-- 3.1 Receitas
INSERT INTO categorias (id, usuario_id, nome, tipo, descricao, icone)
VALUES 
(1, 1, 'Salário e Remuneração', 'RECEITA', 'Salário fixo mensal, adiantamentos, 13º salário e benefícios em folha.', 'fa-briefcase'),
(2, 1, 'Rendimentos & Investimentos', 'RECEITA', 'Dividendos, juros sobre capital próprio (JCP), rendimentos de CDI/Poupança e fundos imobiliários.', 'fa-chart-line'),
(3, 1, 'Freelance & Serviços Extras', 'RECEITA', 'Trabalhos autônomos, consultorias, projetos paralelos e vendas pontuais.', 'fa-laptop'),
(13, 1, 'Outras Receitas', 'RECEITA', 'Outras entradas e ganhos gerais.', 'fa-tag')
ON CONFLICT (id) DO NOTHING;

-- 3.2 Despesas
INSERT INTO categorias (id, usuario_id, nome, tipo, descricao, icone)
VALUES 
(4, 1, 'Moradia & Habitação', 'DESPESA', 'Aluguel, condomínio, IPTU, contas essenciais (energia elétrica, água, gás, internet).', 'fa-house'),
(5, 1, 'Alimentação & Supermercado', 'DESPESA', 'Compras de supermercado, feira, açougue, padaria e delivery/refeições do dia a dia.', 'fa-utensils'),
(6, 1, 'Transporte & Mobilidade', 'DESPESA', 'Combustível, transporte público, corridas por aplicativo (Uber/99), estacionamento, IPVA e manutenção veicular.', 'fa-car'),
(7, 1, 'Saúde & Bem-Estar', 'DESPESA', 'Plano de saúde, consultas, farmácia/medicamentos, exames e academia/atividades físicas.', 'fa-heart-pulse'),
(8, 1, 'Educação & Desenvolvimento', 'DESPESA', 'Mensalidades escolares/faculdade, cursos online, livros, certificações e workshops.', 'fa-graduation-cap'),
(9, 1, 'Lazer & Entretenimento', 'DESPESA', 'Assinaturas de streaming (Netflix, Spotify), restaurantes/bares, viagens, cinema e passeios.', 'fa-ticket'),
(10, 1, 'Cuidados Pessoais & Compras', 'DESPESA', 'Roupas, calçados, barbearia/salão de beleza, cosméticos e itens de uso pessoal.', 'fa-bag-shopping'),
(11, 1, 'Contas Básicas & Energia', 'DESPESA', 'Contas de energia, água, luz, gás e taxas.', 'fa-bolt'),
(12, 1, 'Outros', 'DESPESA', 'Outros gastos e despesas gerais.', 'fa-tag')
ON CONFLICT (id) DO NOTHING;

-- 4. Contas Bancárias de Exemplo
INSERT INTO contas (id, usuario_id, nome, instituicao_financeira, tipo_conta, saldo, ativo, data_criacao)
VALUES 
(1, 1, 'Nubank - Conta Corrente', 'Nubank', 'CORRENTE', 4250.00, true, CURRENT_TIMESTAMP),
(2, 1, 'Itaú - Reserva Poupança', 'Itaú', 'POUPANCA', 12000.00, true, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 5. Transações de Exemplo para Testes Imediatos
INSERT INTO transacoes (id, usuario_id, conta_id, categoria_id, descricao, valor, tipo, status, data_transacao, observacao, data_registro)
VALUES 
(1, 1, 1, 1, 'Salário Mensal', 6500.00, 'RECEITA', 'PAGA', CURRENT_DATE - INTERVAL '15 days', 'Crédito de salário em conta', CURRENT_TIMESTAMP),
(2, 1, 2, 2, 'Rendimentos FIIs / Dividendos', 320.50, 'RECEITA', 'PAGA', CURRENT_DATE - INTERVAL '10 days', 'Proventos recebidos', CURRENT_TIMESTAMP),
(3, 1, 1, 4, 'Aluguel & Condomínio', 1800.00, 'DESPESA', 'PAGA', CURRENT_DATE - INTERVAL '12 days', 'Despesa fixa moradia', CURRENT_TIMESTAMP),
(4, 1, 1, 5, 'Supermercado Mensal', 850.30, 'DESPESA', 'PAGA', CURRENT_DATE - INTERVAL '8 days', 'Compras do mês', CURRENT_TIMESTAMP),
(5, 1, 1, 6, 'Combustível Posto Shell', 220.00, 'DESPESA', 'PAGA', CURRENT_DATE - INTERVAL '5 days', 'Abastecimento', CURRENT_TIMESTAMP),
(6, 1, 1, 7, 'Farmácia Drogasil', 135.80, 'DESPESA', 'PAGA', CURRENT_DATE - INTERVAL '3 days', 'Medicamentos', CURRENT_TIMESTAMP),
(7, 1, 1, 9, 'Jantar Restaurante', 190.00, 'DESPESA', 'PAGA', CURRENT_DATE - INTERVAL '1 day', 'Lazer fim de semana', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
