package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.controller.dto.DashboardDto;
import br.com.gerenciadorfinanceiro.controller.dto.ResumoSaldosDto;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.model.enums.StatusTransacao;
import br.com.gerenciadorfinanceiro.model.enums.TipoConta;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
import br.com.gerenciadorfinanceiro.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ContaService contaService;

    @Mock
    private TransacaoRepository transacaoRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Usuario usuario;
    private Conta conta;
    private Categoria catAlimentacao;
    private Categoria catSalario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Teste", "teste@email.com", "123");
        usuario.setId(1L);

        conta = new Conta(usuario, "Conta Corrente", "Banco", TipoConta.CORRENTE, BigDecimal.valueOf(2000));
        conta.setId(10L);

        catAlimentacao = new Categoria(usuario, "Alimentação", TipoTransacao.DESPESA, "Mercado e Refeições", "fa-utensils");
        catAlimentacao.setId(100L);

        catSalario = new Categoria(usuario, "Salário", TipoTransacao.RECEITA, "Renda Mensal", "fa-briefcase");
        catSalario.setId(200L);
    }

    @Test
    @DisplayName("Deve consolidar métricas do mês atual, despesas por categoria e evolução mensal via consultas agregadas CQRS")
    void deveConsolidarDashboardCorretamente() {
        LocalDate hoje = LocalDate.now();

        Transacao t1 = new Transacao(usuario, "Salário", BigDecimal.valueOf(5000), TipoTransacao.RECEITA, StatusTransacao.PAGA, hoje, conta, catSalario, "");
        t1.setId(1L);

        Transacao t2 = new Transacao(usuario, "Supermercado", BigDecimal.valueOf(800), TipoTransacao.DESPESA, StatusTransacao.PAGA, hoje, conta, catAlimentacao, "");
        t2.setId(2L);

        Transacao t3 = new Transacao(usuario, "Internet Pendente", BigDecimal.valueOf(150), TipoTransacao.DESPESA, StatusTransacao.PENDENTE, hoje, conta, catAlimentacao, "");
        t3.setId(3L);

        ResumoSaldosDto resumoSaldos = new ResumoSaldosDto(
                BigDecimal.valueOf(2000), BigDecimal.valueOf(500), BigDecimal.valueOf(2500), 1, 1
        );

        when(contaService.obterResumoSaldos(1L)).thenReturn(resumoSaldos);
        when(transacaoRepository.somarPorTipoEStatusECompetencia(eq(1L), eq(TipoTransacao.RECEITA), eq(StatusTransacao.PAGA), anyInt(), anyInt()))
                .thenReturn(BigDecimal.valueOf(5000));
        when(transacaoRepository.somarPorTipoEStatusECompetencia(eq(1L), eq(TipoTransacao.DESPESA), eq(StatusTransacao.PAGA), anyInt(), anyInt()))
                .thenReturn(BigDecimal.valueOf(800));
        when(transacaoRepository.somarPorTipoEStatusECompetencia(eq(1L), eq(TipoTransacao.RECEITA), eq(StatusTransacao.PENDENTE), anyInt(), anyInt()))
                .thenReturn(BigDecimal.ZERO);
        when(transacaoRepository.somarPorTipoEStatusECompetencia(eq(1L), eq(TipoTransacao.DESPESA), eq(StatusTransacao.PENDENTE), anyInt(), anyInt()))
                .thenReturn(BigDecimal.valueOf(150));

        when(transacaoRepository.findTop5ByUsuarioIdAndDataTransacaoLessThanEqualOrderByDataTransacaoDescIdDesc(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of(t1, t2, t3));

        List<Object[]> rowsCategorias = java.util.Collections.singletonList(
                new Object[]{100L, "Alimentação", "fa-utensils", BigDecimal.valueOf(800)}
        );
        when(transacaoRepository.agruparDespesasPagasPorCategoria(1L)).thenReturn(rowsCategorias);
        when(transacaoRepository.somarEvolucaoMensalPorPeriodo(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        DashboardDto dashboard = dashboardService.obterDashboard(1L);

        assertNotNull(dashboard);
        assertEquals(BigDecimal.valueOf(2000), dashboard.saldos().saldoContasCorrentes());
        assertEquals(BigDecimal.valueOf(5000), dashboard.metricasMes().receitasMes());
        assertEquals(BigDecimal.valueOf(800), dashboard.metricasMes().despesasMes());
        assertEquals(BigDecimal.valueOf(4200), dashboard.metricasMes().balancoMes());
        assertEquals(BigDecimal.valueOf(150), dashboard.metricasMes().despesasPendentes());

        // Transações recentes
        assertFalse(dashboard.transacoesRecentes().isEmpty());
        assertEquals(3, dashboard.transacoesRecentes().size());

        // Despesas por Categoria
        assertFalse(dashboard.despesasPorCategoria().isEmpty());
        assertEquals(1, dashboard.despesasPorCategoria().size());
        assertEquals("Alimentação", dashboard.despesasPorCategoria().get(0).nome());
        assertEquals(BigDecimal.valueOf(800), dashboard.despesasPorCategoria().get(0).total());

        // Evolução Mensal (6 meses)
        assertEquals(6, dashboard.evolucaoMensal().labels().size());
        assertEquals(6, dashboard.evolucaoMensal().receitas().size());
        assertEquals(6, dashboard.evolucaoMensal().despesas().size());
    }

    @Test
    @DisplayName("Deve computar métricas operacionais ignorando transferências patrimoniais")
    void deveIgnorarTransferenciasNoCalculoDeReceitasEDespesasDoMes() {
        ResumoSaldosDto resumoSaldos = new ResumoSaldosDto(
                BigDecimal.valueOf(1300), BigDecimal.valueOf(1500), BigDecimal.valueOf(2800), 1, 1
        );

        when(contaService.obterResumoSaldos(1L)).thenReturn(resumoSaldos);
        when(transacaoRepository.somarPorTipoEStatusECompetencia(eq(1L), eq(TipoTransacao.RECEITA), eq(StatusTransacao.PAGA), anyInt(), anyInt()))
                .thenReturn(BigDecimal.ZERO);
        when(transacaoRepository.somarPorTipoEStatusECompetencia(eq(1L), eq(TipoTransacao.DESPESA), eq(StatusTransacao.PAGA), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("200.00"));
        when(transacaoRepository.somarPorTipoEStatusECompetencia(eq(1L), eq(TipoTransacao.RECEITA), eq(StatusTransacao.PENDENTE), anyInt(), anyInt()))
                .thenReturn(BigDecimal.ZERO);
        when(transacaoRepository.somarPorTipoEStatusECompetencia(eq(1L), eq(TipoTransacao.DESPESA), eq(StatusTransacao.PENDENTE), anyInt(), anyInt()))
                .thenReturn(BigDecimal.ZERO);

        when(transacaoRepository.findTop5ByUsuarioIdAndDataTransacaoLessThanEqualOrderByDataTransacaoDescIdDesc(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of());

        List<Object[]> rowsCategorias = java.util.Collections.singletonList(
                new Object[]{100L, "Alimentação", "fa-utensils", new BigDecimal("200.00")}
        );
        when(transacaoRepository.agruparDespesasPagasPorCategoria(1L)).thenReturn(rowsCategorias);
        when(transacaoRepository.somarEvolucaoMensalPorPeriodo(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        DashboardDto dashboard = dashboardService.obterDashboard(1L);

        assertEquals(0, BigDecimal.ZERO.compareTo(dashboard.metricasMes().receitasMes()));
        assertEquals(0, new BigDecimal("200.00").compareTo(dashboard.metricasMes().despesasMes()));
        assertEquals(1, dashboard.despesasPorCategoria().size());
        assertEquals(0, dashboard.transacoesRecentes().size());
    }
}
