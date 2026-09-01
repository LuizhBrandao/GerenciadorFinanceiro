package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.controller.dto.CategoriaRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.CategoriaResponseDto;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Usuario;

import java.util.List;

public interface CategoriaService {

    List<CategoriaResponseDto> listar(Long usuarioId);

    CategoriaResponseDto buscarPorId(Long id, Long usuarioId);

    Categoria buscarEntidadePorId(Long id, Long usuarioId);

    CategoriaResponseDto criar(CategoriaRequestDto request, Usuario usuario);

    List<CategoriaResponseDto> inicializarCategoriasPadrao(Usuario usuario);

    void excluir(Long id, Long usuarioId);
}
