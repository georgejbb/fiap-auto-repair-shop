package br.com.fiap.autorepairshop.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class VehicleTest {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-29T12:00:00Z");
    private static final UUID OWNER = UUID.randomUUID();
    private static final LicensePlate PLATE = LicensePlate.of("BRA2E19");

    @ParameterizedTest
    @CsvSource({"abc-1234, ABC1234", "ABC1234, ABC1234", "bra2e19, BRA2E19", "BRA 2E19, BRA2E19"})
    void normalizesValidPlates(String raw, String expected) {
        assertThat(LicensePlate.of(raw).value()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB1234", "ABCD123", "ABC12345", "1BC1234", "ABC1D2E", "ABCDE12"})
    void rejectsInvalidPlates(String raw) {
        assertThatThrownBy(() -> LicensePlate.of(raw))
                .isInstanceOfSatisfying(InvalidValueException.class,
                        e -> assertThat(e.field()).isEqualTo("licensePlate"))
                .hasMessage("deve seguir o formato AAA9999 ou AAA9A99");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void requiresPlate(String raw) {
        assertThatThrownBy(() -> LicensePlate.of(raw)).hasMessage("é obrigatória");
    }

    @ParameterizedTest
    @ValueSource(ints = {1900, 2026, 2027})
    void acceptsYearsWithinRange(int year) {
        assertThat(Vehicle.register(PLATE, "Fiat", "Argo", year, OWNER, NOW).year()).isEqualTo(year);
    }

    @ParameterizedTest
    @ValueSource(ints = {1899, 2028})
    void rejectsYearsOutsideRange(int year) {
        assertThatThrownBy(() -> Vehicle.register(PLATE, "Fiat", "Argo", year, OWNER, NOW))
                .isInstanceOfSatisfying(InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("year"))
                .hasMessage("deve estar entre 1900 e 2027");
    }

    @Test
    void requiresBrandAndModel() {
        assertThatThrownBy(() -> Vehicle.register(PLATE, " ", "Argo", 2020, OWNER, NOW))
                .isInstanceOfSatisfying(InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("brand"));
        assertThatThrownBy(() -> Vehicle.register(PLATE, "Fiat", null, 2020, OWNER, NOW))
                .isInstanceOfSatisfying(InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("model"));
    }

    @Test
    void updateKeepsPlateAndChangesOwner() {
        Vehicle vehicle = Vehicle.register(PLATE, " Fiat ", " Argo ", 2020, OWNER, NOW);
        UUID newOwner = UUID.randomUUID();

        vehicle.update("Fiat", "Cronos", 2021, newOwner, LATER);

        assertThat(vehicle.licensePlate()).isEqualTo(PLATE);
        assertThat(vehicle.model()).isEqualTo("Cronos");
        assertThat(vehicle.year()).isEqualTo(2021);
        assertThat(vehicle.ownerId()).isEqualTo(newOwner);
        assertThat(vehicle.updatedAt()).isEqualTo(LATER);
        assertThat(vehicle.createdAt()).isEqualTo(NOW);
    }

    @Test
    void updateValidatesYear() {
        Vehicle vehicle = Vehicle.register(PLATE, "Fiat", "Argo", 2020, OWNER, NOW);

        assertThatThrownBy(() -> vehicle.update("Fiat", "Argo", 1800, OWNER, LATER))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void deleteIsLogical() {
        Vehicle vehicle = Vehicle.register(PLATE, "Fiat", "Argo", 2020, OWNER, NOW);

        vehicle.delete(LATER);

        assertThat(vehicle.isActive()).isFalse();
        assertThat(vehicle.deletedAt()).isEqualTo(LATER);
    }
}
