package br.com.fiap.autorepairshop.workorder.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SqlWorkOrderQueriesTest {

    private static final Instant MOMENT = Instant.parse("2026-10-28T12:00:00Z");

    @Test
    void convertsDriverDateTypesToInstant() {
        assertThat(SqlWorkOrderQueries.instant(MOMENT)).isEqualTo(MOMENT);
        assertThat(SqlWorkOrderQueries.instant(OffsetDateTime.ofInstant(MOMENT, ZoneOffset.ofHours(-3))))
                .isEqualTo(MOMENT);
        assertThat(SqlWorkOrderQueries.instant(Timestamp.from(MOMENT))).isEqualTo(MOMENT);
    }

    @Test
    void rejectsUnexpectedType() {
        assertThatThrownBy(() -> SqlWorkOrderQueries.instant("2026-10-28")).isInstanceOf(IllegalStateException.class);
    }
}
