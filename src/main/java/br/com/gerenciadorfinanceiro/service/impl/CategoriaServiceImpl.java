package br.com.gerenciadorfinanceiro.service.impl;

import br.com.gerenciadorfinanceiro.controller.dto.CategoriaRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.CategoriaResponseDto;
import br.com.gerenciadorfinanceiro.controller.mapper.DtoMapper;
import br.com.gerenciadorfinanceiro.exception.EntidadeNaoEncontradaException;
import br.com.gerenciadorfinanceiro.exception.RegraNegocioException;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.repository.CategoriaRepository;
import br.com.gerenciadorfinanceiro.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponseDto> listar(Long usuarioId) {
        List<Categoria> categorias = categoriaRepository.findByUsuarioId(usuarioId);
        return categorias.stream()
                .map(DtoMapper::toCategoriaResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponseDto buscarPorId(Long id, Long usuarioId) {
        return DtoMapper.toCategoriaResponse(buscarEntidadePorId(id, usuarioId));
    }

    @Override
    @Transactional(readOnly = true)
    public Categoria buscarEntidadePorId(Long id, Long usuarioId) {
        return categoriaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Categoria não encontrada ou não pertence ao usuário."));
    }

    @Override
    @Transactional
    public CategoriaResponseDto criar(CategoriaRequestDto request, Usuario usuario) {
        String nomeLimpo = request.nome() != null ? request.nome().trim() : "";
        if (categoriaRepository.existsByUsuarioIdAndNomeIgnoreCaseAndTipo(usuario.getId(), nomeLimpo, request.tipo())) {
            throw new RegraNegocioException("Já existe uma categoria cadastrada com o nome '" + nomeLimpo + "' para o tipo " + request.tipo() + ".");
        }

        String icone = (request.icone() != null && !request.icone().isBlank()) ? request.icone() : "fa-tag";
        Categoria categoria = new Categoria(
                usuario,
                nomeLimpo,
                request.tipo(),
                request.descricao(),
                icone
        );
        try {
            Categoria salva = categoriaRepository.save(categoria);
            return DtoMapper.toCategoriaResponse(salva);
        } catch (DataIntegrityViolationException e) {
            throw new RegraNegocioException("Já existe uma categoria cadastrada com o nome '" + nomeLimpo + "' para o tipo " + request.tipo() + ".");
        }
    }

    @Override
    @Transactional
    public List<CategoriaResponseDto> inicializarCategoriasPadrao(Usuario usuario) {
        List<Categoria> existentes = categoriaRepository.findByUsuarioId(usuario.getId());
        if (existentes.isEmpty()) {
            List<Categoria> padrao = Categoria.criarCatalogoPadrao(usuario);
            try {
                List<Categoria> salvas = categoriaRepository.saveAll(padrao);
                return salvas.stream()
                        .map(DtoMapper::toCategoriaResponse)
                        .collect(Collectors.toList());
            } catch (DataIntegrityViolationException e) {
                // Em caso de concorrência simultânea, a outra thread já gravou o catálogo padrão
                return categoriaRepository.findByUsuarioId(usuario.getId()).stream()
                        .map(DtoMapper::toCategoriaResponse)
                        .collect(Collectors.toList());
            }
        }
        return existentes.stream()
                .map(DtoMapper::toCategoriaResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void excluir(Long id, Long usuarioId) {
        Categoria categoria = buscarEntidadePorId(id, usuarioId);
        categoriaRepository.delete(categoria);
    }
}
