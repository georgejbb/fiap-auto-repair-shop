package br.com.fiap.autorepairshop.workorder.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.autorepairshop.shared.domain.Money;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PartLineTest {

    private static final Money PRICE = Money.of("unitPrice", new BigDecimal("35.50"));

    @Test
    void totalIsUnitPriceTimesQuantity() {
        PartLine line = new PartLine(UUID.randomUUID(), UUID.randomUUID(), "Filtro", PRICE, 3);

        assertThat(line.total()).isEqualTo(Money.of("total", new BigDecimal("106.50")));
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> new PartLine(UUID.randomUUID(), UUID.randomUUID(), "Filtro", PRICE, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
