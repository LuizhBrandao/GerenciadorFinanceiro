/**
 * Módulo de Transações Recorrentes & Despesas Fixas - Gerenciador Financeiro
 * Agregações e rotinas de recorrências processadas no servidor Java
 */

const recorrenciasModule = {
    recorrencias: [],

    init() {
        this.bindEvents();
    },

    bindEvents() {
        const formRecorrencia = document.getElementById('form-recorrencia');
        if (formRecorrencia) {
            formRecorrencia.addEventListener('submit', (e) => this.handleSaveRecorrencia(e));
        }
    },

    async loadRecorrencias() {
        try {
            const [data, resumo] = await Promise.all([
                api.get('/recorrencias').catch(() => []),
                api.get('/recorrencias/resumo').catch(() => null)
            ]);

            this.recorrencias = data || [];
            this.renderRecorrenciasCards();
            this.renderResumo(resumo);
            this.updateRecorrenciasSelects();
            return this.recorrencias;
        } catch (error) {
            showToast('Erro ao carregar transações recorrentes.', 'error');
            return [];
        }
    },

    renderResumo(resumo) {
        let totalDespesasFixas = 0;
        let totalReceitasRecorrentes = 0;
        let totalAtivas = 0;

        if (resumo) {
            totalDespesasFixas = parseFloat(resumo.totalDespesasFixas) || 0;
            totalReceitasRecorrentes = parseFloat(resumo.totalReceitasRecorrentes) || 0;
            totalAtivas = resumo.totalAtivas || 0;
        } else {
            this.recorrencias.forEach(rec => {
                if (rec.ativo) {
                    totalAtivas++;
                    const valor = parseFloat(rec.valor) || 0;
                    if (rec.tipo === 'DESPESA') {
                        totalDespesasFixas += valor;
                    } else if (rec.tipo === 'RECEITA') {
                        totalReceitasRecorrentes += valor;
                    }
                }
            });
        }

        const elDespesas = document.getElementById('rec-total-despesas-fixas');
        const elReceitas = document.getElementById('rec-total-receitas');
        const elAtivas = document.getElementById('rec-total-ativas');

        if (elDespesas) elDespesas.textContent = formatCurrency(totalDespesasFixas);
        if (elReceitas) elReceitas.textContent = formatCurrency(totalReceitasRecorrentes);
        if (elAtivas) elAtivas.textContent = totalAtivas;
    },

    renderRecorrenciasCards() {
        const container = document.getElementById('recorrencias-list-container');
        if (!container) return;

        if (this.recorrencias.length === 0) {
            container.innerHTML = `
                <div class="col-span-full py-12 text-center text-slate-400 bg-white rounded-2xl border border-slate-100 p-8 shadow-sm">
                    <div class="w-16 h-16 mx-auto mb-4 bg-indigo-50 text-indigo-500 rounded-full flex items-center justify-center text-2xl">
                        <i class="fas fa-repeat"></i>
                    </div>
                    <h4 class="text-base font-semibold text-slate-700 mb-1">Nenhuma recorrência ou despesa fixa</h4>
                    <p class="text-sm text-slate-400 mb-4">Cadastre contas mensais fixas (aluguel, streaming, internet) ou receitas recorrentes (salário).</p>
                    <button onclick="recorrenciasModule.openNewRecorrenciaModal()" class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold rounded-xl transition-colors shadow-sm">
                        <i class="fas fa-plus mr-1.5"></i> Criar Primeira Recorrência
                    </button>
                </div>
            `;
            return;
        }

        container.innerHTML = this.recorrencias.map(rec => {
            const isReceita = rec.tipo === 'RECEITA';
            const colorClass = isReceita ? 'text-emerald-600' : 'text-rose-600';
            const sinal = isReceita ? '+' : '-';
            const badgeTipo = isReceita ? 'badge-receita' : 'badge-despesa';
            const catIcon = rec.categoria ? (rec.categoria.icone || 'fa-tag') : 'fa-tag';
            const catNome = rec.categoria ? rec.categoria.nome : 'Sem Categoria';
            const contaNome = rec.conta ? rec.conta.nome : 'Sem Conta';
            const statusBadge = rec.ativo 
                ? '<span class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-600 border border-emerald-200">Ativa</span>'
                : '<span class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-500 border border-slate-200">Pausada</span>';

            const ultimoLanc = rec.ultimoLancamento ? formatDate(rec.ultimoLancamento) : 'Nenhum lançamento';

            return `
                <div class="bg-white rounded-2xl p-5 border ${rec.ativo ? 'border-slate-100' : 'border-slate-200 opacity-75'} shadow-sm hover-card relative overflow-hidden flex flex-col justify-between">
                    <div>
                        <div class="flex items-start justify-between mb-3">
                            <div class="flex items-center gap-3">
                                <div class="w-10 h-10 rounded-xl flex items-center justify-center text-base ${isReceita ? 'bg-emerald-50 text-emerald-600' : 'bg-rose-50 text-rose-600'}">
                                    <i class="fas ${catIcon}"></i>
                                </div>
                                <div>
                                    <div class="flex items-center gap-2">
                                        <h3 class="font-bold text-slate-800 text-base leading-tight">${rec.descricao}</h3>
                                        ${statusBadge}
                                    </div>
                                    <p class="text-xs text-slate-400 font-medium">${catNome} • ${contaNome}</p>
                                </div>
                            </div>
                            <span class="badge ${badgeTipo}">${formatTipoTransacao(rec.tipo)}</span>
                        </div>

                        <div class="my-3 py-2 px-3 bg-slate-50 rounded-xl border border-slate-100 flex items-center justify-between text-xs">
                            <div class="flex items-center gap-1.5 text-slate-600">
                                <i class="fas fa-calendar-check text-indigo-500"></i>
                                <span>Todo dia <strong>${rec.diaVencimento}</strong> (${formatFrequenciaRecorrencia(rec.frequencia)})</span>
                            </div>
                            <span class="text-[11px] text-slate-400">Último: ${ultimoLanc}</span>
                        </div>

                        ${rec.observacao ? `<p class="text-xs text-slate-400 mb-3 italic">"${rec.observacao}"</p>` : ''}
                    </div>

                    <div class="pt-3 border-t border-slate-100 flex items-center justify-between mt-2">
                        <div class="text-xl font-black ${colorClass} tracking-tight">
                            ${sinal} ${formatCurrency(rec.valor)}
                        </div>

                        <div class="flex items-center gap-1">
                            <button onclick="recorrenciasModule.lancarAgora(${rec.id})" title="Lançar Transação Agora" class="p-2 text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors text-xs font-bold flex items-center gap-1">
                                <i class="fas fa-bolt text-xs"></i> Lançar
                            </button>
                            <button onclick="recorrenciasModule.toggleStatus(${rec.id})" title="${rec.ativo ? 'Pausar Recorrência' : 'Ativar Recorrência'}" class="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-50 rounded-lg transition-colors">
                                <i class="fas ${rec.ativo ? 'fa-pause' : 'fa-play'} text-xs"></i>
                            </button>
                            <button onclick="recorrenciasModule.openEditRecorrenciaModal(${rec.id})" title="Editar" class="p-2 text-slate-400 hover:text-amber-600 hover:bg-amber-50 rounded-lg transition-colors">
                                <i class="fas fa-edit text-xs"></i>
                            </button>
                            <button onclick="recorrenciasModule.excluirRecorrencia(${rec.id}, '${rec.descricao}')" title="Excluir" class="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors">
                                <i class="fas fa-trash text-xs"></i>
                            </button>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
    },

    updateRecorrenciasSelects() {
        const selectConta = document.getElementById('recorrencia-conta');
        const selectCategoria = document.getElementById('recorrencia-categoria');

        if (selectConta && window.contasModule && window.contasModule.contas) {
            const current = selectConta.value;
            let html = '<option value="">Selecione uma conta</option>';
            window.contasModule.contas.forEach(c => {
                html += `<option value="${c.id}">${c.nome}</option>`;
            });
            selectConta.innerHTML = html;
            if (current) selectConta.value = current;
        }

        if (selectCategoria && window.categoriasModule && window.categoriasModule.categorias) {
            const current = selectCategoria.value;
            let html = '<option value="">Selecione uma categoria</option>';
            window.categoriasModule.categorias.forEach(cat => {
                html += `<option value="${cat.id}">${cat.nome} (${formatTipoTransacao(cat.tipo)})</option>`;
            });
            selectCategoria.innerHTML = html;
            if (current) selectCategoria.value = current;
        }
    },

    openNewRecorrenciaModal() {
        if (window.transacoesModule) {
            window.transacoesModule.openNewTransacaoModal('RECORRENTE');
        } else {
            const form = document.getElementById('form-recorrencia');
            if (form) form.reset();
            document.getElementById('recorrencia-id').value = '';
            document.getElementById('recorrencia-data-inicio').value = getTodayIsoDate();
            document.getElementById('recorrencia-dia').value = new Date().getDate();
            document.getElementById('modal-recorrencia-title').textContent = 'Nova Despesa Fixa / Recorrência';
            
            this.updateRecorrenciasSelects();
            openModal('modal-recorrencia');
        }
    },

    openEditRecorrenciaModal(id) {
        const rec = this.recorrencias.find(r => r.id === id);
        if (!rec) return;

        this.updateRecorrenciasSelects();

        document.getElementById('recorrencia-id').value = rec.id;
        document.getElementById('recorrencia-descricao').value = rec.descricao;
        document.getElementById('recorrencia-valor').value = rec.valor;
        document.getElementById('recorrencia-tipo').value = rec.tipo;
        document.getElementById('recorrencia-frequencia').value = rec.frequencia || 'MENSAL';
        document.getElementById('recorrencia-dia').value = rec.diaVencimento || 1;
        document.getElementById('recorrencia-conta').value = rec.conta ? rec.conta.id : '';
        document.getElementById('recorrencia-categoria').value = rec.categoria ? rec.categoria.id : '';
        document.getElementById('recorrencia-data-inicio').value = rec.dataInicio || getTodayIsoDate();
        document.getElementById('recorrencia-data-fim').value = rec.dataFim || '';
        document.getElementById('recorrencia-observacao').value = rec.observacao || '';

        document.getElementById('modal-recorrencia-title').textContent = 'Editar Recorrência';
        openModal('modal-recorrencia');
    },

    async handleSaveRecorrencia(e) {
        e.preventDefault();
        const id = document.getElementById('recorrencia-id').value;
        const descricao = document.getElementById('recorrencia-descricao').value.trim();
        const valor = parseFloat(document.getElementById('recorrencia-valor').value);
        const tipo = document.getElementById('recorrencia-tipo').value;
        const frequencia = document.getElementById('recorrencia-frequencia').value;
        const diaVencimento = parseInt(document.getElementById('recorrencia-dia').value) || 1;
        const contaId = document.getElementById('recorrencia-conta').value;
        const categoriaId = document.getElementById('recorrencia-categoria').value;
        const dataInicio = document.getElementById('recorrencia-data-inicio').value;
        const dataFim = document.getElementById('recorrencia-data-fim').value || null;
        const observacao = document.getElementById('recorrencia-observacao').value.trim();

        if (!descricao || isNaN(valor) || valor <= 0 || !tipo || !frequencia || !diaVencimento || !contaId || !categoriaId || !dataInicio) {
            showToast('Preencha todos os campos obrigatórios.', 'warning');
            return;
        }

        const payload = {
            descricao,
            valor,
            tipo,
            frequencia,
            diaVencimento,
            contaId: parseInt(contaId),
            categoriaId: parseInt(categoriaId),
            dataInicio,
            dataFim,
            observacao
        };

        try {
            if (id) {
                await api.put(`/recorrencias/${id}`, payload);
                showToast('Recorrência atualizada com sucesso!', 'success');
            } else {
                await api.post('/recorrencias', payload);
                showToast('Recorrência cadastrada com sucesso!', 'success');
            }

            closeModal('modal-recorrencia');
            await this.loadRecorrencias();
            if (window.transacoesModule) {
                await window.transacoesModule.loadTransacoes();
            }
            if (window.dashboardModule) {
                window.dashboardModule.loadSummary();
            }
        } catch (error) {
            showToast(error.message || 'Erro ao salvar recorrência.', 'error');
        }
    },

    async toggleStatus(id) {
        try {
            await api.request(`/recorrencias/${id}/status`, { method: 'PATCH' });
            showToast('Status da recorrência atualizado!', 'success');
            await this.loadRecorrencias();
        } catch (error) {
            showToast(error.message || 'Erro ao alternar status.', 'error');
        }
    },

    async lancarAgora(id) {
        if (!confirm('Deseja gerar o lançamento imediato desta transação recorrente no histórico de transações?')) {
            return;
        }

        try {
            await api.post(`/recorrencias/${id}/lancar`);
            showToast('Transação lançada com sucesso no histórico!', 'success');
            await this.loadRecorrencias();
            if (window.transacoesModule) {
                await window.transacoesModule.loadTransacoes();
            }
            if (window.dashboardModule) {
                window.dashboardModule.loadSummary();
            }
        } catch (error) {
            showToast(error.message || 'Erro ao lançar transação recorrente.', 'error');
        }
    },

    async processarPendentes() {
        try {
            const geradas = await api.post('/recorrencias/processar');
            const count = (geradas && Array.isArray(geradas)) ? geradas.length : 0;
            if (count > 0) {
                showToast(`${count} transação(ões) recorrente(s) pendente(s) foram lançadas!`, 'success');
            } else {
                showToast('Todas as recorrências deste período já estão em dia!', 'info');
            }
            await this.loadRecorrencias();
            if (window.transacoesModule) {
                await window.transacoesModule.loadTransacoes();
            }
            if (window.contasModule) {
                await window.contasModule.loadContas();
            }
            if (window.dashboardModule) {
                window.dashboardModule.loadSummary();
            }
        } catch (error) {
            showToast(error.message || 'Erro ao processar pendências.', 'error');
        }
    },

    async excluirRecorrencia(id, nome) {
        if (!confirm(`Deseja realmente excluir a recorrência "${nome}"?`)) {
            return;
        }

        try {
            await api.delete(`/recorrencias/${id}`);
            showToast('Recorrência excluída com sucesso.', 'success');
            await this.loadRecorrencias();
        } catch (error) {
            showToast(error.message || 'Erro ao excluir recorrência.', 'error');
        }
    }
};

window.recorrenciasModule = recorrenciasModule;
