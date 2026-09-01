package br.com.gerenciadorfinanceiro.controller;

import br.com.gerenciadorfinanceiro.controller.dto.RecorrenciaRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.RecorrenciaResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.ResumoRecorrenciasDto;
import br.com.gerenciadorfinanceiro.controller.dto.TransacaoResponseDto;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.service.TransacaoRecorrenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/recorrencias")
@RequiredArgsConstructor
public class TransacaoRecorrenteController {

    private final TransacaoRecorrenteService recorrenteService;

    @GetMapping
    public ResponseEntity<List<RecorrenciaResponseDto>> listar(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(recorrenteService.listar(usuario.getId()));
    }

    @GetMapping("/resumo")
    public ResponseEntity<ResumoRecorrenciasDto> obterResumo(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(recorrenteService.obterResumo(usuario.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecorrenciaResponseDto> buscarPorId(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(recorrenteService.buscarPorId(id, usuario.getId()));
    }

    @PostMapping
    public ResponseEntity<RecorrenciaResponseDto> criar(@RequestBody @Valid RecorrenciaRequestDto request, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(recorrenteService.salvar(request, usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecorrenciaResponseDto> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid RecorrenciaRequestDto request,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(recorrenteService.atualizar(id, request, usuario.getId()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RecorrenciaResponseDto> alternarStatus(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(recorrenteService.alternarStatus(id, usuario.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        recorrenteService.excluir(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/lancar")
    public ResponseEntity<TransacaoResponseDto> lancarAgora(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(recorrenteService.lancarInstancia(id, usuario.getId(), data));
    }

    @PostMapping("/processar")
    public ResponseEntity<List<TransacaoResponseDto>> processarPendentes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataReferencia,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(recorrenteService.processarRecorrenciasUsuario(usuario.getId(), dataReferencia));
    }
}
