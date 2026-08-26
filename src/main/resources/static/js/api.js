/**
 * Cliente HTTP API - Gerenciador Financeiro
 */

const API_BASE = (window.location.port === '8080' || window.location.protocol === 'file:') 
    ? (window.location.protocol === 'file:' ? 'http://localhost:8080' : '') 
    : 'http://localhost:8080';

const api = {
    // Obter Token do LocalStorage
    getToken() {
        return localStorage.getItem('gf_token');
    },

    // Salvar Token
    setToken(token) {
        localStorage.setItem('gf_token', token);
    },

    // Remover Token
    clearToken() {
        localStorage.removeItem('gf_token');
        localStorage.removeItem('gf_user');
    },

    // Executar requisição genérica com headers de autorização e tratamento de erros
    async request(endpoint, options = {}) {
        const url = `${API_BASE}${endpoint}`;
        const headers = {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        };

        const token = this.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            ...options,
            headers
        };

        try {
            const response = await fetch(url, config);

            // Trata expiração ou falta de autorização (401 / 403)
            if (response.status === 401 || response.status === 403) {
                if (token && !endpoint.includes('/auth/login')) {
                    this.clearToken();
                    showToast('Sessão expirada. Faça login novamente.', 'warning');
                    if (window.authModule) {
                        window.authModule.showAuthSection();
                    }
                }
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || 'Não autorizado ou credenciais inválidas.');
            }

            // Resposta sem conteúdo (ex: 204 No Content ou 200 OK sem body)
            if (response.status === 204 || response.headers.get('content-length') === '0') {
                return null;
            }

            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const data = await response.json();
                if (!response.ok) {
                    throw new Error(data.message || data.error || 'Erro na requisição.');
                }
                return data;
            }

            if (!response.ok) {
                const text = await response.text();
                throw new Error(text || `Erro ${response.status}: ${response.statusText}`);
            }

            return await response.text();
        } catch (error) {
            console.error(`Erro na requisição ${endpoint}:`, error);
            throw error;
        }
    },

    // Atalhos HTTP
    get(endpoint, options = {}) {
        return this.request(endpoint, { method: 'GET', ...options });
    },

    post(endpoint, body, options = {}) {
        return this.request(endpoint, {
            method: 'POST',
            body: JSON.stringify(body),
            ...options
        });
    },

    put(endpoint, body, options = {}) {
        return this.request(endpoint, {
            method: 'PUT',
            body: JSON.stringify(body),
            ...options
        });
    },

    delete(endpoint, options = {}) {
        return this.request(endpoint, { method: 'DELETE', ...options });
    }
};

window.api = api;
