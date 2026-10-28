package br.com.fiap.autorepairshop.identity.infrastructure;

import br.com.fiap.autorepairshop.identity.domain.AdminUser;
import br.com.fiap.autorepairshop.identity.domain.AdminUserRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
class PanacheAdminUserRepository implements AdminUserRepository, PanacheRepositoryBase<AdminUserEntity, UUID> {

    @Override
    public Optional<AdminUser> findByUsername(String username) {
        return find("username", username).firstResultOptional().map(PanacheAdminUserRepository::toDomain);
    }

    @Override
    public void add(AdminUser user) {
        AdminUserEntity entity = new AdminUserEntity();
        entity.id = user.id();
        entity.username = user.username();
        entity.passwordHash = user.passwordHash();
        entity.role = user.role();
        entity.createdAt = user.createdAt();
        persistAndFlush(entity);
    }

    private static AdminUser toDomain(AdminUserEntity entity) {
        return new AdminUser(entity.id, entity.username, entity.passwordHash, entity.role, entity.createdAt);
    }
}
