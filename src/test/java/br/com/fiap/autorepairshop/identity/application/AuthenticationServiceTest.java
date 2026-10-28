package br.com.fiap.autorepairshop.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.autorepairshop.identity.domain.AdminUser;
import br.com.fiap.autorepairshop.identity.domain.AdminUserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.AuthenticationFailedException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthenticationServiceTest {

    private final AdminUserRepository users = mock(AdminUserRepository.class);
    private final TokenIssuer tokens = mock(TokenIssuer.class);
    private final AuthenticationService service = new AuthenticationService(users, tokens);

    private final AdminUser admin = AdminUser.create("admin", BcryptUtil.bcryptHash("admin123", 4), Instant.EPOCH);

    @Test
    void validCredentialsIssueToken() {
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(tokens.issue(admin)).thenReturn(new AccessToken("jwt", 3600));

        assertThat(service.login("admin", "admin123")).isEqualTo(new AccessToken("jwt", 3600));
    }

    @Test
    void wrongPasswordFailsWithoutIssuingToken() {
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.login("admin", "errada"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Login ou senha inválidos.");
        verify(tokens, never()).issue(any());
    }

    @Test
    void unknownUserFailsWithSameMessage() {
        when(users.findByUsername("ninguem")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("ninguem", "qualquer"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Login ou senha inválidos.");
        verify(tokens, never()).issue(any());
    }
}
