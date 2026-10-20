package br.com.gerenciadorfinanceiro.controller;

import br.com.gerenciadorfinanceiro.controller.dto.AuthDto;
import br.com.gerenciadorfinanceiro.controller.dto.LoginResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.RegisterDto;
import br.com.gerenciadorfinanceiro.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody @Valid AuthDto data) {
        return ResponseEntity.ok(this.authService.autenticar(data));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterDto data) {
        this.authService.registrar(data);
        return ResponseEntity.ok().build();
    }
}
