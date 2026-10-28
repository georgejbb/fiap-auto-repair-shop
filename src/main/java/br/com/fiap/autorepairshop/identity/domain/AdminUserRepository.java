package br.com.fiap.autorepairshop.identity.domain;

import java.util.Optional;

public interface AdminUserRepository {

    Optional<AdminUser> findByUsername(String username);

    void add(AdminUser user);
}
