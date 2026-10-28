package br.com.fiap.autorepairshop.identity.application;

import br.com.fiap.autorepairshop.identity.domain.AdminUser;

/**
 * Emite o token de acesso de um usuário autenticado.
 */
public interface TokenIssuer {

    AccessToken issue(AdminUser user);
}
