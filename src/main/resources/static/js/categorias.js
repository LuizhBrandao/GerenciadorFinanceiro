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

    updateCategoriasSelects() {
        const selects = [
            document.getElementById('transacao-categoria'),
            document.getElementById('filtro-categoria')
        ];

        selects.forEach(select => {
            if (!select) return;
            const currentValue = select.value;
            const isFilter = select.id === 'filtro-categoria';

            let html = isFilter ? '<option value="">Todas as Categorias</option>' : '<option value="">Selecione uma categoria</option>';

            this.categorias.forEach(c => {
                html += `<option value="${c.id}">${c.nome} (${formatTipoTransacao(c.tipo)})</option>`;
            });

            select.innerHTML = html;
            if (currentValue) select.value = currentValue;
        });
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
