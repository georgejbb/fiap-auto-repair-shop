package br.com.fiap.autorepairshop.identity.infrastructure;

import br.com.fiap.autorepairshop.identity.application.AccessToken;
import br.com.fiap.autorepairshop.identity.application.TokenIssuer;
import br.com.fiap.autorepairshop.identity.domain.AdminUser;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.Set;

/**
 * Emite JWT assinado com RS256. O emissor e a chave de assinatura vêm da configuração.
 */
@ApplicationScoped
class JwtTokenIssuer implements TokenIssuer {

    static final Duration LIFESPAN = Duration.ofHours(1);

    @Override
    public AccessToken issue(AdminUser user) {
        String token = Jwt.upn(user.username())
                .groups(Set.of(user.role()))
                .expiresIn(LIFESPAN)
                .sign();
        return new AccessToken(token, LIFESPAN.toSeconds());
    }
}
