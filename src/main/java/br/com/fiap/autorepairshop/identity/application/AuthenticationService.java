package br.com.fiap.autorepairshop.identity.application;

import br.com.fiap.autorepairshop.identity.domain.AdminUser;
import br.com.fiap.autorepairshop.identity.domain.AdminUserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.AuthenticationFailedException;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/**
 * Autentica usuários administrativos e emite o token de acesso.
 */
@ApplicationScoped
public class AuthenticationService {

    /**
     * Hash usado quando o login não existe, para que a verificação bcrypt aconteça em todos os casos
     * e o tempo de resposta não indique quais logins estão cadastrados.
     */
    private static final String DUMMY_HASH = BcryptUtil.bcryptHash("senha-inexistente", PasswordPolicy.BCRYPT_COST);

    private final AdminUserRepository users;
    private final TokenIssuer tokens;

    public AuthenticationService(AdminUserRepository users, TokenIssuer tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    public AccessToken login(String username, String password) {
        Optional<AdminUser> user = users.findByUsername(username);
        boolean matches = BcryptUtil.matches(password, user.map(AdminUser::passwordHash).orElse(DUMMY_HASH));
        if (user.isEmpty() || !matches) {
            throw new AuthenticationFailedException("Login ou senha inválidos.");
        }
        return tokens.issue(user.get());
    }
}
