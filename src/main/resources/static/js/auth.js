/**
 * Módulo de Autenticação - Gerenciador Financeiro
 */

const authModule = {
    init() {
        this.bindEvents();
        this.checkAuthStatus();
    },

    bindEvents() {
        const loginForm = document.getElementById('login-form');
        const registerForm = document.getElementById('register-form');
        const showRegisterBtn = document.getElementById('btn-show-register');
        const showLoginBtn = document.getElementById('btn-show-login');
        const logoutBtn = document.getElementById('btn-logout');

        if (loginForm) {
            loginForm.addEventListener('submit', (e) => this.handleLogin(e));
        }

        if (registerForm) {
            registerForm.addEventListener('submit', (e) => this.handleRegister(e));
        }

        if (showRegisterBtn) {
            showRegisterBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.toggleAuthView('register');
            });
        }

        if (showLoginBtn) {
            showLoginBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.toggleAuthView('login');
            });
        }

        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => this.logout());
        }
    },

    toggleAuthView(view) {
        const loginCard = document.getElementById('auth-login-card');
        const registerCard = document.getElementById('auth-register-card');

        if (view === 'register') {
            loginCard.classList.add('hidden');
            registerCard.classList.remove('hidden');
        } else {
            registerCard.classList.add('hidden');
            loginCard.classList.remove('hidden');
        }
    },

    async handleLogin(e) {
        e.preventDefault();
        const email = document.getElementById('login-email').value.trim();
        const senha = document.getElementById('login-password').value;
        const submitBtn = e.target.querySelector('button[type="submit"]');

        if (!email || !senha) {
            showToast('Preencha todos os campos.', 'warning');
            return;
        }

        this.setButtonLoading(submitBtn, true, 'Entrando...');

        try {
            const data = await api.post('/auth/login', { email, senha });
            if (data && data.token) {
                api.setToken(data.token);
                // Armazena email do usuário logado
                localStorage.setItem('gf_user', JSON.stringify({ email }));
                showToast('Login realizado com sucesso!', 'success');
                this.showAppSection();
                if (window.app) {
                    window.app.loadDashboardData();
                }
            } else {
                showToast('Falha ao autenticar. Verifique seus dados.', 'error');
            }
        } catch (error) {
            showToast(error.message || 'Erro ao realizar login.', 'error');
        } finally {
            this.setButtonLoading(submitBtn, false, 'Entrar');
        }
    },

    async handleRegister(e) {
        e.preventDefault();
        const nome = document.getElementById('register-name').value.trim();
        const email = document.getElementById('register-email').value.trim();
        const senha = document.getElementById('register-password').value;
        const submitBtn = e.target.querySelector('button[type="submit"]');

        if (!nome || !email || !senha) {
            showToast('Preencha todos os campos.', 'warning');
            return;
        }

        if (senha.length < 6) {
            showToast('A senha deve ter pelo menos 6 caracteres.', 'warning');
            return;
        }

        this.setButtonLoading(submitBtn, true, 'Cadastrando...');

        try {
            await api.post('/auth/register', { nome, email, senha });
            showToast('Cadastro realizado com sucesso! Faça seu login.', 'success');
            document.getElementById('register-form').reset();
            this.toggleAuthView('login');
            // Preenche o email cadastrado na tela de login
            document.getElementById('login-email').value = email;
            document.getElementById('login-password').focus();
        } catch (error) {
            showToast(error.message || 'Erro ao realizar cadastro.', 'error');
        } finally {
            this.setButtonLoading(submitBtn, false, 'Criar Conta');
        }
    },

    logout() {
        api.clearToken();
        showToast('Sessão finalizada com sucesso.', 'info');
        this.showAuthSection();
    },

    checkAuthStatus() {
        const token = api.getToken();
        if (token) {
            this.showAppSection();
        } else {
            this.showAuthSection();
        }
    },

    showAuthSection() {
        document.getElementById('auth-section').classList.remove('hidden');
        document.getElementById('app-section').classList.add('hidden');
        this.toggleAuthView('login');
    },

    showAppSection() {
        document.getElementById('auth-section').classList.add('hidden');
        document.getElementById('app-section').classList.remove('hidden');

        // Atualizar informações do usuário na barra superior
        const storedUser = localStorage.getItem('gf_user');
        if (storedUser) {
            try {
                const user = JSON.parse(storedUser);
                const userDisplay = document.getElementById('user-email-display');
                if (userDisplay) {
                    userDisplay.textContent = user.email || 'Usuário';
                }
            } catch (e) {}
        }
    },

    setButtonLoading(button, isLoading, text) {
        if (!button) return;
        if (isLoading) {
            button.disabled = true;
            button.dataset.originalText = button.innerHTML;
            button.innerHTML = `<span class="inline-flex items-center gap-2"><div class="spinner"></div> ${text}</span>`;
        } else {
            button.disabled = false;
            button.innerHTML = button.dataset.originalText || text;
        }
    }
};

window.authModule = authModule;
