package br.com.gerenciadorfinanceiro.controller;

import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.TransacaoRecorrente;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.service.TransacaoRecorrenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/recorrencias")
public class TransacaoRecorrenteController {

    @Autowired
    private TransacaoRecorrenteService recorrenteService;

    @GetMapping
    public ResponseEntity<List<TransacaoRecorrente>> listar(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(recorrenteService.listar(usuario.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransacaoRecorrente> buscarPorId(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(recorrenteService.buscarPorId(id, usuario.getId()));
    }

    @PostMapping
    public ResponseEntity<TransacaoRecorrente> criar(@RequestBody TransacaoRecorrente recorrencia, @AuthenticationPrincipal Usuario usuario) {
        recorrencia.setUsuario(usuario);
        return ResponseEntity.ok(recorrenteService.salvar(recorrencia));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransacaoRecorrente> atualizar(
            @PathVariable Long id,
            @RequestBody TransacaoRecorrente dados,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(recorrenteService.atualizar(id, dados, usuario.getId()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TransacaoRecorrente> alternarStatus(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(recorrenteService.alternarStatus(id, usuario.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        recorrenteService.excluir(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/lancar")
    public ResponseEntity<Transacao> lancarAgora(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(recorrenteService.lancarInstancia(id, usuario.getId(), data));
    }

    @PostMapping("/processar")
    public ResponseEntity<List<Transacao>> processarPendentes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataReferencia,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(recorrenteService.processarRecorrenciasUsuario(usuario.getId(), dataReferencia));
    }
}
