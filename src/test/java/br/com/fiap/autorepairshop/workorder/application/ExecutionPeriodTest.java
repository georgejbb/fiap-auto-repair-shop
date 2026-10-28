package br.com.fiap.autorepairshop.workorder.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ExecutionPeriodTest {

    @Test
    void parsesIsoInstants() {
        ExecutionPeriod period = ExecutionPeriod.of("2026-10-01T00:00:00Z", " 2026-10-31T23:59:59Z ");

        assertThat(period.from()).isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
        assertThat(period.to()).isEqualTo(Instant.parse("2026-10-31T23:59:59Z"));
    }

    @Test
    void absentLimitsAreOpen() {
        assertThat(ExecutionPeriod.of(null, " ")).isEqualTo(new ExecutionPeriod(null, null));
        assertThat(ExecutionPeriod.of("2026-10-01T00:00:00Z", null).to()).isNull();
    }

    @Test
    void rejectsInvalidFormat() {
        assertThatThrownBy(() -> ExecutionPeriod.of("01/10/2026", null))
                .isInstanceOfSatisfying(InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("from"));
        assertThatThrownBy(() -> ExecutionPeriod.of(null, "ontem"))
                .isInstanceOfSatisfying(InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("to"));
    }

    @Test
    void rejectsInvertedPeriod() {
        assertThatThrownBy(() -> ExecutionPeriod.of("2026-10-31T00:00:00Z", "2026-10-01T00:00:00Z"))
                .isInstanceOf(InvalidValueException.class)
                .hasMessage("deve ser anterior ou igual a to");
    }
}
