package br.com.gerenciadorfinanceiro.controller.mapper;

import br.com.gerenciadorfinanceiro.controller.dto.CategoriaResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.ContaResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.RecorrenciaResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.TransacaoResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.TransacaoResumoDto;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Tag;
import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.TransacaoRecorrente;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static ContaResponseDto toContaResponse(Conta c) {
        if (c == null) return null;
        return new ContaResponseDto(
                c.getId(),
                c.getNome(),
                c.getInstituicaoFinanceira(),
                c.getTipoConta(),
                c.getSaldo(),
                c.getAtivo(),
                c.getDataCriacao()
        );
    }

    public static CategoriaResponseDto toCategoriaResponse(Categoria c) {
        if (c == null) return null;
        return new CategoriaResponseDto(
                c.getId(),
                c.getNome(),
                c.getTipo(),
                c.getDescricao(),
                c.getIcone()
        );
    }

    public static TransacaoResponseDto toTransacaoResponse(Transacao t) {
        if (t == null) return null;
        Set<String> tagNames = t.getTags() != null
                ? t.getTags().stream().map(Tag::getNome).collect(Collectors.toSet())
                : Collections.emptySet();

        return new TransacaoResponseDto(
                t.getId(),
                t.getDescricao(),
                t.getValor(),
                t.getTipo(),
                t.getStatus(),
                t.getDataTransacao(),
                toContaResponse(t.getConta()),
                toCategoriaResponse(t.getCategoria()),
                t.getObservacao(),
                tagNames
        );
    }

    public static TransacaoResumoDto toTransacaoResumo(Transacao t) {
        if (t == null) return null;
        return new TransacaoResumoDto(
                t.getId(),
                t.getDescricao(),
                t.getValor(),
                t.getTipo(),
                t.getStatus(),
                t.getDataTransacao(),
                t.getConta() != null ? t.getConta().getNome() : "-",
                t.getCategoria() != null ? t.getCategoria().getNome() : "Sem Categoria",
                t.getCategoria() != null ? t.getCategoria().getIcone() : "fa-tag",
                t.getObservacao()
        );
    }

    public static RecorrenciaResponseDto toRecorrenciaResponse(TransacaoRecorrente r) {
        if (r == null) return null;
        return new RecorrenciaResponseDto(
                r.getId(),
                r.getDescricao(),
                r.getValor(),
                r.getTipo(),
                r.getFrequencia(),
                r.getDiaVencimento(),
                toContaResponse(r.getConta()),
                toCategoriaResponse(r.getCategoria()),
                r.getDataInicio(),
                r.getDataFim(),
                r.getUltimoLancamento(),
                r.getAtivo(),
                r.getObservacao()
        );
    }
}
