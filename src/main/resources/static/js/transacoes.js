/**
 * Módulo de Transações & Lançamentos - Gerenciador Financeiro
 */

const transacoesModule = {
    transacoes: [],
    currentTab: 'extrato',

    init() {
        this.bindEvents();
    },

    bindEvents() {
        const formTransacao = document.getElementById('form-transacao');
        const formFiltro = document.getElementById('form-filtro-transacoes');
        const btnLimparFiltros = document.getElementById('btn-limpar-filtros');
        const tipoSelect = document.getElementById('transacao-tipo');
        const dataInput = document.getElementById('transacao-data');

        if (tipoSelect) {
            tipoSelect.addEventListener('change', (e) => {
                if (window.categoriasModule) {
                    categoriasModule.updateCategoriasSelects(e.target.value);
                }
            });
        }

        // Auto-sincronizar dia do vencimento quando a data da transação mudar
        if (dataInput) {
            dataInput.addEventListener('change', (e) => {
                if (e.target.value) {
                    const dia = parseInt(e.target.value.split('-')[2], 10);
                    const diaInput = document.getElementById('transacao-dia-vencimento');
                    if (diaInput && !isNaN(dia)) {
                        diaInput.value = dia;
                    }
                }
            });
        }

        // Ouvintes dos Radio Buttons de Modo de Lançamento
        document.querySelectorAll('input[name="transacao-modo"]').forEach(radio => {
            radio.addEventListener('change', (e) => {
                this.handleModoLancamentoChange(e.target.value);
            });
        });

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
    },

    switchTab(tab) {
        this.currentTab = tab;
        const btnExtrato = document.getElementById('tab-btn-extrato');
        const btnRecorrencias = document.getElementById('tab-btn-recorrencias');
        const contentExtrato = document.getElementById('tab-content-extrato');
        const contentRecorrencias = document.getElementById('tab-content-recorrencias');
        const btnProcessar = document.getElementById('btn-processar-pendencias-transacoes');

        if (tab === 'extrato') {
            if (btnExtrato) {
                btnExtrato.className = 'px-4 py-2.5 text-xs font-bold rounded-xl transition-all flex items-center gap-2 bg-indigo-600 text-white shadow-sm';
            }
            if (btnRecorrencias) {
                btnRecorrencias.className = 'px-4 py-2.5 text-xs font-bold rounded-xl transition-all flex items-center gap-2 text-slate-500 hover:text-slate-800 hover:bg-slate-100';
            }
            if (contentExtrato) contentExtrato.classList.remove('hidden');
            if (contentRecorrencias) contentRecorrencias.classList.add('hidden');
            if (btnProcessar) btnProcessar.classList.add('hidden');

            this.loadTransacoes();
        } else {
            if (btnRecorrencias) {
                btnRecorrencias.className = 'px-4 py-2.5 text-xs font-bold rounded-xl transition-all flex items-center gap-2 bg-purple-600 text-white shadow-sm';
            }
            if (btnExtrato) {
                btnExtrato.className = 'px-4 py-2.5 text-xs font-bold rounded-xl transition-all flex items-center gap-2 text-slate-500 hover:text-slate-800 hover:bg-slate-100';
            }
            if (contentExtrato) contentExtrato.classList.add('hidden');
            if (contentRecorrencias) contentRecorrencias.classList.remove('hidden');
            if (btnProcessar) btnProcessar.classList.remove('hidden');

            if (window.recorrenciasModule) {
                recorrenciasModule.loadRecorrencias();
            }
        }
    },

    handleModoLancamentoChange(modo) {
        const groupParcelas = document.getElementById('transacao-parcelas-group');
        const groupRecorrencia = document.getElementById('transacao-recorrencia-group');

        if (groupParcelas) {
            if (modo === 'PARCELADO') groupParcelas.classList.remove('hidden');
            else groupParcelas.classList.add('hidden');
        }

        if (groupRecorrencia) {
            if (modo === 'RECORRENTE') groupRecorrencia.classList.remove('hidden');
            else groupRecorrencia.classList.add('hidden');
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
        const dataInicial = document.getElementById('filtro-data-inicial')?.value;
        const dataFinal = document.getElementById('filtro-data-final')?.value;
        const categoriaId = document.getElementById('filtro-categoria')?.value;
        const contaId = document.getElementById('filtro-conta')?.value;
        const valorMin = document.getElementById('filtro-valor-min')?.value;

        const params = new URLSearchParams();
        if (dataInicial) params.append('dataInicial', dataInicial);
        if (dataFinal) params.append('dataFinal', dataFinal);
        if (categoriaId) params.append('categoriaId', categoriaId);
        if (contaId) params.append('contaId', contaId);
        if (valorMin && parseFloat(valorMin) > 0) params.append('valorMin', valorMin);

        try {
            const query = params.toString() ? `?${params.toString()}` : '';
            const data = await api.get(`/transacoes/busca${query}`);
            this.transacoes = data || [];
            this.renderTransacoesTable();
            showToast(`Filtro aplicado: ${this.transacoes.length} transação(ões) encontrada(s).`, 'info');
        } catch (error) {
            console.error('Erro ao buscar transações com filtros:', error);
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

            // Detecta se é recorrente ou parcelado para exibir selo informativo
            const isRecorrente = t.observacao && t.observacao.toLowerCase().includes('recorrente');
            const isParcelado = t.descricao && t.descricao.includes('(Parcela');

            return `
                <tr class="hover:bg-slate-50/80 transition-colors border-b border-slate-100 last:border-0">
                    <td class="px-5 py-3.5 text-xs text-slate-500 whitespace-nowrap">
                        ${formatDate(t.dataTransacao)}
                    </td>
                    <td class="px-5 py-3.5 whitespace-nowrap">
                        <div class="flex flex-col">
                            <div class="flex items-center gap-1.5">
                                <span class="font-semibold text-slate-800 text-sm">${t.descricao}</span>
                                ${isRecorrente ? '<span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-purple-50 text-purple-600 border border-purple-200" title="Lançamento Recorrente / Fixo"><i class="fas fa-arrows-rotate mr-0.5"></i>Fixa</span>' : ''}
                                ${isParcelado ? '<span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-indigo-50 text-indigo-600 border border-indigo-200" title="Lançamento Parcelado"><i class="fas fa-layer-group mr-0.5"></i>Parcelada</span>' : ''}
                            </div>
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

    async openNewTransacaoModal(modoInicial = 'UNICO') {
        const form = document.getElementById('form-transacao');
        if (form) form.reset();
        document.getElementById('transacao-id').value = '';
        
        const today = getTodayIsoDate();
        document.getElementById('transacao-data').value = today;
        
        const diaAtual = new Date().getDate();
        const diaVencInput = document.getElementById('transacao-dia-vencimento');
        if (diaVencInput) diaVencInput.value = diaAtual;

        document.getElementById('modal-transacao-title').textContent = 'Nova Transação';
        document.getElementById('transacao-modo-container').classList.remove('hidden');

        // Selecionar o modo inicial
        const radioUnico = document.getElementById('modo-unico');
        const radioParcelado = document.getElementById('modo-parcelado');
        const radioRecorrente = document.getElementById('modo-recorrente');

        if (modoInicial === 'PARCELADO' && radioParcelado) radioParcelado.checked = true;
        else if (modoInicial === 'RECORRENTE' && radioRecorrente) radioRecorrente.checked = true;
        else if (radioUnico) radioUnico.checked = true;

        this.handleModoLancamentoChange(modoInicial);

        // Atualizar selects de categorias e contas
        const tipoInicial = document.getElementById('transacao-tipo')?.value || 'DESPESA';
        if (window.categoriasModule) {
            if (categoriasModule.categorias.length === 0) {
                await categoriasModule.loadCategorias();
            }
            categoriasModule.updateCategoriasSelects(tipoInicial);
        }
        if (window.contasModule) {
            if (contasModule.contas.length === 0) {
                await contasModule.loadContas();
            }
            contasModule.updateContasSelects();
        }

        openModal('modal-transacao');
    },

    async openEditTransacaoModal(id) {
        const transacao = this.transacoes.find(t => t.id === id);
        if (!transacao) return;

        if (window.categoriasModule && categoriasModule.categorias.length === 0) {
            await categoriasModule.loadCategorias();
        }
        if (window.contasModule && contasModule.contas.length === 0) {
            await contasModule.loadContas();
        }

        document.getElementById('transacao-id').value = transacao.id;
        document.getElementById('transacao-descricao').value = transacao.descricao;
        document.getElementById('transacao-valor').value = transacao.valor;
        document.getElementById('transacao-tipo').value = transacao.tipo;
        document.getElementById('transacao-status').value = transacao.status;
        document.getElementById('transacao-data').value = transacao.dataTransacao;

        if (window.categoriasModule) {
            categoriasModule.updateCategoriasSelects(transacao.tipo);
        }
        if (window.contasModule) {
            contasModule.updateContasSelects();
        }

        document.getElementById('transacao-conta').value = transacao.conta ? transacao.conta.id : '';
        document.getElementById('transacao-categoria').value = transacao.categoria ? transacao.categoria.id : '';
        document.getElementById('transacao-observacao').value = transacao.observacao || '';

        document.getElementById('modal-transacao-title').textContent = 'Editar Transação';
        document.getElementById('transacao-modo-container').classList.add('hidden');
        document.getElementById('transacao-parcelas-group').classList.add('hidden');
        document.getElementById('transacao-recorrencia-group').classList.add('hidden');

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

        // Obter modo selecionado (Único, Parcelado ou Recorrente)
        const modo = document.querySelector('input[name="transacao-modo"]:checked')?.value || 'UNICO';
        const parcelas = parseInt(document.getElementById('transacao-parcelas')?.value, 10) || 1;
        const frequencia = document.getElementById('transacao-frequencia')?.value || 'MENSAL';
        const diaVencimento = parseInt(document.getElementById('transacao-dia-vencimento')?.value, 10) || 1;
        const dataFim = document.getElementById('transacao-data-fim')?.value || null;

        if (!descricao || isNaN(valor) || valor <= 0 || !tipo || !status || !dataTransacao || !contaId || !categoriaId) {
            showToast('Preencha todos os campos obrigatórios da transação.', 'warning');
            return;
        }

        try {
            if (id) {
                // Edição de transação existente
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
                await api.put(`/transacoes/${id}`, payload);
                showToast('Transação atualizada com sucesso!', 'success');
            } else if (modo === 'PARCELADO' && parcelas > 1) {
                // Criação Parcelada
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
                await api.post(`/transacoes/parcelado?parcelas=${parcelas}`, payload);
                showToast(`Transação parcelada em ${parcelas}x criada com sucesso!`, 'success');
            } else if (modo === 'RECORRENTE') {
                // Criação de Transação Fixa / Recorrente:
                // O backend gera automaticamente as ocorrências retroativas (meses passados) e a previsão para os meses futuros do ano
                const payloadRecorrencia = {
                    descricao,
                    valor,
                    tipo,
                    frequencia,
                    diaVencimento,
                    conta: { id: parseInt(contaId) },
                    categoria: { id: parseInt(categoriaId) },
                    dataInicio: dataTransacao,
                    dataFim,
                    observacao
                };
                await api.post('/recorrencias', payloadRecorrencia);

                showToast('Despesa/Receita fixa cadastrada e histórico/previsão do ano gerados com sucesso!', 'success');
            } else {
                // Criação Simples / Única
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
                await api.post('/transacoes', payload);
                showToast('Transação registrada com sucesso!', 'success');
            }

            closeModal('modal-transacao');
            await this.loadTransacoes();

            if (window.recorrenciasModule) {
                await window.recorrenciasModule.loadRecorrencias();
            }
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
