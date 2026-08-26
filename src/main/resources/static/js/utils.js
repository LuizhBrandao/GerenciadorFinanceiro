/**
 * Utilitários e Formatação - Gerenciador Financeiro
 */

// Formata valores numéricos para a moeda brasileira (BRL)
function formatCurrency(value) {
    if (value === null || value === undefined || isNaN(value)) {
        return 'R$ 0,00';
    }
    return new Intl.NumberFormat('pt-BR', {
        style: 'currency',
        currency: 'BRL'
    }).format(value);
}

// Formata data ISO (YYYY-MM-DD ou YYYY-MM-DDTHH:mm:ss) para DD/MM/AAAA
function formatDate(dateString) {
    if (!dateString) return '-';
    // Se vier YYYY-MM-DD, divide para evitar problemas de fuso horário
    const parts = dateString.split('T')[0].split('-');
    if (parts.length === 3) {
        return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }
    const d = new Date(dateString);
    return isNaN(d.getTime()) ? dateString : d.toLocaleDateString('pt-BR');
}

// Obter a data atual no formato YYYY-MM-DD para campos <input type="date">
function getTodayIsoDate() {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

// Formatação legível dos Enums do backend
function formatTipoConta(tipo) {
    const map = {
        'CORRENTE': 'Conta Corrente',
        'POUPANCA': 'Conta Poupança',
        'INVESTIMENTO': 'Investimento',
        'CARTEIRA': 'Carteira / Dinheiro'
    };
    return map[tipo] || tipo || 'Outro';
}

function formatTipoTransacao(tipo) {
    const map = {
        'RECEITA': 'Receita',
        'DESPESA': 'Despesa'
    };
    return map[tipo] || tipo;
}

function formatStatusTransacao(status) {
    const map = {
        'PAGA': 'Paga',
        'PENDENTE': 'Pendente',
        'ATRASADA': 'Atrasada',
        'CANCELADA': 'Cancelada'
    };
    return map[status] || status;
}

function formatFrequenciaRecorrencia(freq) {
    const map = {
        'MENSAL': 'Mensal',
        'SEMANAL': 'Semanal',
        'QUINZENAL': 'Quinzenal',
        'ANUAL': 'Anual',
        'DIARIA': 'Diária'
    };
    return map[freq] || freq || 'Mensal';
}

// Sistema de Notificações Toast
function showToast(message, type = 'info', duration = 3500) {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    const icons = {
        success: 'fa-check-circle text-emerald-500',
        error: 'fa-exclamation-circle text-rose-500',
        warning: 'fa-exclamation-triangle text-amber-500',
        info: 'fa-info-circle text-sky-500'
    };

    const iconClass = icons[type] || icons.info;

    toast.innerHTML = `
        <i class="fas ${iconClass} text-lg"></i>
        <div class="text-sm font-medium text-slate-800 flex-1 leading-snug">${message}</div>
        <button class="text-slate-400 hover:text-slate-600 transition-colors ml-2" onclick="this.parentElement.remove()">
            <i class="fas fa-times text-xs"></i>
        </button>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'fadeOut 0.3s forwards';
        setTimeout(() => {
            if (toast.parentElement) toast.remove();
        }, 300);
    }, duration);
}

// Controle de Abertura e Fechamento de Modais
function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('hidden');
        // Força reflow para animação CSS
        setTimeout(() => {
            modal.classList.add('active');
        }, 10);
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('active');
        setTimeout(() => {
            modal.classList.add('hidden');
        }, 200);
    }
}
