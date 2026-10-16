package br.com.gerenciadorfinanceiro.controller;

import br.com.gerenciadorfinanceiro.controller.dto.OrcamentoRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.OrcamentoResponseDto;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.service.OrcamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/orcamentos")
@RequiredArgsConstructor
public class OrcamentoController {

    private final OrcamentoService orcamentoService;

    @GetMapping
    public ResponseEntity<List<OrcamentoResponseDto>> listar(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(orcamentoService.listarPorUsuario(usuario.getId()));
    }

    @GetMapping("/competencia")
    public ResponseEntity<List<OrcamentoResponseDto>> listarPorCompetencia(
            @RequestParam Integer ano,
            @RequestParam Integer mes,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(orcamentoService.listarPorCompetencia(usuario.getId(), ano, mes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrcamentoResponseDto> buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(orcamentoService.buscarPorId(id, usuario.getId()));
    }

    @PostMapping
    public ResponseEntity<OrcamentoResponseDto> criar(
            @RequestBody @Valid OrcamentoRequestDto request,
            @AuthenticationPrincipal Usuario usuario) {
        OrcamentoResponseDto criado = orcamentoService.salvar(request, usuario.getId());
        return ResponseEntity.created(URI.create("/orcamentos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrcamentoResponseDto> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid OrcamentoRequestDto request,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(orcamentoService.atualizar(id, request, usuario.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        orcamentoService.excluir(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }
}
