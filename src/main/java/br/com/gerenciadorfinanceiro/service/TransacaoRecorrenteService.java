package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.controller.dto.RecorrenciaRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.RecorrenciaResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.ResumoRecorrenciasDto;
import br.com.gerenciadorfinanceiro.controller.dto.TransacaoResponseDto;
import br.com.gerenciadorfinanceiro.model.TransacaoRecorrente;
import br.com.gerenciadorfinanceiro.model.Usuario;

import java.time.LocalDate;
import java.util.List;

public interface TransacaoRecorrenteService {

    List<RecorrenciaResponseDto> listar(Long usuarioId);

    List<RecorrenciaResponseDto> listarAtivas(Long usuarioId);

    RecorrenciaResponseDto buscarPorId(Long id, Long usuarioId);

    TransacaoRecorrente buscarEntidadePorId(Long id, Long usuarioId);

    RecorrenciaResponseDto salvar(RecorrenciaRequestDto request, Usuario usuario);

    RecorrenciaResponseDto atualizar(Long id, RecorrenciaRequestDto request, Long usuarioId);

    RecorrenciaResponseDto alternarStatus(Long id, Long usuarioId);

    void excluir(Long id, Long usuarioId);

    TransacaoResponseDto lancarInstancia(Long id, Long usuarioId, LocalDate dataLancamento);

    List<TransacaoResponseDto> processarRecorrenciasUsuario(Long usuarioId, LocalDate dataReferencia);

    ResumoRecorrenciasDto obterResumo(Long usuarioId);

    void processarTodasRecorrenciasAutomaticamente();
}
