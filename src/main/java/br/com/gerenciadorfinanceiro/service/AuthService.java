package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.controller.dto.AuthDto;
import br.com.gerenciadorfinanceiro.controller.dto.LoginResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.RegisterDto;

public interface AuthService {

    LoginResponseDto autenticar(AuthDto data);

    void registrar(RegisterDto data);
}
