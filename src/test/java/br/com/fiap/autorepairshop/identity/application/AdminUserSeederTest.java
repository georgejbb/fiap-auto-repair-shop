package br.com.fiap.autorepairshop.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.autorepairshop.identity.domain.AdminUser;
import br.com.fiap.autorepairshop.identity.domain.AdminUserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdminUserSeederTest {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    private final AdminUserRepository users = mock(AdminUserRepository.class);
    private final AdminUserSeeder seeder =
            new AdminUserSeeder(users, Clock.fixed(NOW, ZoneOffset.UTC), "admin", "admin123");

    @Test
    void createsConfiguredUserWithHashedPassword() {
        when(users.findByUsername("admin")).thenReturn(Optional.empty());

        seeder.ensureAdminUser();

        ArgumentCaptor<AdminUser> created = ArgumentCaptor.forClass(AdminUser.class);
        verify(users).add(created.capture());
        AdminUser user = created.getValue();
        assertThat(user.username()).isEqualTo("admin");
        assertThat(user.role()).isEqualTo(AdminUser.ADMIN_ROLE);
        assertThat(user.createdAt()).isEqualTo(NOW);
        assertThat(user.passwordHash()).startsWith("$2").isNotEqualTo("admin123");
        assertThat(BcryptUtil.matches("admin123", user.passwordHash())).isTrue();
    }

    @Test
    void keepsExistingUser() {
        when(users.findByUsername("admin")).thenReturn(Optional.of(AdminUser.create("admin", "hash", NOW)));

        seeder.ensureAdminUser();

        verify(users, never()).add(any());
    }
}
