package br.com.fiap.autorepairshop.identity.application;

import br.com.fiap.autorepairshop.identity.domain.AdminUser;

public interface TokenIssuer {

    AccessToken issue(AdminUser user);
}
