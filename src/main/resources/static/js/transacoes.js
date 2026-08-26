/**
 * Módulo de Transações - Gerenciador Financeiro
 */

const transacoesModule = {
    transacoes: [],

    init() {
        this.bindEvents();
    },

    bindEvents() {
        const formTransacao = document.getElementById('form-transacao');
        const formFiltro = document.getElementById('form-filtro-transacoes');
        const btnLimparFiltros = document.getElementById('btn-limpar-filtros');
        const checkParcelado = document.getElementById('transacao-parcelado-check');

        if (formTransacao) {
            formTransacao.addEventListener('submit', (e) => this.handleSaveTransacao(e));
        }

        if (formFiltro) {
            formFiltro.addEventListener('submit', (e) => {
                e.preventDefault();
                this.aplicarFiltros();
            });
        }

        if (btnLimparFiltros) {
            btnLimparFiltros.addEventListener('click', () => {
                formFiltro.reset();
                this.loadTransacoes();
            });
        }

        if (checkParcelado) {
            checkParcelado.addEventListener('change', (e) => {
                const group = document.getElementById('transacao-parcelas-group');
                if (group) {
                    if (e.target.checked) {
                        group.classList.remove('hidden');
                    } else {
                        group.classList.add('hidden');
                    }
                }
            });
        }
    },

    async loadTransacoes() {
        try {
            const data = await api.get('/transacoes');
            this.transacoes = data || [];
            this.renderTransacoesTable();
            return this.transacoes;
        } catch (error) {
            showToast('Erro ao carregar transações.', 'error');
            return [];
        }
    },

    async aplicarFiltros() {
        const dataInicial = document.getElementById('filtro-data-inicial').value;
        const dataFinal = document.getElementById('filtro-data-final').value;
        const categoriaId = document.getElementById('filtro-categoria').value;
        const contaId = document.getElementById('filtro-conta').value;
        const valorMin = document.getElementById('filtro-valor-min').value;
        const valorMax = document.getElementById('filtro-valor-max').value;

        const params = new URLSearchParams();
        if (dataInicial) params.append('dataInicial', dataInicial);
        if (dataFinal) params.append('dataFinal', dataFinal);
        if (categoriaId) params.append('categoriaId', categoriaId);
        if (contaId) params.append('contaId', contaId);
        if (valorMin) params.append('valorMin', valorMin);
        if (valorMax) params.append('valorMax', valorMax);

        try {
            const query = params.toString() ? `?${params.toString()}` : '';
            const data = await api.get(`/transacoes/busca${query}`);
            this.transacoes = data || [];
            this.renderTransacoesTable();
            showToast(`Filtro aplicado: ${this.transacoes.length} transação(ões) encontrada(s).`, 'info');
        } catch (error) {
            showToast('Erro ao buscar transações com filtros.', 'error');
        }
    },

    renderTransacoesTable() {
        const tbody = document.getElementById('transacoes-table-body');
        const emptyState = document.getElementById('transacoes-empty-state');
        if (!tbody) return;

        if (this.transacoes.length === 0) {
            tbody.innerHTML = '';
            if (emptyState) emptyState.classList.remove('hidden');
            return;
        }

        if (emptyState) emptyState.classList.add('hidden');

        // Ordena por data decrescente
        const sorted = [...this.transacoes].sort((a, b) => new Date(b.dataTransacao) - new Date(a.dataTransacao));

        tbody.innerHTML = sorted.map(t => {
            const isReceita = t.tipo === 'RECEITA';
            const colorClass = isReceita ? 'text-emerald-600' : 'text-rose-600';
            const sinal = isReceita ? '+' : '-';
            const badgeTipo = isReceita ? 'badge-receita' : 'badge-despesa';

            return `
                <tr class="hover:bg-slate-50/80 transition-colors border-b border-slate-100 last:border-0">
                    <td class="px-5 py-3.5 text-xs text-slate-500 whitespace-nowrap">
                        ${formatDate(t.dataTransacao)}
                    </td>
                    <td class="px-5 py-3.5 whitespace-nowrap">
                        <div class="flex flex-col">
                            <span class="font-semibold text-slate-800 text-sm">${t.descricao}</span>
                            ${t.observacao ? `<span class="text-xs text-slate-400 truncate max-w-xs">${t.observacao}</span>` : ''}
                        </div>
                    </td>
                    <td class="px-5 py-3.5 whitespace-nowrap">
                        <span class="inline-flex items-center gap-1.5 text-xs font-medium text-slate-700 bg-slate-100 px-2.5 py-1 rounded-lg">
                            <i class="fas ${t.categoria ? (t.categoria.icone || 'fa-tag') : 'fa-tag'} text-xs text-slate-500"></i>
                            ${t.categoria ? t.categoria.nome : 'Sem Categoria'}
                        </span>
                    </td>
                    <td class="px-5 py-3.5 whitespace-nowrap text-xs text-slate-600 font-medium">
                        ${t.conta ? t.conta.nome : '-'}
                    </td>
                    <td class="px-5 py-3.5 whitespace-nowrap">
                        <span class="badge ${badgeTipo}">${formatTipoTransacao(t.tipo)}</span>
                    </td>
                    <td class="px-5 py-3.5 whitespace-nowrap">
                        <span class="badge badge-status-${t.status.toLowerCase()}">${formatStatusTransacao(t.status)}</span>
                    </td>
                    <td class="px-5 py-3.5 whitespace-nowrap text-right font-bold text-sm ${colorClass}">
                        ${sinal} ${formatCurrency(t.valor)}
                    </td>
                    <td class="px-5 py-3.5 whitespace-nowrap text-right text-xs">
                        <button onclick="transacoesModule.openEditTransacaoModal(${t.id})" title="Editar" class="p-1.5 text-slate-400 hover:text-amber-600 hover:bg-amber-50 rounded-lg transition-colors mr-1">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button onclick="transacoesModule.excluirTransacao(${t.id})" title="Excluir" class="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors">
                            <i class="fas fa-trash"></i>
                        </button>
                    </td>
                </tr>
            `;
        }).join('');
    },

    openNewTransacaoModal() {
        const form = document.getElementById('form-transacao');
        if (form) form.reset();
        document.getElementById('transacao-id').value = '';
        document.getElementById('transacao-data').value = getTodayIsoDate();
        document.getElementById('modal-transacao-title').textContent = 'Nova Transação';
        document.getElementById('transacao-parcelado-container').classList.remove('hidden');
        document.getElementById('transacao-parcelas-group').classList.add('hidden');
        document.getElementById('transacao-parcelado-check').checked = false;

        openModal('modal-transacao');
    },

    openEditTransacaoModal(id) {
        const transacao = this.transacoes.find(t => t.id === id);
        if (!transacao) return;

        document.getElementById('transacao-id').value = transacao.id;
        document.getElementById('transacao-descricao').value = transacao.descricao;
        document.getElementById('transacao-valor').value = transacao.valor;
        document.getElementById('transacao-tipo').value = transacao.tipo;
        document.getElementById('transacao-status').value = transacao.status;
        document.getElementById('transacao-data').value = transacao.dataTransacao;
        document.getElementById('transacao-conta').value = transacao.conta ? transacao.conta.id : '';
        document.getElementById('transacao-categoria').value = transacao.categoria ? transacao.categoria.id : '';
        document.getElementById('transacao-observacao').value = transacao.observacao || '';

        document.getElementById('modal-transacao-title').textContent = 'Editar Transação';
        document.getElementById('transacao-parcelado-container').classList.add('hidden');
        document.getElementById('transacao-parcelas-group').classList.add('hidden');

        openModal('modal-transacao');
    },

    async handleSaveTransacao(e) {
        e.preventDefault();
        const id = document.getElementById('transacao-id').value;
        const descricao = document.getElementById('transacao-descricao').value.trim();
        const valor = parseFloat(document.getElementById('transacao-valor').value);
        const tipo = document.getElementById('transacao-tipo').value;
        const status = document.getElementById('transacao-status').value;
        const dataTransacao = document.getElementById('transacao-data').value;
        const contaId = document.getElementById('transacao-conta').value;
        const categoriaId = document.getElementById('transacao-categoria').value;
        const observacao = document.getElementById('transacao-observacao').value.trim();
        const isParcelado = document.getElementById('transacao-parcelado-check')?.checked;
        const parcelas = parseInt(document.getElementById('transacao-parcelas')?.value) || 1;

        if (!descricao || isNaN(valor) || valor <= 0 || !tipo || !status || !dataTransacao || !contaId || !categoriaId) {
            showToast('Preencha todos os campos obrigatórios da transação.', 'warning');
            return;
        }

        const payload = {
            descricao,
            valor,
            tipo,
            status,
            dataTransacao,
            conta: { id: parseInt(contaId) },
            categoria: { id: parseInt(categoriaId) },
            observacao
        };

        try {
            if (id) {
                // Atualizar
                await api.put(`/transacoes/${id}`, payload);
                showToast('Transação atualizada com sucesso!', 'success');
            } else if (isParcelado && parcelas > 1) {
                // Criar Parcelado
                await api.post(`/transacoes/parcelado?parcelas=${parcelas}`, payload);
                showToast(`Transação parcelada em ${parcelas}x criada com sucesso!`, 'success');
            } else {
                // Criar Simples
                await api.post('/transacoes', payload);
                showToast('Transação registrada com sucesso!', 'success');
            }

            closeModal('modal-transacao');
            await this.loadTransacoes();
            if (window.contasModule) {
                await window.contasModule.loadContas();
            }
            if (window.dashboardModule) {
                window.dashboardModule.loadSummary();
            }
        } catch (error) {
            showToast(error.message || 'Erro ao salvar transação.', 'error');
        }
    },

    async excluirTransacao(id) {
        if (!confirm('Deseja realmente excluir esta transação? O saldo da conta será recalculado.')) {
            return;
        }

        try {
            await api.delete(`/transacoes/${id}`);
            showToast('Transação excluída com sucesso.', 'success');
            await this.loadTransacoes();
            if (window.contasModule) {
                await window.contasModule.loadContas();
            }
            if (window.dashboardModule) {
                window.dashboardModule.loadSummary();
            }
        } catch (error) {
            showToast(error.message || 'Erro ao excluir transação.', 'error');
        }
    }
};

window.transacoesModule = transacoesModule;
