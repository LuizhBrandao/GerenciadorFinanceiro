package br.com.gerenciadorfinanceiro.controller;

import br.com.gerenciadorfinanceiro.config.TokenService;
import br.com.gerenciadorfinanceiro.controller.dto.AuthDto;
import br.com.gerenciadorfinanceiro.controller.dto.LoginResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.RegisterDto;
import br.com.gerenciadorfinanceiro.model.PreferenciasUsuario;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private br.com.gerenciadorfinanceiro.repository.CategoriaRepository categoriaRepository;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody @Validated AuthDto data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.senha());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((Usuario) auth.getPrincipal());

        return ResponseEntity.ok(new LoginResponseDto(token));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Validated RegisterDto data) {
        if (this.repository.existsByEmail(data.email())) {
            return ResponseEntity.badRequest().build();
        }

        String encryptedPassword = passwordEncoder.encode(data.senha());
        Usuario newUser = new Usuario(data.nome(), data.email(), encryptedPassword);
        
        PreferenciasUsuario preferencias = new PreferenciasUsuario(newUser);
        newUser.setPreferencias(preferencias);

        newUser = this.repository.save(newUser);

        // Cria as 10 categorias essenciais automaticamente para o novo usuário
        var categorias = br.com.gerenciadorfinanceiro.config.DataInitializer.criarCategoriasPadraoParaUsuario(newUser);
        this.categoriaRepository.saveAll(categorias);

        return ResponseEntity.ok().build();
    }
}
