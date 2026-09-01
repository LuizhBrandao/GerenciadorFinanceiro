package br.com.gerenciadorfinanceiro.controller;

import br.com.gerenciadorfinanceiro.controller.dto.CategoriaRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.CategoriaResponseDto;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    public ResponseEntity<List<CategoriaResponseDto>> listar(@AuthenticationPrincipal Usuario usuario) {
        List<CategoriaResponseDto> categorias = categoriaService.listar(usuario.getId());
        if (categorias.isEmpty()) {
            categorias = categoriaService.inicializarCategoriasPadrao(usuario);
        }
        return ResponseEntity.ok(categorias);
    }

    @PostMapping("/inicializar-padrao")
    public ResponseEntity<List<CategoriaResponseDto>> inicializarPadrao(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(categoriaService.inicializarCategoriasPadrao(usuario));
    }

    @PostMapping
    public ResponseEntity<CategoriaResponseDto> criar(@RequestBody @Valid CategoriaRequestDto request, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(categoriaService.criar(request, usuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        categoriaService.excluir(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }
}
