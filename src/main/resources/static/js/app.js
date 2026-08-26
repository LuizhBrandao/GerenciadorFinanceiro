/**
 * Aplicação Principal (Orquestrador SPA) - Gerenciador Financeiro
 */

const app = {
    currentView: 'dashboard',

    init() {
        // Inicializar módulos
        authModule.init();
        contasModule.init();
        categoriasModule.init();
        transacoesModule.init();
        recorrenciasModule.init();
        dashboardModule.init();

        this.bindNavigation();
        this.bindGlobalModals();

        // Se já estiver logado, carregar os dados
        if (api.getToken()) {
            this.loadDashboardData();
        }
    },

    bindNavigation() {
        const navLinks = document.querySelectorAll('.nav-item');
        navLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const view = link.dataset.view;
                if (view) {
                    this.navigateTo(view);
                }
            });
        });

        // Mobile sidebar toggle
        const mobileToggle = document.getElementById('btn-mobile-menu');
        const sidebar = document.getElementById('app-sidebar');
        if (mobileToggle && sidebar) {
            mobileToggle.addEventListener('click', () => {
                sidebar.classList.toggle('-translate-x-full');
            });
        }
    },

    bindGlobalModals() {
        // Fechar modais ao clicar no backdrop ou botão de fechar
        document.querySelectorAll('.modal').forEach(modal => {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    closeModal(modal.id);
                }
            });

            const closeButtons = modal.querySelectorAll('[data-close-modal]');
            closeButtons.forEach(btn => {
                btn.addEventListener('click', () => {
                    closeModal(modal.id);
                });
            });
        });
    },

    navigateTo(viewName) {
        this.currentView = viewName;

        // Atualiza estilo dos links de navegação
        document.querySelectorAll('.nav-item').forEach(link => {
            if (link.dataset.view === viewName) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });

        // Alterna seções de conteúdo
        document.querySelectorAll('.view-section').forEach(section => {
            if (section.id === `view-${viewName}`) {
                section.classList.remove('hidden');
            } else {
                section.classList.add('hidden');
            }
        });

        // Fechar sidebar no mobile após navegar
        const sidebar = document.getElementById('app-sidebar');
        if (sidebar && window.innerWidth < 1024) {
            sidebar.classList.add('-translate-x-full');
        }

        // Carrega dados específicos da tela
        switch (viewName) {
            case 'dashboard':
                this.loadDashboardData();
                break;
            case 'transacoes':
                transacoesModule.loadTransacoes();
                contasModule.loadContas();
                categoriasModule.loadCategorias();
                break;
            case 'recorrencias':
                contasModule.loadContas();
                categoriasModule.loadCategorias();
                recorrenciasModule.loadRecorrencias();
                break;
            case 'contas':
                contasModule.loadContas();
                break;
            case 'categorias':
                categoriasModule.loadCategorias();
                break;
        }
    },

    async loadDashboardData() {
        await Promise.all([
            contasModule.loadContas(),
            categoriasModule.loadCategorias()
        ]);
        await dashboardModule.loadSummary();
    }
};

window.app = app;

// Iniciar a aplicação quando o DOM estiver pronto
document.addEventListener('DOMContentLoaded', () => {
    app.init();
});
