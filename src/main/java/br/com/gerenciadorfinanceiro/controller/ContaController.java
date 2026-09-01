package br.com.gerenciadorfinanceiro.controller;

import br.com.gerenciadorfinanceiro.controller.dto.ContaRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.ContaResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.ResumoContasResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.ResumoSaldosDto;
import br.com.gerenciadorfinanceiro.controller.dto.TransacaoResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.TransferenciaRequestDto;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.service.ContaService;
import br.com.gerenciadorfinanceiro.service.TransferenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/contas")
@RequiredArgsConstructor
public class ContaController {

    private final ContaService contaService;
    private final TransferenciaService transferenciaService;

    @GetMapping
    public ResponseEntity<List<ContaResponseDto>> listar(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contaService.listarContas(usuario.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContaResponseDto> buscarPorId(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contaService.buscarPorId(id, usuario.getId()));
    }

    @GetMapping("/saldo-consolidado")
    public ResponseEntity<BigDecimal> saldoConsolidado(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contaService.calcularSaldoConsolidado(usuario.getId()));
    }

    @GetMapping("/saldo-investimentos")
    public ResponseEntity<BigDecimal> saldoInvestimentos(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contaService.calcularSaldoInvestimentos(usuario.getId()));
    }

    @GetMapping("/patrimonio-total")
    public ResponseEntity<BigDecimal> patrimonioTotal(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contaService.calcularPatrimonioTotal(usuario.getId()));
    }

    @GetMapping("/resumo")
    public ResponseEntity<ResumoSaldosDto> obterResumoSaldos(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contaService.obterResumoSaldos(usuario.getId()));
    }

    @GetMapping("/resumo-completo")
    public ResponseEntity<ResumoContasResponseDto> obterResumoCompleto(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contaService.obterResumoContas(usuario.getId()));
    }

    @GetMapping("/{id}/extrato")
    public ResponseEntity<List<TransacaoResponseDto>> extrato(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contaService.obterExtratoDetalhado(id, usuario.getId()));
    }

    @PostMapping
    public ResponseEntity<ContaResponseDto> criar(@RequestBody @Valid ContaRequestDto request, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contaService.criar(request, usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContaResponseDto> atualizar(@PathVariable Long id, @RequestBody @Valid ContaRequestDto request, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contaService.atualizar(id, request, usuario.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        contaService.excluir(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transferir")
    public ResponseEntity<Void> transferir(
            @RequestParam(required = false) Long origem,
            @RequestParam(required = false) Long destino,
            @RequestParam(required = false) BigDecimal valor,
            @RequestBody(required = false) TransferenciaRequestDto body,
            @AuthenticationPrincipal Usuario usuario) {

        if (body != null) {
            transferenciaService.transferir(usuario.getId(), body);
        } else if (origem != null && destino != null && valor != null) {
            transferenciaService.transferir(usuario.getId(), origem, destino, valor);
        } else {
            throw new IllegalArgumentException("Parâmetros de transferência incompletos.");
        }
        return ResponseEntity.ok().build();
    }
}
