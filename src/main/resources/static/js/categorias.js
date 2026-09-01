/**
 * Módulo de Gestão de Categorias - Gerenciador Financeiro
 */

const categoriasModule = {
    categorias: [],

    init() {
        this.bindEvents();
    },

    bindEvents() {
        const formCategoria = document.getElementById('form-categoria');
        if (formCategoria) {
            formCategoria.addEventListener('submit', (e) => this.handleSaveCategoria(e));
        }
    },

    async loadCategorias() {
        try {
            const data = await api.get('/categorias');
            this.categorias = data || [];
            this.renderCategoriasList();
            this.updateCategoriasSelects();
            return this.categorias;
        } catch (error) {
            showToast('Erro ao carregar categorias.', 'error');
            return [];
        }
    },

    renderCategoriasList() {
        const container = document.getElementById('categorias-list-container');
        if (!container) return;

        if (this.categorias.length === 0) {
            container.innerHTML = `
                <div class="col-span-full py-12 text-center text-slate-400 bg-white rounded-2xl border border-slate-100 p-8 shadow-sm">
                    <div class="w-16 h-16 mx-auto mb-4 bg-indigo-50 text-indigo-500 rounded-full flex items-center justify-center text-2xl">
                        <i class="fas fa-tags"></i>
                    </div>
                    <h4 class="text-base font-semibold text-slate-700 mb-1">Nenhuma categoria cadastrada</h4>
                    <p class="text-sm text-slate-400 mb-4">Crie categorias personalizadas para organizar seus ganhos e gastos.</p>
                    <button onclick="categoriasModule.openNewCategoriaModal()" class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold rounded-xl transition-colors shadow-sm">
                        <i class="fas fa-plus mr-1.5"></i> Criar Categoria
                    </button>
                </div>
            `;
            return;
        }

        container.innerHTML = this.categorias.map(cat => {
            const isReceita = cat.tipo === 'RECEITA';
            const badgeClass = isReceita ? 'badge-receita' : 'badge-despesa';
            const icon = cat.icone || (isReceita ? 'fa-arrow-down' : 'fa-shopping-bag');

            return `
                <div class="bg-white rounded-2xl p-4 border border-slate-100 shadow-sm hover-card flex items-center justify-between gap-3">
                    <div class="flex items-center gap-3">
                        <div class="w-10 h-10 rounded-xl flex items-center justify-center text-base ${isReceita ? 'text-emerald-600 bg-emerald-50' : 'text-rose-600 bg-rose-50'}">
                            <i class="fas ${icon}"></i>
                        </div>
                        <div>
                            <h4 class="font-bold text-slate-800 text-sm leading-tight">${cat.nome}</h4>
                            <p class="text-xs text-slate-400 font-medium">${cat.descricao || 'Sem descrição'}</p>
                        </div>
                    </div>
                    <div class="flex items-center gap-2">
                        <span class="badge ${badgeClass}">${formatTipoTransacao(cat.tipo)}</span>
                        <button onclick="categoriasModule.excluirCategoria(${cat.id}, '${cat.nome}')" title="Excluir Categoria" class="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors">
                            <i class="fas fa-trash text-xs"></i>
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    },

    getCategoryEmoji(cat) {
        if (!cat) return '🏷️';
        const icone = (cat.icone || '').toLowerCase();
        const nome = (cat.nome || '').toLowerCase();

        if (icone.includes('briefcase') || nome.includes('salário') || nome.includes('salario') || nome.includes('remuneração') || nome.includes('remuneracao')) return '💼';
        if (icone.includes('chart-line') || icone.includes('trending-up') || nome.includes('rendimento') || nome.includes('investimento') || nome.includes('dividendo')) return '📈';
        if (icone.includes('laptop') || nome.includes('freelance') || nome.includes('serviço') || nome.includes('servico')) return '💻';
        if (icone.includes('house') || icone.includes('home') || nome.includes('moradia') || nome.includes('habitação') || nome.includes('habitacao') || nome.includes('aluguel')) return '🏠';
        if (icone.includes('utensils') || icone.includes('cart') || nome.includes('alimentação') || nome.includes('alimentacao') || nome.includes('supermercado') || nome.includes('restaurante')) return '🍽️';
        if (icone.includes('car') || nome.includes('transporte') || nome.includes('mobilidade') || nome.includes('combustível') || nome.includes('combustivel')) return '🚗';
        if (icone.includes('heart') || icone.includes('pulse') || nome.includes('saúde') || nome.includes('saude') || nome.includes('farmácia') || nome.includes('farmacia')) return '💊';
        if (icone.includes('graduation') || icone.includes('book') || nome.includes('educação') || nome.includes('educacao') || nome.includes('desenvolvimento') || nome.includes('curso')) return '📚';
        if (icone.includes('ticket') || icone.includes('tv') || icone.includes('gamepad') || nome.includes('lazer') || nome.includes('entretenimento') || nome.includes('cinema')) return '🎟️';
        if (icone.includes('bag') || icone.includes('shopping') || nome.includes('compras') || nome.includes('cuidados') || nome.includes('vestuário') || nome.includes('vestuario')) return '🛍️';
        if (icone.includes('bolt') || nome.includes('contas básicas') || nome.includes('energia') || nome.includes('luz') || nome.includes('água') || nome.includes('agua')) return '⚡';
        return '🏷️';
    },

    updateCategoriasSelects(filterTipo = null) {
        const selectTransacao = document.getElementById('transacao-categoria');
        const selectFiltro = document.getElementById('filtro-categoria');

        // Lista padrão completa das categorias essenciais
        const categoriasPadrao = [
            { id: 4, nome: 'Moradia & Habitação', tipo: 'DESPESA', icone: 'fa-house' },
            { id: 5, nome: 'Alimentação & Supermercado', tipo: 'DESPESA', icone: 'fa-utensils' },
            { id: 6, nome: 'Transporte & Mobilidade', tipo: 'DESPESA', icone: 'fa-car' },
            { id: 7, nome: 'Saúde & Bem-Estar', tipo: 'DESPESA', icone: 'fa-heart-pulse' },
            { id: 8, nome: 'Educação & Desenvolvimento', tipo: 'DESPESA', icone: 'fa-graduation-cap' },
            { id: 9, nome: 'Lazer & Entretenimento', tipo: 'DESPESA', icone: 'fa-ticket' },
            { id: 10, nome: 'Cuidados Pessoais & Compras', tipo: 'DESPESA', icone: 'fa-bag-shopping' },
            { id: 11, nome: 'Contas Básicas & Energia', tipo: 'DESPESA', icone: 'fa-bolt' },
            { id: 12, nome: 'Outros', tipo: 'DESPESA', icone: 'fa-tag' },
            { id: 1, nome: 'Salário / Remuneração', tipo: 'RECEITA', icone: 'fa-briefcase' },
            { id: 2, nome: 'Rendimentos & Investimentos', tipo: 'RECEITA', icone: 'fa-chart-line' },
            { id: 3, nome: 'Freelance & Serviços Extras', tipo: 'RECEITA', icone: 'fa-laptop' },
            { id: 13, nome: 'Outras Receitas', tipo: 'RECEITA', icone: 'fa-tag' }
        ];

        const categoriasDisponiveis = this.categorias.length > 0 ? this.categorias : categoriasPadrao;

        const despesas = categoriasDisponiveis.filter(c => c.tipo === 'DESPESA');
        const receitas = categoriasDisponiveis.filter(c => c.tipo === 'RECEITA');

        // 1. Atualizar Select do Modal de Transação
        if (selectTransacao) {
            const currentValue = selectTransacao.value;
            let html = '<option value="">Selecione uma categoria</option>';

            if (filterTipo === 'DESPESA') {
                despesas.forEach(c => {
                    const emoji = this.getCategoryEmoji(c);
                    html += `<option value="${c.id}">${emoji} ${c.nome}</option>`;
                });
            } else if (filterTipo === 'RECEITA') {
                receitas.forEach(c => {
                    const emoji = this.getCategoryEmoji(c);
                    html += `<option value="${c.id}">${emoji} ${c.nome}</option>`;
                });
            } else {
                if (despesas.length > 0) {
                    html += '<optgroup label="💳 Despesas">';
                    despesas.forEach(c => {
                        const emoji = this.getCategoryEmoji(c);
                        html += `<option value="${c.id}">${emoji} ${c.nome}</option>`;
                    });
                    html += '</optgroup>';
                }
                if (receitas.length > 0) {
                    html += '<optgroup label="💰 Receitas">';
                    receitas.forEach(c => {
                        const emoji = this.getCategoryEmoji(c);
                        html += `<option value="${c.id}">${emoji} ${c.nome}</option>`;
                    });
                    html += '</optgroup>';
                }
            }

            selectTransacao.innerHTML = html;
            if (currentValue) selectTransacao.value = currentValue;
        }

        // 2. Atualizar Select do Filtro de Transações
        if (selectFiltro) {
            const currentFilterValue = selectFiltro.value;
            let filterHtml = '<option value="">Todas as Categorias</option>';

            filterHtml += '<optgroup label="💳 Despesas">';
            despesas.forEach(c => {
                const emoji = this.getCategoryEmoji(c);
                filterHtml += `<option value="${c.id}">${emoji} ${c.nome}</option>`;
            });
            filterHtml += '</optgroup>';

            filterHtml += '<optgroup label="💰 Receitas">';
            receitas.forEach(c => {
                const emoji = this.getCategoryEmoji(c);
                filterHtml += `<option value="${c.id}">${emoji} ${c.nome}</option>`;
            });
            filterHtml += '</optgroup>';

            selectFiltro.innerHTML = filterHtml;
            if (currentFilterValue) selectFiltro.value = currentFilterValue;
        }
    },

    openNewCategoriaModal() {
        const form = document.getElementById('form-categoria');
        if (form) form.reset();
        openModal('modal-categoria');
    },

    async handleSaveCategoria(e) {
        e.preventDefault();
        const nome = document.getElementById('categoria-nome').value.trim();
        const tipo = document.getElementById('categoria-tipo').value;
        const descricao = document.getElementById('categoria-descricao').value.trim();
        const icone = document.getElementById('categoria-icone').value.trim() || 'fa-tag';

        if (!nome || !tipo) {
            showToast('Preencha os campos obrigatórios da categoria.', 'warning');
            return;
        }

        try {
            await api.post('/categorias', {
                nome,
                tipo,
                descricao,
                icone
            });

            showToast('Categoria criada com sucesso!', 'success');
            closeModal('modal-categoria');
            await this.loadCategorias();
        } catch (error) {
            showToast(error.message || 'Erro ao criar categoria.', 'error');
        }
    },

    async excluirCategoria(id, nome) {
        if (!confirm(`Deseja realmente excluir a categoria "${nome}"?`)) {
            return;
        }

        try {
            await api.delete(`/categorias/${id}`);
            showToast('Categoria excluída com sucesso.', 'success');
            await this.loadCategorias();
        } catch (error) {
            showToast(error.message || 'Erro ao excluir categoria.', 'error');
        }
    }
};

window.categoriasModule = categoriasModule;
