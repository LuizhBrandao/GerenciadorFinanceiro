package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.controller.dto.DashboardDto;

public interface DashboardService {

    DashboardDto obterDashboard(Long usuarioId);
}
