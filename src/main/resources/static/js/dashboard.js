/**
 * Módulo do Dashboard - Gerenciador Financeiro
 * Processamento e agregações matemáticas 100% executadas no servidor Java (Spring Boot)
 */

const dashboardModule = {
    categoryChartInstance: null,
    evolutionChartInstance: null,

    init() {
        // Inicialização do módulo
    },

    async loadSummary() {
        try {
            // Chamada única ao endpoint de consolidação no Java
            const data = await api.get('/dashboard');
            if (!data) return;

            this.renderMetricCards(data.saldos, data.metricasMes);
            this.renderRecentTransactions(data.transacoesRecentes || []);
            this.renderCharts(data.despesasPorCategoria || [], data.evolucaoMensal || {});
        } catch (error) {
            console.error('Erro ao carregar dados do dashboard:', error);
        }
    },

    renderMetricCards(saldos, metricas) {
        const saldoCorrentes = saldos ? (parseFloat(saldos.saldoContasCorrentes) || 0) : 0;
        const saldoInvestimentos = saldos ? (parseFloat(saldos.saldoInvestimentos) || 0) : 0;

        const receitasMes = metricas ? (parseFloat(metricas.receitasMes) || 0) : 0;
        const despesasMes = metricas ? (parseFloat(metricas.despesasMes) || 0) : 0;
        const balancoMes = metricas ? (parseFloat(metricas.balancoMes) || 0) : 0;

        // Atualizar elementos no DOM
        const elSaldoConsolidado = document.getElementById('dash-saldo-consolidado');
        const elTotalInvestimentos = document.getElementById('dash-total-investimentos');
        const elReceitasMes = document.getElementById('dash-receitas-mes');
        const elDespesasMes = document.getElementById('dash-despesas-mes');
        const elBalancoMes = document.getElementById('dash-balanco-mes');
        const elHeaderSaldo = document.getElementById('header-saldo-rapido');
        const elHeaderInvestimentos = document.getElementById('header-investimentos-rapido');

        if (elSaldoConsolidado) elSaldoConsolidado.textContent = formatCurrency(saldoCorrentes);
        if (elTotalInvestimentos) elTotalInvestimentos.textContent = formatCurrency(saldoInvestimentos);
        if (elHeaderSaldo) elHeaderSaldo.textContent = formatCurrency(saldoCorrentes);
        if (elHeaderInvestimentos) elHeaderInvestimentos.textContent = formatCurrency(saldoInvestimentos);
        if (elReceitasMes) elReceitasMes.textContent = formatCurrency(receitasMes);
        if (elDespesasMes) elDespesasMes.textContent = formatCurrency(despesasMes);

        if (elBalancoMes) {
            elBalancoMes.textContent = formatCurrency(balancoMes);
            elBalancoMes.className = `text-2xl font-black ${balancoMes >= 0 ? 'text-emerald-600' : 'text-rose-600'}`;
        }
    },

    renderRecentTransactions(recentes) {
        const container = document.getElementById('dash-recentes-container');
        if (!container) return;

        if (recentes.length === 0) {
            container.innerHTML = `
                <tr>
                    <td colspan="5" class="py-8 text-center text-slate-400 text-sm">
                        Nenhuma transação registrada até o momento.
                    </td>
                </tr>
            `;
            return;
        }

        container.innerHTML = recentes.map(t => {
            const isReceita = t.tipo === 'RECEITA';
            const colorClass = isReceita ? 'text-emerald-600' : 'text-rose-600';
            const sinal = isReceita ? '+' : '-';
            const icon = t.categoriaIcone || 'fa-tag';

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
                        ${t.contaNome || '-'}
                    </td>
                    <td class="py-3 px-4 whitespace-nowrap">
                        <span class="badge badge-status-${(t.status || '').toLowerCase()}">${formatStatusTransacao(t.status)}</span>
                    </td>
                    <td class="py-3 px-4 text-right font-bold text-xs ${colorClass} whitespace-nowrap">
                        ${sinal} ${formatCurrency(t.valor)}
                    </td>
                </tr>
            `;
        }).join('');
    },

    renderCharts(despesasPorCategoria, evolucaoMensal) {
        this.renderCategoryExpensesChart(despesasPorCategoria);
        this.renderFinancialEvolutionChart(evolucaoMensal);
    },

    renderCategoryExpensesChart(categorias) {
        const ctx = document.getElementById('chart-categorias');
        if (!ctx) return;

        const labels = categorias.map(c => c.nome);
        const data = categorias.map(c => parseFloat(c.total) || 0);

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

    renderFinancialEvolutionChart(evolucao) {
        const ctx = document.getElementById('chart-evolucao');
        if (!ctx) return;

        const labels = evolucao.labels || [];
        const receitasData = (evolucao.receitas || []).map(v => parseFloat(v) || 0);
        const despesasData = (evolucao.despesas || []).map(v => parseFloat(v) || 0);

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
                            callback: function (value) {
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
