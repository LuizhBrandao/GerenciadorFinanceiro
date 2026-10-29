package br.com.gerenciadorfinanceiro.service.impl;

import br.com.gerenciadorfinanceiro.model.Tag;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.repository.TagRepository;
import br.com.gerenciadorfinanceiro.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final PlatformTransactionManager transactionManager;

    @Override
    public Tag obterOuCriar(Usuario usuario, String nome) {
        if (nome == null || nome.isBlank() || usuario == null || usuario.getId() == null) {
            return null;
        }
        String nomeLimpo = nome.trim();
        Optional<Tag> existente = tagRepository.findByUsuarioIdAndNomeIgnoreCase(usuario.getId(), nomeLimpo);
        if (existente.isPresent()) {
            return existente.get();
        }

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        try {
            return template.execute(status -> tagRepository.saveAndFlush(new Tag(usuario, nomeLimpo)));
        } catch (DataIntegrityViolationException | UnexpectedRollbackException e) {
            return tagRepository.findByUsuarioIdAndNomeIgnoreCase(usuario.getId(), nomeLimpo).orElse(null);
        }
    }
}
