package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.config.TokenService;
import br.com.gerenciadorfinanceiro.controller.dto.AuthDto;
import br.com.gerenciadorfinanceiro.controller.dto.LoginResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.RegisterDto;
import br.com.gerenciadorfinanceiro.exception.RegraNegocioException;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.repository.UsuarioRepository;
import br.com.gerenciadorfinanceiro.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CategoriaService categoriaService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Lucas Silva", "lucas@email.com", "senhaCriptografada");
        usuario.setId(1L);
    }

    @Test
    @DisplayName("Deve autenticar com sucesso e retornar token JWT")
    void deveAutenticarComSucesso() {
        AuthDto authDto = new AuthDto("lucas@email.com", "senha123");
        Authentication auth = mock(Authentication.class);

        when(auth.getPrincipal()).thenReturn(usuario);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(tokenService.generateToken(usuario)).thenReturn("mock.jwt.token");

        LoginResponseDto response = authService.autenticar(authDto);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.token());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService).generateToken(usuario);
    }

    @Test
    @DisplayName("Deve registrar novo usuário com preferências e categorias essenciais")
    void deveRegistrarNovoUsuarioComSucesso() {
        RegisterDto registerDto = new RegisterDto("Lucas Silva", "lucas@email.com", "senha123");

        when(usuarioRepository.existsByEmail("lucas@email.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("senhaCriptografada");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        authService.registrar(registerDto);

        verify(usuarioRepository).save(any(Usuario.class));
        verify(categoriaService).inicializarCategoriasPadrao(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException ao tentar registrar com e-mail já existente")
    void deveLancarExcecaoAoRegistrarEmailDuplicado() {
        RegisterDto registerDto = new RegisterDto("Lucas Silva", "lucas@email.com", "senha123");

        when(usuarioRepository.existsByEmail("lucas@email.com")).thenReturn(true);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> authService.registrar(registerDto));
        assertEquals("E-mail já cadastrado no sistema.", ex.getMessage());

        verify(usuarioRepository, never()).save(any());
        verify(categoriaService, never()).inicializarCategoriasPadrao(any());
    }
}
