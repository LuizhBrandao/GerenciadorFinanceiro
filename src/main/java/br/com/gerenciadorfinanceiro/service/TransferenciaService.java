package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.model.Conta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransferenciaService {

    @Autowired
    private ContaService contaService;

    @Transactional
    public void transferir(Long usuarioId, Long contaOrigemId, Long contaDestinoId, BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da transferência deve ser maior que zero.");
        }

        if (contaOrigemId.equals(contaDestinoId)) {
            throw new IllegalArgumentException("As contas de origem e destino devem ser diferentes.");
        }

        Conta origem = contaService.buscarPorId(contaOrigemId, usuarioId);
        Conta destino = contaService.buscarPorId(contaDestinoId, usuarioId);

        origem.debitar(valor);
        destino.creditar(valor);

        contaService.salvar(origem);
        contaService.salvar(destino);
    }
}
