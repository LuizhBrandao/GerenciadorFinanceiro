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
            const data = await api.get('/contas');
            this.contas = data || [];
            this.renderContasCards();
            this.updateContasSelects();
            return this.contas;
        } catch (error) {
            showToast('Erro ao carregar contas.', 'error');
            return [];
        }
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
            'POUPANCA': 'fa-piggy-bank text-emerald-500 bg-emerald-50',
            'INVESTIMENTO': 'fa-chart-line text-purple-500 bg-purple-50',
            'CARTEIRA': 'fa-wallet text-amber-500 bg-amber-50'
        };

        container.innerHTML = this.contas.map(conta => {
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
        }).join('');
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

            this.contas.forEach(c => {
                html += `<option value="${c.id}">${c.nome} (${formatCurrency(c.saldo)})</option>`;
            });

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
