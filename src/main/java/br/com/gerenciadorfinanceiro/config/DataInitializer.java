package br.com.gerenciadorfinanceiro.config;

import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.PreferenciasUsuario;
import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.model.enums.StatusTransacao;
import br.com.gerenciadorfinanceiro.model.enums.TipoConta;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import br.com.gerenciadorfinanceiro.repository.CategoriaRepository;
import br.com.gerenciadorfinanceiro.repository.ContaRepository;
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
import br.com.gerenciadorfinanceiro.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String testEmail = "teste@financeiro.com";

        if (!usuarioRepository.existsByEmail(testEmail)) {
            // 1. Criar Usuário Demo
            Usuario demoUser = new Usuario("Usuário de Teste", testEmail, passwordEncoder.encode("123456"));
            PreferenciasUsuario pref = new PreferenciasUsuario(demoUser);
            demoUser.setPreferencias(pref);
            demoUser = usuarioRepository.save(demoUser);

            // 2. Criar as Categorias Essenciais
            List<Categoria> categoriasPadrao = criarCategoriasPadraoParaUsuario(demoUser);
            categoriaRepository.saveAll(categoriasPadrao);

            // 3. Criar Contas Bancárias Demo
            Conta nubank = new Conta(demoUser, "Nubank - Corrente", "Nubank", TipoConta.CORRENTE, new BigDecimal("4250.00"));
            Conta itau = new Conta(demoUser, "Itaú - Reserva", "Itaú", TipoConta.POUPANCA, new BigDecimal("12000.00"));
            contaRepository.saveAll(List.of(nubank, itau));

            // 4. Buscar categorias criadas para vincular transações demo
            Categoria catSalario = categoriasPadrao.stream().filter(c -> c.getNome().contains("Salário")).findFirst().orElse(null);
            Categoria catRendimentos = categoriasPadrao.stream().filter(c -> c.getNome().contains("Rendimentos")).findFirst().orElse(null);
            Categoria catAlimentacao = categoriasPadrao.stream().filter(c -> c.getNome().contains("Alimentação")).findFirst().orElse(null);
            Categoria catMoradia = categoriasPadrao.stream().filter(c -> c.getNome().contains("Moradia")).findFirst().orElse(null);
            Categoria catTransporte = categoriasPadrao.stream().filter(c -> c.getNome().contains("Transporte")).findFirst().orElse(null);
            Categoria catSaude = categoriasPadrao.stream().filter(c -> c.getNome().contains("Saúde")).findFirst().orElse(null);
            Categoria catLazer = categoriasPadrao.stream().filter(c -> c.getNome().contains("Lazer")).findFirst().orElse(null);

            // 5. Inserir Transações de Exemplo
            LocalDate hoje = LocalDate.now();
            List<Transacao> transacoesExemplo = List.of(
                    new Transacao(demoUser, "Salário Mensal", new BigDecimal("6500.00"), TipoTransacao.RECEITA, StatusTransacao.PAGA, hoje.withDayOfMonth(5), nubank, catSalario, "Salário creditado"),
                    new Transacao(demoUser, "Dividendos FIIs", new BigDecimal("320.50"), TipoTransacao.RECEITA, StatusTransacao.PAGA, hoje.withDayOfMonth(15), itau, catRendimentos, "Rendimentos mensais"),
                    new Transacao(demoUser, "Aluguel & Condomínio", new BigDecimal("1800.00"), TipoTransacao.DESPESA, StatusTransacao.PAGA, hoje.withDayOfMonth(10), nubank, catMoradia, "Contas da residência"),
                    new Transacao(demoUser, "Supermercado Mensal", new BigDecimal("850.30"), TipoTransacao.DESPESA, StatusTransacao.PAGA, hoje.withDayOfMonth(8), nubank, catAlimentacao, "Compras do mês"),
                    new Transacao(demoUser, "Combustível Posto Shell", new BigDecimal("220.00"), TipoTransacao.DESPESA, StatusTransacao.PAGA, hoje.withDayOfMonth(12), nubank, catTransporte, "Abastecimento semanal"),
                    new Transacao(demoUser, "Farmácia Drogasil", new BigDecimal("135.80"), TipoTransacao.DESPESA, StatusTransacao.PAGA, hoje.withDayOfMonth(14), nubank, catSaude, "Vitaminas e remédios"),
                    new Transacao(demoUser, "Jantar Restaurante", new BigDecimal("190.00"), TipoTransacao.DESPESA, StatusTransacao.PAGA, hoje.withDayOfMonth(18), nubank, catLazer, "Lazer final de semana")
            );
            transacaoRepository.saveAll(transacoesExemplo);
        }
    }

    public static List<Categoria> criarCategoriasPadraoParaUsuario(Usuario usuario) {
        return List.of(
                new Categoria(usuario, "Salário e Remuneração", TipoTransacao.RECEITA,
                        "Salário fixo mensal, adiantamentos, 13º salário e benefícios em folha.", "fa-briefcase"),
                new Categoria(usuario, "Rendimentos & Investimentos", TipoTransacao.RECEITA,
                        "Dividendos, juros sobre capital próprio (JCP), rendimentos de CDI/Poupança e fundos imobiliários.", "fa-chart-line"),
                new Categoria(usuario, "Freelance & Serviços Extras", TipoTransacao.RECEITA,
                        "Trabalhos autônomos, consultorias, projetos paralelos e vendas pontuais.", "fa-laptop"),
                new Categoria(usuario, "Moradia & Habitação", TipoTransacao.DESPESA,
                        "Aluguel, condomínio, IPTU, contas essenciais (energia elétrica, água, gás, internet).", "fa-house"),
                new Categoria(usuario, "Alimentação & Supermercado", TipoTransacao.DESPESA,
                        "Compras de supermercado, feira, açougue, padaria e delivery/refeições do dia a dia.", "fa-utensils"),
                new Categoria(usuario, "Transporte & Mobilidade", TipoTransacao.DESPESA,
                        "Combustível, transporte público, corridas por aplicativo (Uber/99), estacionamento, IPVA e manutenção veicular.", "fa-car"),
                new Categoria(usuario, "Saúde & Bem-Estar", TipoTransacao.DESPESA,
                        "Plano de saúde, consultas, farmácia/medicamentos, exames e academia/atividades físicas.", "fa-heart-pulse"),
                new Categoria(usuario, "Educação & Desenvolvimento", TipoTransacao.DESPESA,
                        "Mensalidades escolares/faculdade, cursos online, livros, certificações e workshops.", "fa-graduation-cap"),
                new Categoria(usuario, "Lazer & Entretenimento", TipoTransacao.DESPESA,
                        "Assinaturas de streaming (Netflix, Spotify), restaurantes/bares, viagens, cinema e passeios.", "fa-ticket"),
                new Categoria(usuario, "Cuidados Pessoais & Compras", TipoTransacao.DESPESA,
                        "Roupas, calçados, barbearia/salão de beleza, cosméticos e itens de uso pessoal.", "fa-bag-shopping"),
                new Categoria(usuario, "Contas Básicas & Energia", TipoTransacao.DESPESA,
                        "Contas de energia, água, luz, gás e taxas.", "fa-bolt"),
                new Categoria(usuario, "Outros", TipoTransacao.DESPESA,
                        "Outros gastos e despesas gerais.", "fa-tag"),
                new Categoria(usuario, "Outras Receitas", TipoTransacao.RECEITA,
                        "Outras entradas e ganhos gerais.", "fa-tag")
        );
    }
}
