package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.model.Tag;
import br.com.gerenciadorfinanceiro.model.Usuario;

public interface TagService {

    Tag obterOuCriar(Usuario usuario, String nome);
}
