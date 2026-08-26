/**
 * Módulo do Dashboard - Gerenciador Financeiro
 */

const dashboardModule = {
    categoryChartInstance: null,
    evolutionChartInstance: null,

    init() {
        // Inicialização de ouvintes se necessário
    },

    async loadSummary() {
        try {
            // Carregar saldo consolidado, contas e transações em paralelo
            const [saldoConsolidado, contas, transacoes] = await Promise.all([
                api.get('/contas/saldo-consolidado').catch(() => 0),
                api.get('/contas').catch(() => []),
                api.get('/transacoes').catch(() => [])
            ]);

            this.renderMetricCards(saldoConsolidado, transacoes);
            this.renderRecentTransactions(transacoes);
            this.renderCharts(transacoes);
        } catch (error) {
            console.error('Erro ao carregar dados do dashboard:', error);
        }
    },

    renderMetricCards(saldoConsolidado, transacoes) {
        // Calcular totais de receitas e despesas
        let totalReceitas = 0;
        let totalDespesas = 0;

        const currentMonth = new Date().getMonth();
        const currentYear = new Date().getFullYear();

        let receitasMes = 0;
        let despesasMes = 0;

        transacoes.forEach(t => {
            const valor = parseFloat(t.valor) || 0;
            const data = new Date(t.dataTransacao);

            if (t.tipo === 'RECEITA') {
                totalReceitas += valor;
                if (data.getMonth() === currentMonth && data.getFullYear() === currentYear) {
                    receitasMes += valor;
                }
            } else if (t.tipo === 'DESPESA') {
                totalDespesas += valor;
                if (data.getMonth() === currentMonth && data.getFullYear() === currentYear) {
                    despesasMes += valor;
                }
            }
        });

        const saldoMes = receitasMes - despesasMes;

        // Atualizar elementos no DOM
        const elSaldoConsolidado = document.getElementById('dash-saldo-consolidado');
        const elReceitasMes = document.getElementById('dash-receitas-mes');
        const elDespesasMes = document.getElementById('dash-despesas-mes');
        const elBalancoMes = document.getElementById('dash-balanco-mes');
        const elHeaderSaldo = document.getElementById('header-saldo-rapido');

        if (elSaldoConsolidado) elSaldoConsolidado.textContent = formatCurrency(saldoConsolidado);
        if (elHeaderSaldo) elHeaderSaldo.textContent = formatCurrency(saldoConsolidado);
        if (elReceitasMes) elReceitasMes.textContent = formatCurrency(receitasMes);
        if (elDespesasMes) elDespesasMes.textContent = formatCurrency(despesasMes);
        
        if (elBalancoMes) {
            elBalancoMes.textContent = formatCurrency(saldoMes);
            elBalancoMes.className = `text-2xl font-black ${saldoMes >= 0 ? 'text-emerald-600' : 'text-rose-600'}`;
        }
    },

    renderRecentTransactions(transacoes) {
        const container = document.getElementById('dash-recentes-container');
        if (!container) return;

        if (transacoes.length === 0) {
            container.innerHTML = `
                <tr>
                    <td colspan="5" class="py-8 text-center text-slate-400 text-sm">
                        Nenhuma transação registrada até o momento.
                    </td>
                </tr>
            `;
            return;
        }

        // 5 transações mais recentes
        const recentes = [...transacoes]
            .sort((a, b) => new Date(b.dataTransacao) - new Date(a.dataTransacao))
            .slice(0, 5);

        container.innerHTML = recentes.map(t => {
            const isReceita = t.tipo === 'RECEITA';
            const colorClass = isReceita ? 'text-emerald-600' : 'text-rose-600';
            const sinal = isReceita ? '+' : '-';
            const icon = t.categoria ? (t.categoria.icone || 'fa-tag') : 'fa-tag';

            return `
                <tr class="hover:bg-slate-50/60 transition-colors border-b border-slate-100 last:border-0">
                    <td class="py-3 px-4 text-xs text-slate-400 whitespace-nowrap">
                        ${formatDate(t.dataTransacao)}
                    </td>
                    <td class="py-3 px-4">
                        <div class="flex items-center gap-2.5">
                            <div class="w-8 h-8 rounded-lg flex items-center justify-center text-xs ${isReceita ? 'bg-emerald-50 text-emerald-600' : 'bg-rose-50 text-rose-600'}">
                                <i class="fas ${icon}"></i>
                            </div>
                            <span class="font-semibold text-slate-800 text-xs">${t.descricao}</span>
                        </div>
                    </td>
                    <td class="py-3 px-4 text-xs text-slate-500 whitespace-nowrap">
                        ${t.conta ? t.conta.nome : '-'}
                    </td>
                    <td class="py-3 px-4 whitespace-nowrap">
                        <span class="badge badge-status-${t.status.toLowerCase()}">${formatStatusTransacao(t.status)}</span>
                    </td>
                    <td class="py-3 px-4 text-right font-bold text-xs ${colorClass} whitespace-nowrap">
                        ${sinal} ${formatCurrency(t.valor)}
                    </td>
                </tr>
            `;
        }).join('');
    },

    renderCharts(transacoes) {
        this.renderCategoryExpensesChart(transacoes);
        this.renderFinancialEvolutionChart(transacoes);
    },

    renderCategoryExpensesChart(transacoes) {
        const ctx = document.getElementById('chart-categorias');
        if (!ctx) return;

        // Agrupar despesas por categoria
        const despesasPorCategoria = {};
        transacoes.filter(t => t.tipo === 'DESPESA').forEach(t => {
            const catName = t.categoria ? t.categoria.nome : 'Outras';
            despesasPorCategoria[catName] = (despesasPorCategoria[catName] || 0) + parseFloat(t.valor || 0);
        });

        const labels = Object.keys(despesasPorCategoria);
        const data = Object.values(despesasPorCategoria);

        if (this.categoryChartInstance) {
            this.categoryChartInstance.destroy();
        }

        const colors = [
            '#ef4444', '#f97316', '#f59e0b', '#10b981', '#06b6d4',
            '#3b82f6', '#6366f1', '#8b5cf6', '#ec4899', '#64748b'
        ];

        if (labels.length === 0) {
            this.categoryChartInstance = new Chart(ctx, {
                type: 'doughnut',
                data: {
                    labels: ['Sem Despesas'],
                    datasets: [{
                        data: [1],
                        backgroundColor: ['#e2e8f0']
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { position: 'bottom' },
                        tooltip: { enabled: false }
                    }
                }
            });
            return;
        }

        this.categoryChartInstance = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels,
                datasets: [{
                    data,
                    backgroundColor: colors.slice(0, labels.length),
                    borderWidth: 2,
                    borderColor: '#ffffff',
                    hoverOffset: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            boxWidth: 12,
                            font: { size: 11, family: "'Plus Jakarta Sans', sans-serif" }
                        }
                    },
                    tooltip: {
                        callbacks: {
                            label: function (context) {
                                return ` ${context.label}: ${formatCurrency(context.raw)}`;
                            }
                        }
                    }
                },
                cutout: '70%'
            }
        });
    },

    renderFinancialEvolutionChart(transacoes) {
        const ctx = document.getElementById('chart-evolucao');
        if (!ctx) return;

        // Agrupar por últimos 6 meses
        const monthsNames = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];
        const today = new Date();
        const labels = [];
        const receitasData = [];
        const despesasData = [];

        for (let i = 5; i >= 0; i--) {
            const d = new Date(today.getFullYear(), today.getMonth() - i, 1);
            const m = d.getMonth();
            const y = d.getFullYear();
            labels.push(`${monthsNames[m]}/${String(y).slice(2)}`);

            let rec = 0;
            let desp = 0;

            transacoes.forEach(t => {
                const tDate = new Date(t.dataTransacao);
                if (tDate.getMonth() === m && tDate.getFullYear() === y) {
                    if (t.tipo === 'RECEITA') rec += parseFloat(t.valor || 0);
                    if (t.tipo === 'DESPESA') desp += parseFloat(t.valor || 0);
                }
            });

            receitasData.push(rec);
            despesasData.push(desp);
        }

        if (this.evolutionChartInstance) {
            this.evolutionChartInstance.destroy();
        }

        this.evolutionChartInstance = new Chart(ctx, {
            type: 'bar',
            data: {
                labels,
                datasets: [
                    {
                        label: 'Receitas',
                        data: receitasData,
                        backgroundColor: '#10b981',
                        borderRadius: 6
                    },
                    {
                        label: 'Despesas',
                        data: despesasData,
                        backgroundColor: '#ef4444',
                        borderRadius: 6
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: {
                        grid: { display: false }
                    },
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function(value) {
                                return 'R$ ' + value;
                            }
                        }
                    }
                },
                plugins: {
                    legend: {
                        position: 'top',
                        labels: {
                            boxWidth: 12,
                            font: { size: 11, family: "'Plus Jakarta Sans', sans-serif" }
                        }
                    },
                    tooltip: {
                        callbacks: {
                            label: function (context) {
                                return ` ${context.dataset.label}: ${formatCurrency(context.raw)}`;
                            }
                        }
                    }
                }
            }
        });
    }
};

window.dashboardModule = dashboardModule;
