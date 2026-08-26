package br.com.gerenciadorfinanceiro.controller;

import br.com.gerenciadorfinanceiro.exception.EntidadeNaoEncontradaException;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @GetMapping
    public ResponseEntity<List<Categoria>> listar(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(categoriaRepository.findByUsuarioId(usuario.getId()));
    }

    @PostMapping
    public ResponseEntity<Categoria> criar(@RequestBody Categoria categoria, @AuthenticationPrincipal Usuario usuario) {
        categoria.setUsuario(usuario);
        return ResponseEntity.ok(categoriaRepository.save(categoria));
    }

    @DeleteMapping("/{id}")
    @SuppressWarnings("null")
    public ResponseEntity<Void> deletar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        Categoria categoria = categoriaRepository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Categoria não encontrada."));
        categoriaRepository.delete(categoria);
        return ResponseEntity.noContent().build();
    }
}
