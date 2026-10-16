package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.controller.dto.CategoriaRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.CategoriaResponseDto;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import br.com.gerenciadorfinanceiro.repository.CategoriaRepository;
import br.com.gerenciadorfinanceiro.service.impl.CategoriaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaServiceImpl categoriaService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Teste", "teste@email.com", "senha123");
        usuario.setId(1L);
    }

    @Test
    @DisplayName("Deve criar categoria mapeando descrição e ícone nas colunas corretas")
    void deveCriarCategoriaComDescricaoEIconeCorretos() {
        CategoriaRequestDto request = new CategoriaRequestDto(
                "Assinaturas Digitais",
                TipoTransacao.DESPESA,
                "Serviços de streaming e nuvem",
                "fa-cloud"
        );

        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> {
            Categoria c = inv.getArgument(0);
            c.setId(50L);
            return c;
        });

        CategoriaResponseDto response = categoriaService.criar(request, usuario);

        assertNotNull(response);
        assertEquals(50L, response.id());
        assertEquals("Assinaturas Digitais", response.nome());
        assertEquals(TipoTransacao.DESPESA, response.tipo());
        assertEquals("Serviços de streaming e nuvem", response.descricao());
        assertEquals("fa-cloud", response.icone());
    }

    @Test
    @DisplayName("Deve aplicar ícone padrão fa-tag quando ícone não for informado")
    void deveAplicarIconePadraoQuandoVazio() {
        CategoriaRequestDto request = new CategoriaRequestDto(
                "Extra",
                TipoTransacao.RECEITA,
                "Renda eventual",
                "   "
        );

        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoriaResponseDto response = categoriaService.criar(request, usuario);

        assertEquals("Renda eventual", response.descricao());
        assertEquals("fa-tag", response.icone());
    }

    @Test
    @DisplayName("Deve inicializar catálogo padrão de categorias quando usuário não possuir nenhuma")
    void deveInicializarCategoriasPadrao() {
        when(categoriaRepository.findByUsuarioId(1L)).thenReturn(Collections.emptyList());
        when(categoriaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CategoriaResponseDto> criadas = categoriaService.inicializarCategoriasPadrao(usuario);

        assertEquals(13, criadas.size());
        verify(categoriaRepository).saveAll(anyList());
    }
}
