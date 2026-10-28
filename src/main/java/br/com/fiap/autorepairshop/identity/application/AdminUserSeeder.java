package br.com.fiap.autorepairshop.identity.application;

import br.com.fiap.autorepairshop.identity.domain.AdminUser;
import br.com.fiap.autorepairshop.identity.domain.AdminUserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Garante, na inicialização, a existência do usuário administrativo configurado por ambiente.
 * Um usuário já existente não é alterado.
 */
@ApplicationScoped
public class AdminUserSeeder {

    private static final Logger LOG = Logger.getLogger(AdminUserSeeder.class);

    private final AdminUserRepository users;
    private final Clock clock;
    private final String username;
    private final String password;

    public AdminUserSeeder(AdminUserRepository users, Clock clock,
            @ConfigProperty(name = "app.admin.username") String username,
            @ConfigProperty(name = "app.admin.password") String password) {
        this.users = users;
        this.clock = clock;
        this.username = username;
        this.password = password;
    }

    @Transactional
    void onStart(@Observes StartupEvent event) {
        ensureAdminUser();
    }

    void ensureAdminUser() {
        if (users.findByUsername(username).isPresent()) {
            return;
        }
        String hash = BcryptUtil.bcryptHash(password, PasswordPolicy.BCRYPT_COST);
        users.add(AdminUser.create(username, hash, Instant.now(clock)));
        LOG.infof("Usuário administrativo '%s' criado.", username);
    }
}
