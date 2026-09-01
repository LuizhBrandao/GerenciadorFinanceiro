package br.com.gerenciadorfinanceiro.controller;

import br.com.gerenciadorfinanceiro.controller.dto.TransacaoRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.TransacaoResponseDto;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.service.TransacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transacoes")
@RequiredArgsConstructor
public class TransacaoController {

    private final TransacaoService transacaoService;

    @GetMapping
    public ResponseEntity<List<TransacaoResponseDto>> listar(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(transacaoService.listarTransacoes(usuario.getId()));
    }

    @GetMapping("/busca")
    public ResponseEntity<List<TransacaoResponseDto>> buscarAvancado(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long contaId,
            @RequestParam(required = false) BigDecimal valorMin,
            @RequestParam(required = false) BigDecimal valorMax,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(transacaoService.buscarAvancado(
                usuario.getId(), dataInicial, dataFinal, categoriaId, contaId, valorMin, valorMax));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransacaoResponseDto> buscarPorId(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(transacaoService.buscarPorId(id, usuario.getId()));
    }

    @PostMapping
    public ResponseEntity<TransacaoResponseDto> criar(@RequestBody @Valid TransacaoRequestDto request, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(transacaoService.salvar(request, usuario));
    }

    @PostMapping("/parcelado")
    public ResponseEntity<List<TransacaoResponseDto>> criarParcelado(
            @RequestBody @Valid TransacaoRequestDto request,
            @RequestParam int parcelas,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(transacaoService.criarParcelamento(request, parcelas, usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransacaoResponseDto> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid TransacaoRequestDto request,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(transacaoService.atualizar(id, request, usuario.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        transacaoService.excluir(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }
}
