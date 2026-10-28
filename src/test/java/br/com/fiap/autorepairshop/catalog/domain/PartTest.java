package br.com.fiap.autorepairshop.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import br.com.fiap.autorepairshop.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PartTest {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-28T13:00:00Z");
    private static final Money PRICE = Money.of("unitPrice", new BigDecimal("45.90"));

    @Test
    void createsWithZeroStock() {
        Part part = Part.create(" Óleo 5W30 ", PartCategory.SUPPLY, PRICE, NOW);

        assertThat(part.name()).isEqualTo("Óleo 5W30");
        assertThat(part.stockQuantity()).isZero();
        assertThat(part.isActive()).isTrue();
        assertThat(part.version()).isZero();
    }

    @Test
    void requiresNameAndCategory() {
        assertThatThrownBy(() -> Part.create(" ", PartCategory.PART, PRICE, NOW))
                .isInstanceOfSatisfying(InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("name"));
        assertThatThrownBy(() -> Part.create("Filtro", null, PRICE, NOW))
                .isInstanceOfSatisfying(InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("category"));
    }

    @Test
    void inboundAddsToStockAndRecordsMovement() {
        Part part = Part.create("Filtro de óleo", PartCategory.PART, PRICE, NOW);
        part.registerInbound(5, NOW);

        StockMovement movement = part.registerInbound(10, LATER);

        assertThat(part.stockQuantity()).isEqualTo(15);
        assertThat(part.updatedAt()).isEqualTo(LATER);
        assertThat(movement.type()).isEqualTo(StockMovementType.INBOUND);
        assertThat(movement.quantity()).isEqualTo(10);
        assertThat(movement.partId()).isEqualTo(part.id());
        assertThat(movement.occurredAt()).isEqualTo(LATER);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void inboundRequiresPositiveQuantity(int quantity) {
        Part part = Part.create("Filtro", PartCategory.PART, PRICE, NOW);

        assertThatThrownBy(() -> part.registerInbound(quantity, NOW))
                .isInstanceOfSatisfying(InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("quantity"));
        assertThat(part.stockQuantity()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"10, 7, -3", "10, 12, 2", "10, 0, -10", "10, 10, 0"})
    void adjustmentSetsCountedQuantityAndRecordsDelta(int initial, int counted, int expectedDelta) {
        Part part = Part.create("Filtro", PartCategory.PART, PRICE, NOW);
        part.registerInbound(initial, NOW);

        StockMovement movement = part.adjustTo(counted, LATER);

        assertThat(part.stockQuantity()).isEqualTo(counted);
        assertThat(movement.type()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(movement.quantity()).isEqualTo(expectedDelta);
    }

    @Test
    void adjustmentRejectsNegativeCount() {
        Part part = Part.create("Filtro", PartCategory.PART, PRICE, NOW);

        assertThatThrownBy(() -> part.adjustTo(-1, NOW))
                .isInstanceOfSatisfying(InvalidValueException.class,
                        e -> assertThat(e.field()).isEqualTo("countedQuantity"));
    }

    @Test
    void updateChangesRegistrationDataButNotStock() {
        Part part = Part.create("Filtro", PartCategory.PART, PRICE, NOW);
        part.registerInbound(10, NOW);

        part.update("Filtro de ar", PartCategory.SUPPLY, Money.of("unitPrice", BigDecimal.TEN), LATER);

        assertThat(part.name()).isEqualTo("Filtro de ar");
        assertThat(part.category()).isEqualTo(PartCategory.SUPPLY);
        assertThat(part.unitPrice().amount()).isEqualByComparingTo("10");
        assertThat(part.stockQuantity()).isEqualTo(10);
    }

    @Test
    void updateValidatesName() {
        Part part = Part.create("Filtro", PartCategory.PART, PRICE, NOW);

        assertThatThrownBy(() -> part.update(null, PartCategory.PART, PRICE, LATER))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void deleteIsLogical() {
        Part part = Part.create("Filtro", PartCategory.PART, PRICE, NOW);

        part.delete(LATER);

        assertThat(part.isActive()).isFalse();
        assertThat(part.deletedAt()).isEqualTo(LATER);
    }

    @Test
    void cannotRebuildWithNegativeStock() {
        assertThatThrownBy(() -> new Part(UUID.randomUUID(), "Filtro", PartCategory.PART, PRICE, -1, 0, NOW, NOW,
                null)).isInstanceOf(IllegalArgumentException.class);
    }
}
