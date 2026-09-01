/**
 * Módulo de Gestão de Contas - Gerenciador Financeiro
 */

const contasModule = {
    contas: [],

    init() {
        this.bindEvents();
    },

    bindEvents() {
        const formConta = document.getElementById('form-conta');
        const formTransferencia = document.getElementById('form-transferencia');

        if (formConta) {
            formConta.addEventListener('submit', (e) => this.handleSaveConta(e));
        }

        if (formTransferencia) {
            formTransferencia.addEventListener('submit', (e) => this.handleTransferencia(e));
        }
    },

    async loadContas() {
        try {
            const [contasData, resumoData] = await Promise.all([
                api.get('/contas').catch(() => []),
                api.get('/contas/resumo').catch(() => null)
            ]);

            this.contas = contasData || [];
            this.updateResumoSaldos(resumoData);
            this.renderContasCards();
            this.updateContasSelects();
            return this.contas;
        } catch (error) {
            showToast('Erro ao carregar contas.', 'error');
            return [];
        }
    },

    updateResumoSaldos(resumo) {
        let saldoCorrentes = 0;
        let saldoInvestimentos = 0;
        let patrimonioTotal = 0;

        if (resumo) {
            saldoCorrentes = parseFloat(resumo.saldoContasCorrentes) || 0;
            saldoInvestimentos = parseFloat(resumo.saldoInvestimentos) || 0;
            patrimonioTotal = parseFloat(resumo.patrimonioTotal) || 0;
        } else {
            this.contas.forEach(c => {
                const s = parseFloat(c.saldo) || 0;
                if (isContaInvestimentoOuReserva(c.tipoConta)) {
                    saldoInvestimentos += s;
                } else {
                    saldoCorrentes += s;
                }
            });
            patrimonioTotal = saldoCorrentes + saldoInvestimentos;
        }

        const elResumoCorrente = document.getElementById('contas-resumo-corrente');
        const elResumoInvestimento = document.getElementById('contas-resumo-investimento');
        const elResumoPatrimonio = document.getElementById('contas-resumo-patrimonio');
        const elHeaderSaldo = document.getElementById('header-saldo-rapido');
        const elHeaderInvestimentos = document.getElementById('header-investimentos-rapido');

        if (elResumoCorrente) elResumoCorrente.textContent = formatCurrency(saldoCorrentes);
        if (elResumoInvestimento) elResumoInvestimento.textContent = formatCurrency(saldoInvestimentos);
        if (elResumoPatrimonio) elResumoPatrimonio.textContent = formatCurrency(patrimonioTotal);
        if (elHeaderSaldo) elHeaderSaldo.textContent = formatCurrency(saldoCorrentes);
        if (elHeaderInvestimentos) elHeaderInvestimentos.textContent = formatCurrency(saldoInvestimentos);
    },

    renderContasCards() {
        const container = document.getElementById('contas-list-container');
        if (!container) return;

        if (this.contas.length === 0) {
            container.innerHTML = `
                <div class="col-span-full py-12 text-center text-slate-400 bg-white rounded-2xl border border-slate-100 p-8 shadow-sm">
                    <div class="w-16 h-16 mx-auto mb-4 bg-indigo-50 text-indigo-500 rounded-full flex items-center justify-center text-2xl">
                        <i class="fas fa-wallet"></i>
                    </div>
                    <h4 class="text-base font-semibold text-slate-700 mb-1">Nenhuma conta cadastrada</h4>
                    <p class="text-sm text-slate-400 mb-4">Adicione suas contas bancárias, carteira física ou investimentos para começar.</p>
                    <button onclick="contasModule.openNewContaModal()" class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold rounded-xl transition-colors shadow-sm">
                        <i class="fas fa-plus mr-1.5"></i> Adicionar Nova Conta
                    </button>
                </div>
            `;
            return;
        }

        const tipoIcons = {
            'CORRENTE': 'fa-credit-card text-blue-500 bg-blue-50',
            'POUPANCA': 'fa-piggy-bank text-purple-500 bg-purple-50',
            'INVESTIMENTO': 'fa-chart-line text-purple-500 bg-purple-50',
            'CARTEIRA': 'fa-wallet text-amber-500 bg-amber-50'
        };

        const renderCard = (conta) => {
            const iconClass = tipoIcons[conta.tipoConta] || 'fa-university text-indigo-500 bg-indigo-50';
            const saldoColor = (conta.saldo >= 0) ? 'text-slate-800' : 'text-rose-600';

            return `
                <div class="bg-white rounded-2xl p-5 border border-slate-100 shadow-sm hover-card relative overflow-hidden flex flex-col justify-between">
                    <div class="flex items-start justify-between mb-4">
                        <div class="flex items-center gap-3">
                            <div class="w-11 h-11 rounded-xl flex items-center justify-center text-lg ${iconClass}">
                                <i class="fas ${iconClass.split(' ')[0]}"></i>
                            </div>
                            <div>
                                <h3 class="font-bold text-slate-800 text-base leading-tight">${conta.nome}</h3>
                                <p class="text-xs text-slate-400 font-medium">${conta.instituicaoFinanceira || 'Instituição não informada'}</p>
                            </div>
                        </div>
                        <div class="flex items-center gap-1">
                            <button onclick="contasModule.verExtrato(${conta.id})" title="Ver Extrato" class="p-1.5 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors">
                                <i class="fas fa-receipt text-xs"></i>
                            </button>
                            <button onclick="contasModule.openEditContaModal(${conta.id})" title="Editar Conta" class="p-1.5 text-slate-400 hover:text-amber-600 hover:bg-amber-50 rounded-lg transition-colors">
                                <i class="fas fa-edit text-xs"></i>
                            </button>
                            <button onclick="contasModule.excluirConta(${conta.id}, '${conta.nome}')" title="Excluir Conta" class="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors">
                                <i class="fas fa-trash text-xs"></i>
                            </button>
                        </div>
                    </div>

                    <div>
                        <div class="flex items-center justify-between text-xs text-slate-400 mb-1">
                            <span>Saldo Atual</span>
                            <span class="font-semibold text-slate-500">${formatTipoConta(conta.tipoConta)}</span>
                        </div>
                        <div class="text-2xl font-black ${saldoColor} tracking-tight">
                            ${formatCurrency(conta.saldo)}
                        </div>
                    </div>
                </div>
            `;
        };

        // Separar contas em 2 contextos financeiros
        const contasCorrentes = this.contas.filter(c => !isContaInvestimentoOuReserva(c.tipoConta));
        const contasInvestimentos = this.contas.filter(c => isContaInvestimentoOuReserva(c.tipoConta));

        const subtotalCorrentes = contasCorrentes.reduce((acc, c) => acc + (parseFloat(c.saldo) || 0), 0);
        const subtotalInvestimentos = contasInvestimentos.reduce((acc, c) => acc + (parseFloat(c.saldo) || 0), 0);

        let html = '';

        // Seção 1: Contas Correntes & Carteiras (Caixa Operacional)
        html += `
            <div class="space-y-3">
                <div class="flex items-center justify-between">
                    <div class="flex items-center gap-2">
                        <span class="w-3 h-3 rounded-full bg-blue-500"></span>
                        <h3 class="text-base font-extrabold text-slate-800 tracking-tight">Contas Correntes & Carteiras (Caixa Operacional)</h3>
                        <span class="text-xs font-semibold px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">${contasCorrentes.length}</span>
                    </div>
                    <div class="text-xs font-bold text-slate-500">
                        Subtotal Disponível: <span class="text-slate-800 font-extrabold">${formatCurrency(subtotalCorrentes)}</span>
                    </div>
                </div>
                ${contasCorrentes.length > 0 
                    ? `<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">${contasCorrentes.map(renderCard).join('')}</div>`
                    : `<div class="p-5 bg-white rounded-2xl border border-slate-100 text-center text-slate-400 text-xs font-medium">Nenhuma conta corrente ou carteira cadastrada.</div>`
                }
            </div>
        `;

        // Seção 2: Investimentos & Poupança (Patrimônio Alocado & Reservas)
        html += `
            <div class="space-y-3 pt-2">
                <div class="flex items-center justify-between">
                    <div class="flex items-center gap-2">
                        <span class="w-3 h-3 rounded-full bg-purple-500"></span>
                        <h3 class="text-base font-extrabold text-slate-800 tracking-tight">Investimentos & Poupança (Patrimônio & Reservas)</h3>
                        <span class="text-xs font-semibold px-2 py-0.5 rounded-full bg-purple-50 text-purple-700">${contasInvestimentos.length}</span>
                    </div>
                    <div class="text-xs font-bold text-purple-600">
                        Total Investido / Aplicado: <span class="text-purple-700 font-extrabold">${formatCurrency(subtotalInvestimentos)}</span>
                    </div>
                </div>
                ${contasInvestimentos.length > 0 
                    ? `<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">${contasInvestimentos.map(renderCard).join('')}</div>`
                    : `<div class="p-5 bg-white rounded-2xl border border-slate-100 text-center text-slate-400 text-xs font-medium">Nenhuma conta de investimento ou poupança cadastrada. Clique em "Nova Conta" para cadastrar.</div>`
                }
            </div>
        `;

        container.innerHTML = html;
    },

    updateContasSelects() {
        const selects = [
            document.getElementById('transacao-conta'),
            document.getElementById('filtro-conta'),
            document.getElementById('transferencia-origem'),
            document.getElementById('transferencia-destino')
        ];

        selects.forEach(select => {
            if (!select) return;
            const currentValue = select.value;
            const isFilter = select.id === 'filtro-conta';

            let html = isFilter ? '<option value="">Todas as Contas</option>' : '<option value="">Selecione uma conta</option>';

            // Separar opções por grupo no select
            const correntes = this.contas.filter(c => !isContaInvestimentoOuReserva(c.tipoConta));
            const investimentos = this.contas.filter(c => isContaInvestimentoOuReserva(c.tipoConta));

            if (correntes.length > 0) {
                html += `<optgroup label="Contas Correntes & Carteiras">`;
                correntes.forEach(c => {
                    html += `<option value="${c.id}">${c.nome} (${formatCurrency(c.saldo)})</option>`;
                });
                html += `</optgroup>`;
            }

            if (investimentos.length > 0) {
                html += `<optgroup label="Investimentos & Poupança">`;
                investimentos.forEach(c => {
                    html += `<option value="${c.id}">${c.nome} (${formatCurrency(c.saldo)})</option>`;
                });
                html += `</optgroup>`;
            }

            select.innerHTML = html;
            if (currentValue) select.value = currentValue;
        });
    },

    openNewContaModal() {
        const form = document.getElementById('form-conta');
        if (form) form.reset();
        document.getElementById('conta-id').value = '';
        document.getElementById('modal-conta-title').textContent = 'Nova Conta Bancária';
        document.getElementById('conta-saldo-group').classList.remove('hidden');
        openModal('modal-conta');
    },

    openEditContaModal(id) {
        const conta = this.contas.find(c => c.id === id);
        if (!conta) return;

        document.getElementById('conta-id').value = conta.id;
        document.getElementById('conta-nome').value = conta.nome;
        document.getElementById('conta-instituicao').value = conta.instituicaoFinanceira || '';
        document.getElementById('conta-tipo').value = conta.tipoConta;
        document.getElementById('conta-saldo-group').classList.add('hidden'); // saldo inicial não é editável diretamente aqui
        document.getElementById('modal-conta-title').textContent = 'Editar Conta';

        openModal('modal-conta');
    },

    async handleSaveConta(e) {
        e.preventDefault();
        const id = document.getElementById('conta-id').value;
        const nome = document.getElementById('conta-nome').value.trim();
        const instituicaoFinanceira = document.getElementById('conta-instituicao').value.trim();
        const tipoConta = document.getElementById('conta-tipo').value;
        const saldoInicial = parseFloat(document.getElementById('conta-saldo').value) || 0;

        if (!nome || !tipoConta) {
            showToast('Preencha os campos obrigatórios da conta.', 'warning');
            return;
        }

        try {
            if (id) {
                // Atualizar conta existente
                await api.put(`/contas/${id}`, {
                    nome,
                    instituicaoFinanceira,
                    tipoConta
                });
                showToast('Conta atualizada com sucesso!', 'success');
            } else {
                // Criar nova conta
                await api.post('/contas', {
                    nome,
                    instituicaoFinanceira,
                    tipoConta,
                    saldo: saldoInicial
                });
                showToast('Conta criada com sucesso!', 'success');
            }

            closeModal('modal-conta');
            await this.loadContas();
            if (window.dashboardModule) {
                window.dashboardModule.loadSummary();
            }
        } catch (error) {
            showToast(error.message || 'Erro ao salvar conta.', 'error');
        }
    },

    async excluirConta(id, nome) {
        if (!confirm(`Deseja realmente excluir a conta "${nome}"? Transações vinculadas podem ser afetadas.`)) {
            return;
        }

        try {
            await api.delete(`/contas/${id}`);
            showToast('Conta excluída com sucesso.', 'success');
            await this.loadContas();
            if (window.dashboardModule) {
                window.dashboardModule.loadSummary();
            }
        } catch (error) {
            showToast(error.message || 'Erro ao excluir conta.', 'error');
        }
    },

    openTransferenciaModal() {
        if (this.contas.length < 2) {
            showToast('Você precisa de pelo menos 2 contas para realizar transferências.', 'warning');
            return;
        }
        document.getElementById('form-transferencia').reset();
        openModal('modal-transferencia');
    },

    async handleTransferencia(e) {
        e.preventDefault();
        const origem = document.getElementById('transferencia-origem').value;
        const destino = document.getElementById('transferencia-destino').value;
        const valor = parseFloat(document.getElementById('transferencia-valor').value);

        if (!origem || !destino || isNaN(valor) || valor <= 0) {
            showToast('Preencha os dados da transferência corretamente.', 'warning');
            return;
        }

        if (origem === destino) {
            showToast('As contas de origem e destino devem ser diferentes.', 'warning');
            return;
        }

        try {
            await api.post(`/contas/transferir?origem=${origem}&destino=${destino}&valor=${valor}`);
            showToast('Transferência realizada com sucesso!', 'success');
            closeModal('modal-transferencia');
            await this.loadContas();
            if (window.dashboardModule) {
                window.dashboardModule.loadSummary();
            }
        } catch (error) {
            showToast(error.message || 'Erro ao realizar transferência.', 'error');
        }
    },

    async verExtrato(contaId) {
        const conta = this.contas.find(c => c.id === contaId);
        const titleEl = document.getElementById('extrato-modal-title');
        const container = document.getElementById('extrato-list-container');

        if (titleEl) {
            titleEl.textContent = `Extrato - ${conta ? conta.nome : 'Conta'}`;
        }

        if (container) {
            container.innerHTML = '<div class="py-8 text-center text-slate-400"><div class="spinner border-indigo-600 border-t-transparent mx-auto mb-2"></div>Carregando extrato...</div>';
        }

        openModal('modal-extrato');

        try {
            const extrato = await api.get(`/contas/${contaId}/extrato`);
            if (!container) return;

            if (!extrato || extrato.length === 0) {
                container.innerHTML = '<p class="py-8 text-center text-slate-400 text-sm">Nenhuma movimentação registrada nesta conta.</p>';
                return;
            }

            container.innerHTML = `
                <div class="divide-y divide-slate-100 max-h-96 overflow-y-auto">
                    ${extrato.map(t => {
                        const isReceita = t.tipo === 'RECEITA';
                        const valorFormatted = `${isReceita ? '+' : '-'} ${formatCurrency(t.valor)}`;
                        const colorClass = isReceita ? 'text-emerald-600' : 'text-rose-600';
                        const iconClass = isReceita ? 'fa-arrow-down text-emerald-500 bg-emerald-50' : 'fa-arrow-up text-rose-500 bg-rose-50';

                        return `
                            <div class="py-3 flex items-center justify-between gap-4">
                                <div class="flex items-center gap-3">
                                    <div class="w-9 h-9 rounded-xl flex items-center justify-center text-sm ${iconClass}">
                                        <i class="fas ${iconClass.split(' ')[0]}"></i>
                                    </div>
                                    <div>
                                        <p class="text-sm font-semibold text-slate-800">${t.descricao}</p>
                                        <p class="text-xs text-slate-400">${formatDate(t.dataTransacao)} • ${t.categoria ? t.categoria.nome : 'Geral'}</p>
                                    </div>
                                </div>
                                <div class="text-right">
                                    <p class="text-sm font-bold ${colorClass}">${valorFormatted}</p>
                                    <span class="badge badge-status-${t.status.toLowerCase()}">${formatStatusTransacao(t.status)}</span>
                                </div>
                            </div>
                        `;
                    }).join('')}
                </div>
            `;
        } catch (error) {
            if (container) {
                container.innerHTML = '<p class="py-8 text-center text-rose-500 text-sm">Erro ao carregar extrato da conta.</p>';
            }
        }
    }
};

window.contasModule = contasModule;
