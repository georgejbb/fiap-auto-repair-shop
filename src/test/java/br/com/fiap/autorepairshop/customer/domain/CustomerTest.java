package br.com.fiap.autorepairshop.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CustomerTest {

    private static final Instant CREATED = Instant.parse("2026-10-28T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-28T11:00:00Z");
    private static final Document CPF = Document.of("52998224725");

    @Test
    void registersActiveCustomerWithTrimmedData() {
        Customer customer = Customer.register(CPF, " Maria Silva ", " maria@email.com ", "  ", CREATED);

        assertThat(customer.id()).isNotNull();
        assertThat(customer.name()).isEqualTo("Maria Silva");
        assertThat(customer.email()).isEqualTo("maria@email.com");
        assertThat(customer.phone()).isNull();
        assertThat(customer.createdAt()).isEqualTo(CREATED);
        assertThat(customer.updatedAt()).isEqualTo(CREATED);
        assertThat(customer.isActive()).isTrue();
    }

    @Test
    void requiresNameAndEmail() {
        assertThatThrownBy(() -> Customer.register(CPF, " ", "a@b.com", null, CREATED))
                .isInstanceOfSatisfying(InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("name"));
        assertThatThrownBy(() -> Customer.register(CPF, "Maria", null, null, CREATED))
                .isInstanceOfSatisfying(InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("email"));
    }

    @Test
    void updateKeepsDocument() {
        Customer customer = Customer.register(CPF, "Maria", "maria@email.com", null, CREATED);

        customer.update("Maria Souza", "maria.souza@email.com", "11999990000", LATER);

        assertThat(customer.document()).isEqualTo(CPF);
        assertThat(customer.name()).isEqualTo("Maria Souza");
        assertThat(customer.email()).isEqualTo("maria.souza@email.com");
        assertThat(customer.phone()).isEqualTo("11999990000");
        assertThat(customer.updatedAt()).isEqualTo(LATER);
    }

    @Test
    void updateValidatesRequiredFields() {
        Customer customer = Customer.register(CPF, "Maria", "maria@email.com", null, CREATED);

        assertThatThrownBy(() -> customer.update("Maria", "", null, LATER)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void deleteIsLogical() {
        Customer customer = Customer.register(CPF, "Maria", "maria@email.com", null, CREATED);

        customer.delete(LATER);

        assertThat(customer.isActive()).isFalse();
        assertThat(customer.deletedAt()).isEqualTo(LATER);
        assertThat(customer.updatedAt()).isEqualTo(LATER);
    }
}
