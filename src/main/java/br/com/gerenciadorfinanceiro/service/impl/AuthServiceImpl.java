package br.com.gerenciadorfinanceiro.service.impl;

import br.com.gerenciadorfinanceiro.config.TokenService;
import br.com.gerenciadorfinanceiro.controller.dto.AuthDto;
import br.com.gerenciadorfinanceiro.controller.dto.LoginResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.RegisterDto;
import br.com.gerenciadorfinanceiro.exception.RegraNegocioException;
import br.com.gerenciadorfinanceiro.model.PreferenciasUsuario;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.repository.UsuarioRepository;
import br.com.gerenciadorfinanceiro.service.AuthService;
import br.com.gerenciadorfinanceiro.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final CategoriaService categoriaService;

    @Override
    public LoginResponseDto autenticar(AuthDto data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.senha());
        var auth = this.authenticationManager.authenticate(usernamePassword);
        var token = tokenService.generateToken((Usuario) auth.getPrincipal());
        return new LoginResponseDto(token);
    }

    @Override
    @Transactional
    public void registrar(RegisterDto data) {
        if (this.usuarioRepository.existsByEmail(data.email())) {
            throw new RegraNegocioException("E-mail já cadastrado no sistema.");
        }

        String encryptedPassword = passwordEncoder.encode(data.senha());
        Usuario newUser = new Usuario(data.nome(), data.email(), encryptedPassword);

        PreferenciasUsuario preferencias = new PreferenciasUsuario(newUser);
        newUser.setPreferencias(preferencias);

        Usuario salvo = this.usuarioRepository.save(newUser);

        // Cria o catálogo de categorias essenciais automaticamente para o novo usuário
        this.categoriaService.inicializarCategoriasPadrao(salvo);
    }
}
