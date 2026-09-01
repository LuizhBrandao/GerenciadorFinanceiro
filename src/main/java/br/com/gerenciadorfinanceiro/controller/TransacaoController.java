package br.com.gerenciadorfinanceiro.controller;

import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.service.TransacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transacoes")
public class TransacaoController {

    @Autowired
    private TransacaoService transacaoService;

    @GetMapping
    public ResponseEntity<List<Transacao>> listar(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(transacaoService.listarTransacoes(usuario.getId()));
    }

    @GetMapping("/busca")
    public ResponseEntity<List<Transacao>> buscarAvancado(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dataInicial,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dataFinal,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long contaId,
            @RequestParam(required = false) java.math.BigDecimal valorMin,
            @RequestParam(required = false) java.math.BigDecimal valorMax,
            @AuthenticationPrincipal Usuario usuario) {
        
        return ResponseEntity.ok(transacaoService.buscarAvancado(
                usuario.getId(), dataInicial, dataFinal, categoriaId, contaId, valorMin, valorMax));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transacao> buscarPorId(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(transacaoService.buscarPorId(id, usuario.getId()));
    }

    @PostMapping
    public ResponseEntity<Transacao> criar(@RequestBody Transacao transacao, @AuthenticationPrincipal Usuario usuario) {
        transacao.setUsuario(usuario);
        return ResponseEntity.ok(transacaoService.salvar(transacao));
    }

    @PostMapping("/parcelado")
    public ResponseEntity<List<Transacao>> criarParcelado(
            @RequestBody Transacao transacao,
            @RequestParam int parcelas,
            @AuthenticationPrincipal Usuario usuario) {
        transacao.setUsuario(usuario);
        return ResponseEntity.ok(transacaoService.criarParcelamento(transacao, parcelas));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transacao> atualizar(@PathVariable Long id, @RequestBody Transacao transacao, @AuthenticationPrincipal Usuario usuario) {
        Transacao existente = transacaoService.buscarPorId(id, usuario.getId());
        existente.setDescricao(transacao.getDescricao());
        existente.setValor(transacao.getValor());
        existente.setTipo(transacao.getTipo());
        existente.setStatus(transacao.getStatus());
        existente.setDataTransacao(transacao.getDataTransacao());
        existente.setCategoria(transacao.getCategoria());
        existente.setTags(transacao.getTags());
        existente.setObservacao(transacao.getObservacao());

        return ResponseEntity.ok(transacaoService.salvar(existente));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        transacaoService.excluir(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }
}
