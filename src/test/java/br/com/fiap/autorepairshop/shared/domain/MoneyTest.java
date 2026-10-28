package br.com.fiap.autorepairshop.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyTest {

    @ParameterizedTest
    @ValueSource(strings = {"0", "120", "45.9", "45.90", "45.900"})
    void acceptsUpToTwoSignificantDecimalPlaces(String value) {
        Money money = Money.of("price", new BigDecimal(value));

        assertThat(money.amount().scale()).isEqualTo(2);
        assertThat(money.amount()).isEqualByComparingTo(value);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> Money.of("price", null))
                .isInstanceOfSatisfying(InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("price"));
    }

    @Test
    void rejectsNegative() {
        assertThatThrownBy(() -> Money.of("price", new BigDecimal("-0.01")))
                .isInstanceOf(InvalidValueException.class)
                .hasMessage("deve ser maior ou igual a zero");
    }

    @Test
    void rejectsMoreThanTwoDecimalPlaces() {
        assertThatThrownBy(() -> Money.of("unitPrice", new BigDecimal("10.555")))
                .isInstanceOfSatisfying(InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("unitPrice"))
                .hasMessage("deve ter no máximo duas casas decimais");
    }

    @Test
    void constructorRejectsInvalidAmountFromInternalCode() {
        assertThatThrownBy(() -> new Money(new BigDecimal("1.001"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Money(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroHasScaleTwo() {
        assertThat(Money.ZERO.amount()).isEqualTo(new BigDecimal("0.00"));
    }
}
