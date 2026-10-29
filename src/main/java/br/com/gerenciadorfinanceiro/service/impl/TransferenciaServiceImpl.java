package br.com.gerenciadorfinanceiro.service.impl;

import br.com.gerenciadorfinanceiro.controller.dto.TransferenciaRequestDto;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import br.com.gerenciadorfinanceiro.model.vo.ValorMonetario;
import br.com.gerenciadorfinanceiro.repository.CategoriaRepository;
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
import br.com.gerenciadorfinanceiro.service.ContaService;
import br.com.gerenciadorfinanceiro.service.TransferenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferenciaServiceImpl implements TransferenciaService {

    private static final String CATEGORIA_TRANSFERENCIA = "Transferência entre Contas";

    private final ContaService contaService;
    private final TransacaoRepository transacaoRepository;
    private final CategoriaRepository categoriaRepository;

    @Override
    @Transactional
    public void transferir(Long usuarioId, TransferenciaRequestDto request) {
        transferir(usuarioId, request.contaOrigemId(), request.contaDestinoId(), request.valor());
    }

    @Override
    @Transactional
    public void transferir(Long usuarioId, Long contaOrigemId, Long contaDestinoId, BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da transferência deve ser maior que zero.");
        }

        if (contaOrigemId == null || contaDestinoId == null) {
            throw new IllegalArgumentException("Contas de origem e destino são obrigatórias.");
        }

        if (contaOrigemId.equals(contaDestinoId)) {
            throw new IllegalArgumentException("As contas de origem e destino devem ser diferentes.");
        }

        BigDecimal quantia = ValorMonetario.ofPositivo(valor).quantia();

        Conta origem = contaService.buscarEntidadePorId(contaOrigemId, usuarioId);
        Conta destino = contaService.buscarEntidadePorId(contaDestinoId, usuarioId);

        // Operações atômicas no banco com conferência de saldo
        contaService.debitarSaldo(origem.getId(), quantia);
        contaService.creditarSaldo(destino.getId(), quantia);
        try { origem.debitar(quantia); } catch (Exception ignored) {}
        try { destino.creditar(quantia); } catch (Exception ignored) {}

        Categoria catSaida = resolverCategoriaTransferencia(origem.getUsuario(), TipoTransacao.DESPESA);
        Categoria catEntrada = resolverCategoriaTransferencia(destino.getUsuario(), TipoTransacao.RECEITA);

        Transacao lancamentoSaida = Transacao.criarSaidaTransferencia(origem, destino, quantia, catSaida);
        Transacao lancamentoEntrada = Transacao.criarEntradaTransferencia(origem, destino, quantia, catEntrada);

        transacaoRepository.save(lancamentoSaida);
        transacaoRepository.save(lancamentoEntrada);
    }

    private Categoria resolverCategoriaTransferencia(Usuario usuario, TipoTransacao tipo) {
        if (usuario == null || usuario.getId() == null) {
            return null;
        }
        List<Categoria> categorias = categoriaRepository.findByUsuarioId(usuario.getId());
        return categorias.stream()
                .filter(c -> c.getTipo() == tipo && CATEGORIA_TRANSFERENCIA.equalsIgnoreCase(c.getNome()))
                .findFirst()
                .orElseGet(() -> categoriaRepository.save(new Categoria(
                        usuario,
                        CATEGORIA_TRANSFERENCIA,
                        tipo,
                        "Movimentação patrimonial interna entre contas do mesmo titular.",
                        "fa-right-left"
                )));
    }
}
