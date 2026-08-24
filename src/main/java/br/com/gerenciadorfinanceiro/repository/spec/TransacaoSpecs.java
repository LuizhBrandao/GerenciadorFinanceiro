package br.com.gerenciadorfinanceiro.repository.spec;

import br.com.gerenciadorfinanceiro.model.Transacao;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransacaoSpecs {

    public static Specification<Transacao> porUsuario(Long usuarioId) {
        return (root, query, builder) -> builder.equal(root.get("usuario").get("id"), usuarioId);
    }

    public static Specification<Transacao> dataMaiorOuIgual(LocalDate dataInicial) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("dataTransacao"), dataInicial);
    }

    public static Specification<Transacao> dataMenorOuIgual(LocalDate dataFinal) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("dataTransacao"), dataFinal);
    }

    public static Specification<Transacao> porCategoria(Long categoriaId) {
        return (root, query, builder) -> builder.equal(root.get("categoria").get("id"), categoriaId);
    }

    public static Specification<Transacao> porConta(Long contaId) {
        return (root, query, builder) -> builder.equal(root.get("conta").get("id"), contaId);
    }

    public static Specification<Transacao> valorMaiorOuIgual(BigDecimal valorMinimo) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("valor"), valorMinimo);
    }

    public static Specification<Transacao> valorMenorOuIgual(BigDecimal valorMaximo) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("valor"), valorMaximo);
    }
}
