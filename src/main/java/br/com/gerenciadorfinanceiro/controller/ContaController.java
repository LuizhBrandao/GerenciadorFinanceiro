package br.com.gerenciadorfinanceiro.controller;

import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.service.ContaService;
import br.com.gerenciadorfinanceiro.service.TransferenciaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/contas")
public class ContaController {

    @Autowired
    private ContaService contaService;

    @Autowired
    private TransferenciaService transferenciaService;

    @GetMapping
    public ResponseEntity<List<Conta>> listar(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contaService.listarContas(usuario.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Conta> buscarPorId(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contaService.buscarPorId(id, usuario.getId()));
    }

    @GetMapping("/saldo-consolidado")
    public ResponseEntity<BigDecimal> saldoConsolidado(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contaService.calcularSaldoConsolidado(usuario.getId()));
    }

    @GetMapping("/{id}/extrato")
    public ResponseEntity<List<Transacao>> extrato(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contaService.obterExtratoDetalhado(id, usuario.getId()));
    }

    @PostMapping
    public ResponseEntity<Conta> criar(@RequestBody Conta conta, @AuthenticationPrincipal Usuario usuario) {
        conta.setUsuario(usuario);
        return ResponseEntity.ok(contaService.salvar(conta));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Conta> atualizar(@PathVariable Long id, @RequestBody Conta conta, @AuthenticationPrincipal Usuario usuario) {
        Conta existente = contaService.buscarPorId(id, usuario.getId());
        existente.setNome(conta.getNome());
        existente.setInstituicaoFinanceira(conta.getInstituicaoFinanceira());
        existente.setTipoConta(conta.getTipoConta());
        return ResponseEntity.ok(contaService.salvar(existente));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        contaService.excluir(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transferir")
    public ResponseEntity<Void> transferir(
            @RequestParam Long origem,
            @RequestParam Long destino,
            @RequestParam BigDecimal valor,
            @AuthenticationPrincipal Usuario usuario) {
        transferenciaService.transferir(usuario.getId(), origem, destino, valor);
        return ResponseEntity.ok().build();
    }
}
